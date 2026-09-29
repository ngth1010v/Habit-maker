package app.habitmaker.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateFormat {
    private val full = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val dayMonth = DateTimeFormatter.ofPattern("dd/MM")
    private val monthYear = DateTimeFormatter.ofPattern("MM/yyyy")

    fun full(day: Long): String = LocalDate.ofEpochDay(day).format(full)
    fun dayMonth(day: Long): String = LocalDate.ofEpochDay(day).format(dayMonth)
    fun monthYear(day: Long): String = LocalDate.ofEpochDay(day).format(monthYear)

    /** "12/12" for a yearly exception stored as `month * 100 + day`. */
    fun monthDay(value: Int): String = "%02d/%02d".format(value % 100, value / 100)

    fun dayOfWeek(day: Long, locale: Locale = Locale.getDefault()): String =
        LocalDate.ofEpochDay(day).dayOfWeek.getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { it.titlecase(locale) }
}
