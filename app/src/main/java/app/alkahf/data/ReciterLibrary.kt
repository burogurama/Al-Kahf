package app.alkahf.data

import app.alkahf.data.audio.AudioStore
import app.alkahf.data.audio.Reciter
import app.alkahf.data.audio.recitersFor
import app.alkahf.data.user.CustomReciterEntity
import app.alkahf.data.user.ImportedSurahEntity
import app.alkahf.data.user.UserDao
import java.io.File

/**
 * The reciter catalogue and its offline audio: which voice is active, the
 * built-in downloadable reciters and user-created imported profiles, per-sūrah
 * download/import state, and the audio download/delete operations. Bridges the
 * file-backed [AudioStore] and the imported-reciter rows in the user DB.
 */
class ReciterLibrary(
    private val audioStore: AudioStore,
    private val userDao: UserDao,
    private val quranText: QuranTextStore,
    private val settings: UserPreferences,
    private val filesDir: File,
) {
    private val riwayah: Riwayah get() = settings.riwayah

    /** Built-in reciters available for the active riwāyah. */
    val riwayahReciters: List<Reciter> get() = recitersFor(riwayah)

    val activeReciterPath: String
        get() {
            val pool = riwayahReciters
            val stored = settings.activeReciterRaw
            // Keep the stored voice only if it belongs to the active riwāyah.
            return pool.firstOrNull { it.path == stored }?.path ?: pool.first().path
        }

    val activeReciter: Reciter
        get() = riwayahReciters.firstOrNull { it.path == activeReciterPath } ?: riwayahReciters.first()

    fun setActiveReciter(path: String) = settings.setActiveReciter(path)

    suspend fun statuses(riwayah: Riwayah = this.riwayah): List<ReciterStatus> {
        val active = activeReciterPath
        val builtins = recitersFor(riwayah).map { reciter ->
            ReciterStatus(
                key = reciter.path,
                displayName = reciter.displayName,
                arabicInitial = reciterInitial(reciter.path),
                isActive = reciter.path == active,
                isImported = false,
                itemCount = audioStore.downloadedSurahs(reciter.path).size,
                bytes = audioStore.reciterBytes(reciter.path),
            )
        }
        val customs = userDao.customRecitersForRiwayah(riwayah.key).map { reciter ->
            ReciterStatus(
                key = customReciterKey(reciter.id),
                displayName = reciter.name,
                arabicInitial = reciter.initial,
                isActive = false,
                isImported = true,
                itemCount = userDao.importedSurahs(reciter.id).size,
                bytes = 0L,
            )
        }
        return builtins + customs
    }

    suspend fun createCustom(name: String, riwayah: Riwayah = this.riwayah): String {
        val initial = name.trim().firstOrNull { !it.isWhitespace() }?.toString() ?: "ق"
        val id = userDao.insertCustomReciter(
            CustomReciterEntity(
                name = name.trim(),
                initial = initial,
                createdAt = System.currentTimeMillis(),
                riwayah = riwayah.key,
            ),
        )
        return customReciterKey(id)
    }

    /** Re-tags an imported reciter with a riwāyah. */
    suspend fun setRiwayah(reciterKey: String, riwayah: Riwayah) {
        val id = customReciterId(reciterKey) ?: return
        userDao.setReciterRiwayah(id, riwayah.key)
    }

    suspend fun deleteCustom(key: String) {
        val id = customReciterId(key) ?: return
        userDao.deleteImportsForReciter(id)
        userDao.deleteCustomReciter(id)
    }

    suspend fun importSurah(reciterKey: String, surah: Int, uri: String) {
        val id = customReciterId(reciterKey) ?: return
        userDao.deleteImportedSurah(id, surah)
        userDao.insertImportedSurah(ImportedSurahEntity(reciterId = id, surah = surah, uri = uri))
    }

    suspend fun removeImportedSurah(reciterKey: String, surah: Int) {
        val id = customReciterId(reciterKey) ?: return
        userDao.deleteImportedSurah(id, surah)
    }

    /** Per-surah rows for a reciter's surah list (download or import state). */
    suspend fun surahItems(reciterKey: String): List<ReciterSurahItem> {
        val surahs = quranText.allSurahs()
        val customId = customReciterId(reciterKey)
        if (customId == null) {
            return surahs.map { surah ->
                val downloaded = audioStore.downloadedAyahCount(reciterKey, surah.number)
                ReciterSurahItem(
                    surah = surah.number,
                    nameLatin = surah.nameLatin,
                    ayahCount = surah.ayahCount,
                    isImported = false,
                    hasAudio = downloaded >= surah.ayahCount,
                    downloadedAyahs = downloaded,
                    bytes = audioStore.surahBytes(reciterKey, surah.number),
                    timed = false,
                    importedUri = null,
                )
            }
        }
        val imports = userDao.importedSurahs(customId).associateBy { it.surah }
        val tracks = userDao.allTimingTracks()
        return surahs.map { surah ->
            val import = imports[surah.number]
            val timed = import != null && tracks.any {
                it.sourceRef == import.uri && it.surah == surah.number && it.complete
            }
            ReciterSurahItem(
                surah = surah.number,
                nameLatin = surah.nameLatin,
                ayahCount = surah.ayahCount,
                isImported = true,
                hasAudio = import != null,
                downloadedAyahs = if (import != null) surah.ayahCount else 0,
                bytes = 0L,
                timed = timed,
                importedUri = import?.uri,
            )
        }
    }

    suspend fun downloadSurah(reciterPath: String, surah: Int, onProgress: (Float) -> Unit) {
        val ayahCount = quranText.surahAyahCount(surah)
        audioStore.downloadSurah(reciterPath, surah, ayahCount, onProgress)
    }

    /**
     * Downloads just the āyāt [from]..[to] of a built-in reciter's sūrah,
     * reporting progress 0f..1f. Used by the Mushaf range-listening flow so a
     * user only fetches what they're about to hear.
     */
    suspend fun downloadRange(
        reciterPath: String,
        surah: Int,
        from: Int,
        to: Int,
        riwayah: Riwayah = this.riwayah,
        onProgress: (Float) -> Unit,
    ) {
        val audio = quranText.audioHafsRange(surah, from, to, riwayah)
        audioStore.downloadRange(reciterPath, surah, audio.first, audio.last, onProgress)
    }

    /** True when every āyah of [from]..[to] is already cached for the reciter. */
    suspend fun rangeAvailable(
        reciterPath: String,
        surah: Int,
        from: Int,
        to: Int,
        riwayah: Riwayah = this.riwayah,
    ): Boolean {
        val audio = quranText.audioHafsRange(surah, from, to, riwayah)
        return audioStore.rangeDownloaded(reciterPath, surah, audio.first, audio.last)
    }

    suspend fun deleteSurahAudio(reciterPath: String, surah: Int) =
        audioStore.deleteSurah(reciterPath, surah)

    /** Resolves the per-ayah audio files for a reciter source (downloads as needed). */
    suspend fun ayahUris(reciterPath: String, surah: Int, from: Int, to: Int): List<String> =
        (from..to).map { ayah ->
            android.net.Uri.fromFile(audioStore.ayahFile(surah, ayah, reciterPath)).toString()
        }

    /** A riwāyah's reciters (built-in + imported), for drill/Mushaf pickers. */
    suspend fun allReciters(riwayah: Riwayah = this.riwayah): List<Reciter> =
        recitersFor(riwayah) +
            userDao.customRecitersForRiwayah(riwayah.key).map { Reciter(customReciterKey(it.id), it.name) }

    fun storageInfo(): StorageInfo {
        val stat = android.os.StatFs(filesDir.absolutePath)
        return StorageInfo(
            usedBytes = audioStore.totalDownloadedBytes(),
            totalBytes = stat.blockCountLong * stat.blockSizeLong,
        )
    }

    private fun reciterInitial(reciterPath: String): String = when {
        reciterPath.startsWith("Husary") -> "ح"
        reciterPath.startsWith("Alafasy") -> "ع"
        reciterPath.startsWith("Abdul_Basit") -> "ع"
        reciterPath.startsWith("Minshawy") -> "م"
        else -> "ق"
    }
}
