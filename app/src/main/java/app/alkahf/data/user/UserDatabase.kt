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

@Dao
interface UserDao {
    @Insert
    suspend fun addStumble(stumble: StumbleEntity)

    @Query("SELECT * FROM stumbles WHERE ayah_id IN (:ayahIds)")
    suspend fun stumblesForAyahs(ayahIds: List<Int>): List<StumbleEntity>
}

@Database(entities = [StumbleEntity::class], version = 1, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        fun open(context: Context): UserDatabase =
            Room.databaseBuilder(context, UserDatabase::class.java, "user.db").build()
    }
}
