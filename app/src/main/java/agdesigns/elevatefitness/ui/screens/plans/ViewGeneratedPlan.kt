package agdesigns.elevatefitness.ui.screens.plans

import agdesigns.elevatefitness.R
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanDifficulty
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanGoal
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanSplit
import agdesigns.elevatefitness.navigation.AddProgramDestination
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import androidx.compose.foundation.background
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ViewGeneratedPlan(
    navigator: DestinationsNavigator,
    goalChoice: WorkoutPlanGoal,
    expertiseLevel: WorkoutPlanDifficulty,
    workoutSplit: WorkoutPlanSplit,
    excludedExerciseIds: List<Long> = emptyList(),
    customMuscleDays: List<List<Int>> = emptyList(),
    viewModel: GeneratePlanViewModel = hiltViewModel()
) {
    val generationState by viewModel.state.collectAsState()
    LaunchedEffect(goalChoice, expertiseLevel, workoutSplit, customMuscleDays) {
        val resolvedCustomDays = customMuscleDays.map {
            day -> day.mapNotNull { ordinal -> Exercise.Muscle.entries.getOrNull(ordinal) }
        }
        viewModel.onEvent(
            GeneratePlanEvent.GeneratePlan(
                goalChoice,
                expertiseLevel,
                workoutSplit,
                excludedExerciseIds.toSet(),
                resolvedCustomDays
            )
        )
    }

    LaunchedEffect(generationState.generatedPlan) {
        // get a random delay
        delay(Random.nextLong(1000, 5000))
        if (generationState.generatedPlan != null) {
            navigator.navigateUp()
            navigator.navigate(
                AddProgramDestination(
                    generationState.generatedPlan!!.planId
                )
            )
        }
    }

    var waitingMessage by rememberSaveable { mutableIntStateOf(R.string.generating_plan_waiting_text) }
    LaunchedEffect(Unit) {
        val possibleMessages = listOf(
            R.string.generating_plan_waiting_text2,
            R.string.generating_plan_waiting_text3,
            R.string.generating_plan_waiting_text4,
            R.string.generating_plan_waiting_text1, // TODO: remove if no previous workouts
            R.string.generating_plan_waiting_text5,
        )
        for (message in possibleMessages) {
            if (generationState.generatedPlan != null) return@LaunchedEffect
            delay(Random.nextLong(250, 500))
            waitingMessage = message
        }
    }
    Column(Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceContainer),
        Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ContainedLoadingIndicator()
        Spacer(Modifier.height(8.dp))

        Text(
            stringResource(waitingMessage),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
    }
}
