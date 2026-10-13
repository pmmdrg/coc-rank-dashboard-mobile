package com.cocrank.dashboard.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cocrank.dashboard.R
import com.cocrank.dashboard.ui.screens.clan.ClanScreen
import com.cocrank.dashboard.ui.screens.dashboard.DashboardScreen
import com.cocrank.dashboard.ui.screens.dashboard.DashboardViewModel
import com.cocrank.dashboard.ui.screens.personal.PersonalScreen
import com.cocrank.dashboard.ui.screens.settings.SettingsScreen
import com.cocrank.dashboard.ui.theme.GoldAccent

sealed class Screen(val route: String, val title: String, @DrawableRes val iconRes: Int) {
    object Personal : Screen("personal", "Cá nhân", R.drawable.ph_user)
    object Season : Screen("season", "Mùa giải", R.drawable.ph_trophy)
    object Clan : Screen("clan", "Clan", R.drawable.ph_shield)
    object Settings : Screen("settings", "Cài đặt", R.drawable.ph_gear)
}

private fun getScreenIndex(route: String?): Int = when (route) {
    Screen.Personal.route -> 0
    Screen.Season.route -> 1
    Screen.Clan.route -> 2
    Screen.Settings.route -> 3
    else -> 0
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.createEnterTransition(fromIndex: Int, toIndex: Int) =
    if (fromIndex < toIndex) {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(animationSpec = tween(260))
    } else {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(animationSpec = tween(260))
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.createExitTransition(fromIndex: Int, toIndex: Int) =
    if (fromIndex < toIndex) {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeOut(animationSpec = tween(220))
    } else {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeOut(animationSpec = tween(220))
    }

@Composable
fun AppNavigation(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomScreens = listOf(
        Screen.Personal,
        Screen.Season,
        Screen.Clan
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Personal.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(
                route = Screen.Personal.route,
                enterTransition = { createEnterTransition(getScreenIndex(initialState.destination.route), 0) },
                exitTransition = { createExitTransition(0, getScreenIndex(targetState.destination.route)) }
            ) {
                PersonalScreen(
                    viewModel = viewModel,
                    onOpenSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            composable(
                route = Screen.Season.route,
                enterTransition = { createEnterTransition(getScreenIndex(initialState.destination.route), 1) },
                exitTransition = { createExitTransition(1, getScreenIndex(targetState.destination.route)) }
            ) {
                DashboardScreen(viewModel = viewModel)
            }
            composable(
                route = Screen.Clan.route,
                enterTransition = { createEnterTransition(getScreenIndex(initialState.destination.route), 2) },
                exitTransition = { createExitTransition(2, getScreenIndex(targetState.destination.route)) }
            ) {
                ClanScreen(viewModel = viewModel)
            }
            composable(
                route = Screen.Settings.route,
                enterTransition = { createEnterTransition(getScreenIndex(initialState.destination.route), 3) },
                exitTransition = { createExitTransition(3, getScreenIndex(targetState.destination.route)) }
            ) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Floating Liquid Capsule Navigation Bar overlay (chỉ hiện ở 3 màn hình chính)
        if (currentRoute != Screen.Settings.route) {
            LiquidCapsuleNavigationBar(
                screens = bottomScreens,
                currentRoute = currentRoute,
                onNavigate = { screen ->
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
fun LiquidCapsuleNavigationBar(
    screens: List<Screen>,
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = screens.indexOfFirst { it.route == currentRoute }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color.Black.copy(alpha = 0.35f),
                    ambientColor = Color.Black.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            val totalWidth = maxWidth
            val tabCount = screens.size
            val tabWidth = totalWidth / tabCount

            val targetPos = if (selectedIndex >= 0) selectedIndex.toFloat() else 0f
            val isVisible = selectedIndex >= 0

            // Liquid bouncing indicator position with juicy iOS spring physics
            val animatedPosition by animateFloatAsState(
                targetValue = targetPos,
                animationSpec = spring(
                    dampingRatio = 0.68f, // Elastic liquid bounce
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "liquid_indicator_offset"
            )

            // Dynamic liquid pill indicator with generous breathing room
            if (isVisible) {
                Box(
                    modifier = Modifier
                        .offset(x = tabWidth * animatedPosition)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(GoldAccent.copy(alpha = 0.16f))
                        .border(
                            width = 1.dp,
                            color = GoldAccent.copy(alpha = 0.42f),
                            shape = RoundedCornerShape(26.dp)
                        )
                )
            }

            // Tab items row
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEachIndexed { index, screen ->
                    val isSelected = selectedIndex == index
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 0.95f,
                        animationSpec = spring(
                            dampingRatio = 0.65f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "tab_scale_$index"
                    )
                    val tabColor by animateColorAsState(
                        targetValue = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        animationSpec = tween(durationMillis = 200),
                        label = "tab_color_$index"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onNavigate(screen)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = screen.iconRes),
                            contentDescription = screen.title,
                            tint = tabColor,
                            modifier = Modifier
                                .size(19.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                        Spacer(modifier = Modifier.height(1.5.dp))
                        Text(
                            text = screen.title,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = tabColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
