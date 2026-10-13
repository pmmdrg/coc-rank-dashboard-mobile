package com.cocrank.dashboard.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cocrank.dashboard.ui.screens.changes.RankChangesScreen
import com.cocrank.dashboard.ui.screens.clan.ClanScreen
import com.cocrank.dashboard.ui.screens.dashboard.DashboardScreen
import com.cocrank.dashboard.ui.screens.dashboard.DashboardViewModel
import com.cocrank.dashboard.ui.screens.settings.SettingsScreen
import com.cocrank.dashboard.ui.theme.DarkBackground
import com.cocrank.dashboard.ui.theme.DarkSurface
import com.cocrank.dashboard.ui.theme.GoldAccent

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Bảng đấu", Icons.Default.EmojiEvents)
    object Changes : Screen("changes", "Biến động", Icons.AutoMirrored.Filled.TrendingUp)
    object Clan : Screen("clan", "Bang hội", Icons.Default.Group)
    object Settings : Screen("settings", "Cài đặt", Icons.Default.Settings)
}

@Composable
fun AppNavigation(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val screens = listOf(
        Screen.Dashboard,
        Screen.Changes,
        Screen.Clan,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 4.dp
            ) {
                screens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkBackground,
                            selectedTextColor = GoldAccent,
                            indicatorColor = GoldAccent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(viewModel = viewModel)
            }
            composable(Screen.Changes.route) {
                RankChangesScreen(viewModel = viewModel)
            }
            composable(Screen.Clan.route) {
                ClanScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
