package app.alkahf.ui.exercises

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale as drawScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import app.alkahf.R
import app.alkahf.ui.theme.AlkahfColors

/**
 * A self-contained on-screen Quranic Arabic keyboard for the "Finish the Āyah"
 * exercise. Stateless about the answer text: every key reports through one of the
 * callbacks and the host screen owns the answer string.
 *
 * The letter rows use the standard Arabic keyboard layout and read left-to-right
 * exactly as the keys sit on a physical / Gboard Arabic keyboard (ض top-left, ش
 * home-row-left, ئ bottom-left), so a user's typing muscle memory carries over.
 *
 * Glyphs are rendered with the device default Arabic (Naskh) font on purpose —
 * no custom [androidx.compose.ui.text.font.FontFamily] is set, so the combining
 * marks render correctly on the dotted-circle (◌, U+25CC) base. The bundled
 * Amiri font is too calligraphic for keycaps.
 */
@Composable
fun QuranicKeyboard(
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The keyboard mirrors a physical Arabic keyboard, whose keys are laid out
    // left-to-right regardless of the UI language. Force LTR so the layout never
    // flips when the app language is Arabic (RTL ambient direction) — ض must stay
    // top-left, backspace bottom-right, in every language.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                // The tray fills to the very bottom edge (drawn before the inset
                // padding below), so it reads as a native keyboard; the keys are
                // padded above the gesture bar by navigationBarsPadding.
                .background(AlkahfColors.KeyboardTray)
                .navigationBarsPadding()
                .padding(start = 5.dp, end = 5.dp, top = 7.dp, bottom = 9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            CaptionRow(
                left = stringResource(R.string.ex_kbd_harakat_hint),
                right = stringResource(R.string.ex_kbd_harakat_term),
                ink = AlkahfColors.KeyboardHarakatGlyph,
            )
            HarakatStrip(onKey)
            CaptionRow(
                left = stringResource(R.string.ex_kbd_marks_hint),
                right = stringResource(R.string.ex_kbd_marks_term),
                ink = AlkahfColors.KeyboardUthmaniGlyph,
            )
            UthmaniStrip(onKey)
            LetterRow(LETTER_ROW_1, onKey)
            LetterRow(LETTER_ROW_2, onKey, padding = 16.dp)
            LetterRow3(onKey, onBackspace)
            FunctionRow(onSpace, onCheck)
        }
    }
}

// ── Data: keys driven from lists of specs ──────────────────────────────────

/** Ḥarakāt: (combining mark to insert) → (keycap glyph on the ◌ base). */
private val HARAKAT = listOf(
    "َ" to "◌َ", // fatḥa
    "ِ" to "◌ِ", // kasra
    "ُ" to "◌ُ", // ḍamma
    "ْ" to "◌ْ", // sukūn
    "ّ" to "◌ّ", // shadda
    "ً" to "◌ً", // fatḥatān
    "ٍ" to "◌ٍ", // kasratān
    "ٌ" to "◌ٌ", // ḍammatān
)

/** ʿUthmānī marks: (string to insert) → (keycap glyph). The last entry is the waqf key. */
private val UTHMANI = listOf(
    "ٱ" to "ٱ",   // hamzat waṣl (full letter)
    "ٰ" to "◌ٰ",  // dagger / superscript alif
    "ٓ" to "◌ٓ",  // maddah
    "ۡ" to "◌ۡ",  // round ʿUthmānī sukūn
    "ۢ" to "◌ۢ",  // iqlāb small mīm
    "۟" to "◌۟",  // silent "zero"
)

/** Default waqf glyph shown on the waqf key. */
private const val WAQF_KEY_GLYPH = "ۚ" // ۚ

/** Pause signs offered in the waqf long-press drawer. */
private val WAQF_SIGNS = listOf(
    "ۖ", "ۗ", "ۘ", "ۙ", "ۚ", "ۛ",
)

