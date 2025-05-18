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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlan
import androidx.hilt.navigation.compose.hiltViewModel
import agdesigns.elevatefitness.data.workout_program.WorkoutProgram
import agdesigns.elevatefitness.ui.ChangePlanGraph
import agdesigns.elevatefitness.viewmodels.PlansEvent
import agdesigns.elevatefitness.viewmodels.PlansViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddProgramDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Destination<ChangePlanGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedPlans(
    navigator: DestinationsNavigator,
    viewModel: PlansViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold (
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.archived_plans)) },
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
        }) { innerPadding ->
        if (viewModel.state.value.archivedPlans.isEmpty()) {
            // if you have no archived plans (should never happen as navigating here assumes archived plans)
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
                    "You don't have any archived plans!",
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            // if you have some archived plans
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                itemsIndexed(items = viewModel.state.value.archivedPlans, key = { _, it -> it.first.planId })
                { index, plan ->
                    // TODO: consider having only the first plan in card, the others are simple list items
                    Spacer(Modifier.height(4.dp))
                    ArchivedPlanCard(
                        navigator = navigator,
                        plan = plan.first,
                        programs = plan.second,
                        unarchivePlan = {
                            viewModel.onEvent(PlansEvent.UnarchivePlan(it))
                            scope.launch {
                                val snackbarResult = snackbarHostState.showSnackbar(
                                    "Plan restored!",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                when (snackbarResult) {
                                    SnackbarResult.ActionPerformed -> {
                                        viewModel.onEvent(PlansEvent.ArchivePlan(it))
                                    }
                                    SnackbarResult.Dismissed -> {
                                        /* Handle snackbar dismissed */
                                    }
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LazyItemScope.ArchivedPlanCard(
    navigator: DestinationsNavigator,
    plan: WorkoutPlan,
    programs: List<WorkoutProgram>,
    unarchivePlan: (Long) -> Unit
){
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart -> {
                    unarchivePlan(plan.planId)
                    true
                }

                else -> false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val defaultColors = CardDefaults.cardColors()
            val dismissColors by animateColorAsState(
                when (dismissState.targetValue) {  // pastel green
                    SwipeToDismissBoxValue.StartToEnd -> Color.hsl(165f, 0.82f, 0.51f)
                    SwipeToDismissBoxValue.EndToStart -> Color.hsl(165f, 0.82f, 0.51f)
                    SwipeToDismissBoxValue.Settled -> defaultColors.containerColor
                }, label = "Dismiss box anim color"
            )

            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.Start
                SwipeToDismissBoxValue.EndToStart -> Alignment.End
                SwipeToDismissBoxValue.Settled -> Alignment.CenterHorizontally
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Unarchive
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Unarchive
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
                        contentDescription = "Unarchive plan",
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .scale(scale)
                            .align(alignment)
                    )
                }
            }
        }, modifier = Modifier.animateItemPlacement()
    ) {
        ElevatedCard(
            modifier = Modifier
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

                Column(Modifier.weight(1f)) {
                    Text(text = plan.name, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    programs.forEach {
                        Text(it.name)
                    }
                }

                IconButton(
                    onClick = {
                        unarchivePlan(plan.planId)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Unarchive,
                        contentDescription = "Unarchive plan"
                    )
                }
            }
        }
    }
}