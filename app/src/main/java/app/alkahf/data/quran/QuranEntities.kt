package app.alkahf.data.quran

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Read-only entities backed by the bundled quran.db asset.
 * Schema must stay in sync with tools/build_quran_db.py.
 */
@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val number: Int,
    @ColumnInfo(name = "name_arabic") val nameArabic: String,
    @ColumnInfo(name = "name_latin") val nameLatin: String,
    @ColumnInfo(name = "revelation_type") val revelationType: String,
    @ColumnInfo(name = "ayah_count") val ayahCount: Int,
)

@Entity(
    tableName = "ayahs",
    indices = [Index("page"), Index("surah")],
)
data class AyahEntity(
    @PrimaryKey val id: Int, // surah * 1000 + number
    val surah: Int,
    val number: Int,
    val text: String,
    @ColumnInfo(name = "has_basmala") val hasBasmala: Boolean,
    val page: Int,
    val juz: Int,
    @ColumnInfo(name = "hizb_quarter") val hizbQuarter: Int,
)
