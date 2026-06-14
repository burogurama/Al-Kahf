package app.alkahf.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import app.alkahf.AlkahfApplication
import app.alkahf.data.SurahName

/**
 * Resolves a sūrah's localised display name by its number (1–114). Localisation
 * is a presentation concern: the data layer stays language-agnostic and carries
 * only the sūrah number, while the name shown is resolved here.
 *
 * The name comes from the bundled Qur'an DB — the source of truth for Qur'an
 * data — so it follows the active riwāyah (Ḥafṣ / Warsh store different,
 * fully-vocalised Arabic names). The Arabic name is used when the app is in
 * Arabic; otherwise the Latin transliteration.
 *
 * This is the single mechanism for surah-name localisation outside the Mushaf
 * page itself (which resolves the name from the page it is already rendering).
 *
 * Resolution is non-blocking. The names live in a process-wide cache warmed off
 * the main thread at startup ([AlkahfApplication]); the steady state is a cache
 * hit that seeds the resolver synchronously, with no recomposition and no
 * first-frame flash of bare numbers. On the rare cold-start race where the
 * preload has not finished, the cache is loaded asynchronously here (off the
 * main thread, never blocking composition) and the resolver recomposes its
 * callers once the names land. The riwāyah only changes by restarting the
 * process, which recomposes this with the new reading.
 */
@Composable
fun rememberSurahNamer(): (Int) -> String {
    val context = LocalContext.current
    val repository = (context.applicationContext as AlkahfApplication).repository
    val arabic = LocalConfiguration.current.locales[0].language == "ar"
    val riwayah = repository.riwayah

    // Seed synchronously from the cache when it is already in memory (the steady
    // state after startup preload) — no blocking, no flash. On a cache miss the
    // initial value is null and produceState loads the names off the main thread,
    // updating this State (and recomposing callers) when they arrive.
    val names: State<Map<Int, SurahName>?> =
        produceState<Map<Int, SurahName>?>(
            initialValue = repository.cachedSurahNames(riwayah),
            riwayah,
        ) {
            if (value == null) value = repository.loadSurahNames(riwayah)
        }
    val resolved by names

    // Localise once per (names, language) instead of on every lookup.
    val localised = remember(resolved, arabic) {
        resolved?.mapValues { (_, name) -> if (arabic) name.arabic else name.latin }
    }

    // Until the names are loaded, resolve to an empty string rather than a bare
    // number: callers must never render a number where a name belongs, and this
    // gap is at most a brief cold-start window before produceState recomposes
    // with the real names. An unknown/out-of-range number also falls back to
    // empty rather than crashing.
    return { number -> localised?.get(number) ?: "" }
}
