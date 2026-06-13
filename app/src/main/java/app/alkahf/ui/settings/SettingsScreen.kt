package app.alkahf.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.AlkahfApplication
import app.alkahf.R
import app.alkahf.data.ReviewPacing
import app.alkahf.data.SettingsData
import app.alkahf.ui.theme.AlkahfColors
import app.alkahf.ui.theme.AmiriQuran
import app.alkahf.ui.theme.ThemeMode

private const val SAMPLE_BASMALA = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as AlkahfApplication).repository }
    var settings by remember { mutableStateOf(repository.settings()) }

    fun apply(updated: SettingsData) {
        settings = updated
        repository.updateSettings(updated)
    }

    Column(Modifier.fillMaxSize().background(AlkahfColors.Paper).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 10.dp),
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
            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        ) {
            SectionCaption(stringResource(R.string.settings_section_language))
            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    RowLabel(stringResource(R.string.settings_language))
                    Segmented(
                        options = listOf(
                            "system" to stringResource(R.string.settings_language_auto),
                            "en" to "English",
                            "ar" to "العربية",
                        ),
                        selected = settings.appLanguage,
                        onSelect = { language ->
                            if (language != settings.appLanguage) {
                                settings = settings.copy(appLanguage = language)
                                onLanguageChange(language)
                            }
                        },
                    )
                }
            }

            SectionCaption(stringResource(R.string.settings_section_appearance))
            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    RowLabel(stringResource(R.string.settings_theme))
                    Segmented(
                        options = listOf(
                            ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                            ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
                            ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                        ),
                        selected = settings.themeMode.toMode(),
                        onSelect = { mode ->
                            apply(settings.copy(themeMode = mode.name.lowercase()))
                            onThemeModeChange(mode)
                        },
                    )
                    Divider()
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RowLabel(stringResource(R.string.settings_arabic_text_size))
                        Text(
                            text = stringResource(R.string.settings_pt_value, settings.arabicTextSizePt),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlkahfColors.AccentDeep,
                        )
                    }
                    Text(
                        text = SAMPLE_BASMALA,
                        fontFamily = AmiriQuran,
                        fontSize = settings.arabicTextSizePt.sp,
                        color = AlkahfColors.Ink,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Slider(
                        value = settings.arabicTextSizePt.toFloat(),
                        onValueChange = { apply(settings.copy(arabicTextSizePt = it.toInt())) },
                        valueRange = 20f..40f,
                        colors = SliderDefaults.colors(
                            thumbColor = AlkahfColors.Accent,
                            activeTrackColor = AlkahfColors.Accent,
                            inactiveTrackColor = AlkahfColors.NotStarted,
                        ),
                    )
                    Divider()
                    NavRow(label = stringResource(R.string.settings_script), value = "KFGQPC Uthmanic · HAFS")
                }
            }

            SectionCaption(stringResource(R.string.settings_section_review))
            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    StepperRow(
                        label = stringResource(R.string.settings_daily_budget),
                        value = stringResource(R.string.settings_min_value, settings.dailyBudgetMin),
                        onDecrement = { apply(settings.copy(dailyBudgetMin = (settings.dailyBudgetMin - 5).coerceAtLeast(5))) },
                        onIncrement = { apply(settings.copy(dailyBudgetMin = (settings.dailyBudgetMin + 5).coerceAtMost(120))) },
                    )
                    Divider()
                    StepperRow(
                        label = stringResource(R.string.settings_new_per_day),
                        value = "${settings.newPerDay}",
                        onDecrement = { apply(settings.copy(newPerDay = (settings.newPerDay - 1).coerceAtLeast(1))) },
                        onIncrement = { apply(settings.copy(newPerDay = (settings.newPerDay + 1).coerceAtMost(20))) },
                    )
                    Divider()
                    RowLabel(stringResource(R.string.settings_review_pacing))
                    Segmented(
                        options = listOf(
                            ReviewPacing.GENTLE to stringResource(R.string.settings_pacing_gentle),
                            ReviewPacing.STANDARD to stringResource(R.string.settings_pacing_standard),
                            ReviewPacing.AGGRESSIVE to stringResource(R.string.settings_pacing_aggressive),
                        ),
                        selected = settings.reviewPacing,
                        onSelect = { apply(settings.copy(reviewPacing = it)) },
                    )
                    Text(
                        text = pacingHint(settings.reviewPacing),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = AlkahfColors.InkFaint,
                    )
                    Divider()
                    ToggleRow(
                        label = stringResource(R.string.settings_stumbles_lower),
                        subtitle = stringResource(R.string.settings_stumbles_lower_sub),
                        checked = settings.autoLowerOnStumble,
                        onChange = { apply(settings.copy(autoLowerOnStumble = it)) },
                    )
                }
            }

            SectionCaption(stringResource(R.string.settings_section_general))
            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ToggleRow(
                        label = stringResource(R.string.settings_keep_screen_on),
                        subtitle = null,
                        checked = settings.keepScreenOn,
                        onChange = { apply(settings.copy(keepScreenOn = it)) },
                    )
                    Divider()
                    ToggleRow(
                        label = stringResource(R.string.settings_background_audio),
                        subtitle = stringResource(R.string.settings_coming_soon),
                        checked = settings.backgroundAudio,
                        onChange = { apply(settings.copy(backgroundAudio = it)) },
                    )
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.settings_app_version),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.InkFaint,
                )
                Text(
                    text = stringResource(R.string.settings_footer_privacy),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFooter,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun String.toMode(): ThemeMode = when (this) {
    "light" -> ThemeMode.LIGHT
    "dark" -> ThemeMode.DARK
    else -> ThemeMode.SYSTEM
}

@Composable
private fun pacingHint(pacing: ReviewPacing): String = stringResource(
    when (pacing) {
        ReviewPacing.GENTLE -> R.string.settings_pacing_gentle_hint
        ReviewPacing.STANDARD -> R.string.settings_pacing_standard_hint
        ReviewPacing.AGGRESSIVE -> R.string.settings_pacing_aggressive_hint
    },
)

@Composable
private fun SectionCaption(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        color = AlkahfColors.InkFooter,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 10.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) { content() }
}

