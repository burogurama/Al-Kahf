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

    // The juzʼ's span: its first/last āyah (by id order) and its page range,
    // used to resolve a khatam portion. Page numbering differs per riwāyah, so
    // this is read from whichever DB the call targets.
    @Query(
        "SELECT MIN(surah * 1000 + number) AS first_id, MAX(surah * 1000 + number) AS last_id, " +
            "MIN(page) AS page_from, MAX(page) AS page_to FROM ayahs WHERE juz = :juz",
    )
    suspend fun juzSpan(juz: Int): JuzSpan?

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

/** A juzʼ's bounds: first/last āyah id (surah * 1000 + number) and its page span. */
data class JuzSpan(
    @ColumnInfo(name = "first_id") val firstId: Int,
    @ColumnInfo(name = "last_id") val lastId: Int,
    @ColumnInfo(name = "page_from") val pageFrom: Int,
    @ColumnInfo(name = "page_to") val pageTo: Int,
)
