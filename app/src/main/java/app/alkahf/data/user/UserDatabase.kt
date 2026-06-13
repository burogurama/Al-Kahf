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

    @Query("SELECT * FROM loop_presets WHERE is_default = 1 LIMIT 1")
    suspend fun defaultPreset(): LoopPresetEntity?

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
    ],
    version = 6,
    exportSchema = false,
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        fun open(context: Context): UserDatabase =
            Room.databaseBuilder(context, UserDatabase::class.java, "user.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
