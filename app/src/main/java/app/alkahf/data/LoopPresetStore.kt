package app.alkahf.data

import app.alkahf.data.user.LoopPresetEntity
import app.alkahf.data.user.UserDao

/**
 * Saved drill configurations for the loop player, including the single
 * auto-managed sabaq drill (flagged isDefault) that tracks the current sabaq.
 * Reads the active reciter from [ReciterLibrary] and sūrah names from
 * [QuranTextStore] when synthesising or syncing the sabaq drill.
 */
class LoopPresetStore(
    private val userDao: UserDao,
    private val quranText: QuranTextStore,
    private val reciters: ReciterLibrary,
    private val settings: UserPreferences,
) {
    /** All drills, every riwāyah — each card shows its own riwāyah label. */
    suspend fun all(): List<LoopPreset> = userDao.allPresets().map { it.toLoopPreset() }

    /** The single auto-managed sabaq drill, or null when there's no sabaq. */
    suspend fun sabaqDrill(): LoopPreset? = userDao.defaultPreset()?.toLoopPreset()

    /** A non-null base preset for the loop player (sabaq drill, else a template). */
    suspend fun defaultPreset(): LoopPreset =
        sabaqDrill()
            ?: userDao.allPresets().firstOrNull()?.toLoopPreset()
            ?: reciters.activeReciter.let { r ->
                LoopPreset(
                    surah = 18,
                    surahNameLatin = "Al-Kahf",
                    ayahFrom = 1,
                    ayahTo = 5,
                    reciterPath = r.path,
                    reciterName = r.displayName,
                    perAyah = 3,
                    perChain = 5,
                    gapMultiplier = 1.5f,
                    speed = 1.0f,
                )
            }

    /**
     * Keeps a single auto-generated drill in step with the sabaq: created when a
     * sabaq exists, its range refreshed (keeping the user's config) when the
     * sabaq advances, and removed when there's no sabaq. Flagged isDefault to
     * tell it apart from the user's own presets.
     */
    suspend fun syncSabaqDrill() {
        val range = settings.sabaqRange
        val riwayah = settings.riwayah
        val defaults = userDao.allPresets().filter { it.isDefault }
        if (range == null) {
            defaults.forEach { userDao.deletePreset(it.id) }
            return
        }
        // Keep exactly one sabaq drill, dropping any duplicates.
        val existing = defaults.firstOrNull()
        defaults.drop(1).forEach { userDao.deletePreset(it.id) }
        val surahName = quranText.surahNameLatin(range.surah)
        if (existing == null) {
            val reciter = reciters.activeReciter
            userDao.insertPreset(
                LoopPresetEntity(
                    name = surahName,
                    surah = range.surah,
                    surahName = surahName,
                    ayahFrom = range.from,
                    ayahTo = range.to,
                    reciterPath = reciter.path,
                    reciterName = reciter.displayName,
                    perAyah = 3,
                    perChain = 5,
                    gapMultiplier = 1.5f,
                    speed = 1.0f,
                    isDefault = true,
                    riwayah = riwayah.key,
                ),
            )
            return
        }
        // The sabaq drill follows the system riwāyah: convert its reading and (if
        // needed) reciter when the system reading toggled, and track the sabaq range.
        val reciterValid = isCustomReciter(existing.reciterPath) ||
            reciters.riwayahReciters.any { it.path == existing.reciterPath }
        val reciter = if (existing.riwayah != riwayah.key || !reciterValid) reciters.activeReciter else null
        userDao.updatePreset(
            existing.copy(
                name = surahName,
                surah = range.surah,
                surahName = surahName,
                ayahFrom = range.from,
                ayahTo = range.to,
                riwayah = riwayah.key,
                reciterPath = reciter?.path ?: existing.reciterPath,
                reciterName = reciter?.displayName ?: existing.reciterName,
            ),
        )
    }

    /** Inserts a new preset, or updates it in place when it already has an id. */
    suspend fun save(preset: LoopPreset): Long {
        val entity = LoopPresetEntity(
            id = preset.id,
            name = preset.name,
            surah = preset.surah,
            surahName = preset.surahNameLatin,
            ayahFrom = preset.ayahFrom,
            ayahTo = preset.ayahTo,
            reciterPath = preset.reciterPath,
            reciterName = preset.reciterName,
            perAyah = preset.perAyah,
            perChain = preset.perChain,
            gapMultiplier = preset.gapMultiplier,
            speed = preset.speed,
            isDefault = preset.isDefault,
            // The drill carries its own riwāyah, chosen in the editor.
            riwayah = preset.riwayah.key,
        )
        return if (preset.id == 0L) {
            userDao.insertPreset(entity)
        } else {
            userDao.updatePreset(entity)
            preset.id
        }
    }

    suspend fun byId(id: Long): LoopPreset? =
        userDao.allPresets().firstOrNull { it.id == id }?.toLoopPreset()

    suspend fun delete(id: Long) {
        userDao.deletePreset(id)
    }

    private fun LoopPresetEntity.toLoopPreset() = LoopPreset(
        id = id,
        name = name,
        surah = surah,
        surahNameLatin = surahName,
        ayahFrom = ayahFrom,
        ayahTo = ayahTo,
        reciterPath = reciterPath,
        reciterName = reciterName,
        perAyah = perAyah,
        perChain = perChain,
        gapMultiplier = gapMultiplier,
        speed = speed,
        isDefault = isDefault,
        riwayah = Riwayah.fromKey(riwayah),
    )
}
