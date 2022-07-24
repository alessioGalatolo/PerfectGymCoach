@file:OptIn(ExperimentalMaterial3Api::class)

package com.anexus.perfectgymcoach

import androidx.annotation.StringRes
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
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
        ElevatedCard(modifier = Modifier
            .fillMaxWidth()
            .padding(all = 8.dp),
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
        repeat(6) {
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

data class PlansState(
    val workoutPlans: List<WorkoutPlan> = emptyList(),
    val openAddPlanDialogue: MutableState<Boolean> = mutableStateOf(false)
)

class PlansViewModel: ViewModel() {

    var plansState by mutableStateOf(PlansState())
        private set

}

@Composable
fun ChangePlan(navController: NavHostController, viewModel: PlansViewModel = viewModel()) {
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        decayAnimationSpec,
        rememberTopAppBarScrollState()
    )
    val openDialog = remember { mutableStateOf(false) }
    val plans = mutableListOf<WorkoutPlan>() // TODO: get list from somewhere
//    plans.add(WorkoutPlan("Hellooo"))
    CreatePlanDialogue(openDialog, plans)
    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SmallTopAppBar(title = { Text(stringResource(R.string.manage_workout_plans)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                })
        }, floatingActionButton = {
            LargeFloatingActionButton (
                onClick = {
                          openDialog.value = true
                },
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add workout plan",
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                )
            }}, content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)){
                if (plans.isEmpty()) {
                    // if you have no plans
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inventory,
                            contentDescription = "",
                            modifier = Modifier.size(160.dp)
                        ) //TODO
                        Text(
                            stringResource(id = R.string.empty_plans),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    // if you have some plans
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(items = plans, key = { it }) { plan ->
                            Text(plan.name)
                        }
                    }
                }
            }})
}

@Composable
fun CreatePlanDialogue(openDialog: MutableState<Boolean>, plans: MutableList<WorkoutPlan>) {
    // alert dialogue to enter the workout plan name

    var text by rememberSaveable { mutableStateOf("") }
    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = {
                // Dismiss the dialog when the user clicks outside the dialog or on the back
                // button. If you want to disable that functionality, simply use an empty
                // onDismissRequest.
                openDialog.value = false
            },
            title = {
                Text(text = "Create workout plan")
            },
            text = {
                TextField(value = text,
                    onValueChange = { text = it },
                    label = { Text("Name of the plan" ) },
                    singleLine = true)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        plans.add(WorkoutPlan(text))
                        openDialog.value = false
                        text = ""
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}