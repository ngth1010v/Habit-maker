package app.habitmaker.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Which rewards a habit has earned so far. Periods are calendar weeks (Monday–Sunday) and calendar
 * months; a period cut by the habit's start or end date only counts its required days inside the
 * habit's range. A period is earned once its done days reach `required - tolerance` (at least 1),
 * which can happen before the period ends: with 7 required days and a tolerance of 2, the 5th done
 * day earns the week. The final reward needs the whole range and is granted from the end date on.
 */
object RewardEngine {

    fun earned(habit: Habit, doneDays: Set<Long>, today: Long): List<EarnedReward> {
        if (today < habit.startDay) return emptyList()
        val out = ArrayList<EarnedReward>()
        val lastDay = habit.endDay?.let { minOf(it, today) } ?: today
        habit.weekly?.let { rule ->
            var start = weekStart(habit.startDay)
            while (start <= lastDay) {
                check(habit, rule, PeriodKind.WEEK, start, start + 6, doneDays)?.let(out::add)
                start += 7
            }
        }
        habit.monthly?.let { rule ->
            var month = LocalDate.ofEpochDay(habit.startDay).withDayOfMonth(1)
            while (month.toEpochDay() <= lastDay) {
                val end = month.with(TemporalAdjusters.lastDayOfMonth()).toEpochDay()
                check(habit, rule, PeriodKind.MONTH, month.toEpochDay(), end, doneDays)?.let(out::add)
                month = month.plusMonths(1)
            }
        }
        val end = habit.endDay
        val final = habit.final
        if (final != null && end != null && today >= end) {
            check(habit, final, PeriodKind.FINAL, habit.startDay, end, doneDays)?.let(out::add)
        }
        return out
    }

    /** Required days of [habit] in the period, clipped to the habit's range. */
    fun requiredDays(habit: Habit, from: Long, to: Long): Int {
        val a = maxOf(from, habit.startDay)
        val b = habit.endDay?.let { minOf(to, it) } ?: to
        var n = 0
        for (d in a..b) if (habit.isRequiredOn(d)) n++
        return n
    }

    /**
     * Progress toward each of [habit]'s rewards in the period around [day] (weekly, monthly, final
     * order). [day] is first clamped to the habit's range, so a planned habit shows its first period
     * and a finished one its last. A period with no required days is left out.
     */
    fun progress(habit: Habit, doneDays: Set<Long>, day: Long, doneUpTo: Long = Long.MAX_VALUE): List<RewardProgress> {
        val d = habit.endDay?.let { minOf(maxOf(day, habit.startDay), it) } ?: maxOf(day, habit.startDay)
        val month = LocalDate.ofEpochDay(d).withDayOfMonth(1)
        return listOfNotNull(
            habit.weekly?.let { tally(habit, it, PeriodKind.WEEK, weekStart(d), weekStart(d) + 6, doneDays, doneUpTo) },
            habit.monthly?.let {
                tally(habit, it, PeriodKind.MONTH, month.toEpochDay(), month.with(TemporalAdjusters.lastDayOfMonth()).toEpochDay(), doneDays, doneUpTo)
            },
            habit.final?.let { rule -> habit.endDay?.let { tally(habit, rule, PeriodKind.FINAL, habit.startDay, it, doneDays, doneUpTo) } },
        )
    }

    private fun check(
        habit: Habit,
        rule: RewardRule,
        kind: PeriodKind,
        periodStart: Long,
        periodEnd: Long,
        doneDays: Set<Long>,
    ): EarnedReward? = tally(habit, rule, kind, periodStart, periodEnd, doneDays)
        ?.takeIf { it.done >= it.needed }
        ?.let { EarnedReward(habit.id, rule.rewardId, kind, periodStart, periodEnd) }

    /**
     * Done and needed days of the period, clipped to the habit's range; null when nothing is required.
     * Only days up to [doneUpTo] count as done, so a past day shows the progress it had back then.
     */
    private fun tally(
        habit: Habit,
        rule: RewardRule,
        kind: PeriodKind,
        periodStart: Long,
        periodEnd: Long,
        doneDays: Set<Long>,
        doneUpTo: Long = Long.MAX_VALUE,
    ): RewardProgress? {
        val a = maxOf(periodStart, habit.startDay)
        val b = habit.endDay?.let { minOf(periodEnd, it) } ?: periodEnd
        var required = 0
        var done = 0
        for (d in a..b) {
            if (habit.isRequiredOn(d)) {
                required++
                if (d <= doneUpTo && d in doneDays) done++
            }
        }
        if (required == 0) return null
        return RewardProgress(kind, rule.rewardId, done, needed = maxOf(1, required - rule.tolerance), from = a, to = b)
    }

    /**
     * Each day of [from]..[to] as seen on the [shown] day: after [today] future, else done, or still
     * open today, or missed. A past [shown] day hides what came after it; a future one marks the days
     * from [today] up to it as still open.
     */
    fun dayStates(from: Long, to: Long, doneDays: Set<Long>, today: Long, shown: Long = today): List<DayState> =
        (from..to).map { d ->
            when {
                d > today && d <= shown -> DayState.TODAY_PENDING
                d > minOf(today, shown) -> DayState.FUTURE
                d in doneDays -> DayState.DONE
                d == today -> DayState.TODAY_PENDING
                else -> DayState.MISSED
            }
        }

    fun weekStart(day: Long): Long =
        LocalDate.ofEpochDay(day).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
}
