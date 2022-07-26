@file:JvmName("WorkoutPlanKt")

package com.anexus.perfectgymcoach.screens

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlan
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlanDatabase
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.hilt.navigation.compose.hiltViewModel

data class PlansState(
    val workoutPlans: List<WorkoutPlan> = emptyList(),
    val openAddPlanDialogue: Boolean = false
)

sealed class PlansEvent{
    object TogglePlanDialogue : PlansEvent()

    data class AddPlan(val name: String): PlansEvent()

    // TODO: ChangeOrder
    // TODO: RemovePlan
}

@HiltViewModel
class PlansViewModel @Inject constructor(private val repository: WorkoutPlanRepository): ViewModel() {
    private val _state = mutableStateOf(PlansState())
    val state: State<PlansState> = _state

    init {
        viewModelScope.launch {
            repository.getPlans().collect{
                _state.value = state.value.copy(
                    workoutPlans = it
                )
            }
        }
    }

    fun onEvent(event: PlansEvent){
        when (event) {
            is PlansEvent.AddPlan -> {
                viewModelScope.launch {
                    repository.addPlan(WorkoutPlan(0, event.name))
                }

//                _state.value = state.value.copy(
//                    workoutPlans = state.value.workoutPlans + listOf(WorkoutPlan(event.name))
//                )
            }
            is PlansEvent.TogglePlanDialogue -> {
                _state.value = state.value.copy(
                    openAddPlanDialogue = !state.value.openAddPlanDialogue
                )
            }

        }
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePlan(navController: NavHostController, viewModel: PlansViewModel = hiltViewModel()) {
    val decayAnimationSpec = rememberSplineBasedDecay<Float>()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        decayAnimationSpec,
        rememberTopAppBarScrollState()
    )
//    val openDialog = remember { mutableStateOf(false) }
//    val plans = mutableListOf<WorkoutPlan>() // TODO: get list from somewhere
//    plans.add(WorkoutPlan("Hellooo"))
    // FIXME: should not propagate the entire viewModel, use lambda instead
    CreatePlanDialogue(viewModel)
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
                    viewModel.onEvent(PlansEvent.TogglePlanDialogue)
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
                if (viewModel.state.value.workoutPlans.isEmpty()) {
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
                        items(items = viewModel.state.value.workoutPlans, key = { it }) { plan ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row {
                                    Image(
                                        painter = painterResource(R.drawable.full_body),
                                        contentDescription = "Contact profile picture",
                                        modifier = Modifier
                                            // Set image size to 40 dp
                                            .size(40.dp)
                                            .padding(all = 4.dp)
                                            // Clip image to be shaped as a circle
                                            .clip(CircleShape)
                                    )

                                    // Add a horizontal space between the image and the column
//                Spacer(modifier = Modifier.width(8.dp))

                                    Column {
                                        Text(text = plan.name)
                                        // Add a vertical space between the author and message texts
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Some exercises...") // TODO
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CreatePlanDialogue(viewModel: PlansViewModel) {
    // alert dialogue to enter the workout plan name

    var text by rememberSaveable { mutableStateOf("") }
    if (viewModel.state.value.openAddPlanDialogue) {
        AlertDialog(
            onDismissRequest = {
                // Dismiss the dialog when the user clicks outside the dialog or on the back
                // button. If you want to disable that functionality, simply use an empty
                // onDismissRequest.
                viewModel.onEvent(PlansEvent.TogglePlanDialogue)
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
                        viewModel.onEvent(PlansEvent.AddPlan(text))
                        viewModel.onEvent(PlansEvent.TogglePlanDialogue)
                        text = ""
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(PlansEvent.TogglePlanDialogue)
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}