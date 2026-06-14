package app.alkahf.data

import android.content.Context
import app.alkahf.data.quran.AyahLocation
import app.alkahf.data.quran.QuranDatabase
import app.alkahf.data.quran.SurahEntity

/**
 * Read-only access to the Qur'an text: both riwāyāt's bundled databases, the
 * mushaf page layout, sūrah metadata, and the Ḥafṣ ↔ Warsh audio-numbering
 * mapping. The single owner of the Qur'an DBs; user data lives elsewhere.
 *
 * Most queries default to the active [riwayahProvider] reading but accept an
 * explicit riwāyah so the Mushaf can briefly show the other one. Both DBs are
 * opened lazily — Warsh only when first used.
 */
class QuranTextStore(context: Context, private val riwayahProvider: () -> Riwayah) {
    private val hafsDb by lazy { QuranDatabase.openFor(context, "hafs") }
    private val warshDb by lazy { QuranDatabase.openFor(context, "warsh") }
    private fun daoFor(riwayah: Riwayah) =
        (if (riwayah == Riwayah.WARSH) warshDb else hafsDb).quranDao()
    private val riwayah: Riwayah get() = riwayahProvider()
    private val quranDao get() = daoFor(riwayah)
    private var cachedHafsBasmala: String? = null

    suspend fun surah(number: Int): SurahEntity = quranDao.surah(number)

    suspend fun surahAyahCount(surah: Int): Int = quranDao.surah(surah).ayahCount

    suspend fun surahNameLatin(surah: Int): String = quranDao.surah(surah).nameLatin

    suspend fun pageOfAyah(surah: Int, ayah: Int): Int = quranDao.pageOfAyahId(surah * 1000 + ayah)

    suspend fun firstPageOfSurah(surah: Int): Int = quranDao.firstPageOfSurah(surah)

    suspend fun ayahLocations(): List<AyahLocation> = quranDao.ayahLocations()

    suspend fun surahOptions(): List<SurahOption> =
        quranDao.allSurahs().map { SurahOption(it.number, it.nameLatin, it.nameArabic, it.ayahCount) }

    suspend fun allSurahs(): List<SurahEntity> = quranDao.allSurahs()

    /** A mushaf page, optionally in a riwāyah other than the active one. */
    suspend fun page(number: Int, riwayah: Riwayah = this.riwayah): MushafPage {
        val dao = daoFor(riwayah)
        val ayahs = dao.ayahsOnPage(number)
        // In Warsh, id 1001 is "al-ḥamdu" (basmala isn't a counted āyah), so the
        // basmala header comes from a fixed Warsh-orthography string instead.
        val basmala = if (riwayah == Riwayah.WARSH) {
            WARSH_BASMALA
        } else {
            cachedHafsBasmala ?: dao.basmala().also { cachedHafsBasmala = it }
        }
        val groups = ayahs
            .groupBy { it.surah }
            .map { (surahNumber, surahAyahs) ->
                val surah = dao.surah(surahNumber)
                val beginsHere = surahAyahs.first().number == 1
                PageGroup(
                    surahNumber = surahNumber,
                    surahNameArabic = "سُورَةُ ${surah.nameArabic}",
                    surahNameLatin = surah.nameLatin,
                    surahMeta = surahMeta(surah.revelationType, surah.ayahCount),
                    showSurahHeader = beginsHere,
                    basmala = basmala.takeIf { beginsHere && surahAyahs.first().hasBasmala },
                    ayahs = surahAyahs.map { ayah ->
                        PageAyah(
                            id = ayah.id,
                            surah = ayah.surah,
                            number = ayah.number,
                            words = ayah.text.split(' '),
                            marker = "۝${ayah.number.toArabicIndic()}",
                        )
                    },
                )
            }
        val first = ayahs.first()
        return MushafPage(
            number = number,
            juz = first.juz,
            hizb = (first.hizbQuarter - 1) / 4 + 1,
            groups = groups,
        )
    }

    suspend fun ayahsForRange(
        surah: Int,
        from: Int,
        to: Int,
        riwayah: Riwayah = this.riwayah,
    ): List<PageAyah> =
        daoFor(riwayah).ayahRange(surah, from, to).map { ayah ->
            PageAyah(
                id = ayah.id,
                surah = ayah.surah,
                number = ayah.number,
                words = ayah.text.split(' '),
                marker = "۝${ayah.number.toArabicIndic()}",
            )
        }

    /**
     * The standard (Hafs-numbered) audio āyāt for an app āyah. Identity in Hafs;
     * in Warsh it maps each verse to the everyayah file(s) that cover it (the
     * everyayah library numbers all audio by the Hafs counting).
     */
    suspend fun audioAyahs(surah: Int, ayah: Int, riwayah: Riwayah = this.riwayah): List<Int> {
        val r = daoFor(riwayah).audioRange(surah * 1000 + ayah) ?: return listOf(ayah)
        return (r.from..r.to).toList()
    }

    /** The Hafs audio āyāh range spanning an app range [from]..[to]. */
    suspend fun audioHafsRange(
        surah: Int,
        from: Int,
        to: Int,
        riwayah: Riwayah = this.riwayah,
    ): IntRange {
        val dao = daoFor(riwayah)
        val lo = dao.audioRange(surah * 1000 + from)?.from ?: from
        val hi = dao.audioRange(surah * 1000 + to)?.to ?: to
        return lo..hi
    }

    /**
     * Converts an āyah range from one riwāyah's numbering to the other's so the
     * same passage is referenced (Ḥafṣ ↔ Warsh differ where verses split/merge).
     */
    suspend fun convertAyahRange(
        surah: Int,
        from: Int,
        to: Int,
        fromRiwayah: Riwayah,
        toRiwayah: Riwayah,
    ): IntRange {
        if (fromRiwayah == toRiwayah) return from..to
        // Map both endpoints to the canonical Hafs numbering first.
        val (hafsFrom, hafsTo) = if (fromRiwayah == Riwayah.WARSH) {
            val a = warshDb.quranDao().audioRange(surah * 1000 + from)?.from ?: from
            val b = warshDb.quranDao().audioRange(surah * 1000 + to)?.to ?: to
            a to b
        } else {
            from to to
        }
        if (toRiwayah == Riwayah.HAFS) return hafsFrom..hafsTo
        // Hafs → Warsh: the Warsh āyāt whose audio covers those Hafs āyāt.
        val wDao = warshDb.quranDao()
        val wFrom = wDao.ayahCoveringHafsFirst(surah, hafsFrom) ?: hafsFrom
        val wTo = wDao.ayahCoveringHafsLast(surah, hafsTo) ?: hafsTo
        return minOf(wFrom, wTo)..maxOf(wFrom, wTo)
    }

    private fun surahMeta(revelationType: String, ayahCount: Int): String {
        val place = if (revelationType == "Meccan") "MAKKĪ" else "MADANĪ"
        return "$place · $ayahCount ĀYĀT"
    }

    companion object {
        // Warsh-orthography basmala (basmala is not a counted āyah in Warsh).
        private const val WARSH_BASMALA = "بِسْمِ اِ۬للَّهِ اِ۬لرَّحْمَٰنِ اِ۬لرَّحِيمِ"
    }
}
