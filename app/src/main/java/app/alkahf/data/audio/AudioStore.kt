package app.alkahf.data.audio

import android.content.Context
import java.io.File
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Per-ayah reciter audio, downloaded on demand from everyayah.com and cached
 * in app storage for offline replay. A proper per-surah download manager
 * (Library screen) will build on this cache.
 */
class AudioStore(private val context: Context) {

    suspend fun ayahFile(surah: Int, ayah: Int, reciter: String = DEFAULT_RECITER): File =
        withContext(Dispatchers.IO) {
            val name = String.format(Locale.ROOT, "%03d%03d.mp3", surah, ayah)
            val dir = File(context.filesDir, "audio/$reciter").apply { mkdirs() }
            val file = File(dir, name)
            if (file.exists() && file.length() > 0) return@withContext file
            val temp = File(dir, "$name.part")
            URL("$BASE_URL/$reciter/$name").openStream().use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            }
            temp.renameTo(file)
            file
        }

    companion object {
        private const val BASE_URL = "https://everyayah.com/data"
        const val DEFAULT_RECITER = "Husary_128kbps"
    }
}

/** An everyayah.com reciter: directory name + display name. */
data class Reciter(val path: String, val displayName: String)

val RECITERS = listOf(
    Reciter("Husary_128kbps", "Ḥuṣarī"),
    Reciter("Alafasy_128kbps", "Alafasy"),
    Reciter("Abdul_Basit_Murattal_192kbps", "Abdul Basit"),
    Reciter("Minshawy_Murattal_128kbps", "Minshawy"),
)
