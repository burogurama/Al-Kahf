package app.alkahf.ui.mushaf

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import app.alkahf.AlkahfApplication
import app.alkahf.data.AyahRange
import app.alkahf.data.MushafPage
import app.alkahf.data.PageAyah
import app.alkahf.data.PageGroup
import app.alkahf.data.MemorizationState
import app.alkahf.data.QuranRepository
import app.alkahf.data.audio.AudioStore
import app.alkahf.ui.theme.AlkahfColors
import app.alkahf.ui.theme.KfgqpcHafs
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
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as AlkahfApplication).repository }
    val scope = rememberCoroutineScope()
    val settings = remember { repository.settings() }
    // Keep the screen awake while reading, if enabled in Settings.
    if (settings.keepScreenOn) {
        val view = androidx.compose.ui.platform.LocalView.current
        DisposableEffect(Unit) {
            view.keepScreenOn = true
            onDispose { view.keepScreenOn = false }
        }
    }
    // Opening the sabaq lands in reading mode (text shown); otherwise reopen
    // in whichever mode the Mushaf was left in.
    var hideMode by remember {
        mutableStateOf(if (highlightRange != null) false else repository.lastMushafHideMode)
    }
    val highlightIds = remember(highlightRange) { highlightRange?.ayahIds ?: emptySet() }
    val scrollToAyahId = highlightRange?.let { it.surah * 1000 + it.from }

    // A highlighted range opens on its page; otherwise an explicit page/surah
    // target wins, then the last page read.
    val resolvedStartPage by produceState<Int?>(initialValue = null) {
        value = highlightRange?.let { repository.pageOfAyah(it.surah, it.from) }
            ?: startPage
            ?: startSurah?.let { repository.firstPageOfSurah(it) }
            ?: repository.lastMushafPage
            ?: repository.firstPageOfSurah(DEFAULT_SURAH)
    }
    val initialPage = resolvedStartPage
    if (initialPage == null) {
        Box(Modifier.fillMaxSize().background(AlkahfColors.Paper))
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage - 1,
        pageCount = { QuranRepository.PAGE_COUNT },
    )
    val sessions = remember { mutableStateMapOf<Int, SelfTestSession>() }
    val currentPageNumber = pagerState.currentPage + 1
    val currentSession = sessions[currentPageNumber]

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            repository.lastMushafPage = page + 1
        }
    }

    val audioController = remember {
        val reciter = repository.activeReciter
        MushafAudioController(
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
    LaunchedEffect(currentPageNumber) { selection = null }
    val selectedIds = currentSession?.page?.ayahs?.let { ayahs ->
        // Bounds-guard: during a page swap the old range may not fit the new page.
        selection?.takeIf { it.first >= 0 && it.last < ayahs.size }
            ?.let { range -> ayahs.slice(range).map { it.id }.toSet() }
    } ?: emptySet()
    var showStateSheet by remember { mutableStateOf(false) }

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

    Column(Modifier.fillMaxSize().background(AlkahfColors.Paper)) {
        MushafTopBar(
            title = currentSession?.page?.primarySurahLatin ?: "",
            location = currentSession?.page?.let { "Juzʼ ${it.juz} · Page ${it.number}" } ?: "",
            hideMode = hideMode,
            audioActive = audioDockOpen,
            onToggleAudio = {
                val opening = !audioDockOpen
                audioDockOpen = opening
                if (!opening) audioController.stop()
            },
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
                    MushafPageView(
                        pageNumber = pageIndex + 1,
                        repository = repository,
                        hideMode = hideMode,
                        session = sessions[pageIndex + 1],
                        onSessionReady = { sessions[pageIndex + 1] = it },
                        persistReveal = persistReveal,
                        onSelectAyah = onSelectAyah,
                        playingAyahId = audioState.currentAyahId,
                        onAudioTap = onAudioTap,
                        highlightIds = highlightIds,
                        selectedIds = if (pageIndex + 1 == currentPageNumber) selectedIds else emptySet(),
                        scrollToAyahId = scrollToAyahId,
                    )
                }
            }
        }
        when {
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
            selectedIds.isNotEmpty() -> SelectionDock(
                count = selectedIds.size,
                onSetState = { showStateSheet = true },
                onClear = { selection = null },
            )
            hideMode -> SelfTestDock(session = currentSession)
        }
    }

    val session = currentSession
    if (showStateSheet && session != null && selectedIds.isNotEmpty()) {
        val currentStates = selectedIds.map { session.memStates[it] ?: MemorizationState.NOT_STARTED }
        MemorizationSheet(
            title = if (selectedIds.size == 1) "Mark āyah" else "Mark ${selectedIds.size} āyāt",
            current = currentStates.distinct().singleOrNull(),
            onPick = { state ->
                selectedIds.forEach { id ->
                    session.memStates[id] = state
                    scope.launch { repository.setAyahState(id, state) }
                }
                showStateSheet = false
                selection = null
            },
            onDismiss = { showStateSheet = false },
        )
    }
}

