package agdesigns.elevatefitness.ui.screens.plans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import agdesigns.elevatefitness.R
import androidx.hilt.navigation.compose.hiltViewModel
import agdesigns.elevatefitness.navigation.ChangePlanGraph
import agdesigns.elevatefitness.navigation.SlideTransition
import agdesigns.elevatefitness.ui.common.EmptyScreenInfo
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.ui.graphics.Color
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Destination<ChangePlanGraph>(style = SlideTransition::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArchivedPlans(
    navigator: DestinationsNavigator,
    viewModel: PlansViewModel = hiltViewModel()
) {
    val archiveState by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold (
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = { Text(stringResource(R.string.archived_plans_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navigator.navigateUp() },
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back_icon)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }) { innerPadding ->
        if (archiveState.archivedPlans.isEmpty()) {
            // if you have no archived plans (happens if unarchiving all plans)
            EmptyScreenInfo(
                Icons.Default.ContentPaste,
                R.string.you_don_t_have_any_archived_plans,
                R.string.you_don_t_have_any_archived_plans
            )
        } else {
            // if you have some archived plans
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                itemsIndexed(items = archiveState.archivedPlans, key = { _, it -> it.first.planId })
                { index, plan ->
                    // TODO: consider having only the first plan in card, the others are simple list items
                    Spacer(Modifier.height(4.dp))
                    val planRestoredString = stringResource(R.string.plan_restored)
                    val undoString = stringResource(R.string.undo)
                    val positionalThresholdFun = SwipeToDismissBoxDefaults.positionalThreshold
                    val swipeToDismissBoxState = remember(plan.first.planId, plan.first.archived) {
                        SwipeToDismissBoxState(
                            initialValue = SwipeToDismissBoxValue.Settled,
                            positionalThreshold = positionalThresholdFun
                        )
                    }
                    PlanCard(
                        navigator = navigator,
                        plan = plan.first,
                        programs = plan.second,
                        swipeToDismissBoxState = swipeToDismissBoxState,
                        onSwipe = {
                            viewModel.onEvent(PlansEvent.UnarchivePlan(it))
                            scope.launch {
                                val snackbarResult = snackbarHostState.showSnackbar(
                                    planRestoredString,
                                    actionLabel = undoString,
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
                        },
                        swipeBackgroundColor = Color.Green.copy(alpha = 0.3f),
                        swipeIcon = Icons.Default.Unarchive,
                        swipeDescription = stringResource(R.string.unarchive_plan),
                        primaryActionIcon = Icons.Default.Unarchive,
                        primaryActionDescription = stringResource(R.string.unarchive_plan),
                        onPrimaryAction = {
                            scope.launch {
                                swipeToDismissBoxState.dismiss(SwipeToDismissBoxValue.StartToEnd)
                                viewModel.onEvent(PlansEvent.UnarchivePlan(plan.first.planId))
                                snackbarHostState.showSnackbar(planRestoredString)
                            }
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                }
                item {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}