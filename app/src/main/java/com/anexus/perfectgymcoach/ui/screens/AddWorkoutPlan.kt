package com.anexus.perfectgymcoach.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlan
import androidx.hilt.navigation.compose.hiltViewModel
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import com.anexus.perfectgymcoach.ui.ChangePlanGraph
import com.anexus.perfectgymcoach.ui.components.InsertNameDialog
import com.anexus.perfectgymcoach.viewmodels.PlansEvent
import com.anexus.perfectgymcoach.viewmodels.PlansViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddProgramDestination
import com.ramcosta.composedestinations.generated.destinations.CustomizePlanGenerationDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

@Destination<ChangePlanGraph>(start=true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkoutPlan(
    navigator: DestinationsNavigator,
    openDialogNow: Boolean = false,
    viewModel: PlansViewModel = hiltViewModel()
) {
    InsertNameDialog(
        prompt = "Name of the new plan",
        dialogueIsOpen = viewModel.state.value.openAddPlanDialogue,
        toggleDialog = { viewModel.onEvent(PlansEvent.TogglePlanDialogue) },
        insertName = { planName -> viewModel.onEvent(PlansEvent.AddPlan(WorkoutPlan(name = planName))) }
    )
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val openDialog = rememberSaveable { mutableStateOf(openDialogNow) }
    scope.launch {
        if (openDialog.value) {
            awaitFrame()
            awaitFrame()
            viewModel.onEvent(PlansEvent.TogglePlanDialogue)
            openDialog.value = false
        }
    }
    Scaffold (
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_workout_plans)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }, floatingActionButton = {
            LargeFloatingActionButton (
                modifier = Modifier.navigationBarsPadding(),
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
        }) { innerPadding ->
        if (viewModel.state.value.workoutPlanMapPrograms.isEmpty()) {
            // if you have no plans
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentPaste,
                    contentDescription = "",
                    modifier = Modifier.size(160.dp)
                )
                Text(
                    stringResource(id = R.string.empty_plans),
                    modifier = Modifier.padding(16.dp)
                )
                GeneratePlanButton(navigator)
            }
        } else {
            // if you have some plans
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.padding(16.dp)
            ) {
                itemsIndexed(items = viewModel.state.value.workoutPlanMapPrograms, key = { _, it -> it.first.planId })
                { index, plan ->
                    if (index == 0){
                        Text("Current plan", fontWeight = FontWeight.Bold)
                    } else if (index == 1) {
                        Column (Modifier.fillMaxWidth()){
                            Text("Other plans", fontWeight = FontWeight.Bold)
                        }
                    }
                    PlanCard(
                        navigator = navigator,
                        plan = plan.first,
                        programs = plan.second,
                        currentPlanId = viewModel.state.value.currentPlanId
                    ) {
                        viewModel.onEvent(PlansEvent.SetCurrentPlan(it))
                        scope.launch {
                            snackbarHostState.showSnackbar("Plan set as current")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (index == 0) {
                        Column (Modifier.fillMaxWidth()) {
                            GeneratePlanButton(navigator)
                        }
                    }
                }
                item{
                    var finalSpacerSize = 96.dp + 8.dp // large fab size + its padding FIXME: not hardcode
                    finalSpacerSize += 8.dp
                    Spacer(modifier = Modifier.navigationBarsPadding())
                    Spacer(Modifier.height(finalSpacerSize))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.PlanCard(
    navigator: DestinationsNavigator,
    plan: WorkoutPlan,
    programs: List<WorkoutProgram>,
    currentPlanId: Long?,
    setAsCurrent: (Long) -> Unit
){
    ElevatedCard (
        Modifier
            .animateItemPlacement()
            .clickable {
                navigator.navigate(
                    AddProgramDestination(
                        planName = plan.name,
                        planId = plan.planId
                    ),
                    onlyIfResumed = true
                )
            }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.card_inner_padding)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
//          // TODO: maybe add back image as random icon

            Column {
                Text(text = plan.name, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(4.dp))
                programs.forEach {
                    Text(it.name)
                }
            }

            IconToggleButton(
                checked = plan.planId == currentPlanId,
                onCheckedChange = { setAsCurrent(plan.planId) }
            ) {
                val transition = updateTransition(
                    plan.planId == currentPlanId,
                    label = "Checked indicator"
                )

                val default_icon_size = 24.dp
                val size by transition.animateDp(
                    transitionSpec = {
                        if (false isTransitioningTo true) {
                            keyframes {
                                durationMillis = 250
                                default_icon_size + 5.dp at 0 with LinearOutSlowInEasing // for 0-15 ms
                                default_icon_size + 10.dp at 15 with FastOutLinearInEasing // for 15-75 ms
                                default_icon_size + 15.dp at 75 // ms
                                default_icon_size + 10.dp at 150 // ms
                            }
                        } else {
                            spring(stiffness = Spring.StiffnessVeryLow)
                        }
                    },
                    label = "Size"
                ) { default_icon_size }

                Icon(
                    imageVector = if (plan.planId == currentPlanId) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
//                tint = tint,
                    modifier = Modifier.size(size)
                )
            }
        }
    }
}


@Composable
fun ColumnScope.GeneratePlanButton(navigator: DestinationsNavigator){
    FilledTonalButton(
        onClick = {
            navigator.navigate(CustomizePlanGenerationDestination(), onlyIfResumed = true)
        },
        modifier = Modifier.align(Alignment.CenterHorizontally))
    {
        Icon(Icons.Filled.AutoAwesome, null)
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text("Generate a new plan")
    }
}