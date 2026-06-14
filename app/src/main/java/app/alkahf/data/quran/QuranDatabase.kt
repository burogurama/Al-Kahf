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
        /**
         * Opens the Qur'an DB for the active riwāyah. Hafs and Warsh are bundled
         * as separate assets and cached under distinct filenames, so switching
         * just reopens the other (the process restarts on a riwāyah change).
         */
        fun open(context: Context): QuranDatabase {
            val warsh = context
                .getSharedPreferences("alkahf_prefs", Context.MODE_PRIVATE)
                .getString("riwayah", "hafs") == "warsh"
            val asset = if (warsh) "quran_warsh.db" else "quran.db"
            return Room.databaseBuilder(context, QuranDatabase::class.java, asset)
                .createFromAsset(asset)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
