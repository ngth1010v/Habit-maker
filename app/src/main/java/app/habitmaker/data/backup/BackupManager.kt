package app.habitmaker.data.backup

import android.database.Cursor
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import app.habitmaker.data.db.HabitDatabase
import app.habitmaker.domain.BackupFormatException
import app.habitmaker.domain.BackupTable
import app.habitmaker.domain.SqlBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The backup was written by a newer version of the app than this one. */
class BackupTooNewException : Exception()

/**
 * Exports every table as one SQL script and restores from one (see [SqlBackup]). Columns are read
 * from the database itself, so a new column is backed up without touching this class; a new table
 * must be added to [Tables].
 */
class BackupManager(private val database: HabitDatabase) {

    suspend fun export(): String = withContext(Dispatchers.IO) {
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            SqlBackup.write(
                db.version,
                Tables.map { table ->
                    val columns = columnsOf(db, table)
                    val names = columns.joinToString(", ") { "\"$it\"" }
                    BackupTable(table, columns, db.query("SELECT $names FROM \"$table\" ORDER BY rowid").use(::rows))
                },
            )
        }
    }

    /**
     * Replaces everything in the database with the rows of [sql]; all or nothing. Returns the number
     * of rows restored. Throws [BackupFormatException] for a file that is not a backup (or holds
     * rows the database refuses) and [BackupTooNewException] for one from a newer app version.
     */
    suspend fun import(sql: String): Int = withContext(Dispatchers.IO) {
        val backup = SqlBackup.parse(sql)
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            if (backup.schema > db.version) throw BackupTooNewException()
            val columns = Tables.associateWith { columnsOf(db, it).toSet() }
            for (insert in backup.inserts) {
                val known = columns[insert.table] ?: throw BackupFormatException("unknown table")
                if (!known.containsAll(insert.columns)) throw BackupFormatException("unknown column")
            }
            Tables.asReversed().forEach { db.execSQL("DELETE FROM \"$it\"") }
            // In table order whatever the file's order, so foreign keys always find their rows.
            for (table in Tables) {
                for (insert in backup.inserts) {
                    if (insert.table != table) continue
                    val names = insert.columns.joinToString(", ") { "\"$it\"" }
                    val marks = insert.columns.joinToString(", ") { "?" }
                    try {
                        db.execSQL("INSERT INTO \"$table\" ($names) VALUES ($marks)", insert.values.toTypedArray())
                    } catch (e: android.database.SQLException) {
                        throw BackupFormatException("row refused: ${e.message}")
                    }
                }
            }
            backup.inserts.size
        }
    }

    private fun columnsOf(db: SupportSQLiteDatabase, table: String): List<String> =
        db.query("PRAGMA table_info(\"$table\")").use { c ->
            val name = c.getColumnIndexOrThrow("name")
            buildList { while (c.moveToNext()) add(c.getString(name)) }
        }

    private fun rows(c: Cursor): List<List<Any?>> = buildList {
        while (c.moveToNext()) {
            add(
                List(c.columnCount) { i ->
                    when (c.getType(i)) {
                        Cursor.FIELD_TYPE_NULL -> null
                        Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
                        Cursor.FIELD_TYPE_STRING -> c.getString(i)
                        else -> error("unsupported column type in ${c.getColumnName(i)}")
                    }
                },
            )
        }
    }

    private companion object {
        /** Every table, each after the ones its foreign keys point to. */
        val Tables = listOf("reward", "habit", "habit_record", "reward_claim")
    }
}
