package app.alkahf.ui.bookmarks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import app.alkahf.AlkahfApplication
import app.alkahf.R
import app.alkahf.data.BookmarkLabel
import app.alkahf.data.BookmarkStore
import app.alkahf.ui.theme.AlkahfColors
import app.alkahf.ui.theme.AmiriQuran

/**
 * The "Add / Edit bookmark" bottom sheet (design frame 02): a gold range-preview
 * card, a note field with a live character counter, and a single-select label
 * picker. Reused for both creating and editing a bookmark.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookmarkSheet(
    surah: Int,
    from: Int,
    to: Int,
    initialNote: String = "",
    initialLabel: BookmarkLabel = BookmarkLabel.NONE,
    isEdit: Boolean = false,
    onSave: (note: String, label: BookmarkLabel) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as AlkahfApplication).repository }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var note by remember { mutableStateOf(initialNote) }
    var label by remember { mutableStateOf(initialLabel) }
    var focused by remember { mutableStateOf(false) }

    // Resolve the range's sūrah name, page, and a short Arabic preview.
    val preview by produceState<RangePreview?>(initialValue = null, surah, from, to) {
        val name = repository.surahNameLatin(surah)
        val page = repository.pageOfAyah(surah, from)
        val text = repository.ayahsForRange(surah, from, to)
            .flatMap { it.words }.joinToString(" ")
        value = RangePreview(name, page, text)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AlkahfColors.NavSurface,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 44.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(AlkahfColors.HeaderRule))
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 22.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(if (isEdit) R.string.bookmark_sheet_edit else R.string.bookmark_sheet_new),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AlkahfColors.Ink,
                )
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(50)).background(AlkahfColors.ChipBg)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, null, tint = AlkahfColors.InkSecondary, modifier = Modifier.size(15.dp))
                }
            }

            // Range preview card (gold tint).
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AlkahfColors.KhatamCardTint,
                border = BorderStroke(1.dp, AlkahfColors.KhatamCardBorder),
                modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
            ) {
                Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bookmark, null, tint = AlkahfColors.BookmarkRibbon, modifier = Modifier.size(16.dp))
                            Text(
                                text = bookmarkTitle(preview?.surahName ?: "", from, to),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlkahfColors.Ink,
                                modifier = Modifier.padding(start = 9.dp),
                            )
                        }
                        Text(
                            text = preview?.let { stringResource(R.string.bookmark_page, it.page) } ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AlkahfColors.KhatamGoldDeep,
                        )
                    }
                    CompositionLocalProviderRtl {
                        Text(
                            text = preview?.text?.let { "$it …" } ?: "",
                            fontFamily = AmiriQuran,
                            fontSize = 18.sp,
                            lineHeight = 34.sp,
                            color = AlkahfColors.KhatamGoldDeep,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                        )
                    }
                }
            }

            // Note field.
            Kicker(stringResource(R.string.bookmark_note_kicker), Modifier.padding(top = 17.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AlkahfColors.Surface,
                border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) AlkahfColors.Accent else AlkahfColors.CardBorder),
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
            ) {
                BasicTextField(
                    value = note,
                    onValueChange = { if (it.length <= BookmarkStore.NOTE_MAX) note = it },
                    textStyle = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = AlkahfColors.Ink),
                    cursorBrush = SolidColor(AlkahfColors.Accent),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp)
                        .padding(horizontal = 15.dp, vertical = 14.dp)
                        .onFocusChanged { focused = it.isFocused },
                    decorationBox = { inner ->
                        if (note.isEmpty()) {
                            Text(
                                stringResource(R.string.bookmark_note_placeholder),
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                color = AlkahfColors.InkFaint,
                            )
                        }
                        inner()
                    },
                )
            }
            Text(
                text = stringResource(R.string.bookmark_char_count, note.length, BookmarkStore.NOTE_MAX),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFooter,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                textAlign = TextAlign.End,
            )

            // Label picker.
            Kicker(stringResource(R.string.bookmark_label_kicker), Modifier.padding(top = 8.dp))
            FlowRow(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    BookmarkLabel.REFLECTION,
                    BookmarkLabel.TAFSIR,
                    BookmarkLabel.MEMORIZE,
                    BookmarkLabel.DUA,
                ).forEach { l ->
                    LabelChip(label = l, selected = l == label, onClick = { label = if (label == l) BookmarkLabel.NONE else l })
                }
            }

            Surface(
                onClick = { onSave(note, label) },
                shape = RoundedCornerShape(15.dp),
                color = AlkahfColors.Accent,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            ) {
                Box(Modifier.fillMaxWidth().padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.bookmark_save),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlkahfColors.OnAccent,
                    )
                }
            }
        }
    }
}

private data class RangePreview(val surahName: String, val page: Int, val text: String)

@Composable
private fun Kicker(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = AlkahfColors.InkMuted,
        modifier = modifier,
    )
}

@Composable
fun LabelChip(label: BookmarkLabel, selected: Boolean, onClick: () -> Unit) {
    val colors = labelColors(label)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) colors.chipBg else AlkahfColors.Surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) colors.accent else AlkahfColors.CardBorder),
    ) {
        Row(
            Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(colors.accent))
            Text(
                text = labelName(label) ?: "",
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) colors.chipText else AlkahfColors.InkSecondary,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun CompositionLocalProviderRtl(content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        content = content,
    )
}
