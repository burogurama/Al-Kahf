package app.alkahf.ui.mushaf

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.media3.exoplayer.ExoPlayer
import app.alkahf.AlkahfApplication
import app.alkahf.R
import app.alkahf.data.AyahRange
import app.alkahf.data.MushafPage
import app.alkahf.data.PageAyah
import app.alkahf.data.PageGroup
import app.alkahf.data.MemorizationState
import app.alkahf.data.QuranRepository
import app.alkahf.data.Riwayah
import app.alkahf.data.ReciterStatus
import app.alkahf.data.SurahOption
import app.alkahf.data.audio.AudioStore
import app.alkahf.ui.theme.AlkahfColors
import app.alkahf.ui.theme.LocalQuranFont
import app.alkahf.ui.theme.quranFontFor
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Character range of one word or end-of-ayah marker inside a group's text. */
private data class PageSpan(
    val ayahId: Int,
    val wordIndex: Int,
    val start: Int,
    val end: Int,
    val isMarker: Boolean,
)

private data class GroupText(val annotated: AnnotatedString, val spans: List<PageSpan>)

/** First-launch fallback before any page has been read: the current sabaq surah. */
private const val DEFAULT_SURAH = 18

/** The mushaf body font size (pt/sp), set from Settings. */
val LocalMushafTextSize = androidx.compose.runtime.compositionLocalOf { 24 }

/**
 * Hide/reveal progress and stumbles for one page's self-test. Reveal changes
 * are reported through [onRevealChanged] so they persist and the test resumes
 * where it was left.
 */
class SelfTestSession(
    val page: MushafPage,
    private val onRevealChanged: (ayahId: Int, revealedCount: Int) -> Unit,
) {
    val revealedCounts = mutableStateMapOf<Int, Int>()
    val memStates = mutableStateMapOf<Int, MemorizationState>()

    fun revealedOf(ayah: PageAyah): Int = revealedCounts.getOrDefault(ayah.id, 0)

    val currentAyah: PageAyah?
        get() = page.ayahs.firstOrNull { revealedOf(it) < it.words.size }

    /** Reveals through the tapped word (concealment is prefix-based). */
    fun revealUpTo(ayahId: Int, wordIndex: Int) {
        val ayah = page.ayahs.first { it.id == ayahId }
        val target = (wordIndex + 1).coerceIn(1, ayah.words.size)
        if (target > revealedCounts.getOrDefault(ayahId, 0)) {
            revealedCounts[ayahId] = target
            onRevealChanged(ayahId, target)
        }
    }

    fun revealAyah(ayahId: Int) {
        val size = page.ayahs.first { it.id == ayahId }.words.size
        revealedCounts[ayahId] = size
        onRevealChanged(ayahId, size)
    }

    fun resetReveals() {
        revealedCounts.clear()
    }
}

