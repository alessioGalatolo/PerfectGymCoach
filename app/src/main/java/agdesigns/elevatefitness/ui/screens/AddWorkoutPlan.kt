package agdesigns.elevatefitness.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlan
import androidx.hilt.navigation.compose.hiltViewModel
import agdesigns.elevatefitness.data.workout_program.WorkoutProgram
import agdesigns.elevatefitness.ui.ChangePlanGraph
import agdesigns.elevatefitness.ui.SlideTransition
import agdesigns.elevatefitness.ui.components.InsertNameDialog
import agdesigns.elevatefitness.viewmodels.PlansEvent
import agdesigns.elevatefitness.viewmodels.PlansViewModel
import androidx.compose.material3.IconButtonDefaults
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddProgramDestination
import com.ramcosta.composedestinations.generated.destinations.ArchivedPlansDestination
import com.ramcosta.composedestinations.generated.destinations.CustomizePlanGenerationDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

@Destination<ChangePlanGraph>(start=true, style = SlideTransition::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
        insertName = {
            planName -> viewModel.onEvent(
            PlansEvent.AddPlan(
                WorkoutPlan(
                    name = planName,
                    creationDate = ZonedDateTime.now(),
                ))) }
    )
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val openDialog = rememberSaveable { mutableStateOf(openDialogNow) }
    LaunchedEffect(openDialog.value) {
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                modifier = Modifier.padding(horizontal=16.dp)
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
                    // TODO: consider having only the first plan in card, the others are simple list items
                    PlanCard(
                        navigator = navigator,
                        plan = plan.first,
                        programs = plan.second,
                        currentPlanId = viewModel.state.value.currentPlanId,
                        setAsCurrent = {
                            viewModel.onEvent(PlansEvent.SetCurrentPlan(it))
                            scope.launch {
                                snackbarHostState.showSnackbar("Plan set as current")
                            }
                        }, archivePlan = {
                            viewModel.onEvent(PlansEvent.ArchivePlan(it))
                            scope.launch {
                                val snackbarResult = snackbarHostState.showSnackbar(
                                    "Plan archived",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                when (snackbarResult) {
                                    SnackbarResult.ActionPerformed -> {
                                        viewModel.onEvent(PlansEvent.UnarchivePlan(it))
                                    }
                                    SnackbarResult.Dismissed -> {
                                        /* Handle snackbar dismissed */
                                    }
                                }
                            }
                        }, canBeArchived = index != 0
                    )
                    Spacer(Modifier.height(8.dp))
                    if (index == 0) {
                        Column (Modifier.fillMaxWidth()) {
                            GeneratePlanButton(navigator)
                        }
                    }
                }
                if (viewModel.state.value.archivedPlans.isNotEmpty()) {
                    item {
                        if (viewModel.state.value.workoutPlanMapPrograms.size <= 1) {
                            Column (Modifier.fillMaxWidth()){
                                Text("Other plans", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        // Archived chat card
                        OutlinedCard(onClick = {
                            navigator.navigate(ArchivedPlansDestination)
                        }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(dimensionResource(R.dimen.card_inner_padding)),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = "")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.archived_plans), style = MaterialTheme.typography.headlineSmall)
                            }
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

@Composable
fun LazyItemScope.PlanCard(
    navigator: DestinationsNavigator,
    plan: WorkoutPlan,
    programs: List<WorkoutProgram>,
    currentPlanId: Long?,
    setAsCurrent: (Long) -> Unit,
    archivePlan: (Long) -> Unit,
    canBeArchived: Boolean
){
    val haptics = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart -> {
                    archivePlan(plan.planId)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    true
                }

                else -> false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = canBeArchived,
        enableDismissFromStartToEnd = canBeArchived,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val defaultColors = CardDefaults.cardColors()
            val dismissColors by animateColorAsState(
                when (dismissState.targetValue) {  // pastel red
                    SwipeToDismissBoxValue.StartToEnd -> Color.hsl(348f, 1f, 0.55f)
                    SwipeToDismissBoxValue.EndToStart -> Color.hsl(348f, 1f, 0.55f)
                    SwipeToDismissBoxValue.Settled -> defaultColors.containerColor
                }, label = "Dismiss box anim color"
            )

            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.Start
                SwipeToDismissBoxValue.EndToStart -> Alignment.End
                SwipeToDismissBoxValue.Settled -> Alignment.CenterHorizontally
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Archive
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Archive
                SwipeToDismissBoxValue.Settled -> Icons.Default.Close
            }
            val scale by animateFloatAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) 0.75f else 1f,
                label = "Dismiss box anim"
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = dismissColors
                ),
                modifier = Modifier.fillMaxSize(),
//                contentAlignment = alignment
            ) {
                Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        icon,
                        contentDescription = "Archive plan",
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .scale(scale)
                            .align(alignment)
                    )
                }
            }
        }, modifier = Modifier.animateItem()
    ) {
        ElevatedCard(
            modifier = Modifier
                .clickable {
                    navigator.navigate(
                        AddProgramDestination(
                            planName = plan.name,
                            planId = plan.planId
                        )
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

                Column(Modifier.weight(1f)) {
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

                    // FIXME: this should not be hardcoded but IconButtonTokens.IconSize is internal
                    val defaultIconSize = 24.dp
                    val size by transition.animateDp(
                        transitionSpec = {
                            if (false isTransitioningTo true) {
                                keyframes {
                                    durationMillis = 300
                                    defaultIconSize + 5.dp at 0 using LinearOutSlowInEasing // for 0-15 ms
                                    defaultIconSize + 10.dp at (durationMillis / 10) using FastOutSlowInEasing // for 15-75 ms
                                    defaultIconSize + 15.dp at (durationMillis / 4) // ms
                                    defaultIconSize + 10.dp at (durationMillis / 2) // ms
                                }
                            } else {
                                spring(stiffness = Spring.StiffnessVeryLow)
                            }
                        },
                        label = "Size"
                    ) { defaultIconSize }

                    Icon(
                        imageVector = if (plan.planId == currentPlanId) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (plan.planId == currentPlanId) "Current plan" else "Set as current plan",
                        modifier = Modifier.size(size)
                    )
                }
            }
        }
    }
}


@Composable
fun ColumnScope.GeneratePlanButton(navigator: DestinationsNavigator){
    FilledTonalButton(
        onClick = {
            navigator.navigate(CustomizePlanGenerationDestination())
        },
        modifier = Modifier.align(Alignment.CenterHorizontally))
    {
        Icon(Icons.Filled.AutoAwesome, "Magic")
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text("Generate a new plan")
    }
}