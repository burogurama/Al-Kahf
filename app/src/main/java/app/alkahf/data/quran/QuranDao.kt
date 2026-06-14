package app.alkahf.data.quran

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query

@Dao
interface QuranDao {
    @Query("SELECT * FROM ayahs WHERE page = :page ORDER BY id")
    suspend fun ayahsOnPage(page: Int): List<AyahEntity>

    @Query("SELECT * FROM surahs WHERE number = :number")
    suspend fun surah(number: Int): SurahEntity

    @Query("SELECT MIN(page) FROM ayahs WHERE surah = :surah")
    suspend fun firstPageOfSurah(surah: Int): Int

    @Query("SELECT page FROM ayahs WHERE id = :id")
    suspend fun pageOfAyahId(id: Int): Int

    @Query("SELECT text FROM ayahs WHERE id = 1001")
    suspend fun basmala(): String

    @Query("SELECT * FROM ayahs WHERE surah = :surah AND number BETWEEN :from AND :to ORDER BY id")
    suspend fun ayahRange(surah: Int, from: Int, to: Int): List<AyahEntity>

    @Query("SELECT id, page, juz FROM ayahs")
    suspend fun ayahLocations(): List<AyahLocation>

    @Query("SELECT * FROM surahs ORDER BY number")
    suspend fun allSurahs(): List<SurahEntity>

    @Query("SELECT audio_from, audio_to FROM ayahs WHERE id = :id")
    suspend fun audioRange(id: Int): AudioRange?

    // Reverse map (Hafs āyah → this DB's āyah): the lowest/highest āyah whose
    // audio range covers a given Hafs-numbered āyah, for converting ranges.
    @Query("SELECT number FROM ayahs WHERE surah = :surah AND :hafsAyah BETWEEN audio_from AND audio_to ORDER BY number ASC LIMIT 1")
    suspend fun ayahCoveringHafsFirst(surah: Int, hafsAyah: Int): Int?

    @Query("SELECT number FROM ayahs WHERE surah = :surah AND :hafsAyah BETWEEN audio_from AND audio_to ORDER BY number DESC LIMIT 1")
    suspend fun ayahCoveringHafsLast(surah: Int, hafsAyah: Int): Int?
}

/** Minimal projection for whole-mushaf aggregations (Progress screen). */
data class AyahLocation(val id: Int, val page: Int, val juz: Int)

/** The standard (Hafs-numbered) audio āyāt covering an app āyah. */
data class AudioRange(
    @ColumnInfo(name = "audio_from") val from: Int,
    @ColumnInfo(name = "audio_to") val to: Int,
)