// The standard Arabic keyboard layout (the same key positions as Gboard / iOS /
// any physical Arabic keyboard), so muscle memory carries over. The rows read
// left-to-right exactly as they sit on a real keyboard — ض top-left, ش on the
// home-row left, ئ bottom-left — NOT mirrored.
private val LETTER_ROW_1 = listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "د")
private val LETTER_ROW_2 = listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط")
private val LETTER_ROW_3 = listOf("ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "ظ")

/**
 * Long-press variants for letters that carry hamza / ʿUthmānī alternates, shown
 * in a popover (iOS-style). The base letter stays the tap action; long-press
 * offers the family. The first entry is the base letter itself.
 */
private val LETTER_VARIANTS = mapOf(
    "ا" to listOf("ا", "أ", "إ", "آ", "ٱ"),
    "ء" to listOf("ء", "أ", "إ", "آ", "ؤ", "ئ"),
    "و" to listOf("و", "ؤ"),
    "ي" to listOf("ي", "ئ", "ى"),
    "ه" to listOf("ه", "ة"),
    "ة" to listOf("ة", "ه"),
    "د" to listOf("د", "ذ"),
)

private val KEY_HEIGHT = 44.dp
private val KEY_GAP = 5.dp

// ── Rows ───────────────────────────────────────────────────────────────────

@Composable
private fun CaptionRow(left: String, right: String, ink: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = left,
            color = AlkahfColors.InkMuted,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
        // Force RTL so the Arabic caption sits on the trailing (right) edge.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(text = right, color = AlkahfColors.InkMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun HarakatStrip(onKey: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(AlkahfColors.KeyboardHarakatBg)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
    ) {
        HARAKAT.forEach { (insert, cap) ->
            Key(
                modifier = Modifier.weight(1f),
                glyph = cap,
                glyphColor = AlkahfColors.KeyboardHarakatGlyph,
                background = AlkahfColors.KeyboardHarakatKey,
                borderColor = AlkahfColors.KeyboardHarakatBorder,
                shape = RoundedCornerShape(8.dp),
                glyphSize = 22.sp,
                onClick = { onKey(insert) },
            )
        }
    }
}

@Composable
private fun UthmaniStrip(onKey: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(AlkahfColors.KeyboardUthmaniBg)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
    ) {
        UTHMANI.forEach { (insert, cap) ->
            Key(
                modifier = Modifier.weight(1f),
                glyph = cap,
                glyphColor = AlkahfColors.KeyboardUthmaniGlyph,
                background = AlkahfColors.KeyboardUthmaniKey,
                borderColor = AlkahfColors.KeyboardUthmaniBorder,
                shape = RoundedCornerShape(8.dp),
                glyphSize = 22.sp,
                onClick = { onKey(insert) },
            )
        }
        WaqfKey(modifier = Modifier.weight(1f), onKey = onKey)
    }
}

@Composable
private fun LetterRow(letters: List<String>, onKey: (String) -> Unit, padding: Dp = 0.dp) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = padding),
        horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
    ) {
        letters.forEach { letter -> LetterKey(letter, onKey, Modifier.weight(1f)) }
    }
}

@Composable
private fun LetterRow3(onKey: (String) -> Unit, onBackspace: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
    ) {
        LETTER_ROW_3.forEach { letter -> LetterKey(letter, onKey, Modifier.weight(1f)) }
        // Backspace sits bottom-right (its position on a real keyboard), widened
        // to fill the two trailing columns so the letters align under row 1.
        SpecialKey(modifier = Modifier.weight(2f), onClick = onBackspace) { BackspaceIcon() }
    }
}

@Composable
private fun FunctionRow(onSpace: () -> Unit, onCheck: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KEY_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpaceKey(modifier = Modifier.weight(5f), onClick = onSpace)
        CheckKey(modifier = Modifier.weight(2f), onClick = onCheck)
    }
}

/**
 * The space bar. Shows a language-neutral underscore mark (no text) so it reads
 * the same regardless of the app's language.
 */
@Composable
private fun SpaceKey(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = modifier
            .height(46.dp)
            .scale(if (pressed) 0.96f else 1f)
            .clip(shape)
            .background(AlkahfColors.KeyboardLetterKey)
            .border(1.dp, AlkahfColors.CardBorder, shape)
            .clickableKey(interaction, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.28f)
                .height(2.dp)
                .clip(RoundedCornerShape(50))
                .background(AlkahfColors.InkFaint),
        )
    }
}

