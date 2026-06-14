package app.alkahf.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoopSequencerTest {

    @Test
    fun single_repeatsTheOneAyah() {
        val steps = LoopSequencer.steps(LoopMode.SINGLE, start = 5, end = 5, perAyah = 3, perChain = 4)

        assertEquals(3, steps.size)
        assertTrue(steps.all { it.ayah == 5 && it.spanStart == 5 && it.spanEnd == 5 && it.nextToAdd == null })
        assertEquals(listOf(1, 2, 3), steps.map { it.pass })
        assertTrue(steps.all { it.passCount == 3 })
    }

    @Test
    fun range_runsTheWholeRangeOncePerPass() {
        val steps = LoopSequencer.steps(LoopMode.RANGE, start = 2, end = 4, perAyah = 9, perChain = 2)

        // perChain passes over (2..4) = 2 * 3
        assertEquals(6, steps.size)
        assertEquals(listOf(2, 3, 4, 2, 3, 4), steps.map { it.ayah })
        assertEquals(listOf(1, 1, 1, 2, 2, 2), steps.map { it.pass })
        assertTrue(steps.all { it.spanStart == 2 && it.spanEnd == 4 })
    }

    @Test
    fun chain_solosFirstAyahThenGrowsTheChain() {
        val steps = LoopSequencer.steps(LoopMode.CHAIN, start = 1, end = 3, perAyah = 2, perChain = 2)

        // ayah1: 2 solo; ayah2: 2 solo + 2*(1,2); ayah3: 2 solo + 2*(1,2,3)
        assertEquals(2 + (2 + 4) + (2 + 6), steps.size)
        assertEquals(LoopStep(1, 1, 1, 1, 2, null), steps.first())

        // The first chain block (after ayah 2 is introduced) spans 1..2 and
        // points at ayah 3 as the next to join.
        val firstChain = steps.first { it.spanStart == 1 && it.spanEnd == 2 }
        assertEquals(3, firstChain.nextToAdd)

        // The final chain covering the whole 1..3 has nothing left to add.
        assertEquals(3, steps.last().spanEnd)
        assertEquals(null, steps.last().nextToAdd)
    }

    @Test
    fun chain_singleAyahRangeHasNoChainBlock() {
        val steps = LoopSequencer.steps(LoopMode.CHAIN, start = 7, end = 7, perAyah = 3, perChain = 5)

        assertEquals(3, steps.size)
        assertTrue(steps.all { it.ayah == 7 && it.nextToAdd == null })
    }
}
