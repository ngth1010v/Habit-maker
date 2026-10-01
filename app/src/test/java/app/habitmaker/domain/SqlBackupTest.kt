package app.habitmaker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SqlBackupTest {

    private val tables = listOf(
        BackupTable(
            "reward",
            listOf("id", "name", "color"),
            listOf(listOf(1L, "Coffee", -16742021L), listOf(2L, "It's; a 'treat'\n-- schema: 99\n(two lines)", 0L)),
        ),
        BackupTable("habit", listOf("id", "name", "end_day", "weekly_reward_id"), listOf(listOf(7L, "", null, 1L))),
        BackupTable("habit_record", listOf("habit_id", "day"), emptyList()),
    )

    @Test
    fun roundTrip() {
        val backup = SqlBackup.parse(SqlBackup.write(3, tables))
        assertEquals(3, backup.schema)
        assertEquals(
            tables.flatMap { t -> t.rows.map { BackupInsert(t.name, t.columns, it) } },
            backup.inserts,
        )
    }

    @Test
    fun writesOneScript() {
        val sql = SqlBackup.write(3, tables)
        assertTrue(sql.startsWith("-- Habit maker backup\n-- schema: 3\nBEGIN TRANSACTION;\n"))
        // Rows go out before the rows they point to.
        assertTrue(sql.indexOf("DELETE FROM \"habit_record\"") < sql.indexOf("DELETE FROM \"reward\""))
        assertTrue(sql.contains("INSERT INTO \"habit\" (\"id\", \"name\", \"end_day\", \"weekly_reward_id\") VALUES (7, '', NULL, 1);\n"))
        assertTrue(sql.endsWith("COMMIT;\n"))
    }

    @Test
    fun rejectsOtherSql() {
        fun bad(body: String) = assertThrows(BackupFormatException::class.java) { SqlBackup.parse("-- schema: 3\n$body") }
        assertThrows(BackupFormatException::class.java) { SqlBackup.parse("INSERT INTO \"reward\" (\"id\") VALUES (1);") }
        bad("DROP TABLE habit;")
        bad("INSERT INTO \"reward\" (\"id\") VALUES (1.5);")
        bad("INSERT INTO \"reward\" (\"id\") VALUES (1, 2);")
        bad("INSERT INTO \"reward\" (\"id\") VALUES ((SELECT 1));")
        bad("INSERT INTO \"reward\" (\"id\") VALUES (1); garbage")
        bad("INSERT INTO \"reward\" (\"name\") VALUES ('open);")
        bad("INSERT INTO \"reward\" (\"id\") SELECT 1;")
    }
}
