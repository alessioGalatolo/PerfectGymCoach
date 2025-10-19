package agdesigns.elevatefitness.ui.screens.plans

import agdesigns.elevatefitness.R
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanDifficulty
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanGoal
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanSplit
import agdesigns.elevatefitness.navigation.GeneratePlanGraph
import agdesigns.elevatefitness.navigation.SlideTransition
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddProgramDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Destination<GeneratePlanGraph>(style = SlideTransition::class)
@Composable
fun ViewGeneratedPlan(
    navigator: DestinationsNavigator,
    goalChoice: WorkoutPlanGoal,
    expertiseLevel: WorkoutPlanDifficulty,
    workoutSplit: WorkoutPlanSplit,
    viewModel: GeneratePlanViewModel = hiltViewModel()
) {
    val generationState by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.onEvent(
            GeneratePlanEvent.GeneratePlan(
                goalChoice,
                expertiseLevel,
                workoutSplit
            )
        )
    }

    // We are using this loading stage to also load the AI model into memory
    LaunchedEffect(generationState.generatedPlan, generationState.isLoadingAI) {
        if (generationState.generatedPlan != null && !generationState.isLoadingAI) {
            navigator.navigateUp()
            navigator.navigate(
                AddProgramDestination(
                    generationState.generatedPlan!!.planId,
                    hasJustBeenGenerated = true
                )
            )
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

        // TODO: circle on messages
        Text(
            stringResource(R.string.generating_plan_waiting_text),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
    }
}