@Composable
fun MushafScreen(
    startSurah: Int? = null,
    startPage: Int? = null,
    highlightRange: AyahRange? = null,
    onBack: () -> Unit = {},
    onImportReciter: (ReciterStatus) -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as AlkahfApplication).repository }
    val scope = rememberCoroutineScope()
    val settings = remember { repository.settings() }
    // Keep the screen awake while reading, when enabled in Settings.
    if (settings.keepScreenOn) {
        val view = androidx.compose.ui.platform.LocalView.current
        DisposableEffect(Unit) {
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = false }
        }
    }
    // Opening the sabaq lands in reading mode (text shown); otherwise reopen
    // in whichever mode the Mushaf was last left in.
    var hideMode by remember {
        mutableStateOf(if (highlightRange != null) false else repository.lastMushafHideMode)
    }
    // Sabaq focus mode: opening for the sabaq (a highlight range) locks the
    // reader to it — only its āyāt can be read, revealed, selected, or listened
    // to; everything else is blurred, and the pager can't leave the pages the
    // sabaq spans.
    val sabaqMode = highlightRange != null
    val sabaqIds = remember(highlightRange) { highlightRange?.ayahIds ?: emptySet() }
    val sabaqOrderedIds = remember(highlightRange) {
        highlightRange?.let { r -> (r.from..r.to).map { r.surah * 1000 + it } } ?: emptyList()
    }
    // Two āyāt of context on each side of the sabaq: shown (muted), but not part
    // of the sabaq — not concealed, selectable, or markable.
    val contextIds = remember(highlightRange) {
        highlightRange?.let { r ->
            listOf(r.from - 2, r.from - 1, r.to + 1, r.to + 2)
                .filter { it >= 1 }
                .map { r.surah * 1000 + it }
                .toSet()
        } ?: emptySet()
    }
    val scrollToAyahId = highlightRange?.let { it.surah * 1000 + it.from }

    // Pager window as (firstPage, pageCount, openAt). In sabaq mode the window
    // spans the pages of the sabaq plus its context āyāt (so the context is
    // reachable); otherwise the whole mushaf, opened at the explicit target.
    val pageSetup by produceState<Triple<Int, Int, Int>?>(initialValue = null) {
        value = if (highlightRange != null) {
            val r = highlightRange
            val len = repository.surahAyahCount(r.surah)
            val a = repository.pageOfAyah(r.surah, (r.from - 2).coerceAtLeast(1))
            val b = repository.pageOfAyah(r.surah, (r.to + 2).coerceAtMost(len))
            val lo = minOf(a, b)
            val hi = maxOf(a, b)
            val openAt = repository.pageOfAyah(r.surah, r.from).coerceIn(lo, hi)
            Triple(lo, hi - lo + 1, openAt)
        } else {
            val start = startPage
                ?: startSurah?.let { repository.firstPageOfSurah(it) }
                ?: repository.lastMushafPage
                ?: repository.firstPageOfSurah(DEFAULT_SURAH)
            Triple(1, QuranRepository.PAGE_COUNT, start)
        }
    }
    val setup = pageSetup
    if (setup == null) {
        Box(Modifier.fillMaxSize().background(AlkahfColors.Paper))
        return
    }
    val (windowStart, windowCount, openAtPage) = setup

    val pagerState = rememberPagerState(
        initialPage = openAtPage - windowStart,
        pageCount = { windowCount },
    )
    val sessions = remember { mutableStateMapOf<Int, SelfTestSession>() }
    val currentPageNumber = pagerState.currentPage + windowStart
    val currentSession = sessions[currentPageNumber]
    // A temporary peek at the other reading — local to this Mushaf; it never
    // changes the saved riwāyah. Text, font, reciters and audio follow it here.
    var displayRiwayah by remember { mutableStateOf(repository.riwayah) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (!sabaqMode) repository.lastMushafPage = page + windowStart
        }
    }

    val audioController = remember {
        val reciter = repository.activeReciter
        MushafAudioController(
            context = context.applicationContext,
            repository = repository,
            audioStore = AudioStore(context.applicationContext),
            player = ExoPlayer.Builder(context).build(),
            coroutineScope = scope,
            reciterPath = reciter.path,
            reciterName = reciter.displayName,
        )
    }
    DisposableEffect(Unit) {
        onDispose { audioController.release() }
    }
    val audioState by audioController.state.collectAsState()
    var audioDockOpen by remember { mutableStateOf(false) }

    // Audio modes take priority over reveal/stumble taps while the dock is open.
    val onAudioTap: (Int) -> Boolean = { ayahId ->
        when {
            !audioDockOpen -> false
            audioState.mode == MushafAudioMode.TAP -> {
                audioController.playSingle(ayahId)
                true
            }
            audioState.scope == MushafAudioScope.FROM_AYAH -> {
                scope.launch {
                    val ids = repository
                        .ayahsForRange(ayahId / 1000, ayahId % 1000, 300)
                        .map { it.id }
                    audioController.playAyahIds(ids)
                }
                true
            }
            else -> false
        }
    }

    val persistReveal: (Int, Int) -> Unit = remember {
        { ayahId, count -> scope.launch { repository.saveRevealState(ayahId, count) } }
    }

    // Reader-mode selection: a contiguous index range into the current page's
    // ayat, for marking several at once. Cleared on page change / mode toggle.
    var selection by remember { mutableStateOf<IntRange?>(null) }
    // Floating action menu for the selection: anchored at the long-press point
    // in window coordinates (null = closed).
    var menuAnchor by remember { mutableStateOf<IntOffset?>(null) }
    // The range-listening config sheet (long-press the headset with a selection).
    var rangeAudioOpen by remember { mutableStateOf(false) }
    var rangeMode by remember { mutableStateOf(MushafAudioMode.LISTEN) }
    var rangeSpeed by remember { mutableStateOf(1f) }
    var rangeTimes by remember { mutableStateOf(1) }
    // Reciters available in the config panel (built-in + imported) and the one
    // currently chosen for the range. Refreshed whenever the panel opens.
    var reciters by remember { mutableStateOf<List<ReciterStatus>>(emptyList()) }
    var rangeReciterKey by remember { mutableStateOf(repository.activeReciterPath) }
    LaunchedEffect(rangeAudioOpen, displayRiwayah) {
        if (rangeAudioOpen) {
            reciters = repository.reciterStatuses(displayRiwayah)
            if (reciters.none { it.key == rangeReciterKey }) {
                rangeReciterKey = reciters.firstOrNull()?.key ?: rangeReciterKey
            }
        }
    }
    // Toggling the reading updates the reciter list and the audio mapping for
    // this Mushaf; the page text reloads via the toggle handler and the font
    // follows LocalQuranFont below.
    LaunchedEffect(displayRiwayah) {
        audioController.setRiwayah(displayRiwayah)
        reciters = repository.reciterStatuses(displayRiwayah)
        rangeReciterKey = reciters.firstOrNull { it.key == rangeReciterKey }?.key
            ?: reciters.firstOrNull()?.key ?: rangeReciterKey
    }
    // Pending download confirmation: (reciter, surah, from, to) once the user
    // hits Listen on an undownloaded built-in range; null while idle.
    var downloadPrompt by remember { mutableStateOf<RangeDownloadRequest?>(null) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var downloadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    LaunchedEffect(currentPageNumber) {
        selection = null
        menuAnchor = null
        rangeAudioOpen = false
    }
    val orderedSelectedIds: List<Int> = currentSession?.page?.ayahs?.let { ayahs ->
        // Bounds-guard: during a page swap the old range may not fit the new page.
        selection?.takeIf { it.first >= 0 && it.last < ayahs.size }
            ?.let { range -> ayahs.slice(range).map { it.id } }
    } ?: emptyList()
    val selectedIds = orderedSelectedIds.toSet()
    // The surah whose "start learning" confirmation is open (header tapped).
    var learnSurah by remember { mutableStateOf<Pair<Int, String>?>(null) }
    // The "go to" sheet (jump to a surah or page) for the regular mushaf.
    var showJump by remember { mutableStateOf(false) }
    val surahOptions by produceState(initialValue = emptyList<SurahOption>()) {
        value = repository.surahOptions()
    }
    fun jumpToPage(page: Int) {
        val target = page.coerceIn(windowStart, windowStart + windowCount - 1)
        scope.launch { pagerState.scrollToPage(target - windowStart) }
        showJump = false
    }

    // Re-pull the current page's memorization states after a bulk change.
    suspend fun reloadMemStates() {
        currentSession?.let { s ->
            val st = repository.ayahStatesForPage(s.page.ayahs.map { it.id })
            s.memStates.clear()
            s.memStates.putAll(st)
        }
    }

    val onSelectAyah: (Int) -> Unit = onSelect@{ ayahId ->
        val ayahs = currentSession?.page?.ayahs ?: return@onSelect
        val index = ayahs.indexOfFirst { it.id == ayahId }
        if (index < 0) return@onSelect
        val sel = selection
        selection = when {
            sel == null -> index..index
            index == sel.first - 1 -> index..sel.last
            index == sel.last + 1 -> sel.first..index
            sel.first == sel.last && index == sel.first -> null
            index == sel.first -> (sel.first + 1)..sel.last
            index == sel.last -> sel.first..(sel.last - 1)
            index in sel -> index..index
            else -> index..index
        }
    }

    // Audio targets the current selection, or the whole sabaq when nothing is
    // selected in sabaq mode (so the headset always listens to the sabaq there).
    fun audioRangeIds(): List<Int> = orderedSelectedIds.ifEmpty { sabaqOrderedIds }

    fun playRange(mode: MushafAudioMode) {
        val ids = audioRangeIds()
        if (ids.isEmpty()) return
        audioController.setMode(mode)
        audioController.setSpeed(rangeSpeed)
        audioController.playAyahIds(ids, repeat = rangeTimes)
        rangeMode = mode
    }

    // Listen pressed in the config panel. Applies the chosen reciter, then:
    //  - imported reciter whose sūrah isn't imported+timed → route to import/time
    //  - built-in reciter with audio not yet cached → confirm + download, play after
    //  - otherwise → play immediately.
    fun onRangeListen() {
        val ids = audioRangeIds()
        if (ids.isEmpty()) return
        val chosen = reciters.firstOrNull { it.key == rangeReciterKey }
        if (chosen == null) {
            rangeAudioOpen = false
            playRange(rangeMode)
            return
        }
        if (chosen.isImported) {
            // Imported reciter: play from the Tawqīt timings if the range is
            // timed (partial timings are fine for their covered āyāt); otherwise
            // route the user to import + time the sūrah.
            val surah = ids.first() / 1000
            val nums = ids.filter { it / 1000 == surah }.map { it % 1000 }
            scope.launch {
                val playback = repository.importedRangePlayback(chosen.key, surah, nums.min(), nums.max())
                rangeAudioOpen = false
                if (playback != null) {
                    audioController.setMode(rangeMode)
                    audioController.setSpeed(rangeSpeed)
                    audioController.playImported(
                        playback.fileUri,
                        playback.ayahIds,
                        playback.segments,
                        repeat = rangeTimes,
                    )
                } else {
                    onImportReciter(chosen)
                }
            }
            return
        }
        // Built-in reciter.
        repository.setActiveReciter(chosen.key)
        audioController.setReciter(chosen.key, chosen.displayName)
        val surah = ids.first() / 1000
        val nums = ids.filter { it / 1000 == surah }.map { it % 1000 }
        val from = nums.min()
        val to = nums.max()
        scope.launch {
            val ready = repository.rangeAudioAvailable(chosen.key, surah, from, to, displayRiwayah)
            if (ready) {
                rangeAudioOpen = false
                playRange(rangeMode)
            } else {
                downloadPrompt = RangeDownloadRequest(
                    reciterKey = chosen.key,
                    reciterName = chosen.displayName,
                    surah = surah,
                    from = from,
                    to = to,
                    count = ids.size,
                )
            }
        }
    }

    fun startRangeDownload(request: RangeDownloadRequest) {
        downloadProgress = 0f
        downloadJob = scope.launch {
            try {
                repository.downloadRange(
                    request.reciterKey, request.surah, request.from, request.to, displayRiwayah,
                ) {
                    downloadProgress = it
                }
                downloadProgress = null
                downloadPrompt = null
                downloadJob = null
                rangeAudioOpen = false
                playRange(rangeMode)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                downloadProgress = null
                downloadPrompt = null
                downloadJob = null
            }
        }
    }

    fun cancelRangeDownload() {
        downloadJob?.cancel()
        downloadJob = null
        downloadProgress = null
        downloadPrompt = null
    }

    // Long-pressing an ayah in reading mode opens the floating action menu,
    // re-selecting that ayah alone when it lies outside the current selection.
    val onAyahLongPress: (Int, IntOffset) -> Unit = { ayahId, offset ->
        val ayahs = currentSession?.page?.ayahs
        val index = ayahs?.indexOfFirst { it.id == ayahId } ?: -1
        if (index >= 0) {
            val sel = selection
            if (sel == null || index !in sel) selection = index..index
            menuAnchor = offset
        }
    }

    fun toggleAudioDock() {
        val opening = !audioDockOpen
        audioDockOpen = opening
        if (!opening) audioController.stop()
    }
    // The headset overrides its utility when a range is selected: tap plays the
    // range, double-tap plays it recite-back, long-press opens the range config.
    val onHeadsetTap = {
        if (audioRangeIds().isNotEmpty()) playRange(MushafAudioMode.LISTEN) else toggleAudioDock()
    }
    val onHeadsetDoubleTap = {
        if (audioRangeIds().isNotEmpty()) playRange(MushafAudioMode.RECITE_BACK) else Unit
    }
    val onHeadsetLongPress = {
        if (audioRangeIds().isNotEmpty()) {
            // Long-press toggles the config open/closed (closing it doesn't play;
            // only the Listen button starts playback).
            audioDockOpen = false
            rangeAudioOpen = !rangeAudioOpen
        } else {
            toggleAudioDock()
        }
    }

    CompositionLocalProvider(LocalQuranFont provides quranFontFor(displayRiwayah)) {
        Column(Modifier.fillMaxSize().background(AlkahfColors.Paper)) {
            MushafTopBar(
                title = currentSession?.page?.primarySurahLatin ?: "",
                location = currentSession?.page?.let {
                    stringResource(R.string.mushaf_location, it.juz, it.number)
                } ?: "",
                // Tapping the title opens "go to"; the locked sabaq view can't jump.
                onTitleClick = { showJump = true }.takeIf { !sabaqMode },
                hideMode = hideMode,
                audioActive = audioDockOpen || rangeAudioOpen,
                onAudioTap = onHeadsetTap,
                onAudioDoubleTap = onHeadsetDoubleTap,
                onAudioLongPress = onHeadsetLongPress,
                onBack = onBack,
                onToggleHideMode = {
                    val turningOn = !hideMode
                    hideMode = turningOn
                    repository.lastMushafHideMode = turningOn
                    selection = null
                    // Re-entering hide mode starts a fresh self-test on this page.
                    if (turningOn) {
                        currentSession?.let { session ->
                            session.resetReveals()
                            scope.launch {
                                repository.clearRevealStates(session.page.ayahs.map { it.id })
                            }
                        }
                    }
                },
                // Temporary reading toggle — hidden in the locked sabaq view.
                riwayahLabel = if (sabaqMode) {
                    null
                } else {
                    stringResource(
                        if (displayRiwayah == Riwayah.WARSH) R.string.settings_riwayah_warsh else R.string.settings_riwayah_hafs,
                    )
                },
                onToggleRiwayah = {
                    // Clear sessions in the same frame as the riwāyah change so the
                    // page reloads with the new text (the load guard checks session).
                    sessions.clear()
                    displayRiwayah = if (displayRiwayah == Riwayah.WARSH) Riwayah.HAFS else Riwayah.WARSH
                }.takeIf { !sabaqMode },
            )
            // RTL pager: swiping toward the right turns to the next page, as in a
            // physical mushaf. Page content restores LTR for its own chrome.
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalMushafTextSize provides settings.arabicTextSizePt,
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) { pageIndex ->
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        val pageNumber = pageIndex + windowStart
                        MushafPageView(
                            pageNumber = pageNumber,
                            riwayah = displayRiwayah,
                            repository = repository,
                            hideMode = hideMode,
                            session = sessions[pageNumber],
                            onSessionReady = { sessions[pageNumber] = it },
                            persistReveal = persistReveal,
                            onSelectAyah = onSelectAyah,
                            onAyahLongPress = onAyahLongPress,
                            // The surah header can't start a new sabaq from inside the
                            // focused sabaq view.
                            onLearnSurah = if (sabaqMode) {
                                { _, _ -> }
                            } else {
                                { surah, name -> learnSurah = surah to name }
                            },
                            playingAyahId = audioState.currentAyahId,
                            onAudioTap = onAudioTap,
                            sabaqIds = sabaqIds,
                            contextIds = contextIds,
                            selectedIds = if (pageNumber == currentPageNumber) selectedIds else emptySet(),
                            scrollToAyahId = scrollToAyahId,
                        )
                    }
                }
            }
            when {
                rangeAudioOpen -> RangeAudioDock(
                    mode = rangeMode,
                    speed = rangeSpeed,
                    times = rangeTimes,
                    reciters = reciters,
                    selectedReciterKey = rangeReciterKey,
                    onMode = { rangeMode = it },
                    onSpeed = { rangeSpeed = it },
                    onTimes = { rangeTimes = it },
                    onReciter = { rangeReciterKey = it },
                    onListen = { onRangeListen() },
                )
                audioDockOpen -> MushafAudioDock(
                    state = audioState,
                    onMode = audioController::setMode,
                    onScope = audioController::setScope,
                    onPlayPause = {
                        if (audioState.phase != MushafAudioPhase.IDLE) {
                            audioController.togglePause()
                        } else {
                            val session = currentSession
                            when {
                                audioState.mode == MushafAudioMode.TAP -> {}
                                audioState.scope == MushafAudioScope.PAGE -> session?.let {
                                    audioController.playAyahIds(it.page.ayahs.map { ayah -> ayah.id })
                                }
                                audioState.scope == MushafAudioScope.SURAH -> session?.let {
                                    val surah = it.page.ayahs.first().surah
                                    scope.launch {
                                        audioController.playAyahIds(
                                            repository.ayahsForRange(surah, 1, 300).map { ayah -> ayah.id },
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                    },
                    onStop = audioController::stop,
                )
                selectedIds.isNotEmpty() -> SelectionHintBar(
                    count = selectedIds.size,
                    playing = audioState.phase != MushafAudioPhase.IDLE,
                    onStop = audioController::stop,
                    onClear = { selection = null },
                )
                hideMode -> SelfTestDock(session = currentSession)
            }
        }

        val anchor = menuAnchor
        val menuSession = currentSession
        if (anchor != null && menuSession != null && selectedIds.isNotEmpty()) {
            val currentState = selectedIds
                .map { menuSession.memStates[it] ?: MemorizationState.NOT_STARTED }
                .distinct()
                .singleOrNull()
            SelectionContextMenu(
                anchor = anchor,
                currentState = currentState,
                onListen = {
                    playRange(MushafAudioMode.LISTEN)
                    menuAnchor = null
                },
                onSetSabaq = {
                    val picked = menuSession.page.ayahs.filter { it.id in selectedIds }
                    if (picked.isNotEmpty()) {
                        val surah = picked.first().surah
                        val nums = picked.filter { it.surah == surah }.map { it.number }
                        scope.launch {
                            repository.setSabaqToRange(surah, nums.min(), nums.max())
                            reloadMemStates()
                        }
                    }
                    selection = null
                    menuAnchor = null
                },
                onSetState = { state ->
                    val ids = selectedIds.toList()
                    ids.forEach { menuSession.memStates[it] = state }
                    scope.launch {
                        ids.forEach { repository.setAyahState(it, state) }
                        // Memorizing the sabaq's ayat may complete its section.
                        repository.maybeAdvanceSabaq()
                    }
                    selection = null
                    menuAnchor = null
                },
                onDismiss = { menuAnchor = null },
            )
        }

        val prompt = learnSurah
        if (prompt != null) {
            LearnSurahSheet(
                surahName = prompt.second,
                onConfirm = {
                    scope.launch {
                        repository.startLearningSurah(prompt.first)
                        reloadMemStates()
                    }
                    learnSurah = null
                },
                onDismiss = { learnSurah = null },
            )
        }

        if (showJump) {
            GoToSheet(
                surahs = surahOptions,
                onPickSurah = { surah ->
                    scope.launch { jumpToPage(repository.firstPageOfSurah(surah)) }
                },
                onPickPage = { page -> jumpToPage(page) },
                onDismiss = { showJump = false },
            )
        }

        val request = downloadPrompt
        if (request != null) {
            RangeDownloadDialog(
                reciterName = request.reciterName,
                count = request.count,
                progress = downloadProgress,
                onConfirm = { startRangeDownload(request) },
                onCancel = { cancelRangeDownload() },
            )
        }
    }
}

/** A pending built-in-reciter range download awaiting the user's confirmation. */
private data class RangeDownloadRequest(
    val reciterKey: String,
    val reciterName: String,
    val surah: Int,
    val from: Int,
    val to: Int,
    val count: Int,
)

@Composable
private fun MushafTopBar(
    title: String,
    location: String,
    onTitleClick: (() -> Unit)? = null,
    hideMode: Boolean,
    audioActive: Boolean,
    onAudioTap: () -> Unit,
    onAudioDoubleTap: () -> Unit,
    onAudioLongPress: () -> Unit,
    onBack: () -> Unit,
    onToggleHideMode: () -> Unit,
    riwayahLabel: String? = null,
    onToggleRiwayah: (() -> Unit)? = null,
) {
    // The headset gesture detector lives in a pointerInput(Unit) that never
    // restarts, so capture the latest handlers to avoid acting on stale state.
    val currentAudioTap by rememberUpdatedState(onAudioTap)
    val currentAudioDoubleTap by rememberUpdatedState(onAudioDoubleTap)
    val currentAudioLongPress by rememberUpdatedState(onAudioLongPress)
    Column(Modifier.fillMaxWidth().background(AlkahfColors.Paper).statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.common_back),
                    tint = AlkahfColors.InkChrome,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onTitleClick != null) {
                            Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onTitleClick)
                        } else {
                            Modifier
                        },
                    )
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                    letterSpacing = (-0.2).sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = location,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AlkahfColors.InkFaint,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                    if (onTitleClick != null) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.mushaf_goto_title),
                            tint = AlkahfColors.InkFaint,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            if (riwayahLabel != null && onToggleRiwayah != null) {
                Surface(
                    onClick = onToggleRiwayah,
                    shape = RoundedCornerShape(8.dp),
                    color = AlkahfColors.AccentTint,
                    modifier = Modifier.padding(end = 2.dp),
                ) {
                    Text(
                        text = riwayahLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlkahfColors.AccentDeep,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    )
                }
            }
            Box(
                modifier = Modifier.size(40.dp).pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { currentAudioTap() },
                        onDoubleTap = { currentAudioDoubleTap() },
                        onLongPress = { currentAudioLongPress() },
                    )
                },
                contentAlignment = Alignment.Center,
            ) {
                if (audioActive) {
                    Surface(shape = CircleShape, color = AlkahfColors.AccentTint) {
                        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Headphones,
                                contentDescription = stringResource(R.string.mushaf_listening_on),
                                tint = AlkahfColors.AccentDeep,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Headphones,
                        contentDescription = stringResource(R.string.mushaf_listening_off),
                        tint = AlkahfColors.InkMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Box(
                modifier = Modifier.size(40.dp).clickable(onClick = onToggleHideMode),
                contentAlignment = Alignment.Center,
            ) {
                if (hideMode) {
                    Surface(shape = CircleShape, color = AlkahfColors.AccentTint) {
                        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.VisibilityOff,
                                contentDescription = stringResource(R.string.mushaf_hide_mode_on),
                                tint = AlkahfColors.AccentDeep,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = stringResource(R.string.mushaf_hide_mode_off),
                        tint = AlkahfColors.InkMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Box(
                modifier = Modifier.size(40.dp).clickable { /* TODO: font-size controls */ },
                contentAlignment = Alignment.Center,
            ) {
                Row {
                    Text(
                        text = "A",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlkahfColors.InkChrome,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        text = "A",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlkahfColors.InkChrome,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = AlkahfColors.Hairline)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MushafPageView(
    pageNumber: Int,
    riwayah: Riwayah,
    repository: QuranRepository,
    hideMode: Boolean,
    session: SelfTestSession?,
    onSessionReady: (SelfTestSession) -> Unit,
    persistReveal: (Int, Int) -> Unit,
    onSelectAyah: (Int) -> Unit,
    onAyahLongPress: (Int, IntOffset) -> Unit,
    onLearnSurah: (Int, String) -> Unit,
    playingAyahId: Int?,
    onAudioTap: (Int) -> Boolean,
    sabaqIds: Set<Int>,
    contextIds: Set<Int>,
    selectedIds: Set<Int>,
    scrollToAyahId: Int?,
) {
    LaunchedEffect(pageNumber, riwayah) {
        if (session == null) {
            val page = repository.page(pageNumber, riwayah)
            val newSession = SelfTestSession(page, persistReveal)
            val ayahIds = page.ayahs.map { it.id }
            newSession.revealedCounts.putAll(repository.revealStates(ayahIds))
            newSession.memStates.putAll(repository.ayahStatesForPage(ayahIds))
            onSessionReady(newSession)
        }
    }
    if (session == null) {
        Box(Modifier.fillMaxSize().background(AlkahfColors.PageSurface))
        return
    }

    val bringIntoView = remember { BringIntoViewRequester() }
    // The group holding the target ayah scrolls itself into view once.
    val targetGroup = scrollToAyahId?.let { id ->
        session.page.groups.firstOrNull { g -> g.ayahs.any { it.id == id } }
    }
    LaunchedEffect(targetGroup, session.page.number) {
        if (targetGroup != null) bringIntoView.bringIntoView()
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(AlkahfColors.PageSurface)) {
        val minHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight)
                .padding(start = 24.dp, top = 14.dp, end = 24.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                session.page.groups.forEach { group ->
                    val groupModifier = if (group == targetGroup) {
                        Modifier.bringIntoViewRequester(bringIntoView)
                    } else {
                        Modifier
                    }
                    Column(groupModifier) {
                        PageGroupView(
                            group = group,
                            hideMode = hideMode,
                            session = session,
                            playingAyahId = playingAyahId,
                            onAudioTap = onAudioTap,
                            onSelectAyah = onSelectAyah,
                            onAyahLongPress = onAyahLongPress,
                            onLearnSurah = onLearnSurah,
                            sabaqIds = sabaqIds,
                            contextIds = contextIds,
                            selectedIds = selectedIds,
                        )
                    }
                }
            }
            PageFooter(session.page)
        }
    }
}

@Composable
private fun PageGroupView(
    group: PageGroup,
    hideMode: Boolean,
    session: SelfTestSession,
    playingAyahId: Int?,
    onAudioTap: (Int) -> Boolean,
    onSelectAyah: (Int) -> Unit,
    onAyahLongPress: (Int, IntOffset) -> Unit,
    onLearnSurah: (Int, String) -> Unit,
    sabaqIds: Set<Int>,
    contextIds: Set<Int>,
    selectedIds: Set<Int>,
) {
    if (group.showSurahHeader) {
        SurahHeaderBand(group, onClick = { onLearnSurah(group.surahNumber, group.surahNameLatin) })
    }
    if (group.basmala != null) {
        Text(
            text = group.basmala,
            fontFamily = LocalQuranFont.current,
            fontSize = 20.sp,
            color = AlkahfColors.InkChrome,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 10.dp),
        )
    }
    AyatBody(group, hideMode, session, playingAyahId, onAudioTap, onSelectAyah, onAyahLongPress, sabaqIds, contextIds, selectedIds)
}

@Composable
private fun SurahHeaderBand(group: PageGroup, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, AlkahfColors.HeaderRule),
                    ),
                ),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = group.surahNameArabic,
                fontFamily = LocalQuranFont.current,
                fontSize = 25.sp,
                lineHeight = 34.sp,
                color = AlkahfColors.Ink,
                maxLines = 1,
            )
            Text(
                text = group.surahMeta,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = AlkahfColors.InkFooter,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(AlkahfColors.HeaderRule, Color.Transparent),
                    ),
                ),
        )
    }
}

