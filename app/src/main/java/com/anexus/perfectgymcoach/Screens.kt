@file:OptIn(ExperimentalMaterial3Api::class)

package com.anexus.perfectgymcoach

import androidx.annotation.StringRes
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

sealed class Screen(val route: String, @StringRes val resourceId: Int) {
    object Main : Screen("main", R.string.main)
    object Home : Screen("home", R.string.home)
    object History : Screen("history", R.string.history)
    object Statistics : Screen("statistics", R.string.statistics)
    object Profile : Screen("profile", R.string.profile)
    object Program : Screen("program", R.string.program)
    object ChangePlan : Screen("change_plan", R.string.change_plan)
}

@Composable
fun Home(navController: NavHostController) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        // Coming next
        Text(text = stringResource(id = R.string.coming_next), fontWeight = FontWeight.Bold)
        ElevatedCard(modifier = Modifier.fillMaxWidth().padding(all = 8.dp),
            onClick = {
                navController.navigate(Screen.Program.route)
            }) {
            Row {
                Image(
                    painter = painterResource(R.drawable.full_body),
                    contentDescription = "Contact profile picture",
                    modifier = Modifier
                        // Set image size to 40 dp
                        .size(160.dp)
                        .padding(all = 4.dp)
                        // Clip image to be shaped as a circle
                        .clip(CircleShape)
                )

                // Add a horizontal space between the image and the column
//                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(text = "msg.author")
                    // Add a vertical space between the author and message texts
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "msg.body")
                }
            }

        }
        Text(text = stringResource(id = R.string.other_programs), fontWeight = FontWeight.Bold)
        repeat(30) {
            Card(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)) {
                Row {
                    Image(
                        painter = painterResource(R.drawable.full_body),
                        contentDescription = "Contact profile picture",
                        modifier = Modifier
                            // Set image size to 40 dp
                            .size(60.dp)
                            .padding(all = 4.dp)
                            // Clip image to be shaped as a circle
                            .clip(CircleShape)
                    )

                    // Add a horizontal space between the image and the column
                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(text = "msg.author")
                        // Add a vertical space between the author and message texts
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "msg.body")
                    }
                }

            }

            Spacer(modifier = Modifier.height(4.dp))
        }
        TextButton(onClick = { navController.navigate(Screen.ChangePlan.route) },
            modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Change workout plan") }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun History(onNavigate: NavHostController) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        repeat(30) {
            Card(Modifier.fillMaxWidth()) {
                Greeting("History")
                Greeting("Alessio")
            }
        }
    }
}

@Composable
fun Statistics(onNavigate: NavHostController) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        repeat(30) {
            Card(Modifier.fillMaxWidth()) {
                Greeting("Statistics")
                Greeting("Alessio")
            }
        }
    }
}

@Composable
fun Profile(onNavigate: NavHostController) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        repeat(30) {
            Card(Modifier.fillMaxWidth()) {
                Greeting("Profile")
                Greeting("Alessio")
            }
        }
    }
}

@Composable
fun Program(navController: NavHostController) {
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        decayAnimationSpec,
        rememberTopAppBarScrollState()
    )
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
            Greeting(", this is the program page (WIP)")}})
}

@Composable
fun ChangePlan(navController: NavHostController) {
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        decayAnimationSpec,
        rememberTopAppBarScrollState()
    )
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
            Greeting(", this is the change plan page (WIP)")}})
}

//@Composable
//fun TopBarWSettings() {
//
//}