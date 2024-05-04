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
import com.anexus.perfectgymcoach.ui.GeneratePlanGraph
import com.anexus.perfectgymcoach.viewmodels.GeneratePlanViewModel
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.ViewGeneratedPlanDestination
import kotlinx.coroutines.launch

@Destination<GeneratePlanGraph>(start = true)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomizePlanGeneration(
    navigator: DestinationsNavigator
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val totalPageCount = 3
    val pagerState = rememberPagerState(pageCount = { totalPageCount })
    val scope = rememberCoroutineScope()
    val goalChoice = rememberSaveable { mutableStateOf("") }
    val expertiseLevel = rememberSaveable { mutableStateOf("") }
    val workoutSplit = rememberSaveable { mutableStateOf("") }

    val canGoNext = remember { derivedStateOf {
        when(pagerState.currentPage) {
            0 -> goalChoice.value.isNotBlank()
            1 -> expertiseLevel.value.isNotBlank()
            2 -> workoutSplit.value.isNotBlank()
            else -> false
        }
    }}

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
                    .fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (it) {
                        0 -> GoalChoice(goalChoice)
                        1 -> ExpertiseLevel(expertiseLevel)
                        2 -> WorkoutSplit(workoutSplit)
                    }
                }
            }
        }, bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceAround) {
                TextButton(enabled = pagerState.currentPage != 0,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }, modifier = Modifier.align(CenterVertically)) {
                    Text("Back", modifier = Modifier.align(CenterVertically))
                }
                HorizontalPagerIndicator(pagerState = pagerState, pageCount = totalPageCount, modifier = Modifier.align(CenterVertically))
                TextButton(
                    enabled = canGoNext.value,
                    onClick = {
                        if (pagerState.currentPage != totalPageCount-1)
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage+1)
                            }
                        else
                            navigator.navigate(ViewGeneratedPlanDestination(
                                goalChoice.value,
                                expertiseLevel.value,
                                workoutSplit.value
                            ))
                    }, modifier = Modifier.align(CenterVertically)) {
                    Text(if (pagerState.currentPage != totalPageCount-1) "Next" else "Generate!")
                }
            }
        }
    )
}

@Composable
fun GoalChoice(goalChoice: MutableState<String>){
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)) {
        Text("What is your goal when training?",
            style = MaterialTheme.typography.titleLarge)
        val radioOptions = WorkoutPlanGoal.entries.map { it.goal }

        // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
        Column(Modifier.selectableGroup()) {
            radioOptions.forEach { text ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (text == goalChoice.value),
                            onClick = { goalChoice.value = text },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = CenterVertically
                ) {
                    RadioButton(
                        selected = (text == goalChoice.value),
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
fun ExpertiseLevel(expertiseLevel: MutableState<String>){
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)) {
        Text("What is your expertise level?",
            style = MaterialTheme.typography.titleLarge)
        val radioOptions = WorkoutPlanDifficulty.entries.map { it.expertiseLevel }
        // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
        Column(Modifier.selectableGroup()) {
            radioOptions.forEach { text ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (text == expertiseLevel.value),
                            onClick = { expertiseLevel.value = text },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = CenterVertically
                ) {
                    RadioButton(
                        selected = (text == expertiseLevel.value),
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
fun WorkoutSplit(workoutSplit: MutableState<String>){
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)) {
        Text("How many times per week do you want to exercise?",
            style = MaterialTheme.typography.titleLarge)
        val radioOptions = WorkoutPlanSplit.entries.map { it.split }
        // Note that Modifier.selectableGroup() is essential to ensure correct accessibility behavior
        Column(Modifier.selectableGroup()) {
            radioOptions.forEach { text ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (text == workoutSplit.value),
                            onClick = { workoutSplit.value = text },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = CenterVertically
                ) {
                    RadioButton(
                        selected = (text == workoutSplit.value),
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

