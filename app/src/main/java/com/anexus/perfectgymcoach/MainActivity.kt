package com.anexus.perfectgymcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Modifier
import com.anexus.perfectgymcoach.ui.theme.PerfectGymCoachTheme
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.anexus.perfectgymcoach.ui.*
import com.anexus.perfectgymcoach.ui.screens.*
import com.anexus.perfectgymcoach.ui.screens.destinations.HistoryDestination
import com.anexus.perfectgymcoach.ui.screens.destinations.HomeDestination
import com.anexus.perfectgymcoach.ui.screens.destinations.ProfileDestination
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.navigation.popBackStack
import com.ramcosta.composedestinations.navigation.popUpTo
import com.ramcosta.composedestinations.utils.isRouteOnBackStack
import com.ramcosta.composedestinations.utils.startDestination
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
        ExperimentalMaterialNavigationApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ComposeView(this).consumeWindowInsets = true
        setContent {
            val sysUiController = rememberSystemUiController()
            sysUiController.setStatusBarColor(
                color = Color.Transparent,
                darkIcons = !isSystemInDarkTheme()
            )
            // navigation controller for everything (main screen)
            val engine = rememberAnimatedNavHostEngine()
            val navController = engine.rememberNavController()

            // scroll behaviour for top bar
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                rememberTopAppBarState()
            )

            PerfectGymCoachTheme {
                val startRoute = NavGraphs.root.startRoute
                val currentDestination = navController.appCurrentDestinationAsState().value ?: startRoute.startAppDestination
                Scaffold(modifier = Modifier
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
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
                    }, content = { innerPadding ->
                        DestinationsNavHost(
                            navGraph = NavGraphs.root,
                            engine = engine,
                            navController = navController,
                            modifier = Modifier.padding(innerPadding),
                            startRoute = startRoute
                        )
                    }, bottomBar = {
                        NavigationBar (windowInsets = WindowInsets.navigationBars) {
                            BottomBarDestination.values().forEach { destination ->
                                val selected = navController.isRouteOnBackStack(destination.direction)
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
                                            popUpTo(NavGraphs.root) {
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
                )
            }
        }
    }
}
