package com.anexus.perfectgymcoach

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import com.anexus.perfectgymcoach.ui.theme.PerfectGymCoachTheme
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.navigation.NavArgument
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.anexus.perfectgymcoach.ui.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
//            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
////            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
//        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ComposeView(this).consumeWindowInsets = true
        setContent {
            // navigation controller for everything (main screen)
            val navControllerMain = rememberNavController()
            val fragments = listOf( // FIXME: should probably be moved inside class
                NavigationScreen.Home,
                NavigationScreen.History,
                NavigationScreen.Statistics,
                NavigationScreen.Profile
            )

            // scroll behaviour for top bar
            val decayAnimationSpec = rememberSplineBasedDecay<Float>()
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                decayAnimationSpec,
                rememberTopAppBarState()
            )

            PerfectGymCoachTheme {

                NavHost(
                    navController = navControllerMain,
                    startDestination = MainScreen.Main.route,
                    modifier = Modifier.fillMaxSize()
                ) {

                    // FIXME: should maybe be moved to a single navigation
                    composable("${MainScreen.Workout.route}/{programId}/{quickStart}",
                        arguments = listOf(
                            navArgument("programId") { type = NavType.LongType },
                            navArgument("quickStart") { type = NavType.BoolType }
                        )
                    )
                    { Workout(
                        navControllerMain,
                        it.arguments?.getLong("programId") ?: 0L,
                        it.arguments?.getBoolean("quickStart") ?: false
                    )}
                    composable("${MainScreen.AddProgram.route}/{name}/{planId}/{openDialogNow}",
                        arguments = listOf(
                            navArgument("planId") { type = NavType.LongType },
                            navArgument("openDialogNow") { type = NavType.BoolType }
                        )
                    ) {
                        AddProgram(
                            navControllerMain,
                            it.arguments?.getString("name") ?: "",
                            it.arguments?.getLong("planId") ?: 0L,
                            it.arguments?.getBoolean("openDialogNow") ?: false
                        )
                    }
                    composable(
                        "${MainScreen.AddExercise.route}/{name}/{programId}",
                        arguments = listOf(navArgument("programId") { type = NavType.LongType })
                    ) {
                        AddExercise(
                            navControllerMain,
                            it.arguments?.getString("name") ?: "",
                            it.arguments?.getLong("programId") ?: 0L
                        )
                    }
                    composable(
                        "${MainScreen.ExercisesByMuscle.route}/{name}/{programId}",
                        arguments = listOf(navArgument("programId") { type = NavType.LongType })
                    ) {
                        ExercisesByMuscle(
                            navControllerMain,
                            it.arguments?.getString("name") ?: "",
                            it.arguments?.getLong("programId") ?: 0L
                        )
                    }
                    composable(
                        "${MainScreen.ViewExercises.route}/{name}/{programId}/{muscle}",
                        arguments = listOf(
                            navArgument("programId") { type = NavType.LongType },
                            navArgument("muscle") { type = NavType.IntType })
                    ) {
                        ViewExercises(
                            navControllerMain,
                            it.arguments?.getString("name") ?: "",
                            it.arguments?.getLong("programId") ?: 0L,
                            it.arguments?.getInt("muscle") ?: -1
                        )
                    }
                    composable("${MainScreen.ChangePlan.route}/{openDialogNow}",
                        arguments = listOf(navArgument("openDialogNow") {
                            type = NavType.BoolType
                        })
                    ) {
                        AddWorkoutPlan(
                            navControllerMain,
                            it.arguments?.getBoolean("openDialogNow") ?: false
                        )
                    }
                    navigation(
                        startDestination = NavigationScreen.Home.route,
                        route = MainScreen.Main.route
                    ) {
                        fragments.forEach { screen ->
                            composable(screen.route) {
                                Scaffold(modifier = Modifier
                                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                                    topBar = {
                                        val backgroundColors = TopAppBarDefaults.largeTopAppBarColors()
                                        val backgroundColor = backgroundColors.containerColor(
                                            colorTransitionFraction = scrollBehavior.state.collapsedFraction
                                        ).value
                                        val foregroundColors = TopAppBarDefaults.largeTopAppBarColors(
                                            containerColor = Color.Transparent,
                                            scrolledContainerColor = Color.Transparent
                                        )
                                        Box (modifier = Modifier.background(backgroundColor)){
                                            LargeTopAppBar(title = { Text(stringResource(R.string.default_quote)) },
                                                scrollBehavior = scrollBehavior,
                                                actions = {
                                                    IconButton(onClick = { /* doSomething() */ }) { // TODO
                                                        Icon(
                                                            imageVector = Icons.Filled.Settings,
                                                            contentDescription = "App settings"
                                                        )
                                                    }
                                                },
                                                colors = foregroundColors,
                                                modifier = Modifier.statusBarsPadding())
                                        }
                                    }, content = { innerPadding ->
                                        Column(modifier = Modifier.padding(innerPadding)) {
                                            when (screen.route) {
                                                "home" -> Home(navControllerMain)
                                                "history" -> History(navControllerMain)
                                                "statistics" -> Statistics(navControllerMain)
                                                "profile" -> Profile(navControllerMain)
                                            }
                                        }
                                    }, bottomBar = {
                                        val backgroundColor = NavigationBarDefaults.containerColor
                                        Surface (modifier = Modifier.background(backgroundColor),

                                            tonalElevation = NavigationBarDefaults.Elevation) {
                                            NavigationBar(Modifier.navigationBarsPadding(),
                                                containerColor = Color.Transparent
                                            ) {
                                                val navBackStackEntry by navControllerMain.currentBackStackEntryAsState()
                                                val currentDestination =
                                                    navBackStackEntry?.destination
                                                fragments.forEach { screen ->
                                                    val selected =
                                                        currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                                    NavigationBarItem(
                                                        icon = {
                                                            Icon(
                                                                if (selected) screen.iconSelected else screen.icon,
                                                                contentDescription = null
                                                            )
                                                        },
                                                        label = { Text(stringResource(screen.resourceId)) },
                                                        selected = selected,
                                                        onClick = {
                                                            navControllerMain.navigate(screen.route) {
                                                                // Pop up to the start destination of the graph to
                                                                // avoid building up a large stack of destinations
                                                                // on the back stack as users select items
                                                                popUpTo(navControllerMain.graph.findStartDestination().id) {
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
                        }
                    }
                }
            }
        }
    }
}
