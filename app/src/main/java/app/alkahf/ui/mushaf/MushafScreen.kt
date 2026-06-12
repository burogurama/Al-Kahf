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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
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
import app.alkahf.AlkahfApplication
import app.alkahf.data.MushafPage
import app.alkahf.data.PageAyah
import app.alkahf.data.PageGroup
import app.alkahf.data.QuranRepository
import app.alkahf.data.WordStumble
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

/** Hide/reveal progress and stumbles for one page's self-test. */
class SelfTestSession(val page: MushafPage) {
    val revealedCounts = mutableStateMapOf<Int, Int>()
    val stumbles = mutableStateListOf<WordStumble>()

    fun revealedOf(ayah: PageAyah): Int = revealedCounts.getOrDefault(ayah.id, 0)

    val currentAyah: PageAyah?
        get() = page.ayahs.firstOrNull { revealedOf(it) < it.words.size }

    fun revealNextWord(ayahId: Int) {
        val ayah = page.ayahs.first { it.id == ayahId }
        val revealed = revealedCounts.getOrDefault(ayahId, 0)
        if (revealed < ayah.words.size) revealedCounts[ayahId] = revealed + 1
    }

    fun revealAyah(ayahId: Int) {
        revealedCounts[ayahId] = page.ayahs.first { it.id == ayahId }.words.size
    }

    fun revealCurrentAyah() {
        currentAyah?.let { revealedCounts[it.id] = it.words.size }
    }

    /** The word the Stumble button applies to: the most recently revealed word. */
    fun stumbleTarget(): WordStumble? =
        page.ayahs.lastOrNull { revealedOf(it) > 0 }
            ?.let { WordStumble(it.id, revealedOf(it) - 1) }
}

@Composable
fun MushafScreen(
    startSurah: Int? = null,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as AlkahfApplication).repository }
    val scope = rememberCoroutineScope()
    var hideMode by remember { mutableStateOf(true) }

    // A surah target (e.g. the sabaq) wins; otherwise resume the last page read.
    val startPage by produceState<Int?>(initialValue = null) {
        value = startSurah?.let { repository.firstPageOfSurah(it) }
            ?: repository.lastMushafPage
            ?: repository.firstPageOfSurah(DEFAULT_SURAH)
    }
    val initialPage = startPage
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

    Column(Modifier.fillMaxSize().background(AlkahfColors.Paper)) {
        MushafTopBar(
            title = currentSession?.page?.primarySurahLatin ?: "",
            location = currentSession?.page?.let { "Juzʼ ${it.juz} · Page ${it.number}" } ?: "",
            hideMode = hideMode,
            onBack = onBack,
            onToggleHideMode = { hideMode = !hideMode },
        )
        // RTL pager: swiping toward the right turns to the next page, as in a
        // physical mushaf. Page content restores LTR for its own chrome.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                    )
                }
            }
        }
        if (hideMode) {
            SelfTestDock(
                session = currentSession,
                onReveal = { currentSession?.revealCurrentAyah() },
                onStumble = {
                    val session = currentSession ?: return@SelfTestDock
                    val target = session.stumbleTarget() ?: return@SelfTestDock
                    if (target !in session.stumbles) {
                        session.stumbles.add(target)
                        scope.launch { repository.addStumble(target) }
                    }
                },
            )
        }
    }
}