@Composable
private fun AyatBody(
    group: PageGroup,
    hideMode: Boolean,
    session: SelfTestSession,
    playingAyahId: Int?,
    onAudioTap: (Int) -> Boolean,
    onSelectAyah: (Int) -> Unit,
    onAyahLongPress: (Int, IntOffset) -> Unit,
    sabaqIds: Set<Int>,
    contextIds: Set<Int>,
    selectedIds: Set<Int>,
) {
    val revealedByAyah = group.ayahs.associate { it.id to session.revealedOf(it) }
    val memByAyah = group.ayahs.associate { it.id to (session.memStates[it.id] ?: MemorizationState.NOT_STARTED) }
    // Fill is drawn only behind the selection and the playing ayah; the sabaq is
    // set apart by blurring everything else, not by a highlight of its own.
    val litIds = remember(selectedIds, playingAyahId) {
        selectedIds + listOfNotNull(playingAyahId)
    }
    val groupText = remember(revealedByAyah, hideMode, memByAyah, litIds, sabaqIds, contextIds) {
        buildGroupText(group, revealedByAyah, hideMode, memByAyah, litIds, sabaqIds, contextIds)
    }
    val currentOnAudioTap by rememberUpdatedState(onAudioTap)
    val currentOnSelect by rememberUpdatedState(onSelectAyah)
    val currentOnLongPress by rememberUpdatedState(onAyahLongPress)
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val bodySize = LocalMushafTextSize.current.sp
    // The āyah currently being recited gets its own fill so it stands out from a
    // selection (which shares the green fill).
    val selectionFill = AlkahfColors.AyahHighlightFill
    val nowPlayingFill = AlkahfColors.NowPlayingFill

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = groupText.annotated,
            style = TextStyle(
                fontFamily = LocalQuranFont.current,
                fontSize = bodySize,
                lineHeight = bodySize * 1.9f,
                color = AlkahfColors.Ink,
                textAlign = TextAlign.Justify,
            ),
            onTextLayout = { layoutResult.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords = it }
                .drawBehind {
                    val layout = layoutResult.value ?: return@drawBehind
                    val padH = 6.dp.toPx()
                    val padV = 3.dp.toPx()
                    val radius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                    litIds.forEach { ayahId ->
                        val ayahSpans = groupText.spans.filter { it.ayahId == ayahId }
                        if (ayahSpans.isEmpty()) return@forEach
                        val fill = if (ayahId == playingAyahId) nowPlayingFill else selectionFill
                        val start = ayahSpans.minOf { it.start }
                        val end = ayahSpans.maxOf { it.end }
                        // One rounded rect per wrapped line the ayah covers.
                        val firstLine = layout.getLineForOffset(start)
                        val lastLine = layout.getLineForOffset((end - 1).coerceAtLeast(start))
                        for (line in firstLine..lastLine) {
                            val lineStart = layout.getLineStart(line)
                            val lineEnd = layout.getLineEnd(line, visibleEnd = true)
                            val ls = maxOf(start, lineStart)
                            val le = minOf(end, lineEnd)
                            if (le <= ls) continue
                            val b = layout.getPathForRange(ls, le).getBounds()
                            // Extend to the justified line edge wherever the ayah
                            // spans that boundary, so the fill doesn't stop short.
                            val left = if (end >= lineEnd) layout.getLineLeft(line) else b.left
                            val right = if (start <= lineStart) layout.getLineRight(line) else b.right
                            drawRoundRect(
                                color = fill,
                                topLeft = Offset(left - padH, b.top - padV),
                                size = androidx.compose.ui.geometry.Size(
                                    (right - left) + padH * 2,
                                    b.height + padV * 2,
                                ),
                                cornerRadius = radius,
                            )
                        }
                    }
                }
                .pointerInput(hideMode, groupText) {
                    detectTapGestures(
                        onTap = { position ->
                            val span = spanAt(layoutResult.value, groupText, position)
                            val ayahId = span?.ayahId
                                ?: markerAyahIdAt(layoutResult.value, groupText, position)
                                ?: return@detectTapGestures
                            // In sabaq mode only the sabaq's āyāt respond; the rest
                            // stay blurred and locked (can't be revealed/selected).
                            if (sabaqIds.isNotEmpty() && ayahId !in sabaqIds) return@detectTapGestures
                            if (currentOnAudioTap(ayahId)) return@detectTapGestures
                            if (hideMode) {
                                // Reveal through the tapped word, or the whole
                                // ayah when its medallion is tapped.
                                if (span != null) {
                                    session.revealUpTo(span.ayahId, span.wordIndex)
                                } else {
                                    session.revealAyah(ayahId)
                                }
                            } else {
                                currentOnSelect(ayahId)
                            }
                        },
                        onLongPress = { position ->
                            val ayahId = spanAt(layoutResult.value, groupText, position)?.ayahId
                                ?: markerAyahIdAt(layoutResult.value, groupText, position)
                                ?: return@detectTapGestures
                            if (sabaqIds.isNotEmpty() && ayahId !in sabaqIds) return@detectTapGestures
                            if (hideMode) {
                                session.revealAyah(ayahId)
                            } else {
                                // Open the floating action menu at the press
                                // point, in window coordinates.
                                val win = coords?.localToWindow(position) ?: return@detectTapGestures
                                currentOnLongPress(
                                    ayahId,
                                    IntOffset(win.x.roundToInt(), win.y.roundToInt()),
                                )
                            }
                        },
                    )
                },
        )
    }
}

