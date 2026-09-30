package app.habitmaker.domain

/** Whether the habit has to be done on [day]: every day inside its dates. */
fun Habit.isRequiredOn(day: Long): Boolean = day >= startDay && (endDay == null || day <= endDay)

fun Habit.phase(today: Long): HabitPhase = when {
    today < startDay -> HabitPhase.PLANNED
    endDay != null && today > endDay -> HabitPhase.DONE
    else -> HabitPhase.IN_PROCESS
}
