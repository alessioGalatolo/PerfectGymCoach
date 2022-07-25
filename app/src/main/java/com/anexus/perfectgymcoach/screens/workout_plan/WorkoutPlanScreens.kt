package com.anexus.perfectgymcoach.screens.workout_plan

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R

data class PlansState(
    val workoutPlans: List<WorkoutPlan> = emptyList(),
    val openAddPlanDialogue: MutableState<Boolean> = mutableStateOf(false)
)

class PlansViewModel: ViewModel() {

    var plansState by mutableStateOf(PlansState())
        private set

}


@OptIn(ExperimentalMaterial3Api::class)
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
            }
        }, content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)){
                if (plans.isEmpty()) {
                    // if you have no plans
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inventory, // TODO: not the right icon
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
            }
        })
}

@OptIn(ExperimentalComposeUiApi::class)
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
                val keyboardController = LocalSoftwareKeyboardController.current

                TextField(value = text,
                    onValueChange = { text = it },
                    label = { Text("Name of the plan" ) },
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                    }),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
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