private fun spanAt(
    layout: TextLayoutResult?,
    groupText: GroupText,
    position: Offset,
): PageSpan? {
    val offset = layout?.getOffsetForPosition(position) ?: return null
    return groupText.spans.firstOrNull { !it.isMarker && offset in it.start until it.end }
}

private fun markerAyahIdAt(
    layout: TextLayoutResult?,
    groupText: GroupText,
    position: Offset,
): Int? {
    val offset = layout?.getOffsetForPosition(position) ?: return null
    return groupText.spans.firstOrNull { it.isMarker && offset in it.start until it.end }?.ayahId
}

/** Page medallion color in reading mode: by memorization state. */
private fun memMedallionColor(state: MemorizationState): Color = when (state) {
    MemorizationState.STRONG -> AlkahfColors.AccentDeep
    MemorizationState.MEMORIZED -> AlkahfColors.AccentLight
    MemorizationState.LEARNING -> AlkahfColors.Learning
    MemorizationState.NOT_STARTED -> AlkahfColors.ConcealedMedallion
}

private fun buildGroupText(
    group: PageGroup,
    revealedByAyah: Map<Int, Int>,
    hideMode: Boolean,
    memByAyah: Map<Int, MemorizationState> = emptyMap(),
    litIds: Set<Int> = emptySet(),
    sabaqIds: Set<Int> = emptySet(),
    contextIds: Set<Int> = emptySet(),
): GroupText {
    val sabaqMode = sabaqIds.isNotEmpty()
    val spans = mutableListOf<PageSpan>()
    val annotated = buildAnnotatedString {
        group.ayahs.forEachIndexed { ayahIndex, ayah ->
            val revealed = revealedByAyah[ayah.id] ?: 0
            // In sabaq mode the sabaq follows the hide/reveal logic, its context
            // āyāt are always shown (muted), and everything else is blurred.
            val inSabaq = !sabaqMode || ayah.id in sabaqIds
            val inContext = sabaqMode && !inSabaq && ayah.id in contextIds
            ayah.words.forEachIndexed { wordIndex, word ->
                val start = length
                append(word)
                spans += PageSpan(ayah.id, wordIndex, start, length, isMarker = false)
                val concealed = when {
                    inSabaq -> hideMode && wordIndex >= revealed
                    inContext -> false
                    else -> true
                }
                // The highlight fill is drawn behind the text; the glyph colour
                // stays constant, except context āyāt, which read muted.
                addStyle(
                    style = if (concealed) {
                        SpanStyle(
                            color = Color.Transparent,
                            shadow = Shadow(
                                color = AlkahfColors.ConcealedInk,
                                offset = Offset.Zero,
                                blurRadius = 14f,
                            ),
                        )
                    } else {
                        SpanStyle(color = if (inContext) AlkahfColors.InkFaint else AlkahfColors.Ink)
                    },
                    start = start,
                    end = length,
                )
                append(' ')
            }
            val markerStart = length
            append(ayah.marker)
            spans += PageSpan(ayah.id, wordIndex = -1, start = markerStart, end = length, isMarker = true)
            // A lit (selected/sabaq/playing) ayah's medallion shifts to accent.
            // Otherwise hide mode tracks reveal state; reading mode shows the
            // ayah's memorization state.
            val markerColor = when {
                ayah.id in litIds -> AlkahfColors.AccentDeep
                inContext -> AlkahfColors.InkFaint
                !inSabaq -> AlkahfColors.ConcealedMedallion
                hideMode -> if (revealed >= ayah.words.size) AlkahfColors.Accent else AlkahfColors.ConcealedMedallion
                else -> memMedallionColor(memByAyah[ayah.id] ?: MemorizationState.NOT_STARTED)
            }
            addStyle(style = SpanStyle(color = markerColor), start = markerStart, end = length)
            if (ayahIndex != group.ayahs.lastIndex) append(' ')
        }
    }
    return GroupText(annotated, spans)
}

