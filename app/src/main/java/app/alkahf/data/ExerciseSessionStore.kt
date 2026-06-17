package app.alkahf.data

import app.alkahf.data.exercises.ExerciseType
import app.alkahf.data.user.ExerciseSessionEntity
import app.alkahf.data.user.UserDao

/** A persisted Exercises session (the raw [payload] is decoded by the UI layer). */
data class ExerciseSession(
    val id: Long,
    val createdAt: Long,
    val finishedAt: Long?,
    val types: List<ExerciseType>,
    val total: Int,
    val correct: Int,
    val answered: Int,
    val payload: String,
) {
    val isFinished: Boolean get() = finishedAt != null
}

/**
 * Stores Exercises sessions so they survive navigation and process death, until
 * the user deletes one or it is pruned (one day after it was finished). The
 * session contents travel as an opaque JSON [payload] (encoded by the UI's
 * `ExerciseSessionCodec`); this store only keeps the metadata needed to list and
 * prune them.
 */
class ExerciseSessionStore(private val userDao: UserDao) {

    suspend fun save(payload: String, types: Set<ExerciseType>, total: Int, now: Long): Long =
        userDao.insertExerciseSession(
            ExerciseSessionEntity(
                createdAt = now,
                finishedAt = null,
                typesCsv = types.joinToString(",") { it.name },
                total = total,
                correct = 0,
                answered = 0,
                payload = payload,
            ),
        )

    suspend fun updateAnswers(id: Long, payload: String, correct: Int, answered: Int) =
        userDao.updateExerciseSession(id, payload, correct, answered)

    suspend fun finish(id: Long, payload: String, correct: Int, answered: Int, now: Long) =
        userDao.finishExerciseSession(id, now, payload, correct, answered)

    suspend fun all(): List<ExerciseSession> = userDao.allExerciseSessions().map { it.toModel() }

    suspend fun get(id: Long): ExerciseSession? = userDao.exerciseSession(id)?.toModel()

    suspend fun delete(id: Long) = userDao.deleteExerciseSession(id)

    /** Removes sessions finished more than one day ago. */
    suspend fun prune(now: Long) = userDao.pruneFinishedExerciseSessions(now - DAY_MS)

    private fun ExerciseSessionEntity.toModel() = ExerciseSession(
        id = id,
        createdAt = createdAt,
        finishedAt = finishedAt,
        types = typesCsv.split(",").filter { it.isNotBlank() }.mapNotNull {
            runCatching { ExerciseType.valueOf(it) }.getOrNull()
        },
        total = total,
        correct = correct,
        answered = answered,
        payload = payload,
    )

    companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
