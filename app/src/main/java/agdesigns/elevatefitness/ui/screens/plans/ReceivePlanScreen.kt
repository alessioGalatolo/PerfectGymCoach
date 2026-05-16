package agdesigns.elevatefitness.ui.screens.plans

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Exercise.FirstPhase
import agdesigns.elevatefitness.data.db.entity.Exercise.WearRepTrackable
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import agdesigns.elevatefitness.data.db.entity.getNameDescriptionResource
import agdesigns.elevatefitness.data.db.entity.getPlanDisplayName
import agdesigns.elevatefitness.data.db.entity.getProgramDisplayName
import agdesigns.elevatefitness.navigation.AddProgramDestination
import agdesigns.elevatefitness.navigation.AddWorkoutPlanDestination
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import agdesigns.elevatefitness.navigation.HomeDestination
import agdesigns.elevatefitness.shared.Equipment
import agdesigns.elevatefitness.ui.common.GroupedCard
import agdesigns.elevatefitness.ui.common.lazyGroupedCard
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReceivePlanScreen(
    navigator: DestinationsNavigator,
    sharedText: String,
    viewModel: ReceivePlanViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(sharedText) {
        viewModel.parseSharedText(sharedText)
    }

    if (state.imported) {
        LaunchedEffect(Unit) {
            navigator.popAndNavigateToBottomBar(HomeDestination)
            state.newPlanId?.let {
                navigator.navigate(AddProgramDestination(it))
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = { Text(stringResource(R.string.receive_plan_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navigator.navigateUp() },
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back_icon)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (state.plan == null) return@Scaffold
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { viewModel.importPlan() },
                    enabled = !state.importing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.screen_edge_padding))
                ) {
                    if (state.importing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.receive_plan_import))
                }
            }
        }
    ) { innerPadding ->
        when {
            state.parseError -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.receive_plan_error),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            state.plan == null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                val plan = state.plan!!
                LazyColumn(
                    contentPadding = innerPadding,
//                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Column(
                            Modifier.padding(
                                horizontal = dimensionResource(R.dimen.screen_edge_padding),
                                vertical = 16.dp
                            ).fillMaxWidth()
                        ) {
                            Text(
                                text = getPlanDisplayName(plan.planName),
                                style = MaterialTheme.typography.headlineSmallEmphasized,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    lazyGroupedCard {
                        plan.programs.sortedBy { it.orderInWorkoutPlan }.forEach { program ->
                            subCard {
                                Text(
                                    text = getProgramDisplayName(program.name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(
                                        R.string.receive_plan_exercises,
                                        program.exercises.size
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                program.exercises.sortedBy { it.orderInProgram }
                                    .forEach { exercise ->
                                        val name = if (exercise.nameResKey.isNotBlank()) {
                                            val resId =
                                                getNameDescriptionResource(exercise.nameResKey)
                                            if (resId != R.string.exercise_name_error)
                                                stringResource(resId)
                                            else
                                                exercise.localizedName
                                        } else {
                                            exercise.localizedName
                                        }
                                        val sets = exercise.reps.size
                                        val repsStr = if (exercise.reps.distinct().size == 1)
                                            stringResource(
                                                R.string.sets_x_reps,
                                                sets,
                                                exercise.reps.first()
                                            )
                                        else
                                            exercise.reps.mapIndexed { i, r ->
                                                "Set ${i + 1}: $r reps"
                                            }.joinToString(", ")
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = repsStr,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