@Composable
private fun PageFooter(page: MushafPage) {
    Column(Modifier.padding(top = 20.dp)) {
        HorizontalDivider(thickness = 1.dp, color = AlkahfColors.Hairline)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.mushaf_hizb_juz, page.hizb, page.juz),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
                color = AlkahfColors.InkFooter,
            )
            Text(
                text = page.pageNumberArabic,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.InkMuted,
            )
            Text(
                text = page.primarySurahLatin,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
                color = AlkahfColors.InkFooter,
            )
        }
    }
}

@Composable
private fun SelfTestDock(session: SelfTestSession?) {
    Column(Modifier.fillMaxWidth().background(AlkahfColors.NavSurface)) {
        HorizontalDivider(thickness = 1.dp, color = AlkahfColors.DockBorder)
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulsingDot()
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = stringResource(R.string.mushaf_self_test),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = AlkahfColors.AccentDeep,
                    )
                }
                Text(
                    text = recitingLabel(session),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.InkFaint,
                )
            }
            Text(
                text = stringResource(R.string.mushaf_self_test_hint),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 2.dp),
            )
        }
    }
}

@Composable
private fun recitingLabel(session: SelfTestSession?): String {
    val page = session?.page ?: return ""
    val current = session.currentAyah ?: return stringResource(R.string.mushaf_all_recalled)
    val position = page.ayahs.indexOfFirst { it.id == current.id } + 1
    return stringResource(R.string.mushaf_reciting_ayah, position, page.ayahs.size)
}