@Composable
private fun MushafTopBar(
    title: String,
    location: String,
    hideMode: Boolean,
    audioActive: Boolean,
    onToggleAudio: () -> Unit,
    onBack: () -> Unit,
    onToggleHideMode: () -> Unit,
) {
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
                    contentDescription = "Back",
                    tint = AlkahfColors.InkChrome,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                    letterSpacing = (-0.2).sp,
                )
                Text(
                    text = location,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Box(
                modifier = Modifier.size(40.dp).clickable(onClick = onToggleAudio),
                contentAlignment = Alignment.Center,
            ) {
                if (audioActive) {
                    Surface(shape = CircleShape, color = AlkahfColors.AccentTint) {
                        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Headphones,
                                contentDescription = "Listening on",
                                tint = AlkahfColors.AccentDeep,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Headphones,
                        contentDescription = "Listening off",
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
                                contentDescription = "Hide mode on",
                                tint = AlkahfColors.AccentDeep,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = "Hide mode off",
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
    repository: QuranRepository,
    hideMode: Boolean,
    session: SelfTestSession?,
    onSessionReady: (SelfTestSession) -> Unit,
    persistReveal: (Int, Int) -> Unit,
    onSelectAyah: (Int) -> Unit,
    playingAyahId: Int?,
    onAudioTap: (Int) -> Boolean,
    highlightIds: Set<Int>,
    selectedIds: Set<Int>,
    scrollToAyahId: Int?,
) {
    LaunchedEffect(pageNumber) {
        if (session == null) {
            val page = repository.page(pageNumber)
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
                            highlightIds = highlightIds,
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
    highlightIds: Set<Int>,
    selectedIds: Set<Int>,
) {
    if (group.showSurahHeader) {
        SurahHeaderBand(group)
    }
    if (group.basmala != null) {
        Text(
            text = group.basmala,
            fontFamily = KfgqpcHafs,
            fontSize = 20.sp,
            color = AlkahfColors.InkChrome,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 10.dp),
        )
    }
    AyatBody(group, hideMode, session, playingAyahId, onAudioTap, onSelectAyah, highlightIds, selectedIds)
}

@Composable
private fun SurahHeaderBand(group: PageGroup) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
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
                fontFamily = KfgqpcHafs,
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
    highlightIds: Set<Int>,
    selectedIds: Set<Int>,
) {
    val revealedByAyah = group.ayahs.associate { it.id to session.revealedOf(it) }
    val memByAyah = group.ayahs.associate { it.id to (session.memStates[it.id] ?: MemorizationState.NOT_STARTED) }
    // Ayat to draw the rounded highlight fill behind: selection, the sabaq
    // range, and the ayah currently being listened to.
    val litIds = remember(selectedIds, highlightIds, playingAyahId) {
        selectedIds + highlightIds + listOfNotNull(playingAyahId)
    }
    val groupText = remember(revealedByAyah, hideMode, memByAyah, litIds) {
        buildGroupText(group, revealedByAyah, hideMode, memByAyah, litIds)
    }
    val currentOnAudioTap by rememberUpdatedState(onAudioTap)
    val currentOnSelect by rememberUpdatedState(onSelectAyah)
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val bodySize = LocalMushafTextSize.current.sp
    val fill = AlkahfColors.AyahHighlightFill

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = groupText.annotated,
            style = TextStyle(
                fontFamily = KfgqpcHafs,
                fontSize = bodySize,
                lineHeight = bodySize * 1.9f,
                color = AlkahfColors.Ink,
                textAlign = TextAlign.Justify,
            ),
            onTextLayout = { layoutResult.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val layout = layoutResult.value ?: return@drawBehind
                    val padH = 6.dp.toPx()
                    val padV = 3.dp.toPx()
                    val radius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                    litIds.forEach { ayahId ->
                        val ayahSpans = groupText.spans.filter { it.ayahId == ayahId }
                        if (ayahSpans.isEmpty()) return@forEach
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
                            if (currentOnAudioTap(ayahId)) return@detectTapGestures
                            if (hideMode) {
                                // Reveal through the tapped word (or the whole
                                // ayah when its medallion is tapped).
                                if (span != null) {
                                    session.revealUpTo(span.ayahId, span.wordIndex)
                                } else {
                                    session.revealAyah(ayahId)
                                }
                            } else {
                                // Reading mode: tap selects the ayah for marking.
                                currentOnSelect(ayahId)
                            }
                        },
                        onLongPress = { position ->
                            if (!hideMode) return@detectTapGestures
                            val ayahId = spanAt(layoutResult.value, groupText, position)?.ayahId
                                ?: markerAyahIdAt(layoutResult.value, groupText, position)
                                ?: return@detectTapGestures
                            session.revealAyah(ayahId)
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
): GroupText {
    val spans = mutableListOf<PageSpan>()
    val annotated = buildAnnotatedString {
        group.ayahs.forEachIndexed { ayahIndex, ayah ->
            val revealed = revealedByAyah[ayah.id] ?: 0
            ayah.words.forEachIndexed { wordIndex, word ->
                val start = length
                append(word)
                spans += PageSpan(ayah.id, wordIndex, start, length, isMarker = false)
                val concealed = hideMode && wordIndex >= revealed
                // The highlight fill is drawn behind the text; the glyph colour
                // never changes (spec).
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
                        SpanStyle(color = AlkahfColors.Ink)
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
                text = "Ḥizb ${page.hizb} · Juzʼ ${page.juz}",
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
                        text = "SELF-TEST",
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
                text = "Tap to reveal · long-press reveals the ayah · in reading mode, long-press to mark it memorized",
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

private fun recitingLabel(session: SelfTestSession?): String {
    val page = session?.page ?: return ""
    val current = session.currentAyah ?: return "All ayat recalled"
    val position = page.ayahs.indexOfFirst { it.id == current.id } + 1
    return "Reciting ayah $position of ${page.ayahs.size}"
}

@Composable
private fun SelectionDock(count: Int, onSetState: () -> Unit, onClear: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(AlkahfColors.NavSurface)) {
        HorizontalDivider(thickness = 1.dp, color = AlkahfColors.DockBorder)
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$count āyah${if (count > 1) "āt" else ""} selected",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.Ink,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier.clickable(onClick = onClear).padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Clear",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.InkMuted,
                )
            }
            Surface(
                onClick = onSetState,
                shape = RoundedCornerShape(14.dp),
                color = AlkahfColors.Accent,
                modifier = Modifier.height(44.dp),
            ) {
                Box(
                    Modifier.padding(horizontal = 18.dp).fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Set state",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AlkahfColors.OnAccent,
                    )
                }
            }
        }
    }
}

@Composable
private fun MemorizationSheet(
    title: String,
    current: MemorizationState?,
    onPick: (MemorizationState) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        MemorizationState.NOT_STARTED to "Not started",
        MemorizationState.LEARNING to "Learning",
        MemorizationState.MEMORIZED to "Memorized",
        MemorizationState.STRONG to "Strong",
    )
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
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                options.forEach { (state, label) ->
                    val selected = state == current
                    Surface(
                        onClick = { onPick(state) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) AlkahfColors.SurfaceHero else AlkahfColors.Surface,
                        border = BorderStroke(
                            1.dp,
                            if (selected) AlkahfColors.CardBorderHero else AlkahfColors.CardBorder,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(12.dp)
                                    .background(memMedallionColor(state), CircleShape),
                            )
                            Text(
                                text = label,
                                fontSize = 14.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                color = AlkahfColors.Ink,
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                            )
                            if (selected) {
                                Text(
                                    text = "current",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AlkahfColors.AccentDeep,
                                )
                            }
                        }
                    }
                }
            }
        }
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
                    MushafAudioMode.LISTEN to "Listen",
                    MushafAudioMode.RECITE_BACK to "Recite back",
                    MushafAudioMode.TAP to "Tap āyah",
                ),
                selected = state.mode,
                onSelect = onMode,
            )
            Spacer(Modifier.height(8.dp))
            if (state.mode == MushafAudioMode.TAP) {
                AudioHint("Tap any āyah on the page to hear it")
            } else {
                AudioSegmented(
                    options = listOf(
                        MushafAudioScope.PAGE to "Page",
                        MushafAudioScope.SURAH to "Surah",
                        MushafAudioScope.FROM_AYAH to "From āyah",
                    ),
                    selected = state.scope,
                    onSelect = onScope,
                )
                if (state.scope == MushafAudioScope.FROM_AYAH &&
                    state.phase == MushafAudioPhase.IDLE
                ) {
                    Spacer(Modifier.height(6.dp))
                    AudioHint("Tap an āyah on the page to start from it")
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
                            contentDescription = if (showPlay) "Play" else "Pause",
                            tint = AlkahfColors.OnAccent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        text = when {
                            state.errorMessage != null -> state.errorMessage
                            state.phase == MushafAudioPhase.IDLE -> "Ready"
                            state.phase == MushafAudioPhase.PREPARING -> "Preparing audio…"
                            state.phase == MushafAudioPhase.GAP ->
                                "Recite back · āyah ${(state.currentAyahId ?: 0) % 1000}"
                            else -> "Āyah ${(state.currentAyahId ?: 0) % 1000}"
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
                                contentDescription = "Stop",
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
