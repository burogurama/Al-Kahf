package app.alkahf.ui.mushaf

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.R
import app.alkahf.data.Bookmark
import app.alkahf.data.MemorizationState
import app.alkahf.ui.bookmarks.bookmarkRelative
import app.alkahf.ui.bookmarks.bookmarkTitle
import app.alkahf.ui.bookmarks.labelName
import app.alkahf.ui.exercises.toEasternArabicDigits
import app.alkahf.ui.theme.AlkahfColors

/**
 * The redesigned āyah long-press surface (design handoff "Selection Sheet"):
 * a contextual bottom sheet carrying the range's memorization state, its saved
 * bookmark note (if any), a tafsīr slot, and the promoted quick actions.
 *
 * The tafsīr section is intentionally a placeholder here — its content and the
 * full reading view are a later step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionSheet(
    surahName: String,
    from: Int,
    to: Int,
    page: Int,
    currentState: MemorizationState,
    bookmark: Bookmark?,
    onListen: () -> Unit,
    onSabaq: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSetState: (MemorizationState) -> Unit,
    onEditNote: () -> Unit,
    onAddNote: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val count = to - from + 1
    // Local mirror of the range's state so tapping a cell highlights immediately
    // while the sheet stays open; re-seeds if the sheet reopens on a new range.
    var selectedState by remember(currentState) { mutableStateOf(currentState) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AlkahfColors.PageSurface,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 44.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(AlkahfColors.HeaderRule))
            }
        },
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            // ── Header ─────────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, top = 8.dp, end = 18.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(AlkahfColors.AccentTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = toEasternArabicDigits(to),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlkahfColors.AccentDeep,
                    )
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        text = bookmarkTitle(surahName, from, to),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                        color = AlkahfColors.Ink,
                    )
                    Text(
                        text = stringResource(
                            R.string.sel_selected_on_page,
                            pluralStringResource(R.plurals.mushaf_ayah_selected, count, count),
                            page,
                        ),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = AlkahfColors.InkFaint,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(50)).background(AlkahfColors.ChipBg)
                        .clickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.action_close), tint = AlkahfColors.InkSecondary, modifier = Modifier.size(15.dp))
                }
            }

            // ── Quick actions ──────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickAction(Modifier.weight(1f), Icons.Filled.PlayArrow, stringResource(R.string.mushaf_menu_listen), AlkahfColors.Accent, onListen)
                QuickAction(Modifier.weight(1f), Icons.AutoMirrored.Outlined.MenuBook, stringResource(R.string.sel_action_sabaq), AlkahfColors.Accent, onSabaq)
                QuickAction(Modifier.weight(1f), Icons.Outlined.ContentCopy, stringResource(R.string.sel_action_copy), AlkahfColors.InkSecondary, onCopy)
                QuickAction(Modifier.weight(1f), Icons.Outlined.Share, stringResource(R.string.sel_action_share), AlkahfColors.InkSecondary, onShare)
            }

            Column(Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 16.dp)) {
                // ── Memorization ──────────────────────────────────────────────
                Kicker(stringResource(R.string.sel_kicker_memorization))
                Row(
                    Modifier.fillMaxWidth().padding(top = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    listOf(
                        MemorizationState.NOT_STARTED to stringResource(R.string.state_not_started),
                        MemorizationState.LEARNING to stringResource(R.string.state_learning),
                        MemorizationState.MEMORIZED to stringResource(R.string.state_memorized),
                        MemorizationState.STRONG to stringResource(R.string.state_strong),
                    ).forEach { (state, label) ->
                        StateCell(
                            modifier = Modifier.weight(1f),
                            label = label,
                            dot = memMedallionColor(state),
                            isSelected = state == selectedState,
                            neutral = state == MemorizationState.NOT_STARTED,
                            onClick = {
                                selectedState = state
                                onSetState(state)
                            },
                        )
                    }
                }

                // ── Note ──────────────────────────────────────────────────────
                if (bookmark != null && bookmark.hasNote) {
                    NoteSection(bookmark, onEditNote)
                } else {
                    AddNoteSection(onAddNote)
                }

                // ── Tafsīr (placeholder; wired in a later step) ───────────────
                Kicker(stringResource(R.string.sel_kicker_tafsir), Modifier.padding(top = 18.dp))
                Box(
                    Modifier.fillMaxWidth().padding(top = 9.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AlkahfColors.SelSheetCard)
                        .border(BorderStroke(1.dp, AlkahfColors.SelSheetCardBorder), RoundedCornerShape(14.dp))
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.sel_tafsir_coming_soon),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AlkahfColors.InkFaint,
                    )
                }
            }

            // ── Footer ─────────────────────────────────────────────────────────
            HorizontalDivider(thickness = 1.dp, color = AlkahfColors.Hairline, modifier = Modifier.padding(top = 14.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.sel_reselect_hint),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                )
                Text(
                    text = stringResource(R.string.sel_clear),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.AccentDeep,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClear).padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
            Box(Modifier.fillMaxWidth().height(8.dp))
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AlkahfColors.SelSheetChip)
            .border(BorderStroke(1.dp, AlkahfColors.SelSheetCardBorder), RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, label, tint = iconTint, modifier = Modifier.size(20.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.InkSecondaryDark)
    }
}

@Composable
private fun StateCell(
    modifier: Modifier,
    label: String,
    dot: Color,
    isSelected: Boolean,
    neutral: Boolean,
    onClick: () -> Unit,
) {
    val bg = when {
        !isSelected -> AlkahfColors.SelSheetCard
        neutral -> AlkahfColors.SelSheetNeutralBg
        else -> dot.copy(alpha = 0.16f)
    }
    val border = when {
        !isSelected -> AlkahfColors.SelSheetCardBorder
        neutral -> AlkahfColors.SelSheetNeutralBorder
        else -> dot
    }
    Column(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .border(BorderStroke(1.5.dp, border), RoundedCornerShape(13.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { selected = isSelected }
            .padding(vertical = 11.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(50)).background(dot))
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) AlkahfColors.Ink else AlkahfColors.InkMuted,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp,
        )
    }
}

@Composable
private fun NoteSection(bookmark: Bookmark, onEdit: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Bookmark, null, tint = AlkahfColors.BookmarkRibbon, modifier = Modifier.size(14.dp))
            Text(
                text = stringResource(R.string.sel_kicker_your_note),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = AlkahfColors.KhatamGoldDeep,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
        Row(
            Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onEdit).padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Edit, null, tint = AlkahfColors.AccentDeep, modifier = Modifier.size(13.dp))
            Text(
                text = stringResource(R.string.sel_edit),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.AccentDeep,
                modifier = Modifier.padding(start = 5.dp),
            )
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 9.dp).height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(14.dp))
            .background(AlkahfColors.KhatamCardTint)
            .border(BorderStroke(1.dp, AlkahfColors.KhatamCardBorder), RoundedCornerShape(14.dp)),
    ) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(AlkahfColors.BookmarkRibbon))
        Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 13.dp)) {
            Text(
                text = bookmark.note,
                fontSize = 14.5.sp,
                lineHeight = 23.sp,
                color = AlkahfColors.InkSecondaryDark,
            )
            val labelStr = labelName(bookmark.label)
            val edited = stringResource(R.string.sel_note_edited, bookmarkRelative(context, bookmark.updatedAt))
            Text(
                text = if (labelStr != null) "$labelStr · $edited" else edited,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFooter,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun AddNoteSection(onAddNote: () -> Unit) {
    Kicker(stringResource(R.string.sel_kicker_note_bookmark), Modifier.padding(top = 18.dp))
    Row(
        Modifier.fillMaxWidth().padding(top = 9.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AlkahfColors.SelSheetCard)
            .dashedBorder(AlkahfColors.SelSheetDashed, 14.dp)
            .clickable(role = Role.Button, onClick = onAddNote)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(AlkahfColors.KhatamCardTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.BookmarkAdd, null, tint = AlkahfColors.BookmarkRibbon, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = stringResource(R.string.sel_add_note_title),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
            )
            Text(
                text = stringResource(R.string.sel_add_note_subtitle),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFaint,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = AlkahfColors.Chevron, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Kicker(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = AlkahfColors.InkFaint,
        modifier = modifier,
    )
}

/** A rounded dashed outline, matching the design's "add a note" invite. */
private fun Modifier.dashedBorder(color: Color, radius: androidx.compose.ui.unit.Dp) = drawBehind {
    val stroke = 1.5.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx()),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 7.dp.toPx()))),
    )
}
