package app.alkahf.data.review

import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewSchedulerTest {

    @Test
    fun effectiveGrade_stumbledPerfectDropsToHesitant() {
        assertEquals(
            ReviewGrade.HESITANT,
            ReviewScheduler.effectiveGrade(ReviewGrade.PERFECT, stumbleCount = 1, autoLower = true),
        )
    }

    @Test
    fun effectiveGrade_cleanPerfectStaysPerfect() {
        assertEquals(
            ReviewGrade.PERFECT,
            ReviewScheduler.effectiveGrade(ReviewGrade.PERFECT, stumbleCount = 0, autoLower = true),
        )
    }

    @Test
    fun effectiveGrade_ruleOffKeepsPerfect() {
        assertEquals(
            ReviewGrade.PERFECT,
            ReviewScheduler.effectiveGrade(ReviewGrade.PERFECT, stumbleCount = 3, autoLower = false),
        )
    }

    @Test
    fun effectiveGrade_nonPerfectUnchanged() {
        assertEquals(
            ReviewGrade.FORGOT,
            ReviewScheduler.effectiveGrade(ReviewGrade.FORGOT, stumbleCount = 2, autoLower = true),
        )
    }

    @Test
    fun nextInterval_forgotResetsToOne() {
        assertEquals(1, ReviewScheduler.nextIntervalDays(40, ReviewGrade.FORGOT))
    }

    @Test
    fun nextInterval_hesitantIsThree() {
        assertEquals(3, ReviewScheduler.nextIntervalDays(40, ReviewGrade.HESITANT))
    }

    @Test
    fun nextInterval_perfectGrowsButHasAFloorOfFour() {
        assertEquals(4, ReviewScheduler.nextIntervalDays(1, ReviewGrade.PERFECT, growthFactor = 2.5))
        assertEquals(10, ReviewScheduler.nextIntervalDays(4, ReviewGrade.PERFECT, growthFactor = 2.5))
        assertEquals(25, ReviewScheduler.nextIntervalDays(10, ReviewGrade.PERFECT, growthFactor = 2.5))
    }

    @Test
    fun intervalLabel_bucketsByDuration() {
        assertEquals("again tomorrow", ReviewScheduler.intervalLabel(1))
        assertEquals("in 3 days", ReviewScheduler.intervalLabel(3))
        assertEquals("in 1 week", ReviewScheduler.intervalLabel(7))
        assertEquals("in 1 week", ReviewScheduler.intervalLabel(10))
        assertEquals("in 2 weeks", ReviewScheduler.intervalLabel(11))
        assertEquals("in 3 weeks", ReviewScheduler.intervalLabel(21))
    }
}
