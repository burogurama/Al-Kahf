package app.alkahf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

enum class ThemeMode { LIGHT, DARK, SYSTEM }

private fun materialScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
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
} else {
    lightColorScheme(
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
}

@Composable
fun AlkahfTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    // Swap the token palette before content reads it this composition.
    remember(dark) { AlkahfColors.setDark(dark); dark }
    MaterialTheme(
        colorScheme = materialScheme(dark),
        typography = AlkahfTypography,
        content = content,
    )
}