/** Positions a popup so its top edge sits just above [anchor] (window px). */
private class AnchorPositionProvider(private val anchor: IntOffset) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val margin = 12
        val x = (anchor.x - popupContentSize.width / 2)
            .coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
        val above = anchor.y - popupContentSize.height - 16
        val y = if (above >= margin) {
            above
        } else {
            (anchor.y + 16).coerceAtMost((windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin))
        }
        return IntOffset(x, y)
    }
}

/**
 * Floating action menu for the current selection, anchored where the user
 * long-pressed: Listen to the range, set it as the sabaq, or expand the state
 * picker to mark the range's memorization state.
 */
@Composable
private fun SelectionContextMenu(
    anchor: IntOffset,
    currentState: MemorizationState?,
    onListen: () -> Unit,
    onSetSabaq: () -> Unit,
    onSetState: (MemorizationState) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        popupPositionProvider = remember(anchor) { AnchorPositionProvider(anchor) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        var stateOpen by remember { mutableStateOf(false) }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AlkahfColors.Paper,
            border = BorderStroke(1.dp, AlkahfColors.CardBorder),
            shadowElevation = 14.dp,
            modifier = Modifier.width(216.dp),
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                MenuItem(label = stringResource(R.string.mushaf_menu_listen), onClick = onListen)
                MenuItem(label = stringResource(R.string.mushaf_menu_set_sabaq), onClick = onSetSabaq)
                HorizontalDivider(
                    thickness = 1.dp,
                    color = AlkahfColors.Hairline,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                MenuItem(
                    label = stringResource(R.string.mushaf_menu_set_state),
                    trailing = if (stateOpen) "▲" else "▼",
                    onClick = { stateOpen = !stateOpen },
                )
                if (stateOpen) {
                    listOf(
                        MemorizationState.NOT_STARTED to stringResource(R.string.state_not_started),
                        MemorizationState.LEARNING to stringResource(R.string.state_learning),
                        MemorizationState.MEMORIZED to stringResource(R.string.state_memorized),
                        MemorizationState.STRONG to stringResource(R.string.state_strong),
                    ).forEach { (state, label) ->
                        StateMenuItem(
                            label = label,
                            color = memMedallionColor(state),
                            selected = state == currentState,
                            onClick = { onSetState(state) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, trailing: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = AlkahfColors.Ink,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(text = trailing, fontSize = 11.sp, color = AlkahfColors.InkMuted)
        }
    }
}

@Composable
private fun StateMenuItem(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(start = 22.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(11.dp).background(color, CircleShape))
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = AlkahfColors.Ink,
            modifier = Modifier.weight(1f).padding(start = 11.dp),
        )
        if (selected) {
            Text(
                text = stringResource(R.string.mushaf_state_current),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.AccentDeep,
            )
        }
    }
}

/** Slim status bar shown while a selection is active (no action buttons). */
@Composable
private fun SelectionHintBar(
    count: Int,
    playing: Boolean,
    onStop: () -> Unit,
    onClear: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(AlkahfColors.NavSurface)) {
        HorizontalDivider(thickness = 1.dp, color = AlkahfColors.DockBorder)
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = pluralStringResource(R.plurals.mushaf_ayah_selected, count, count),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.Ink,
                )
                Text(
                    text = stringResource(R.string.mushaf_selection_hint),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            if (playing) {
                Surface(
                    onClick = onStop,
                    shape = RoundedCornerShape(12.dp),
                    color = AlkahfColors.PageSurface,
                    border = BorderStroke(1.dp, AlkahfColors.ControlBorder),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = stringResource(R.string.common_stop),
                            tint = AlkahfColors.InkChrome,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
            }
            Box(
                modifier = Modifier.clickable(onClick = onClear).padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.common_clear),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.InkMuted,
                )
            }
        }
    }
}