@Composable
private fun MushafTopBar(
    title: String,
    location: String,
    hideMode: Boolean,
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

@Composable
private fun MushafPageView(
    pageNumber: Int,
    repository: QuranRepository,
    hideMode: Boolean,
    session: SelfTestSession?,
    onSessionReady: (SelfTestSession) -> Unit,
) {
    LaunchedEffect(pageNumber) {
        if (session == null) {
            val page = repository.page(pageNumber)
            val newSession = SelfTestSession(page)
            newSession.stumbles.addAll(repository.stumblesForPage(page))
            onSessionReady(newSession)
        }
    }
    if (session == null) {
        Box(Modifier.fillMaxSize().background(AlkahfColors.PageSurface))
        return
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
                    PageGroupView(group, hideMode, session)
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
    AyatBody(group, hideMode, session)
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
) {
    val revealedByAyah = group.ayahs.associate { it.id to session.revealedOf(it) }
    val groupText = remember(revealedByAyah, hideMode) {
        buildGroupText(group, revealedByAyah, hideMode)
    }
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val stumbleAmber = AlkahfColors.StumbleAmber

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = groupText.annotated,
            style = TextStyle(
                fontFamily = KfgqpcHafs,
                fontSize = 22.sp,
                lineHeight = 42.sp,
                color = AlkahfColors.Ink,
                textAlign = TextAlign.Justify,
            ),
            onTextLayout = { layoutResult.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(hideMode, groupText) {
                    detectTapGestures(
                        onTap = { position ->
                            if (!hideMode) return@detectTapGestures
                            val span = spanAt(layoutResult.value, groupText, position)
                                ?: return@detectTapGestures
                            session.revealNextWord(span.ayahId)
                        },
                        onLongPress = { position ->
                            if (!hideMode) return@detectTapGestures
                            val span = spanAt(layoutResult.value, groupText, position)
                                ?: return@detectTapGestures
                            session.revealAyah(span.ayahId)
                        },
                    )
                }
                .drawBehind {
                    val layout = layoutResult.value ?: return@drawBehind
                    val groupAyahIds = group.ayahs.map { it.id }.toSet()
                    val visibleStumbles = session.stumbles.filter { stumble ->
                        stumble.ayahId in groupAyahIds &&
                            (!hideMode || (revealedByAyah[stumble.ayahId] ?: 0) > stumble.wordIndex)
                    }
                    visibleStumbles.forEach { stumble ->
                        val span = groupText.spans.firstOrNull {
                            !it.isMarker && it.ayahId == stumble.ayahId && it.wordIndex == stumble.wordIndex
                        } ?: return@forEach
                        val bounds = layout.getPathForRange(span.start, span.end).getBounds()
                        val baseline = layout.getLineBaseline(layout.getLineForOffset(span.start))
                        val y = baseline + 9.dp.toPx()
                        drawLine(
                            color = stumbleAmber,
                            start = Offset(bounds.left, y),
                            end = Offset(bounds.right, y),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                    visibleStumbles.map { it.ayahId }.distinct().forEach { ayahId ->
                        val marker = groupText.spans.firstOrNull { it.isMarker && it.ayahId == ayahId }
                            ?: return@forEach
                        val bounds = layout.getPathForRange(marker.start, marker.end).getBounds()
                        drawCircle(
                            color = stumbleAmber,
                            radius = 3.dp.toPx(),
                            center = Offset(bounds.left, bounds.top + 6.dp.toPx()),
                        )
                    }
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

private fun buildGroupText(
    group: PageGroup,
    revealedByAyah: Map<Int, Int>,
    hideMode: Boolean,
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
            val fullyRevealed = !hideMode || revealed >= ayah.words.size
            addStyle(
                style = SpanStyle(
                    color = if (fullyRevealed) AlkahfColors.Accent else AlkahfColors.ConcealedMedallion,
                ),
                start = markerStart,
                end = length,
            )
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
private fun SelfTestDock(
    session: SelfTestSession?,
    onReveal: () -> Unit,
    onStumble: () -> Unit,
) {
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
                text = "Tap a word to reveal · long-press for the full ayah",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 11.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = onStumble,
                    shape = RoundedCornerShape(15.dp),
                    color = AlkahfColors.StumbleBg,
                    border = BorderStroke(1.5.dp, AlkahfColors.StumbleBorder),
                    modifier = Modifier.height(52.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = null,
                            tint = AlkahfColors.StumbleInk,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Stumble",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AlkahfColors.StumbleInk,
                        )
                    }
                }
                Button(
                    onClick = onReveal,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(15.dp),
                            ambientColor = AlkahfColors.Accent.copy(alpha = 0.28f),
                            spotColor = AlkahfColors.Accent.copy(alpha = 0.28f),
                        ),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlkahfColors.Accent,
                        contentColor = AlkahfColors.OnAccent,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = "Reveal next ayah",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
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