/** A letter keycap styled like an app card (soft surface + hairline border). */
@Composable
private fun LetterKey(letter: String, onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    Key(
        modifier = modifier,
        glyph = letter,
        glyphColor = AlkahfColors.Ink,
        background = AlkahfColors.KeyboardLetterKey,
        borderColor = AlkahfColors.CardBorder,
        shape = RoundedCornerShape(7.dp),
        glyphSize = 20.sp,
        variants = LETTER_VARIANTS[letter],
        onVariant = onKey,
        onClick = { onKey(letter) },
    )
}

// ── Keys ───────────────────────────────────────────────────────────────────

/**
 * A keycap: a centered glyph on a rounded fill, optionally bordered. When
 * [variants] is non-null the key shows a long-press indicator and opens a
 * popover of those alternates ([onVariant] inserts the chosen one).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Key(
    glyph: String,
    glyphColor: Color,
    background: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    shape: RoundedCornerShape = RoundedCornerShape(7.dp),
    glyphSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    height: Dp = KEY_HEIGHT,
    variants: List<String>? = null,
    onVariant: (String) -> Unit = {},
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var showPopover by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .height(height)
            .scale(if (pressed) 0.96f else 1f)
            .clip(shape)
            .background(background)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .then(
                if (variants != null) {
                    Modifier.combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                        onLongClick = { showPopover = true },
                    )
                } else {
                    Modifier.clickableKey(interaction, onClick)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Glyphs render with the device default (Naskh) font — no custom family.
        Text(
            text = glyph,
            color = glyphColor,
            fontSize = glyphSize,
            textAlign = TextAlign.Center,
        )
        if (variants != null) {
            LongPressDots(
                color = glyphColor.copy(alpha = 0.3f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 3.dp),
            )
            if (showPopover) {
                GlyphPopover(
                    glyphs = variants,
                    onPick = { onVariant(it); showPopover = false },
                    onDismiss = { showPopover = false },
                )
            }
        }
    }
}

/** A special (function) key: a darker fill hosting custom icon content. */
@Composable
private fun SpecialKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = KEY_HEIGHT,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = modifier
            .height(height)
            .scale(if (pressed) 0.96f else 1f)
            .clip(shape)
            .background(AlkahfColors.KeyboardSpecialKey)
            .border(1.dp, AlkahfColors.CardBorder, shape)
            .clickableKey(interaction, onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** The green Check return key (Accent fill, white check + label). */
@Composable
private fun CheckKey(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = modifier
            .height(46.dp)
            .scale(if (pressed) 0.96f else 1f)
            .clip(RoundedCornerShape(7.dp))
            .background(AlkahfColors.Accent)
            .clickableKey(interaction, onClick),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckIcon()
        Text(
            stringResource(R.string.ex_kbd_check),
            color = AlkahfColors.OnAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * The waqf key (sage accent). Tap inserts the default pause sign (ۚ);
 * long-press opens a drawer of pause signs (ۖ ۗ ۘ ۙ ۚ ۛ).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WaqfKey(onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    var showDrawer by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(KEY_HEIGHT)
            .scale(if (pressed) 0.96f else 1f)
            .clip(shape)
            .background(AlkahfColors.KeyboardWaqfBg)
            .border(1.5.dp, AlkahfColors.KeyboardWaqfRing, shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = { onKey(WAQF_KEY_GLYPH) },
                onLongClick = { showDrawer = true },
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Waqf marks are combining signs: render on the dotted-circle base (◌) so
        // they're visible (a bare mark shows as nothing / tofu).
        Text(dotted(WAQF_KEY_GLYPH), color = AlkahfColors.KeyboardUthmaniGlyph, fontSize = 22.sp)
        LongPressDots(
            color = AlkahfColors.KeyboardWaqfRing,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 4.dp, bottom = 3.dp),
        )
        if (showDrawer) {
            GlyphPopover(
                glyphs = WAQF_SIGNS,
                cap = ::dotted,
                onPick = { onKey(it); showDrawer = false },
                onDismiss = { showDrawer = false },
            )
        }
    }
}

/** The dotted-circle base (U+25CC) plus a combining [mark], so the mark shows. */
private fun dotted(mark: String): String = "◌$mark"

// ── Popover / drawer ────────────────────────────────────────────────────────

/**
 * A light, elevated popover anchored above its parent key, styled to match the
 * app's surfaces (dark ink on a soft surface for strong contrast). Tapping a
 * glyph inserts it and dismisses; tapping outside dismisses without inserting.
 *
 * [glyphs] are the values inserted on pick; [cap] maps each to the glyph shown
 * on its key (identity for letters; the dotted-circle form for combining marks).
 */
@Composable
private fun GlyphPopover(
    glyphs: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    cap: (String) -> String = { it },
) {
    val density = LocalDensity.current
    val positionProvider = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: androidx.compose.ui.unit.IntRect,
                windowSize: androidx.compose.ui.unit.IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: androidx.compose.ui.unit.IntSize,
            ): IntOffset {
                val gap = with(density) { 8.dp.roundToPx() }
                val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                val y = anchorBounds.top - popupContentSize.height - gap
                return IntOffset(
                    x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
                    y.coerceAtLeast(0),
                )
            }
        }
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        val shape = RoundedCornerShape(16.dp)
        Row(
            modifier = Modifier
                .wrapContentSize()
                .shadow(10.dp, shape)
                .background(AlkahfColors.Surface, shape)
                .border(1.dp, AlkahfColors.HeaderRule, shape)
                .padding(7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            val keyShape = RoundedCornerShape(10.dp)
            glyphs.forEach { g ->
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                Box(
                    modifier = Modifier
                        .size(width = 42.dp, height = 50.dp)
                        .scale(if (pressed) 0.94f else 1f)
                        .clip(keyShape)
                        .background(AlkahfColors.Paper)
                        .border(1.dp, AlkahfColors.CardBorder, keyShape)
                        .clickableKey(interaction) { onPick(g) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = cap(g),
                        color = AlkahfColors.Ink,
                        fontSize = 26.sp,
                    )
                }
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

/** A no-ripple clickable wired to a shared [interaction] source for the pressed scale. */
private fun Modifier.clickableKey(
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier = this.then(
    Modifier.combinedClickableNoIndication(interaction, onClick),
)

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableNoIndication(
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier = combinedClickable(
    interactionSource = interaction,
    indication = null,
    onClick = onClick,
)

/** The three-dot "long-press available" indicator. */
@Composable
private fun LongPressDots(color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
        repeat(3) {
            Box(Modifier.size(3.dp).clip(RoundedCornerShape(50)).background(color))
        }
    }
}

// ── Icons (drawn to match the design's inline SVGs) ──────────────────────────

@Composable
private fun BackspaceIcon() {
    val stroke = AlkahfColors.InkSecondaryDark
    Canvas(Modifier.size(22.dp)) {
        scaled(24f) {
            val s = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val body = androidx.compose.ui.graphics.Path().apply {
                moveTo(20f, 6f); lineTo(9.5f, 6f)
                cubicTo(8.9f, 6f, 8.3f, 6.25f, 8f, 6.7f)
                lineTo(3f, 12f); lineTo(8f, 17.3f)
                cubicTo(8.3f, 17.75f, 8.9f, 18f, 9.5f, 18f)
                lineTo(20f, 18f)
                cubicTo(20.55f, 18f, 21f, 17.55f, 21f, 17f)
                lineTo(21f, 7f)
                cubicTo(21f, 6.45f, 20.55f, 6f, 20f, 6f); close()
            }
            drawPath(body, color = stroke, style = s)
            drawLine(stroke, Offset(16f, 9.5f), Offset(12f, 14.5f), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(stroke, Offset(12f, 9.5f), Offset(16f, 14.5f), strokeWidth = 2f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun CheckIcon() {
    val stroke = AlkahfColors.OnAccent
    Canvas(Modifier.size(17.dp)) {
        scaled(24f) {
            val s = Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(20f, 6f); lineTo(9f, 17f); lineTo(4f, 12f)
            }
            drawPath(p, color = stroke, style = s)
        }
    }
}

/** Runs [block] with the canvas scaled so a [viewBox]×[viewBox] coordinate space fills the drawing area. */
private fun DrawScope.scaled(viewBox: Float, block: DrawScope.() -> Unit) {
    val k = size.minDimension / viewBox
    drawScale(k, k, pivot = Offset.Zero) { block() }
}
