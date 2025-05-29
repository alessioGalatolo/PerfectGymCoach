package agdesigns.elevatefitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanDifficulty
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanGoal
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanSplit
import agdesigns.elevatefitness.ui.GeneratePlanGraph
import agdesigns.elevatefitness.ui.SlideTransition
import agdesigns.elevatefitness.viewmodels.GeneratePlanEvent
import agdesigns.elevatefitness.viewmodels.GeneratePlanViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddProgramDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination<GeneratePlanGraph>(style = SlideTransition::class)
@Composable
fun ViewGeneratedPlan(
    navigator: DestinationsNavigator,
    goalChoice: String,
    expertiseLevel: String,
    workoutSplit: String,
    viewModel: GeneratePlanViewModel = hiltViewModel()
) {
    viewModel.onEvent(
        GeneratePlanEvent.GeneratePlan(
            // FIXME: should pass the enum class instead of string
            WorkoutPlanGoal.entries.first { it.goal == goalChoice },
            WorkoutPlanDifficulty.entries.first { it.expertiseLevel == expertiseLevel },
            WorkoutPlanSplit.entries.first { it.split == workoutSplit }
        )
    )

    if (viewModel.state.value.generatedPlan == null) {
        Column(Modifier.fillMaxSize(),
            Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))

            // TODO: circle on messages
            Text("Generating a plan just for you...", style = MaterialTheme.typography.titleLarge)
        }
    } else {
        navigator.navigateUp()
        navigator.navigate(
            AddProgramDestination(
                viewModel.state.value.generatedPlan!!.name,
                viewModel.state.value.generatedPlan!!.planId
            )
        )
    }
}
