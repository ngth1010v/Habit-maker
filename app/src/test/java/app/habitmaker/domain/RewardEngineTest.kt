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
        exceptions: Exceptions = Exceptions(),
    ) = Habit(1, "Run", "person-simple-run", 0, "", start, end, weekly, monthly, final, exceptions, 0)

    // 2026-09-28 is a Monday.
    private val mon = day(2026, 9, 28)

    @Test
    fun exceptionsExcludeDays() {
        val h = habit(
            start = mon,
            exceptions = Exceptions(daysOfWeek = 1 shl 6, daysOfMonth = 1 shl 11, dates = listOf(1212)),
        )
        assertFalse(h.isRequiredOn(day(2026, 10, 4))) // Sunday
        assertFalse(h.isRequiredOn(day(2026, 10, 12))) // day 12 of the month
        assertFalse(h.isRequiredOn(day(2027, 12, 12))) // every 12/12
        assertTrue(h.isRequiredOn(day(2026, 10, 5)))
        assertFalse(h.isRequiredOn(mon - 1)) // before start
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
    fun exceptionDaysDoNotCountAsMisses() {
        // Weekends off: 5 required days, tolerance 0 -> all 5 weekdays needed, weekend ignored.
        val h = habit(start = mon, weekly = RewardRule(9, 0), exceptions = Exceptions(daysOfWeek = 0b1100000))
        val weekdays = (0L..4L).map { mon + it }.toSet()
        assertEquals(1, RewardEngine.earned(h, weekdays, mon + 6).size)
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
}
