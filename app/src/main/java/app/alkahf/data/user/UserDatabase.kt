package app.alkahf.data.user

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update

/** A word the user stumbled on during a self-test. */
@Entity(
    tableName = "stumbles",
    indices = [Index("ayah_id")],
)
data class StumbleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "ayah_id") val ayahId: Int,
    @ColumnInfo(name = "word_index") val wordIndex: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** How many words of an ayah are revealed in the Mushaf self-test (resumable). */
@Entity(tableName = "reveal_states")
data class RevealStateEntity(
    @PrimaryKey @ColumnInfo(name = "ayah_id") val ayahId: Int,
    @ColumnInfo(name = "revealed_count") val revealedCount: Int,
)

/**
 * Memorization state of one ayah: 1 learning, 2 memorized, 3 strong.
 * Ayat with no row are not started.
 */
@Entity(tableName = "ayah_states")
data class AyahStateEntity(
    @PrimaryKey @ColumnInfo(name = "ayah_id") val ayahId: Int,
    val state: Int,
)

/** One unit of practice (a graded review, a loop play) for activity stats. */
@Entity(tableName = "practice_events")
data class PracticeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    @ColumnInfo(name = "ayah_count") val ayahCount: Int,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** A user-created reciter profile that holds imported per-surah audio. */
@Entity(tableName = "custom_reciters")
data class CustomReciterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val initial: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val riwayah: String = "hafs",
)

/** One imported audio file (one surah) belonging to a custom reciter. */
@Entity(tableName = "imported_surahs")
data class ImportedSurahEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "reciter_id") val reciterId: Long,
    val surah: Int,
    val uri: String,
)

/**
 * A Tawqīt timing track: ayah end-times (CSV of ms, 1× scale) aligning one
 * audio source to a portion of the mushaf for the read-along highlight.
 */
@Entity(tableName = "timing_tracks")
data class TimingTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "source_type") val sourceType: String, // "import" | "reciter"
    @ColumnInfo(name = "source_ref") val sourceRef: String, // file URI or reciter path
    @ColumnInfo(name = "source_label") val sourceLabel: String,
    val surah: Int,
    @ColumnInfo(name = "surah_name") val surahName: String,
    @ColumnInfo(name = "ayah_from") val ayahFrom: Int,
    @ColumnInfo(name = "ayah_to") val ayahTo: Int,
    @ColumnInfo(name = "end_times") val endTimesCsv: String,
    @ColumnInfo(name = "global_offset_ms") val globalOffsetMs: Long,
    val complete: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** A saved loop drill routine (one row per preset). */
@Entity(tableName = "loop_presets")
data class LoopPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val surah: Int,
    @ColumnInfo(name = "surah_name") val surahName: String,
    @ColumnInfo(name = "ayah_from") val ayahFrom: Int,
    @ColumnInfo(name = "ayah_to") val ayahTo: Int,
    @ColumnInfo(name = "reciter_path") val reciterPath: String,
    @ColumnInfo(name = "reciter_name") val reciterName: String,
    @ColumnInfo(name = "per_ayah") val perAyah: Int,
    @ColumnInfo(name = "per_chain") val perChain: Int,
    @ColumnInfo(name = "gap_multiplier") val gapMultiplier: Float,
    val speed: Float,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    val riwayah: String = "hafs",
)

/** A memorized portion tracked by the spaced-repetition scheduler. */
@Entity(tableName = "review_portions")
data class ReviewPortionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surah: Int,
    @ColumnInfo(name = "ayah_from") val ayahFrom: Int,
    @ColumnInfo(name = "ayah_to") val ayahTo: Int,
    @ColumnInfo(name = "interval_days") val intervalDays: Int,
    @ColumnInfo(name = "due_epoch_day") val dueEpochDay: Long,
)

/**
 * The single active khatam (full cover-to-cover recitation) being tracked.
 * At most one row carries [active] = true, mirroring the single sabaq drill.
 * Dates are epoch days (LocalDate.toEpochDay), matching the review/practice
 * tables; [reminderTime] is minutes after midnight like the daily reminders.
 */
