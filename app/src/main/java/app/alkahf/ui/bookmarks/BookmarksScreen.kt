package app.alkahf.ui.bookmarks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.AlkahfApplication
import app.alkahf.R
import app.alkahf.data.Bookmark
import app.alkahf.data.BookmarkLabel
import app.alkahf.ui.components.AlkahfBottomNav
import app.alkahf.ui.components.AlkahfTab
import app.alkahf.ui.theme.AlkahfColors
import app.alkahf.ui.theme.AmiriQuran

private sealed interface BookmarkFilter {
    data object All : BookmarkFilter
    data object WithNotes : BookmarkFilter
    data class ByLabel(val label: BookmarkLabel) : BookmarkFilter
}

@Composable
fun BookmarksScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onSelectTab: (AlkahfTab) -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as AlkahfApplication).repository }
    val scope = rememberCoroutineScope()
    val all = remember { mutableStateListOf<Bookmark>() }
    var filter by remember { mutableStateOf<BookmarkFilter>(BookmarkFilter.All) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        all.clear(); all.addAll(repository.bookmarks())
    }

    val withNotes = all.count { it.hasNote }
    val presentLabels = remember(all.toList()) {
        BookmarkLabel.entries.filter { l -> l != BookmarkLabel.NONE && all.any { it.label == l } }
    }
    val visible = all.filter { b ->
        val matchesFilter = when (val f = filter) {
            BookmarkFilter.All -> true
            BookmarkFilter.WithNotes -> b.hasNote
            is BookmarkFilter.ByLabel -> b.label == f.label
        }
        val q = query.trim()
        val matchesQuery = q.isEmpty() ||
            b.surahNameLatin.contains(q, ignoreCase = true) ||
            b.note.contains(q, ignoreCase = true)
        matchesFilter && matchesQuery
    }

    Scaffold(
        containerColor = AlkahfColors.Paper,
        bottomBar = { AlkahfBottomNav(selected = AlkahfTab.LIBRARY, onSelect = onSelectTab) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Header
            Column(Modifier.padding(start = 18.dp, end = 14.dp, top = 8.dp, bottom = 10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = AlkahfColors.Ink, modifier = Modifier.size(26.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 2.dp)) {
                        Text(
                            text = stringResource(R.string.bookmarks_title),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlkahfColors.Ink,
                            letterSpacing = (-0.5).sp,
                        )
                        Text(
                            text = stringResource(R.string.bookmarks_count, all.size, withNotes),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = AlkahfColors.InkFaint,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                            .background(AlkahfColors.Surface)
                            .border(1.dp, AlkahfColors.CardBorder, RoundedCornerShape(12.dp))
                            .clickable { searchOpen = !searchOpen; if (!searchOpen) query = "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (searchOpen) Icons.Outlined.Close else Icons.Outlined.Search,
                            null, tint = AlkahfColors.InkSecondary, modifier = Modifier.size(18.dp),
                        )
                    }
                }

                if (searchOpen) {
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = AlkahfColors.Surface,
                        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) {
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 15.sp, color = AlkahfColors.Ink),
                            cursorBrush = SolidColor(AlkahfColors.Accent),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            decorationBox = { inner ->
                                if (query.isEmpty()) {
                                    Text(stringResource(R.string.bookmark_search_hint), fontSize = 15.sp, color = AlkahfColors.InkFaint)
                                }
                                inner()
                            },
                        )
                    }
                }

                // Filter chips
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    FilterChip(stringResource(R.string.bookmark_filter_all), null, filter == BookmarkFilter.All) { filter = BookmarkFilter.All }
                    FilterChip(stringResource(R.string.bookmark_filter_with_notes), null, filter == BookmarkFilter.WithNotes) { filter = BookmarkFilter.WithNotes }
                    presentLabels.forEach { l ->
                        FilterChip(labelName(l) ?: "", labelColors(l).accent, filter == BookmarkFilter.ByLabel(l)) {
                            filter = BookmarkFilter.ByLabel(l)
                        }
                    }
                }
            }

            if (visible.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.bookmarks_empty),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AlkahfColors.InkFaint,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    items(visible, key = { it.id }) { b -> BookmarkCard(b) { onOpen(b.id) } }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(text: String, dot: androidx.compose.ui.graphics.Color?, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) AlkahfColors.Accent else AlkahfColors.Surface,
        border = if (selected) null else BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dot != null && !selected) {
                Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(dot))
                Box(Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) AlkahfColors.OnAccent else AlkahfColors.InkSecondary,
            )
        }
    }
}

@Composable
private fun BookmarkCard(b: Bookmark, onClick: () -> Unit) {
    val colors = labelColors(b.label)
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.height(intrinsicSize = androidx.compose.foundation.layout.IntrinsicSize.Min)) {
            // ribbon spine
            Box(Modifier.width(5.dp).fillMaxHeight().background(colors.accent))
            Column(Modifier.weight(1f).padding(horizontal = 15.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bookmarkTitle(b),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlkahfColors.Ink,
                    )
                    val name = labelName(b.label)
                    if (name != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.chipBg,
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.chipText,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Box(Modifier.weight(1f))
                    Icon(Icons.Filled.Bookmark, null, tint = AlkahfColors.BookmarkRibbon, modifier = Modifier.size(16.dp))
                }
                androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = b.snippet,
                        fontFamily = AmiriQuran,
                        fontSize = 18.sp,
                        lineHeight = 30.sp,
                        color = AlkahfColors.InkSecondaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                if (b.hasNote) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Notes, null,
                            tint = AlkahfColors.InkFooter,
                            modifier = Modifier.size(15.dp).padding(top = 1.dp),
                        )
                        Text(
                            text = b.note,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = AlkahfColors.InkSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.bookmark_no_note),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        color = AlkahfColors.InkFooter,
                        modifier = Modifier.padding(top = 11.dp),
                    )
                }
                Text(
                    text = bookmarkDate(context, b.updatedAt),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFooter,
                    modifier = Modifier.padding(top = 9.dp),
                )
            }
        }
    }
}
