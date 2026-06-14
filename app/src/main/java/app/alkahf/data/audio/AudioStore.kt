package app.alkahf.data.audio

import android.content.Context
import app.alkahf.data.Riwayah
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Per-ayah reciter audio, downloaded on demand from everyayah.com and cached
 * in app storage for offline replay.
 */
class AudioStore(private val context: Context) {

    suspend fun ayahFile(surah: Int, ayah: Int, reciter: String = DEFAULT_RECITER): File =
        withContext(Dispatchers.IO) {
            val name = String.format(Locale.ROOT, "%03d%03d.mp3", surah, ayah)
            val dir = File(context.filesDir, "audio/$reciter").apply { mkdirs() }
            val file = File(dir, name)
            if (file.exists() && file.length() > 0) return@withContext file
            val temp = File(dir, "$name.part")
            try {
                val connection = (URL("$BASE_URL/$reciter/$name").openConnection() as HttpURLConnection)
                    .apply {
                        connectTimeout = CONNECT_TIMEOUT_MS
                        readTimeout = READ_TIMEOUT_MS
                    }
                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw IOException("HTTP ${connection.responseCode} for $name")
                    }
                    connection.inputStream.use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    }
                } finally {
                    connection.disconnect()
                }
                if (!temp.renameTo(file)) throw IOException("Could not finalise $name")
                file
            } catch (e: IOException) {
                temp.delete()
                throw e
            }
        }

    private fun reciterDir(reciter: String): File = File(context.filesDir, "audio/$reciter")

    /** Count of downloaded ayah files for a surah under a reciter. */
    fun downloadedAyahCount(reciter: String, surah: Int): Int {
        val prefix = String.format(Locale.ROOT, "%03d", surah)
        return reciterDir(reciter).listFiles { f ->
            f.name.startsWith(prefix) && f.name.endsWith(".mp3")
        }?.size ?: 0
    }

    /** Total bytes of all downloaded audio across reciters. */
    fun totalDownloadedBytes(): Long =
        File(context.filesDir, "audio").walkBottomUp()
            .filter { it.isFile && it.name.endsWith(".mp3") }
            .sumOf { it.length() }

    fun reciterBytes(reciter: String): Long =
        reciterDir(reciter).listFiles { f -> f.name.endsWith(".mp3") }
            ?.sumOf { it.length() } ?: 0L

    fun surahBytes(reciter: String, surah: Int): Long {
        val prefix = String.format(Locale.ROOT, "%03d", surah)
        return reciterDir(reciter).listFiles { f ->
            f.name.startsWith(prefix) && f.name.endsWith(".mp3")
        }?.sumOf { it.length() } ?: 0L
    }

    /** Surah numbers that have at least one downloaded ayah for a reciter. */
    fun downloadedSurahs(reciter: String): List<Int> =
        reciterDir(reciter).listFiles { f -> f.name.endsWith(".mp3") }
            ?.mapNotNull { it.name.take(3).toIntOrNull() }
            ?.distinct()
            ?.sorted()
            ?: emptyList()

    /** Downloads every ayah of a surah, reporting progress 0f..1f. */
    suspend fun downloadSurah(
        reciter: String,
        surah: Int,
        ayahCount: Int,
        onProgress: (Float) -> Unit,
    ) {
        for (ayah in 1..ayahCount) {
            ayahFile(surah, ayah, reciter)
            onProgress(ayah.toFloat() / ayahCount)
        }
    }

    /** Downloads āyāt [from]..[to] of a surah, reporting progress 0f..1f. */
    suspend fun downloadRange(
        reciter: String,
        surah: Int,
        from: Int,
        to: Int,
        onProgress: (Float) -> Unit,
    ) {
        val total = (to - from + 1).coerceAtLeast(1)
        var done = 0
        for (ayah in from..to) {
            ayahFile(surah, ayah, reciter)
            done++
            onProgress(done.toFloat() / total)
        }
    }

    /** True when every āyah of [from]..[to] is already cached for the reciter. */
    fun rangeDownloaded(reciter: String, surah: Int, from: Int, to: Int): Boolean {
        for (ayah in from..to) {
            val name = String.format(Locale.ROOT, "%03d%03d.mp3", surah, ayah)
            val file = File(reciterDir(reciter), name)
            if (!file.exists() || file.length() <= 0) return false
        }
        return true
    }

    suspend fun deleteSurah(reciter: String, surah: Int) = withContext(Dispatchers.IO) {
        val prefix = String.format(Locale.ROOT, "%03d", surah)
        reciterDir(reciter).listFiles { f ->
            f.name.startsWith(prefix) && f.name.endsWith(".mp3")
        }?.forEach { it.delete() }
        Unit
    }

    companion object {
        private const val BASE_URL = "https://everyayah.com/data"
        const val DEFAULT_RECITER = "Husary_128kbps"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
    }
}

/** An everyayah.com reciter: directory name + display name + riwāyah. */
data class Reciter(
    val path: String,
    val displayName: String,
    val riwayah: Riwayah = Riwayah.HAFS,
)

val RECITERS = listOf(
    Reciter("Husary_128kbps", "Ḥuṣarī"),
    Reciter("Abdul_Basit_Murattal_192kbps", "Abdul Basit"),
    Reciter("Minshawy_Murattal_128kbps", "Minshawy"),
    Reciter("warsh/warsh_yassin_al_jazaery_64kbps", "Yāsīn al-Jazāʼirī", Riwayah.WARSH),
    Reciter("warsh/warsh_ibrahim_aldosary_128kbps", "Ibrāhīm al-Dawsarī", Riwayah.WARSH),
)

/** Built-in reciters for a riwāyah. */
fun recitersFor(riwayah: Riwayah): List<Reciter> = RECITERS.filter { it.riwayah == riwayah }
