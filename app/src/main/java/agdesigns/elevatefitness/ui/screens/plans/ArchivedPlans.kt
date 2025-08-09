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
import androidx.compose.ui.graphics.Color
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Destination<ChangePlanGraph>(style = SlideTransition::class)
@OptIn(ExperimentalMaterial3Api::class)
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
        if (archiveState.archivedPlans.isEmpty()) {
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
                itemsIndexed(items = archiveState.archivedPlans, key = { _, it -> it.first.planId })
                { index, plan ->
                    // TODO: consider having only the first plan in card, the others are simple list items
                    Spacer(Modifier.height(4.dp))
                    PlanCard(
                        navigator = navigator,
                        plan = plan.first,
                        programs = plan.second,
                        onSwipe = {
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
                        },
                        swipeIcon = Icons.Default.Unarchive,
                        swipeDescription = "Unarchive plan",
                        swipeBackgroundColor = Color.Green.copy(alpha = 0.3f),
                        trailingIcon = Icons.Default.Unarchive,
                        trailingIconDescription = "Unarchive plan",
                        onTrailingIconClick = {
                            viewModel.onEvent(PlansEvent.UnarchivePlan(plan.first.planId))
                            scope.launch {
                                snackbarHostState.showSnackbar("Plan restored")
                            }
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}