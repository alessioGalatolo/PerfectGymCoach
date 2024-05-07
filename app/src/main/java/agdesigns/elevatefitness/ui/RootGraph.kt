package agdesigns.elevatefitness.ui

import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
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
import androidx.navigation.compose.rememberNavController
import agdesigns.elevatefitness.R
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.HistoryDestination
import com.ramcosta.composedestinations.generated.destinations.HomeDestination
import com.ramcosta.composedestinations.generated.destinations.ProfileDestination
import com.ramcosta.composedestinations.generated.destinations.StatisticsDestination
import com.ramcosta.composedestinations.navigation.popBackStack
import com.ramcosta.composedestinations.navigation.popUpTo
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec
import com.ramcosta.composedestinations.utils.currentDestinationAsState
import com.ramcosta.composedestinations.utils.startDestination

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


@Destination<RootGraph>(start=true)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun RootDestinationGraph(){
    // scroll behaviour for top bar
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    val navController = rememberNavController()
    val currentDestination = navController.currentDestinationAsState().value
        ?: NavGraphs.root.startDestination

    Scaffold(modifier = Modifier
        .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AnimatedVisibility(
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically (),
                visible = BottomBarDestination.entries.any { currentDestination == it.direction }
            ) {
                LargeTopAppBar(title = { Text(stringResource(R.string.default_quote)) },
                    scrollBehavior = scrollBehavior,
//                    actions = {
//                        IconButton(onClick = { /* TODO */ }) {
//                            Icon(
//                                imageVector = Icons.Filled.Settings,
//                                contentDescription = "App settings"
//                            )
//                        }
//                    } // TODO: add when app needs settings
                )
            }
        }, content = { innerPadding ->
            val topPadding by animateDpAsState(
                if (BottomBarDestination.entries.any { currentDestination == it.direction })
                    innerPadding.calculateTopPadding()
                else 0.dp, label = ""
            )
            val bottomPadding by animateDpAsState(
                if (BottomBarDestination.entries.any { currentDestination == it.direction })
                    innerPadding.calculateBottomPadding()
                else 0.dp, label = ""
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
                visible = BottomBarDestination.entries.any { currentDestination == it.direction }
            ) {
                NavigationBar {
                    BottomBarDestination.entries.forEach { destination ->
                        val selected = currentDestination == destination.direction
                        //                    navController.isRouteOnBackStack(destination.direction)
                        NavigationBarItem(
                            icon = {
                                if (selected)
                                    Icon(
                                        destination.iconSelected,
                                        contentDescription = stringResource(destination.label) + " (current)"
                                    )
                                else
                                    Icon(
                                        destination.icon,
                                        contentDescription = stringResource(destination.label)
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