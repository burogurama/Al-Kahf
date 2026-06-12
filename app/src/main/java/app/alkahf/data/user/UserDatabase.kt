package app.alkahf.data.user

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

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

    @Query("SELECT * FROM review_portions WHERE due_epoch_day <= :today ORDER BY id")
    suspend fun duePortions(today: Long): List<ReviewPortionEntity>

    @Query("SELECT COUNT(*) FROM review_portions")
    suspend fun portionCount(): Int

    @Insert
    suspend fun insertPortions(portions: List<ReviewPortionEntity>)

    @Query("UPDATE review_portions SET interval_days = :intervalDays, due_epoch_day = :dueEpochDay WHERE id = :id")
    suspend fun updateSchedule(id: Long, intervalDays: Int, dueEpochDay: Long)
}

@Database(
    entities = [StumbleEntity::class, ReviewPortionEntity::class],
    version = 2,
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