/** Bottom config for listening to the selected range (long-press the headset). */
@Composable
private fun RangeAudioDock(
    mode: MushafAudioMode,
    speed: Float,
    times: Int,
    reciters: List<ReciterStatus>,
    selectedReciterKey: String,
    onMode: (MushafAudioMode) -> Unit,
    onSpeed: (Float) -> Unit,
    onTimes: (Int) -> Unit,
    onReciter: (String) -> Unit,
    onListen: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(AlkahfColors.NavSurface)) {
        HorizontalDivider(thickness = 1.dp, color = AlkahfColors.DockBorder)
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.mushaf_listen_to_selection),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            if (reciters.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.mushaf_reciter_label),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = AlkahfColors.InkMuted,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    reciters.forEach { reciter ->
                        ReciterChip(
                            reciter = reciter,
                            selected = reciter.key == selectedReciterKey,
                            onClick = { onReciter(reciter.key) },
                        )
                    }
                }
            }
            AudioSegmented(
                options = listOf(
                    MushafAudioMode.LISTEN to stringResource(R.string.mushaf_mode_normal),
                    MushafAudioMode.RECITE_BACK to stringResource(R.string.mushaf_mode_recite_back),
                ),
                selected = mode,
                onSelect = onMode,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.mushaf_speed),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = AlkahfColors.InkMuted,
                modifier = Modifier.padding(bottom = 5.dp),
            )
            AudioSegmented(
                options = listOf(0.75f to "0.75×", 1f to "1×", 1.25f to "1.25×", 1.5f to "1.5×"),
                selected = speed,
                onSelect = onSpeed,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.mushaf_repeat),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = AlkahfColors.InkMuted,
                    modifier = Modifier.weight(1f),
                )
                RepeatStepper(times = times, onChange = onTimes)
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                onClick = onListen,
                shape = RoundedCornerShape(14.dp),
                color = AlkahfColors.Accent,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = AlkahfColors.OnAccent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.mushaf_listen_start),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AlkahfColors.OnAccent,
                    )
                }
            }
        }
    }
}

/** A selectable reciter pill: Arabic initial medallion + name (· "imported"). */
@Composable
private fun ReciterChip(
    reciter: ReciterStatus,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) AlkahfColors.AccentTint else AlkahfColors.Surface,
        border = BorderStroke(
            1.dp,
            if (selected) AlkahfColors.Accent else AlkahfColors.CardBorder,
        ),
    ) {
        Row(
            Modifier.padding(start = 8.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(26.dp)
                    .background(
                        if (selected) AlkahfColors.Accent else AlkahfColors.SegmentedTrack,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = reciter.arabicInitial,
                    fontFamily = LocalQuranFont.current,
                    fontSize = 14.sp,
                    color = if (selected) AlkahfColors.OnAccent else AlkahfColors.InkChrome,
                )
            }
            Column(Modifier.padding(start = 8.dp)) {
                Text(
                    text = reciter.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) AlkahfColors.AccentDeep else AlkahfColors.Ink,
                    maxLines = 1,
                )
                if (reciter.isImported) {
                    Text(
                        text = stringResource(R.string.mushaf_reciter_imported),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = AlkahfColors.InkFaint,
                    )
                }
            }
        }
    }
}

/**
 * Confirm-and-download modal for a built-in reciter range that isn't cached.
 * Shows a determinate progress bar once [progress] is non-null; recitation
 * starts only after the download finishes (handled by the caller).
 */
@Composable
private fun RangeDownloadDialog(
    reciterName: String,
    count: Int,
    progress: Float?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val downloading = progress != null
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.34f))
            .clickable(enabled = !downloading, onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = AlkahfColors.Paper,
            border = BorderStroke(1.dp, AlkahfColors.CardBorder),
            shadowElevation = 16.dp,
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .fillMaxWidth()
                .clickable(enabled = false) {},
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.mushaf_download_title),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.mushaf_download_body,
                        count,
                        count,
                        reciterName,
                    ),
                    fontSize = 13.5.sp,
                    color = AlkahfColors.InkMuted,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(bottom = 18.dp),
                )
                if (downloading) {
                    LinearProgressIndicator(
                        progress = { progress ?: 0f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = AlkahfColors.Accent,
                        trackColor = AlkahfColors.SegmentedTrack,
                    )
                    Text(
                        text = stringResource(
                            R.string.mushaf_download_progress,
                            ((progress ?: 0f) * 100).roundToInt(),
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AlkahfColors.InkFaint,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        onClick = onCancel,
                        shape = RoundedCornerShape(14.dp),
                        color = AlkahfColors.Surface,
                        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.common_cancel),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AlkahfColors.Ink,
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onCancel,
                            shape = RoundedCornerShape(14.dp),
                            color = AlkahfColors.Surface,
                            border = BorderStroke(1.dp, AlkahfColors.CardBorder),
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(R.string.common_cancel),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AlkahfColors.Ink,
                                )
                            }
                        }
                        Surface(
                            onClick = onConfirm,
                            shape = RoundedCornerShape(14.dp),
                            color = AlkahfColors.Accent,
                            modifier = Modifier.weight(1f).height(48.dp).padding(start = 10.dp),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = stringResource(R.string.mushaf_download_confirm),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AlkahfColors.OnAccent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Repeat count: a stepper (1–99×) with an ∞ (loop-forever) toggle. */
@Composable
private fun RepeatStepper(times: Int, onChange: (Int) -> Unit) {
    val infinite = times <= 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton("−") { onChange(if (infinite) 1 else (times - 1).coerceAtLeast(1)) }
        Box(Modifier.width(50.dp), contentAlignment = Alignment.Center) {
            Text(
                text = if (infinite) "∞" else "$times×",
                fontSize = if (infinite) 20.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
            )
        }
        StepButton("+") { onChange(if (infinite) 1 else (times + 1).coerceAtMost(99)) }
        Spacer(Modifier.width(8.dp))
        Surface(
            onClick = { onChange(if (infinite) 1 else 0) },
            shape = RoundedCornerShape(12.dp),
            color = if (infinite) AlkahfColors.Accent else AlkahfColors.Surface,
            border = BorderStroke(1.dp, if (infinite) AlkahfColors.Accent else AlkahfColors.CardBorder),
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "∞",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (infinite) AlkahfColors.OnAccent else AlkahfColors.InkMuted,
                )
            }
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.InkChrome)
        }
    }
}

