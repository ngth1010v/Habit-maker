package app.habitmaker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RewardEngineTest {

    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    private fun habit(
        start: Long,
        end: Long? = null,
        weekly: RewardRule? = null,
        monthly: RewardRule? = null,
        final: RewardRule? = null,
    ) = Habit(1, "Run", "person-simple-run", 0, "", start, end, weekly, monthly, final, 0)

    // 2026-09-28 is a Monday.
    private val mon = day(2026, 9, 28)

    @Test
    fun everyDayInRangeIsRequired() {
        val h = habit(start = mon, end = mon + 6)
        assertTrue(h.isRequiredOn(mon))
        assertTrue(h.isRequiredOn(mon + 6)) // Sunday
        assertFalse(h.isRequiredOn(mon - 1)) // before start
        assertFalse(h.isRequiredOn(mon + 7)) // after end
    }

    @Test
    fun phases() {
        val h = habit(start = mon, end = mon + 6)
        assertEquals(HabitPhase.PLANNED, h.phase(mon - 1))
        assertEquals(HabitPhase.IN_PROCESS, h.phase(mon))
        assertEquals(HabitPhase.IN_PROCESS, h.phase(mon + 6))
        assertEquals(HabitPhase.DONE, h.phase(mon + 7))
    }

    @Test
    fun weeklyToleranceTwoNeedsFiveOfSeven() {
        val h = habit(start = mon, weekly = RewardRule(9, 2))
        val four = (0L..3L).map { mon + it }.toSet()
        assertTrue(RewardEngine.earned(h, four, mon + 6).isEmpty())
        val five = four + (mon + 4)
        // Earned as soon as the 5th day is done, before the week ends.
        val earned = RewardEngine.earned(h, five, mon + 4)
        assertEquals(1, earned.size)
        assertEquals(PeriodKind.WEEK, earned[0].kind)
        assertEquals(mon, earned[0].periodStart)
        assertEquals(9L, earned[0].rewardId)
    }

    @Test
    fun partialFirstWeekCountsOnlyItsDays() {
        // Starts on Saturday: that week has 2 required days.
        val sat = mon + 5
        val h = habit(start = sat, weekly = RewardRule(9, 0))
        val earned = RewardEngine.earned(h, setOf(sat, sat + 1), sat + 1)
        assertEquals(1, earned.size)
        assertEquals(mon, earned[0].periodStart)
    }

    @Test
    fun monthlyAndFinal() {
        val start = day(2026, 10, 1)
        val end = day(2026, 10, 31)
        val h = habit(start = start, end = end, monthly = RewardRule(5, 3), final = RewardRule(6, 3))
        val done = (0L..27L).map { start + it }.toSet() // 28 of 31
        // Final is only granted from the end date on.
        val before = RewardEngine.earned(h, done, end - 1)
        assertEquals(listOf(PeriodKind.MONTH), before.map { it.kind })
        val after = RewardEngine.earned(h, done, end + 10)
        assertEquals(setOf(PeriodKind.MONTH, PeriodKind.FINAL), after.map { it.kind }.toSet())
        // One more miss breaks both.
        assertTrue(RewardEngine.earned(h, done - start, end + 10).isEmpty())
    }

    @Test
    fun toleranceAboveRequiredStillNeedsOneDay() {
        val h = habit(start = mon, weekly = RewardRule(9, 30))
        assertTrue(RewardEngine.earned(h, emptySet(), mon + 6).isEmpty())
        assertEquals(1, RewardEngine.earned(h, setOf(mon), mon + 6).size)
    }

    @Test
    fun plannedHabitEarnsNothing() {
        val h = habit(start = mon + 10, weekly = RewardRule(9, 0))
        assertTrue(RewardEngine.earned(h, emptySet(), mon).isEmpty())
    }

    @Test
    fun progressCountsTheDaysPeriod() {
        // Mon 28/09 – Fri 30/10; Tue, Wed and Thu 01/10 done.
        val h = habit(
            start = mon,
            end = day(2026, 10, 30),
            weekly = RewardRule(7, 1),
            monthly = RewardRule(8, 0),
            final = RewardRule(9, 2),
        )
        val done = setOf(mon + 1, mon + 2, mon + 3)
        val p = RewardEngine.progress(h, done, mon + 3)
        // Week 28/09–04/10: 7 required days, tolerance 1.
        assertEquals(RewardProgress(PeriodKind.WEEK, 7, 3, 6, mon, mon + 6), p[0])
        // October 1–30 = 30 required; only 01/10 is done in it.
        assertEquals(RewardProgress(PeriodKind.MONTH, 8, 1, 30, day(2026, 10, 1), day(2026, 10, 30)), p[1])
        // Whole range: 33 days, tolerance 2.
        assertEquals(RewardProgress(PeriodKind.FINAL, 9, 3, 31, mon, day(2026, 10, 30)), p[2])
        // Before the start, the first period is shown; no rules, no progress.
        assertEquals(p[0], RewardEngine.progress(h, done, mon - 30)[0])
        assertTrue(RewardEngine.progress(habit(start = mon), done, mon).isEmpty())
    }

    @Test
    fun partialWeekBarCoversOnlyTheHabitsDays() {
        // Starts Tuesday: the week's bar runs Tue–Sun, 6 days.
        val h = habit(start = mon + 1, weekly = RewardRule(7, 0))
        val p = RewardEngine.progress(h, emptySet(), mon + 2)[0]
        assertEquals(mon + 1, p.from)
        assertEquals(mon + 6, p.to)
    }

    @Test
    fun dayStatesFromToday() {
        // Wednesday, Monday done, Tuesday missed, Wednesday open.
        val states = RewardEngine.dayStates(mon, mon + 6, setOf(mon), today = mon + 2)
        assertEquals(
            listOf(DayState.DONE, DayState.MISSED, DayState.TODAY_PENDING) + List(4) { DayState.FUTURE },
            states,
        )
        assertEquals(DayState.DONE, RewardEngine.dayStates(mon + 2, mon + 2, setOf(mon + 2), mon + 2)[0])
    }
}
