package com.anexus.perfectgymcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import com.anexus.perfectgymcoach.ui.theme.PerfectGymCoachTheme
import androidx.compose.material3.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // navigation controller for everything (main screen)
            val navControllerMain = rememberNavController()
            val fragments = listOf(Screen.Home, Screen.History, Screen.Statistics, Screen.Profile)
            //use R8 / ProGuard to remove unused icons from your application.
            val icons = listOf(Icons.Outlined.Home, Icons.Outlined.History, Icons.Outlined.Analytics, Icons.Outlined.Person)

            // scroll behaviour for top bar
            val decayAnimationSpec = rememberSplineBasedDecay<Float>()
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
                decayAnimationSpec,
                rememberTopAppBarScrollState()
            )

            PerfectGymCoachTheme {

                NavHost(navController = navControllerMain,
                    startDestination = Screen.Main.route,
                    modifier = Modifier.fillMaxSize()){
                    composable(Screen.Program.route) { Program(navControllerMain) }
                    composable(Screen.ChangePlan.route) { ChangePlan(navControllerMain) }
                    navigation(startDestination = Screen.Home.route, route = Screen.Main.route){
                        fragments.forEach { screen ->
                            composable(screen.route){
                            Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                                topBar = {
                                    LargeTopAppBar(title = { Text(stringResource(R.string.default_quote)) },
                                        scrollBehavior = scrollBehavior,
                                        actions = {IconButton(onClick = { /* doSomething() */ }) {
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
                                        fragments.forEachIndexed { index, screen ->
                                            NavigationBarItem(
                                                icon = { Icon(icons[index], contentDescription = null) },
                                                label = { Text(stringResource(screen.resourceId)) },
                                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Home", "History", "Statistics", "Profile")
    //use R8 / ProGuard to remove unused icons from your application.
    val icons = listOf(Icons.Outlined.Home, Icons.Outlined.History, Icons.Outlined.Analytics, Icons.Outlined.Person)
    PerfectGymCoachTheme {
        // A surface container using the 'background' color from the theme

        Scaffold(topBar = {
            SmallTopAppBar(title = { Text("Home") })
        }, content = { innerPadding ->
            Column(modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())) {
                repeat(30) {
                    Card(Modifier.fillMaxWidth()) {
                        Greeting("Android")
                        Greeting("Alessio")
                    }
                }
            }}, bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = null) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }})
    }
}