@Entity(tableName = "khatam")
data class KhatamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Juzʼ per day. */
    val pace: Int = 1,
    @ColumnInfo(name = "start_date") val startDate: Long,
    @ColumnInfo(name = "units_completed") val unitsCompleted: Int = 0,
    @ColumnInfo(name = "last_logged_date") val lastLoggedDate: Long? = null,
    @ColumnInfo(name = "streak_days") val streakDays: Int = 0,
    @ColumnInfo(name = "pages_read") val pagesRead: Int = 0,
    @ColumnInfo(name = "time_read_seconds") val timeReadSeconds: Long = 0,
    @ColumnInfo(name = "reminder_enabled") val reminderEnabled: Boolean = false,
    /** Minutes after midnight, or null when no reminder time is set. */
    @ColumnInfo(name = "reminder_time") val reminderTime: Int? = null,
    val riwayah: String = "hafs",
    val active: Boolean = true,
    /** Last muṣḥaf page the user read in this khatam (0 = never opened). */
    @ColumnInfo(name = "last_read_page") val lastReadPage: Int = 0,
)

/**
 * A persisted Exercises session — the full set of questions and the user's
 * answers, kept so a session can be resumed or reviewed. [finishedAt] is null
 * while in progress; finished sessions are pruned one day after [finishedAt].
 * [payload] is the JSON-encoded session (scope, types, questions, answers).
 */
@Entity(tableName = "exercise_sessions")
data class ExerciseSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long? = null,
    @ColumnInfo(name = "types_csv") val typesCsv: String,
    val total: Int,
    val correct: Int,
    val answered: Int,
    val payload: String,
)

/** A user-saved āyah range with an optional note and a label. */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surah: Int,
    @ColumnInfo(name = "ayah_from") val ayahFrom: Int,
    @ColumnInfo(name = "ayah_to") val ayahTo: Int,
    val note: String = "",
    /** "reflection" | "tafsir" | "memorize" | "dua" | "none" (default). */
    val label: String = "none",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Dao
interface UserDao {
    @Insert
    suspend fun addStumble(stumble: StumbleEntity)

    @Query("SELECT * FROM stumbles WHERE ayah_id IN (:ayahIds)")
    suspend fun stumblesForAyahs(ayahIds: List<Int>): List<StumbleEntity>

