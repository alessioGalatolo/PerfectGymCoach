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
import agdesigns.elevatefitness.navigation.AddProgramDestination
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ViewGeneratedPlan(
    navigator: DestinationsNavigator,
    goalChoice: WorkoutPlanGoal,
    expertiseLevel: WorkoutPlanDifficulty,
    workoutSplit: WorkoutPlanSplit,
    viewModel: GeneratePlanViewModel = hiltViewModel()
) {
    val generationState by viewModel.state.collectAsState() 
    viewModel.onEvent(
        GeneratePlanEvent.GeneratePlan(
            goalChoice,
            expertiseLevel,
            workoutSplit
        )
    )

    LaunchedEffect(generationState.generatedPlan) {
        if (generationState.generatedPlan != null) {
            navigator.navigateUp()
            navigator.navigate(
                AddProgramDestination(
                    generationState.generatedPlan!!.planId
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
