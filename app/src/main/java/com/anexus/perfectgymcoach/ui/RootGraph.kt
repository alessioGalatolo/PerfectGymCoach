package com.anexus.perfectgymcoach.ui

import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.ui.destinations.HistoryDestination
import com.anexus.perfectgymcoach.ui.destinations.HomeDestination
import com.anexus.perfectgymcoach.ui.destinations.ProfileDestination
import com.anexus.perfectgymcoach.ui.destinations.StatisticsDestination
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.popBackStack
import com.ramcosta.composedestinations.navigation.popUpTo
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

enum class BottomBarDestination(
    val direction: DirectionDestinationSpec,
    @StringRes val label: Int,
    val icon: ImageVector,
    val iconSelected: ImageVector
) {
    Home(HomeDestination, R.string.home, Icons.Outlined.Home, Icons.Filled.Home),
    History(HistoryDestination, R.string.history, Icons.Outlined.History, Icons.Filled.History),
    Statistics(StatisticsDestination, R.string.statistics, Icons.Outlined.Analytics, Icons.Filled.Analytics),
    Profile(ProfileDestination, R.string.profile, Icons.Outlined.Person, Icons.Filled.Person)
}


@RootNavGraph(start=true)
@Destination
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun RootDestinationGraph(){
    // scroll behaviour for top bar
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    val navController = rememberAnimatedNavController()
    val currentDestination = navController.appCurrentDestinationAsState().value
        ?: NavGraphs.root.startAppDestination

    Scaffold(modifier = Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AnimatedVisibility(
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically (),
                visible = BottomBarDestination.values().any { currentDestination == it.direction }
            ) {
                LargeTopAppBar(title = { Text(stringResource(R.string.default_quote)) },
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "App settings"
                            )
                        }
                    })
            }
        }, content = { innerPadding ->
            val topPadding by animateDpAsState(
                if (BottomBarDestination.values().any { currentDestination == it.direction })
                    innerPadding.calculateTopPadding()
                else 0.dp
            )
            val bottomPadding by animateDpAsState(
                if (BottomBarDestination.values().any { currentDestination == it.direction })
                    innerPadding.calculateBottomPadding()
                else 0.dp
            )
            DestinationsNavHost(
                navGraph = NavGraphs.bottomNavigation,
                navController = navController,
                modifier = Modifier.padding(top = topPadding, bottom = bottomPadding)
            )
        }, bottomBar = {
            AnimatedVisibility(
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                visible = BottomBarDestination.values().any { currentDestination == it.direction }
            ) {
                NavigationBar() {
                    BottomBarDestination.values().forEach { destination ->
                        val selected = currentDestination == destination.direction
                        //                    navController.isRouteOnBackStack(destination.direction)
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) destination.iconSelected else destination.icon,
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(destination.label)) },
                            selected = selected,
                            onClick = {
                                if (selected) {
                                    // When we click again on a bottom bar item and it was already selected
                                    // we want to pop the back stack until the initial destination of this bottom bar item
                                    navController.popBackStack(destination.direction, false)
                                    return@NavigationBarItem
                                }
                                navController.navigate(destination.direction.route) {
                                    // Pop up to the root of the graph to
                                    // avoid building up a large stack of destinations
                                    // on the back stack as users select items
                                    popUpTo(NavGraphs.bottomNavigation.startRoute) {
                                        saveState = true
                                    }

                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}