@Composable
private fun LearnSurahSheet(
    surahName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.34f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = AlkahfColors.Paper,
            modifier = Modifier.fillMaxWidth().clickable(enabled = false) {},
        ) {
            Column(
                Modifier.navigationBarsPadding()
                    .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
            ) {
                Box(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.width(38.dp).height(4.dp).background(AlkahfColors.DashedNode, RoundedCornerShape(2.dp)))
                }
                Text(
                    text = stringResource(R.string.mushaf_start_learning, surahName),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Text(
                    text = stringResource(R.string.mushaf_start_learning_body),
                    fontSize = 13.5.sp,
                    color = AlkahfColors.InkMuted,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        color = AlkahfColors.Surface,
                        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.common_cancel),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AlkahfColors.Ink,
                            )
                        }
                    }
                    Surface(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(14.dp),
                        color = AlkahfColors.Accent,
                        modifier = Modifier.weight(1f).height(48.dp).padding(start = 10.dp),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.mushaf_menu_set_sabaq),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AlkahfColors.OnAccent,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** "Go to" sheet: jump to a page by number, or search a sūrah by name/number. */
@Composable
private fun GoToSheet(
    surahs: List<SurahOption>,
    onPickSurah: (Int) -> Unit,
    onPickPage: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var pageText by remember { mutableStateOf("") }
    val filtered = remember(query, surahs) {
        val q = query.trim()
        if (q.isEmpty()) {
            surahs
        } else {
            surahs.filter {
                it.nameLatin.contains(q, ignoreCase = true) ||
                    it.nameArabic.contains(q) ||
                    it.number.toString().startsWith(q)
            }
        }
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.34f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = AlkahfColors.Paper,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).clickable(enabled = false) {},
        ) {
            Column(
                Modifier.navigationBarsPadding().padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp),
            ) {
                Box(Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(38.dp).height(4.dp).background(AlkahfColors.DashedNode, RoundedCornerShape(2.dp)))
                }
                Text(
                    text = stringResource(R.string.mushaf_goto_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    JumpField(
                        value = pageText,
                        onValueChange = { pageText = it.filter(Char::isDigit).take(3) },
                        placeholder = stringResource(R.string.mushaf_goto_page_hint),
                        numeric = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        onClick = { pageText.toIntOrNull()?.let(onPickPage) },
                        shape = RoundedCornerShape(14.dp),
                        color = AlkahfColors.Accent,
                        modifier = Modifier.height(50.dp),
                    ) {
                        Box(Modifier.padding(horizontal = 22.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.mushaf_goto_go),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AlkahfColors.OnAccent,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                JumpField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.mushaf_goto_surah_hint),
                    numeric = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    items(filtered) { s ->
                        SurahRow(s) { onPickSurah(s.number) }
                    }
                }
            }
        }
    }
}

@Composable
private fun JumpField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    numeric: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .height(50.dp)
            .background(AlkahfColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, AlkahfColors.CardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = AlkahfColors.Ink),
            cursorBrush = SolidColor(AlkahfColors.Accent),
            keyboardOptions = if (numeric) {
                KeyboardOptions(keyboardType = KeyboardType.Number)
            } else {
                KeyboardOptions.Default
            },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 15.sp, color = AlkahfColors.InkFaint)
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun SurahRow(s: SurahOption, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).background(AlkahfColors.Surface, CircleShape)
                .border(1.dp, AlkahfColors.CardBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("${s.number}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.InkSecondary)
        }
        Text(
            text = s.nameLatin,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = AlkahfColors.Ink,
            modifier = Modifier.weight(1f).padding(start = 14.dp),
        )
        Text(
            text = s.nameArabic,
            fontFamily = LocalQuranFont.current,
            fontSize = 19.sp,
            color = AlkahfColors.InkChrome,
        )
    }
}

@Composable
private fun MushafAudioDock(
    state: MushafAudioState,
    onMode: (MushafAudioMode) -> Unit,
    onScope: (MushafAudioScope) -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(AlkahfColors.NavSurface)) {
        HorizontalDivider(thickness = 1.dp, color = AlkahfColors.DockBorder)
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 10.dp),
        ) {
            AudioSegmented(
                options = listOf(
                    MushafAudioMode.LISTEN to stringResource(R.string.mushaf_mode_listen),
                    MushafAudioMode.RECITE_BACK to stringResource(R.string.mushaf_mode_recite_back),
                    MushafAudioMode.TAP to stringResource(R.string.mushaf_mode_tap_ayah),
                ),
                selected = state.mode,
                onSelect = onMode,
            )
            Spacer(Modifier.height(8.dp))
            if (state.mode == MushafAudioMode.TAP) {
                AudioHint(stringResource(R.string.mushaf_hint_tap_to_hear))
            } else {
                AudioSegmented(
                    options = listOf(
                        MushafAudioScope.PAGE to stringResource(R.string.mushaf_scope_page),
                        MushafAudioScope.SURAH to stringResource(R.string.mushaf_scope_surah),
                        MushafAudioScope.FROM_AYAH to stringResource(R.string.mushaf_scope_from_ayah),
                    ),
                    selected = state.scope,
                    onSelect = onScope,
                )
                if (state.scope == MushafAudioScope.FROM_AYAH &&
                    state.phase == MushafAudioPhase.IDLE
                ) {
                    Spacer(Modifier.height(6.dp))
                    AudioHint(stringResource(R.string.mushaf_hint_tap_to_start))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onPlayPause,
                    shape = CircleShape,
                    color = AlkahfColors.Accent,
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val showPlay = state.phase == MushafAudioPhase.IDLE || state.isPaused
                        Icon(
                            imageVector = if (showPlay) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (showPlay) {
                                stringResource(R.string.common_play)
                            } else {
                                stringResource(R.string.common_pause)
                            },
                            tint = AlkahfColors.OnAccent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        text = when {
                            state.errorMessage != null -> state.errorMessage
                            state.phase == MushafAudioPhase.IDLE ->
                                stringResource(R.string.mushaf_audio_ready)
                            state.phase == MushafAudioPhase.PREPARING ->
                                stringResource(R.string.mushaf_audio_preparing)
                            state.phase == MushafAudioPhase.GAP ->
                                stringResource(
                                    R.string.mushaf_audio_recite_back_ayah,
                                    (state.currentAyahId ?: 0) % 1000,
                                )
                            else -> stringResource(
                                R.string.mushaf_audio_ayah,
                                (state.currentAyahId ?: 0) % 1000,
                            )
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.errorMessage != null) {
                            AlkahfColors.StumbleInk
                        } else {
                            AlkahfColors.Ink
                        },
                    )
                    Text(
                        text = state.reciterName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AlkahfColors.InkFaint,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
                if (state.phase != MushafAudioPhase.IDLE) {
                    Surface(
                        onClick = onStop,
                        shape = RoundedCornerShape(12.dp),
                        color = AlkahfColors.PageSurface,
                        border = BorderStroke(1.dp, AlkahfColors.ControlBorder),
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = stringResource(R.string.common_stop),
                                tint = AlkahfColors.InkChrome,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioHint(text: String) {
    Text(
        text = text,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Medium,
        color = AlkahfColors.InkFaint,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun <T> AudioSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AlkahfColors.SegmentedTrack, RoundedCornerShape(12.dp))
            .padding(3.dp),
    ) {
        options.forEach { (option, label) ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .let {
                        if (isSelected) {
                            it.background(AlkahfColors.SegmentedSelected, RoundedCornerShape(9.dp))
                        } else {
                            it
                        }
                    }
                    .clickable { onSelect(option) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) AlkahfColors.AccentDeep else AlkahfColors.InkMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PulsingDot() {
    val transition = rememberInfiniteTransition(label = "selfTestPulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "selfTestPulseAlpha",
    )
    Box(
        Modifier
            .size(7.dp)
            .alpha(alpha)
            .background(AlkahfColors.Accent, CircleShape),
    )
}
