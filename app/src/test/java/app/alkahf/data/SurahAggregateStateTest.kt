package app.alkahf.data

import app.alkahf.data.MemorizationState.LEARNING
import app.alkahf.data.MemorizationState.MEMORIZED
import app.alkahf.data.MemorizationState.NOT_STARTED
import app.alkahf.data.MemorizationState.STRONG
import org.junit.Assert.assertEquals
import org.junit.Test

class SurahAggregateStateTest {
    @Test fun empty_isNotStarted() {
        assertEquals(NOT_STARTED, surahAggregateState(emptyList()))
    }

    @Test fun allStrong_isStrong() {
        assertEquals(STRONG, surahAggregateState(listOf(STRONG, STRONG, STRONG)))
    }

    @Test fun allMemorizedOrStrong_butNotAllStrong_isMemorized() {
        assertEquals(MEMORIZED, surahAggregateState(listOf(STRONG, MEMORIZED, STRONG)))
        assertEquals(MEMORIZED, surahAggregateState(listOf(MEMORIZED, MEMORIZED)))
    }

    @Test fun anyLearning_isLearning() {
        assertEquals(LEARNING, surahAggregateState(listOf(STRONG, LEARNING, MEMORIZED)))
        assertEquals(LEARNING, surahAggregateState(listOf(LEARNING, NOT_STARTED)))
    }

    @Test fun memorizedWithUntouched_noLearning_isNotStarted() {
        // Not all strong/memorized (an untouched āyah), and nothing learning.
        assertEquals(NOT_STARTED, surahAggregateState(listOf(MEMORIZED, NOT_STARTED)))
    }

    @Test fun allNotStarted_isNotStarted() {
        assertEquals(NOT_STARTED, surahAggregateState(listOf(NOT_STARTED, NOT_STARTED)))
    }
}
