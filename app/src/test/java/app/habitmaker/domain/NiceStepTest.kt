package app.habitmaker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NiceStepTest {

    @Test
    fun walksTheLadder() {
        assertEquals(1, niceStep(0f))
        assertEquals(1, niceStep(1f))
        assertEquals(2, niceStep(1.1f))
        assertEquals(5, niceStep(2.5f))
        assertEquals(10, niceStep(5.1f))
        assertEquals(20, niceStep(12f))
        assertEquals(100, niceStep(51f))
    }

    @Test
    fun keepsDayLabelsToSeven() {
        // A week labels every day; a month every fifth.
        assertEquals(1, niceStep(7 / 7f))
        assertEquals(2, niceStep(8 / 7f))
        assertEquals(5, niceStep(31 / 7f))
        assertEquals(20, niceStep(90 / 7f))
    }
}
