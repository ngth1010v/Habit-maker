package app.habitmaker.domain

import java.time.LocalDate

fun Exceptions.excludes(date: LocalDate): Boolean =
    (daysOfWeek shr (date.dayOfWeek.value - 1)) and 1 == 1 ||
        (daysOfMonth shr (date.dayOfMonth - 1)) and 1 == 1 ||
        (date.monthValue * 100 + date.dayOfMonth) in dates

fun Habit.inRange(day: Long): Boolean = day >= startDay && (endDay == null || day <= endDay)

/** Whether the habit has to be done on [day]: inside its dates and not an exception. */
fun Habit.isRequiredOn(day: Long): Boolean = inRange(day) && !exceptions.excludes(LocalDate.ofEpochDay(day))

fun Habit.phase(today: Long): HabitPhase = when {
    today < startDay -> HabitPhase.PLANNED
    endDay != null && today > endDay -> HabitPhase.DONE
    else -> HabitPhase.IN_PROCESS
}
