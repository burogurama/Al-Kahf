package app.alkahf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AlkahfColors.Accent,
    onPrimary = AlkahfColors.OnAccent,
    secondaryContainer = AlkahfColors.AccentTint,
    onSecondaryContainer = AlkahfColors.AccentDeep,
    background = AlkahfColors.Paper,
    onBackground = AlkahfColors.Ink,
    surface = AlkahfColors.Surface,
    onSurface = AlkahfColors.Ink,
    surfaceVariant = AlkahfColors.ChipBg,
    onSurfaceVariant = AlkahfColors.InkSecondary,
    outline = AlkahfColors.CardBorder,
    outlineVariant = AlkahfColors.CardBorder,
)

@Composable
fun AlkahfTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AlkahfTypography,
        content = content,
    )
}
