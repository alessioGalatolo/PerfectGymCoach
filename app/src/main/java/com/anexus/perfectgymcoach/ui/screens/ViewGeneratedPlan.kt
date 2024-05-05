package com.anexus.perfectgymcoach.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlanDifficulty
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlanGoal
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlanSplit
import com.anexus.perfectgymcoach.ui.ChangePlanGraph
import com.anexus.perfectgymcoach.ui.GeneratePlanGraph
import com.anexus.perfectgymcoach.viewmodels.GeneratePlanEvent
import com.anexus.perfectgymcoach.viewmodels.GeneratePlanViewModel
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddProgramDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Destination<GeneratePlanGraph>
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
