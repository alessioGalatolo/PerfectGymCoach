package com.anexus.perfectgymcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import com.anexus.perfectgymcoach.ui.theme.PerfectGymCoachTheme
import androidx.compose.material3.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.anexus.perfectgymcoach.screens.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                rememberTopAppBarScrollState()
            )

            PerfectGymCoachTheme {

                NavHost(navController = navControllerMain,
                    startDestination = MainScreen.Main.route,
                    modifier = Modifier.fillMaxSize()){

                    // FIXME: should maybe be moved to a single navigation
                    composable(MainScreen.Workout.route) { Workout(navControllerMain) }
                    composable("${MainScreen.AddProgram.route}/{name}/{planId}",
                        arguments = listOf(navArgument("planId") { type = NavType.IntType })
                    ) {
                        AddProgram(navControllerMain,
                            it.arguments?.getString("name") ?: "",
                            it.arguments?.getInt("planId") ?: 0)
                    }
                    composable("${MainScreen.AddExercise.route}/{name}/{programId}",
                        arguments = listOf(navArgument("programId") { type = NavType.IntType })
                    ) {
                        AddExercise(navControllerMain,
                            it.arguments?.getString("name") ?: "",
                            it.arguments?.getInt("programId") ?: 0)
                    }
                    composable("${MainScreen.ExercisesByMuscle.route}/{name}/{programId}",
                        arguments = listOf(navArgument("programId") { type = NavType.IntType })
                    ) {
                        ExercisesByMuscle(navControllerMain,
                            it.arguments?.getString("name") ?: "",
                            it.arguments?.getInt("programId") ?: 0)
                    }
                    composable("${MainScreen.ViewExercises.route}/{name}/{programId}/{muscle}",
                        arguments = listOf(
                            navArgument("programId") { type = NavType.IntType },
                            navArgument("muscle") { type = NavType.IntType })
                    ) {
                        ViewExercises(navControllerMain,
                            it.arguments?.getString("name") ?: "",
                            it.arguments?.getInt("programId") ?: 0,
                            it.arguments?.getInt("muscle") ?: -1)
                    }
                    composable(MainScreen.ChangePlan.route) { ChangePlan(navControllerMain) }
                    navigation(startDestination = NavigationScreen.Home.route,
                        route = MainScreen.Main.route){
                        fragments.forEach { screen ->
                            composable(screen.route){
                            Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                                topBar = {
                                    LargeTopAppBar(title = { Text(stringResource(R.string.default_quote)) },
                                        scrollBehavior = scrollBehavior,
                                        actions = {IconButton(onClick = { /* doSomething() */ }) { // TODO
                                            Icon(
                                                imageVector = Icons.Filled.Settings,
                                                contentDescription = "App settings"
                                            )
                                        }})
                                }, content = { innerPadding -> Column(modifier = Modifier.padding(innerPadding)){
                                    when (screen.route) {
                                        "home" -> Home(navControllerMain)
                                        "history" -> History(navControllerMain)
                                        "statistics" -> Statistics(navControllerMain)
                                        "profile" -> Profile(navControllerMain)
                                    }}
                                }, bottomBar = {
                                    NavigationBar {
                                        val navBackStackEntry by navControllerMain.currentBackStackEntryAsState()
                                        val currentDestination = navBackStackEntry?.destination
                                        fragments.forEach { screen ->
                                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                            NavigationBarItem(
                                                icon = {Icon(if (selected) screen.iconSelected else screen.icon, contentDescription = null) },
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
                            )
                            }
                        }
                    }
                }
            }
        }
    }
}