@Composable
private fun RowLabel(text: String) {
    Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AlkahfColors.Ink)
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(AlkahfColors.CardBorder))
}

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(AlkahfColors.SegmentedTrack, RoundedCornerShape(12.dp)).padding(3.dp),
    ) {
        options.forEach { (option, label) ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .let {
                        if (isSelected) it.background(AlkahfColors.SegmentedSelected, RoundedCornerShape(9.dp)) else it
                    }
                    .clickable { onSelect(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) AlkahfColors.AccentDeep else AlkahfColors.InkMuted,
                )
            }
        }
    }
}

@Composable
private fun StepperRow(label: String, value: String, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RowLabel(label)
        Box(Modifier.weight(1f))
        StepButton("−", onDecrement)
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = AlkahfColors.Ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.width(64.dp),
        )
        StepButton("+", onIncrement)
    }
}

@Composable
private fun StepButton(glyph: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = AlkahfColors.PageSurface,
        border = BorderStroke(1.dp, AlkahfColors.ControlBorder),
        modifier = Modifier.size(30.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = glyph, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.InkChrome)
        }
    }
}

@Composable
private fun ToggleRow(label: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            RowLabel(label)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Toggle(checked = checked, onChange = onChange)
    }
}

@Composable
private fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .size(width = 48.dp, height = 28.dp)
            .background(
                if (checked) AlkahfColors.Accent else AlkahfColors.DashedNode,
                CircleShape,
            )
            .clickable { onChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(22.dp).background(AlkahfColors.OnAccent, CircleShape))
    }
}

@Composable
private fun NavRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RowLabel(label)
        Box(Modifier.weight(1f))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AlkahfColors.InkFaint,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = AlkahfColors.Chevron,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
