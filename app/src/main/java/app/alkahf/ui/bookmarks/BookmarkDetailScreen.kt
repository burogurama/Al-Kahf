package app.alkahf.ui.bookmarks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.AlkahfApplication
import app.alkahf.R
import app.alkahf.data.Bookmark
import app.alkahf.data.PageAyah
import app.alkahf.ui.theme.AlkahfColors
import app.alkahf.ui.theme.AmiriQuran
import kotlinx.coroutines.launch

@Composable
fun BookmarkDetailScreen(
    bookmarkId: Long,
    onBack: () -> Unit,
    onOpenMushaf: (surah: Int, from: Int) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as AlkahfApplication).repository }
    val scope = rememberCoroutineScope()

    var bookmark by remember { mutableStateOf<Bookmark?>(null) }
    var ayat by remember { mutableStateOf<List<PageAyah>>(emptyList()) }
    var reloadKey by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf(false) }

    LaunchedEffect(bookmarkId, reloadKey) {
        val b = repository.bookmark(bookmarkId)
        bookmark = b
        ayat = if (b != null) repository.ayahsForRange(b.surah, b.from, b.to) else emptyList()
    }

    val b = bookmark
    Column(Modifier.fillMaxSize().background(AlkahfColors.Paper).statusBarsPadding()) {
        // top bar
        Row(
            Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = AlkahfColors.InkChrome, modifier = Modifier.size(26.dp))
            }
            Text(
                text = stringResource(R.string.bookmark_detail_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Box(Modifier.size(40.dp))
        }

        if (b == null) return@Column

        val colors = labelColors(b.label)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // range header
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(AlkahfColors.KhatamCardTint)
                        .border(1.dp, AlkahfColors.KhatamCardBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Bookmark, null, tint = AlkahfColors.BookmarkRibbon, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f).padding(start = 11.dp)) {
                    Text(bookmarkTitle(b), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = AlkahfColors.Ink, letterSpacing = (-0.3).sp)
                    Text(
                        text = stringResource(R.string.bookmark_juz_page, b.juz, b.page),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = AlkahfColors.InkFaint,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                val name = labelName(b.label)
                if (name != null) {
                    Surface(shape = RoundedCornerShape(7.dp), color = colors.chipBg) {
                        Text(name, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.chipText, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
                    }
                }
            }

            // āyah card (gilded)
            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, AlkahfColors.KhatamCardBorder),
                color = AlkahfColors.Surface,
            ) {
                Box(
                    Modifier.background(
                        Brush.verticalGradient(listOf(AlkahfColors.KhatamCardTint, AlkahfColors.Surface)),
                    ),
                ) {
                    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = ayat.joinToString("  ") { it.words.joinToString(" ") + " " + it.marker },
                            fontFamily = AmiriQuran,
                            fontSize = 23.sp,
                            lineHeight = 46.sp,
                            color = AlkahfColors.Ink,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 18.dp),
                        )
                    }
                }
            }

            // note block
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = AlkahfColors.PageSurface,
                border = BorderStroke(1.dp, AlkahfColors.LoopCardBorder),
            ) {
                Column(Modifier.padding(horizontal = 17.dp, vertical = 16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.bookmark_note_kicker),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                            color = AlkahfColors.KhatamGoldDeep,
                        )
                        Row(
                            Modifier.clip(RoundedCornerShape(8.dp)).clickable { editing = true }.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Edit, null, tint = AlkahfColors.AccentDeep, modifier = Modifier.size(14.dp))
                            Text(stringResource(R.string.bookmark_edit), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.AccentDeep, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    Text(
                        text = b.note.ifBlank { stringResource(R.string.bookmark_no_note) },
                        fontSize = 15.5.sp,
                        lineHeight = 25.sp,
                        fontWeight = if (b.hasNote) FontWeight.Normal else FontWeight.Medium,
                        color = if (b.hasNote) AlkahfColors.InkSecondaryDark else AlkahfColors.InkFooter,
                        modifier = Modifier.padding(top = 11.dp),
                    )
                }
            }

            // meta tiles
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetaTile(stringResource(R.string.bookmark_saved_kicker), bookmarkDate(context, b.createdAt), Modifier.weight(1f))
                if (b.updatedAt > b.createdAt) {
                    MetaTile(stringResource(R.string.bookmark_edited_kicker), bookmarkRelative(context, b.updatedAt), Modifier.weight(1f))
                } else {
                    Box(Modifier.weight(1f))
                }
            }
        }

        // dock
        Surface(color = AlkahfColors.NavSurface, border = BorderStroke(1.dp, AlkahfColors.DockBorder), modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    onClick = { scope.launch { repository.deleteBookmark(b.id); onBack() } },
                    shape = RoundedCornerShape(15.dp),
                    color = AlkahfColors.Surface,
                    border = BorderStroke(1.dp, AlkahfColors.LabelTerracotta),
                    modifier = Modifier.size(width = 56.dp, height = 52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.bookmark_delete), tint = AlkahfColors.LabelTerracottaText, modifier = Modifier.size(20.dp))
                    }
                }
                Surface(
                    onClick = { onOpenMushaf(b.surah, b.from) },
                    shape = RoundedCornerShape(15.dp),
                    color = AlkahfColors.Accent,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, null, tint = AlkahfColors.OnAccent, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.bookmark_open_mushaf), fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.OnAccent, modifier = Modifier.padding(start = 9.dp))
                    }
                }
            }
        }
    }

    if (editing && b != null) {
        BookmarkSheet(
            surah = b.surah,
            from = b.from,
            to = b.to,
            initialNote = b.note,
            initialLabel = b.label,
            isEdit = true,
            onSave = { note, label ->
                scope.launch {
                    repository.updateBookmark(b.id, note, label)
                    editing = false
                    reloadKey++
                }
            },
            onDismiss = { editing = false },
        )
    }
}

@Composable
private fun MetaTile(kicker: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = AlkahfColors.Surface, border = BorderStroke(1.dp, AlkahfColors.CardBorder), modifier = modifier) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Text(kicker, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = AlkahfColors.InkFooter)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.Ink, modifier = Modifier.padding(top = 3.dp))
        }
    }
}
