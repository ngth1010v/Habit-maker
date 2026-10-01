package app.habitmaker.domain

/** A table's rows for a backup; values are `Long`, `String` or null. */
data class BackupTable(val name: String, val columns: List<String>, val rows: List<List<Any?>>)

/** One `INSERT` read from a backup file. */
data class BackupInsert(val table: String, val columns: List<String>, val values: List<Any?>)

data class Backup(val schema: Int, val inserts: List<BackupInsert>)

class BackupFormatException(message: String) : Exception(message)

/**
 * The backup file: one SQL script that empties the tables and re-inserts every row, so it can also
 * be run by hand with any SQLite tool. [parse] accepts only what [write] produces (comments,
 * `BEGIN`, `COMMIT`, `DELETE FROM`, and `INSERT ... VALUES` with integers, strings and NULL) and
 * returns the rows as data: an imported file is never executed as SQL.
 */
object SqlBackup {
    private const val SCHEMA = "-- schema: "
    private val SchemaLine = Regex("^-- schema: (\\d+)\\s*$", RegexOption.MULTILINE)

    /** [tables] in insert order (a table after the ones its foreign keys point to). */
    fun write(schema: Int, tables: List<BackupTable>): String = buildString {
        append("-- Habit maker backup\n")
        append(SCHEMA).append(schema).append('\n')
        append("BEGIN TRANSACTION;\n")
        tables.asReversed().forEach { append("DELETE FROM ").append(quoteName(it.name)).append(";\n") }
        for (table in tables) {
            val head = "INSERT INTO ${quoteName(table.name)} (${table.columns.joinToString(", ", transform = ::quoteName)}) VALUES ("
            for (row in table.rows) {
                append(head)
                row.joinTo(this, ", ") { value ->
                    when (value) {
                        null -> "NULL"
                        is Number -> value.toLong().toString()
                        else -> "'" + value.toString().replace("'", "''") + "'"
                    }
                }
                append(");\n")
            }
        }
        append("COMMIT;\n")
    }

    private fun quoteName(name: String) = "\"" + name.replace("\"", "\"\"") + "\""

    fun parse(text: String): Backup {
        val schema = SchemaLine.find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: throw BackupFormatException("no schema line")
        val inserts = ArrayList<BackupInsert>()
        val statement = ArrayList<Token>()
        for (token in tokens(text)) {
            if (token != Token.Symbol(';')) {
                statement.add(token)
                continue
            }
            readStatement(statement)?.let(inserts::add)
            statement.clear()
        }
        if (statement.isNotEmpty()) throw BackupFormatException("unfinished statement")
        return Backup(schema, inserts)
    }

    private sealed interface Token {
        /** A bare word, upper-cased. */
        data class Word(val text: String) : Token
        data class Name(val text: String) : Token
        data class Text(val text: String) : Token
        data class Number(val value: Long) : Token
        data class Symbol(val char: Char) : Token
    }

    private fun tokens(text: String): Sequence<Token> = sequence {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c.isWhitespace() -> i++
                c == '-' && text.startsWith("--", i) -> i = text.indexOf('\n', i).let { if (it < 0) text.length else it }
                c == '\'' || c == '"' -> {
                    // A quote inside is written twice.
                    val out = StringBuilder()
                    i++
                    while (true) {
                        val end = text.indexOf(c, i)
                        if (end < 0) throw BackupFormatException("unclosed quote")
                        out.append(text, i, end)
                        i = end + 1
                        if (i < text.length && text[i] == c) {
                            out.append(c)
                            i++
                        } else {
                            break
                        }
                    }
                    yield(if (c == '\'') Token.Text(out.toString()) else Token.Name(out.toString()))
                }
                c == '-' || c.isDigit() -> {
                    val start = i++
                    while (i < text.length && text[i].isDigit()) i++
                    yield(Token.Number(text.substring(start, i).toLongOrNull() ?: throw BackupFormatException("bad number")))
                }
                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '_')) i++
                    yield(Token.Word(text.substring(start, i).uppercase()))
                }
                c == '(' || c == ')' || c == ',' || c == ';' -> {
                    yield(Token.Symbol(c))
                    i++
                }
                else -> throw BackupFormatException("unexpected '$c'")
            }
        }
    }

    /** The insert of an `INSERT` statement; null for the statements that carry no data. */
    private fun readStatement(tokens: List<Token>): BackupInsert? {
        fun word(index: Int) = (tokens.getOrNull(index) as? Token.Word)?.text
        return when (word(0)) {
            "BEGIN", "COMMIT" -> null
            // Importing always replaces every table, whatever the file deletes.
            "DELETE" -> null
            "INSERT" -> readInsert(tokens)
            else -> throw BackupFormatException("unsupported statement")
        }
    }

    private fun readInsert(tokens: List<Token>): BackupInsert {
        var i = 0
        fun next() = tokens.getOrNull(i++) ?: throw BackupFormatException("unfinished INSERT")
        fun expect(token: Token) {
            if (next() != token) throw BackupFormatException("malformed INSERT")
        }
        fun name() = (next() as? Token.Name)?.text ?: throw BackupFormatException("malformed INSERT")

        /** `( item, item, ... )`. */
        fun <T> list(item: () -> T): List<T> {
            expect(Token.Symbol('('))
            val out = ArrayList<T>()
            while (true) {
                out.add(item())
                val sep = next()
                if (sep == Token.Symbol(')')) return out
                if (sep != Token.Symbol(',')) throw BackupFormatException("malformed INSERT")
            }
        }

        expect(Token.Word("INSERT"))
        expect(Token.Word("INTO"))
        val table = name()
        val columns = list(::name)
        expect(Token.Word("VALUES"))
        val values = list {
            when (val token = next()) {
                is Token.Number -> token.value
                is Token.Text -> token.text
                Token.Word("NULL") -> null
                else -> throw BackupFormatException("unsupported value")
            }
        }
        if (i != tokens.size || values.size != columns.size) throw BackupFormatException("malformed INSERT")
        return BackupInsert(table, columns, values)
    }
}
