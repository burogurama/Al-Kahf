package app.alkahf.data

import app.alkahf.data.user.TimingTrackEntity
import app.alkahf.data.user.UserDao

/**
 * Tawqīt timing tracks (aligning an audio source to a portion of the mushaf)
 * and the playback they resolve for imported reciters: a single combined audio
 * file plus the start..end millisecond segment of each timed āyah.
 */
class TawqitStore(
    private val userDao: UserDao,
    private val quranText: QuranTextStore,
) {
    suspend fun tracks(): List<TawqitTrack> =
        userDao.allTimingTracks().map { it.toTawqitTrack() }

    suspend fun track(id: Long): TawqitTrack? =
        userDao.timingTrack(id)?.toTawqitTrack()

    suspend fun saveTrack(track: TawqitTrack): Long {
        val entity = TimingTrackEntity(
            id = track.id,
            sourceType = if (track.sourceType == TawqitSourceType.IMPORT) "import" else "reciter",
            sourceRef = track.sourceRef,
            sourceLabel = track.sourceLabel,
            surah = track.surah,
            surahName = track.surahNameLatin,
            ayahFrom = track.ayahFrom,
            ayahTo = track.ayahTo,
            endTimesCsv = track.endTimesMs.joinToString(","),
            globalOffsetMs = track.globalOffsetMs,
            complete = track.complete,
            updatedAt = System.currentTimeMillis(),
        )
        return if (track.id == 0L) {
            userDao.insertTimingTrack(entity)
        } else {
            userDao.updateTimingTrack(entity)
            track.id
        }
    }

    suspend fun deleteTrack(id: Long) = userDao.deleteTimingTrack(id)

    /** A Tawqīt draft (new or resumed) for an imported surah of a custom reciter. */
    suspend fun draftForImport(reciterKey: String, surah: Int): TawqitTrack? {
        val id = customReciterId(reciterKey) ?: return null
        val import = userDao.importedSurah(id, surah) ?: return null
        val surahRow = quranText.surah(surah)
        val reciterName = userDao.customReciters().firstOrNull { it.id == id }?.name ?: "Imported"
        val existing = userDao.allTimingTracks()
            .firstOrNull { it.sourceRef == import.uri && it.surah == surah }
        return existing?.toTawqitTrack() ?: TawqitTrack(
            sourceType = TawqitSourceType.IMPORT,
            sourceRef = import.uri,
            sourceLabel = "$reciterName (imported)",
            surah = surah,
            surahNameLatin = surahRow.nameLatin,
            ayahFrom = 1,
            ayahTo = surahRow.ayahCount,
            endTimesMs = emptyList(),
            globalOffsetMs = 0,
            complete = false,
        )
    }

    /**
     * Resolves playback for an imported reciter over [from]..[to] of a sūrah from
     * its Tawqīt timings, or null when the reciter isn't imported, the sūrah has
     * no imported file, or the requested range isn't (yet) timed — partial timings
     * are fine as long as they cover the requested range. Imports are timed from
     * āyah 1, so endTimesMs[n-1] is the cumulative end of āyah n within the file.
     */
    suspend fun rangePlayback(
        reciterKey: String,
        surah: Int,
        from: Int,
        to: Int,
    ): ImportedPlayback? {
        val customId = customReciterId(reciterKey) ?: return null
        val import = userDao.importedSurah(customId, surah) ?: return null
        val track = userDao.allTimingTracks()
            .firstOrNull { it.sourceRef == import.uri && it.surah == surah }
            ?.toTawqitTrack() ?: return null
        val ends = track.endTimesMs
        val firstTimed = track.ayahFrom
        val lastTimed = firstTimed + ends.size - 1
        if (ends.isEmpty() || from < firstTimed || to > lastTimed) return null
        val offset = track.globalOffsetMs
        val segments = (from..to).map { n ->
            val startMs = if (n == firstTimed) 0L else ends[n - firstTimed - 1]
            val endMs = ends[n - firstTimed]
            (startMs + offset).coerceAtLeast(0L)..(endMs + offset).coerceAtLeast(0L)
        }
        return ImportedPlayback(
            fileUri = import.uri,
            ayahIds = (from..to).map { surah * 1000 + it },
            segments = segments,
        )
    }

    /**
     * The imported file for [reciterKey]'s [surah] plus the start..end segment of
     * every timed āyah (keyed by āyah number). Null when the reciter hasn't
     * imported/timed that sūrah. Used to play imported audio in the drill.
     */
    suspend fun surahAudio(reciterKey: String, surah: Int): ImportedSurahAudio? {
        val customId = customReciterId(reciterKey) ?: return null
        val import = userDao.importedSurah(customId, surah) ?: return null
        val track = userDao.allTimingTracks()
            .firstOrNull { it.sourceRef == import.uri && it.surah == surah }
            ?.toTawqitTrack() ?: return null
        val ends = track.endTimesMs
        if (ends.isEmpty()) return null
        val firstTimed = track.ayahFrom
        val offset = track.globalOffsetMs
        val segments = buildMap {
            for (i in ends.indices) {
                val startMs = if (i == 0) 0L else ends[i - 1]
                put(
                    firstTimed + i,
                    (startMs + offset).coerceAtLeast(0L)..(ends[i] + offset).coerceAtLeast(0L),
                )
            }
        }
        return ImportedSurahAudio(import.uri, segments)
    }

    private fun TimingTrackEntity.toTawqitTrack() = TawqitTrack(
        id = id,
        sourceType = if (sourceType == "import") TawqitSourceType.IMPORT else TawqitSourceType.RECITER,
        sourceRef = sourceRef,
        sourceLabel = sourceLabel,
        surah = surah,
        surahNameLatin = surahName,
        ayahFrom = ayahFrom,
        ayahTo = ayahTo,
        endTimesMs = endTimesCsv.split(",").filter { it.isNotBlank() }.map { it.toLong() },
        globalOffsetMs = globalOffsetMs,
        complete = complete,
    )
}
