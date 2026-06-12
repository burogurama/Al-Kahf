package app.alkahf.data.quran

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SurahEntity::class, AyahEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao

    companion object {
        fun open(context: Context): QuranDatabase =
            Room.databaseBuilder(context, QuranDatabase::class.java, "quran.db")
                .createFromAsset("quran.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
