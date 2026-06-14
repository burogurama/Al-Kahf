package app.alkahf.data

import app.alkahf.data.user.AyahStateEntity
import app.alkahf.data.user.RevealStateEntity
import app.alkahf.data.user.StumbleEntity
import app.alkahf.data.user.UserDao

/**
 * Per-āyah memorization progress and the Mushaf's self-test bookkeeping: each
 * āyah's [MemorizationState], the words the reader stumbled on, and how far each
 * āyah has been revealed in hide mode.
 */
class MemorizationStore(private val userDao: UserDao) {

    /** The memorization state of each of [ayahIds] that has one recorded. */
    suspend fun statesFor(ayahIds: List<Int>): Map<Int, MemorizationState> =
        userDao.ayahStatesIn(ayahIds).associate { it.ayahId to MemorizationState.of(it.state) }

    /** Every recorded āyah state, by āyah id (raw int value), for roll-ups. */
    suspend fun allStates(): Map<Int, Int> =
        userDao.allAyahStates().associate { it.ayahId to it.state }

    suspend fun setState(ayahId: Int, state: MemorizationState) {
        userDao.upsertAyahStates(listOf(AyahStateEntity(ayahId, state.value)))
    }

    /** Sets every āyah in [ayahIds] to [state] in one write. */
    suspend fun markStates(ayahIds: List<Int>, state: MemorizationState) {
        if (ayahIds.isEmpty()) return
        userDao.upsertAyahStates(ayahIds.map { AyahStateEntity(it, state.value) })
    }

    suspend fun stumblesFor(ayahIds: List<Int>): List<WordStumble> =
        userDao.stumblesForAyahs(ayahIds).map { WordStumble(it.ayahId, it.wordIndex) }

    suspend fun addStumble(stumble: WordStumble) {
        userDao.addStumble(
            StumbleEntity(
                ayahId = stumble.ayahId,
                wordIndex = stumble.wordIndex,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun removeStumble(stumble: WordStumble) {
        userDao.deleteStumble(stumble.ayahId, stumble.wordIndex)
    }

    suspend fun revealStates(ayahIds: List<Int>): Map<Int, Int> =
        userDao.revealStatesForAyahs(ayahIds).associate { it.ayahId to it.revealedCount }

    suspend fun saveRevealState(ayahId: Int, revealedCount: Int) {
        userDao.upsertRevealState(RevealStateEntity(ayahId, revealedCount))
    }

    suspend fun clearRevealStates(ayahIds: List<Int>) {
        userDao.clearRevealStates(ayahIds)
    }
}
