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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.ui.theme.AlkahfColors

enum class AlkahfTab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Outlined.Home),
    MUSHAF("Mushaf", Icons.AutoMirrored.Outlined.MenuBook),
    REVIEW("Review", Icons.Outlined.Autorenew),
    PROGRESS("Progress", Icons.Outlined.BarChart),
    LIBRARY("Library", Icons.Outlined.Download),
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
                            contentDescription = tab.label,
                            modifier = Modifier.size(23.dp),
                        )
                    },
                    label = {
                        Text(
                            text = tab.label,
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
