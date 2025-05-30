package com.deaworks.notifisync.ui.components

import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.deaworks.notifisync.R


data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    val iconR: Int? = null
)

@Composable
fun BottomNavigationBar(navController: NavHostController, modifier: Modifier = Modifier) {

    val isXiaomi = Build.MANUFACTURER.equals("xiaomi", ignoreCase = true)
    val bottomPadding = if (isXiaomi) 0.dp else 0.dp

    NavigationBar(
        modifier = modifier
            .padding(bottom = bottomPadding) // 小米设备增加底部间距
            .windowInsetsPadding(
                WindowInsets.systemBars
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
            )
    ) {
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination
        val items = listOf(
            BottomNavItem("home", "主页", Icons.Default.Home),
            BottomNavItem("logs", "日志", iconR = R.drawable.baseline_assignment_24),
            BottomNavItem("settings", "设置", Icons.Default.Settings)
        )
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    if (item.icon != null) {
                        Icon(item.icon, contentDescription = item.label)
                    } else if (item.iconR != null) {
                        //不然就用drawable里的
                        Icon(
                            painter = painterResource(id = item.iconR),
                            contentDescription = item.label
                        )
                    }
                },
                label = { Text(item.label) },
                selected = currentDestination?.route == item.route,
                onClick = {
                    //TODO: 找个好方法代替这个检查,顺便解决一下无触摸反馈的问题
                    if (currentDestination?.route != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}