    @Query("DELETE FROM stumbles WHERE ayah_id = :ayahId AND word_index = :wordIndex")
    suspend fun deleteStumble(ayahId: Int, wordIndex: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRevealState(state: RevealStateEntity)

    @Query("SELECT * FROM reveal_states WHERE ayah_id IN (:ayahIds)")
    suspend fun revealStatesForAyahs(ayahIds: List<Int>): List<RevealStateEntity>

    @Query("DELETE FROM reveal_states WHERE ayah_id IN (:ayahIds)")
    suspend fun clearRevealStates(ayahIds: List<Int>)

    @Query("SELECT * FROM review_portions WHERE due_epoch_day <= :today ORDER BY id")
    suspend fun duePortions(today: Long): List<ReviewPortionEntity>

    @Query("SELECT * FROM review_portions WHERE surah = :surah AND ayah_from = :from AND ayah_to = :to LIMIT 1")
    suspend fun portionFor(surah: Int, from: Int, to: Int): ReviewPortionEntity?

    @Query("SELECT COUNT(*) FROM review_portions")
    suspend fun portionCount(): Int

    @Insert
    suspend fun insertPortions(portions: List<ReviewPortionEntity>)

    @Query("UPDATE review_portions SET interval_days = :intervalDays, due_epoch_day = :dueEpochDay WHERE id = :id")
    suspend fun updateSchedule(id: Long, intervalDays: Int, dueEpochDay: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAyahStates(states: List<AyahStateEntity>)

    @Query("SELECT * FROM ayah_states")
    suspend fun allAyahStates(): List<AyahStateEntity>

    @Query("SELECT * FROM ayah_states WHERE ayah_id IN (:ids)")
    suspend fun ayahStatesIn(ids: List<Int>): List<AyahStateEntity>

    @Insert
    suspend fun addPracticeEvent(event: PracticeEventEntity)

    @Query("SELECT DISTINCT epoch_day FROM practice_events ORDER BY epoch_day DESC")
    suspend fun practiceDays(): List<Long>

    @Query("SELECT COALESCE(SUM(ayah_count), 0) FROM practice_events WHERE epoch_day >= :sinceDay")
    suspend fun ayahCountSince(sinceDay: Long): Int

    @Query("SELECT COALESCE(SUM(duration_ms), 0) FROM practice_events")
    suspend fun totalPracticeMs(): Long

    @Query("SELECT * FROM loop_presets ORDER BY is_default DESC, id")
    suspend fun allPresets(): List<LoopPresetEntity>

    @Query("SELECT * FROM loop_presets WHERE riwayah = :riwayah ORDER BY is_default DESC, id")
    suspend fun presetsForRiwayah(riwayah: String): List<LoopPresetEntity>

    @Query("SELECT * FROM loop_presets WHERE is_default = 1 LIMIT 1")
    suspend fun defaultPreset(): LoopPresetEntity?

    @Query("SELECT * FROM loop_presets WHERE is_default = 1 AND riwayah = :riwayah LIMIT 1")
    suspend fun defaultPresetForRiwayah(riwayah: String): LoopPresetEntity?

    @Query("SELECT COUNT(*) FROM loop_presets")
    suspend fun presetCount(): Int

    @Insert
    suspend fun insertPreset(preset: LoopPresetEntity): Long

    @Update
    suspend fun updatePreset(preset: LoopPresetEntity)

    @Query("UPDATE loop_presets SET is_default = 0")
    suspend fun clearDefaultPresets()

    @Query("UPDATE loop_presets SET is_default = 1 WHERE id = :id")
    suspend fun markDefaultPreset(id: Long)

    @Query("DELETE FROM loop_presets WHERE id = :id")
    suspend fun deletePreset(id: Long)

    @Query("SELECT * FROM timing_tracks ORDER BY updated_at DESC")
    suspend fun allTimingTracks(): List<TimingTrackEntity>

    @Query("SELECT * FROM timing_tracks WHERE id = :id")
    suspend fun timingTrack(id: Long): TimingTrackEntity?

    @Insert
    suspend fun insertTimingTrack(track: TimingTrackEntity): Long

    @Update
    suspend fun updateTimingTrack(track: TimingTrackEntity)

    @Query("DELETE FROM timing_tracks WHERE id = :id")
    suspend fun deleteTimingTrack(id: Long)

    @Query("SELECT * FROM custom_reciters ORDER BY created_at")
    suspend fun customReciters(): List<CustomReciterEntity>

    @Query("SELECT * FROM custom_reciters WHERE riwayah = :riwayah ORDER BY created_at")
    suspend fun customRecitersForRiwayah(riwayah: String): List<CustomReciterEntity>

    @Query("UPDATE custom_reciters SET riwayah = :riwayah WHERE id = :id")
    suspend fun setReciterRiwayah(id: Long, riwayah: String)

    @Insert
    suspend fun insertCustomReciter(reciter: CustomReciterEntity): Long

    @Query("DELETE FROM custom_reciters WHERE id = :id")
    suspend fun deleteCustomReciter(id: Long)

    @Query("DELETE FROM imported_surahs WHERE reciter_id = :reciterId")
    suspend fun deleteImportsForReciter(reciterId: Long)

    @Query("SELECT * FROM imported_surahs WHERE reciter_id = :reciterId")
    suspend fun importedSurahs(reciterId: Long): List<ImportedSurahEntity>

    @Query("SELECT * FROM imported_surahs WHERE reciter_id = :reciterId AND surah = :surah LIMIT 1")
    suspend fun importedSurah(reciterId: Long, surah: Int): ImportedSurahEntity?

    @Insert
    suspend fun insertImportedSurah(entity: ImportedSurahEntity)

    @Query("DELETE FROM imported_surahs WHERE reciter_id = :reciterId AND surah = :surah")
    suspend fun deleteImportedSurah(reciterId: Long, surah: Int)

    @Query("SELECT * FROM khatam WHERE active = 1 ORDER BY id DESC LIMIT 1")
    suspend fun activeKhatam(): KhatamEntity?

    @Insert
    suspend fun insertKhatam(khatam: KhatamEntity): Long

    @Update
    suspend fun updateKhatam(khatam: KhatamEntity)

    @Query("DELETE FROM khatam WHERE active = 1")
    suspend fun deleteActiveKhatam()

    @Query("UPDATE khatam SET last_read_page = :page WHERE active = 1")
    suspend fun setKhatamReadPage(page: Int)

    @Insert
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("SELECT * FROM bookmarks ORDER BY updated_at DESC")
    suspend fun allBookmarks(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE id = :id LIMIT 1")
    suspend fun bookmark(id: Long): BookmarkEntity?

    @Query("UPDATE bookmarks SET note = :note, label = :label, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateBookmark(id: Long, note: String, label: String, updatedAt: Long)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Insert
    suspend fun insertExerciseSession(session: ExerciseSessionEntity): Long

    // In-progress (finished_at IS NULL) first, then most recent.
    @Query("SELECT * FROM exercise_sessions ORDER BY finished_at IS NULL DESC, created_at DESC")
    suspend fun allExerciseSessions(): List<ExerciseSessionEntity>

    @Query("SELECT * FROM exercise_sessions WHERE id = :id LIMIT 1")
    suspend fun exerciseSession(id: Long): ExerciseSessionEntity?

    @Query("UPDATE exercise_sessions SET payload = :payload, correct = :correct, answered = :answered WHERE id = :id")
    suspend fun updateExerciseSession(id: Long, payload: String, correct: Int, answered: Int)

    @Query("UPDATE exercise_sessions SET finished_at = :finishedAt, payload = :payload, correct = :correct, answered = :answered WHERE id = :id")
    suspend fun finishExerciseSession(id: Long, finishedAt: Long, payload: String, correct: Int, answered: Int)

    @Query("DELETE FROM exercise_sessions WHERE id = :id")
    suspend fun deleteExerciseSession(id: Long)

    @Query("DELETE FROM exercise_sessions WHERE finished_at IS NOT NULL AND finished_at < :before")
    suspend fun pruneFinishedExerciseSessions(before: Long)
}

@Database(
    entities = [
        StumbleEntity::class,
        ReviewPortionEntity::class,
        RevealStateEntity::class,
        AyahStateEntity::class,
        PracticeEventEntity::class,
        LoopPresetEntity::class,
        TimingTrackEntity::class,
        CustomReciterEntity::class,
        ImportedSurahEntity::class,
        KhatamEntity::class,
        BookmarkEntity::class,
        ExerciseSessionEntity::class,
    ],
    version = 12,
    exportSchema = false,
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        // v8: adds a riwayah column to custom_reciters and loop_presets.
        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE custom_reciters ADD COLUMN riwayah TEXT NOT NULL DEFAULT 'hafs'")
                db.execSQL("ALTER TABLE loop_presets ADD COLUMN riwayah TEXT NOT NULL DEFAULT 'hafs'")
            }
        }

        // v9: adds the khatam table (single active cover-to-cover recitation).
        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS khatam (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "pace INTEGER NOT NULL DEFAULT 1, " +
                        "start_date INTEGER NOT NULL, " +
                        "units_completed INTEGER NOT NULL DEFAULT 0, " +
                        "last_logged_date INTEGER, " +
                        "streak_days INTEGER NOT NULL DEFAULT 0, " +
                        "pages_read INTEGER NOT NULL DEFAULT 0, " +
                        "time_read_seconds INTEGER NOT NULL DEFAULT 0, " +
                        "reminder_enabled INTEGER NOT NULL DEFAULT 0, " +
                        "reminder_time INTEGER, " +
                        "riwayah TEXT NOT NULL DEFAULT 'hafs', " +
                        "active INTEGER NOT NULL DEFAULT 1)",
                )
            }
        }

