package app.alkahf.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.R
import app.alkahf.ui.theme.AlkahfColors

enum class AlkahfTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    TODAY(R.string.nav_today, Icons.Outlined.Home),
    MUSHAF(R.string.nav_mushaf, Icons.AutoMirrored.Outlined.MenuBook),
    REVIEW(R.string.nav_review, Icons.Outlined.Autorenew),
    PROGRESS(R.string.nav_progress, Icons.Outlined.BarChart),
    LIBRARY(R.string.nav_library, Icons.Outlined.Download),
}

@Composable
fun AlkahfBottomNav(selected: AlkahfTab, onSelect: (AlkahfTab) -> Unit) {
    Column {
        HorizontalDivider(thickness = 1.dp, color = AlkahfColors.CardBorder)
        NavigationBar(containerColor = AlkahfColors.NavSurface) {
            AlkahfTab.entries.forEach { tab ->
                val isSelected = tab == selected
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { if (!isSelected) onSelect(tab) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.labelRes),
                            modifier = Modifier.size(23.dp),
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(tab.labelRes),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = AlkahfColors.AccentTint,
                        selectedIconColor = AlkahfColors.AccentDeep,
                        selectedTextColor = AlkahfColors.AccentDeep,
                        unselectedIconColor = AlkahfColors.InkMuted,
                        unselectedTextColor = AlkahfColors.InkMuted,
                    ),
                )
            }
        }
    }
}
