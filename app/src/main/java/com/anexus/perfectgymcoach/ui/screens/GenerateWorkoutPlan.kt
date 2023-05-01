package com.anexus.perfectgymcoach.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Bottom
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlan
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import com.anexus.perfectgymcoach.ui.ChangePlanNavGraph
import com.anexus.perfectgymcoach.ui.GeneratePlanNavGraph
import com.anexus.perfectgymcoach.ui.components.InsertNameDialog
import com.anexus.perfectgymcoach.ui.components.TextFieldWithButtons
import com.anexus.perfectgymcoach.ui.destinations.AddProgramDestination
import com.anexus.perfectgymcoach.ui.destinations.ExercisesByMuscleDestination
import com.anexus.perfectgymcoach.viewmodels.AddExerciseEvent
import com.anexus.perfectgymcoach.viewmodels.GeneratePlansViewModel
import com.anexus.perfectgymcoach.viewmodels.PlansEvent
import com.anexus.perfectgymcoach.viewmodels.PlansViewModel
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

@GeneratePlanNavGraph(start = true)
@Destination
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun GenerateWorkoutPlan(
    navigator: DestinationsNavigator,
    openDialogNow: Boolean = false,
    viewModel: GeneratePlansViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val totalPageCount = 3

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
        topBar = {
            TopAppBar(title = { Text("Generate a new plan") },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
                        )
                    }
                })
        }, content = { innerPadding ->
            Column(
                Modifier
                    .padding(innerPadding)
                    .padding(8.dp)
                    .fillMaxSize()) {
                HorizontalPager(
                    pageCount = totalPageCount,
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (it) {
                        0 -> GoalChoice()
                        1 -> ExpertiseLevel()
                        2 -> WorkoutFrequency()
                    }
                }
            }
        }, bottomBar = {
            Row(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceAround) {
                TextButton(enabled = pagerState.currentPage != 0,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }, modifier = Modifier.align(CenterVertically)) {
                    Text("Back", modifier = Modifier.align(CenterVertically))
                }
                HorizontalPagerIndicator(pagerState = pagerState, pageCount = totalPageCount, modifier = Modifier.align(CenterVertically))
                TextButton(enabled = pagerState.currentPage != totalPageCount-1,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage+1)
                        }
                    }, modifier = Modifier.align(CenterVertically)) {
                    Text(if (pagerState.currentPage != totalPageCount-1) "Next" else "")
                }
            }
        }
    )
}

@Composable
fun GoalChoice(){
    Column(Modifier.fillMaxSize()) {
        Text("What is your goal when training?",
            style = MaterialTheme.typography.titleLarge)
        val radioOptions = listOf("Build muscle (hypertrophy)", "Increase strength", "Lose weight (cardio training)")
        val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0]) }
        // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
        Column(Modifier.selectableGroup()) {
            radioOptions.forEach { text ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (text == selectedOption),
                            onClick = { onOptionSelected(text) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (text == selectedOption),
                        onClick = null // null recommended for accessibility with screenreaders
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpertiseLevel(){
    Column(Modifier.fillMaxSize()) {
        Text("What is your goal when training?",
            style = MaterialTheme.typography.titleLarge)
        val radioOptions = listOf("Build muscle (hypertrophy)", "Increase strength", "Lose weight (cardio training)")
        val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0]) }
        // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
        Column(Modifier.selectableGroup()) {
            radioOptions.forEach { text ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (text == selectedOption),
                            onClick = { onOptionSelected(text) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (text == selectedOption),
                        onClick = null // null recommended for accessibility with screenreaders
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutFrequency(){

}