        // v10: adds last_read_page to khatam (resume the muṣḥaf where reading left off).
        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE khatam ADD COLUMN last_read_page INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v11: adds the bookmarks table (saved āyah ranges with an optional note).
        private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS bookmarks (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "surah INTEGER NOT NULL, " +
                        "ayah_from INTEGER NOT NULL, " +
                        "ayah_to INTEGER NOT NULL, " +
                        "note TEXT NOT NULL DEFAULT '', " +
                        "label TEXT NOT NULL DEFAULT 'none', " +
                        "created_at INTEGER NOT NULL, " +
                        "updated_at INTEGER NOT NULL DEFAULT 0)",
                )
            }
        }

        // v12: adds the exercise_sessions table (persisted Exercises sessions).
        private val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS exercise_sessions (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "created_at INTEGER NOT NULL, " +
                        "finished_at INTEGER, " +
                        "types_csv TEXT NOT NULL DEFAULT '', " +
                        "total INTEGER NOT NULL DEFAULT 0, " +
                        "correct INTEGER NOT NULL DEFAULT 0, " +
                        "answered INTEGER NOT NULL DEFAULT 0, " +
                        "payload TEXT NOT NULL DEFAULT '')",
                )
            }
        }

        fun open(context: Context): UserDatabase =
            Room.databaseBuilder(context, UserDatabase::class.java, "user.db")
                .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                // Pre-7 schemas have no migration path and may be recreated; from 7
                // on, a missing migration must fail loudly rather than wipe data.
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6)
                .build()
    }
}
