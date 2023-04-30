package com.anexus.perfectgymcoach.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.ui.BottomNavigationNavGraph
import com.anexus.perfectgymcoach.ui.components.ResumeWorkout
import com.anexus.perfectgymcoach.ui.components.WorkoutCard
import com.anexus.perfectgymcoach.ui.destinations.AddProgramDestination
import com.anexus.perfectgymcoach.ui.destinations.AddProgramExerciseDestination
import com.anexus.perfectgymcoach.ui.destinations.AddWorkoutPlanDestination
import com.anexus.perfectgymcoach.ui.destinations.WorkoutDestination
import com.anexus.perfectgymcoach.viewmodels.HomeEvent
import com.anexus.perfectgymcoach.viewmodels.HomeViewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay


@BottomNavigationNavGraph(start=true)
@Destination
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class
)
fun Home(
    navigator: DestinationsNavigator,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val sysUiController = rememberSystemUiController()
    val darkTheme = isSystemInDarkTheme()
    LaunchedEffect(darkTheme) {
        sysUiController.statusBarDarkContentEnabled = !darkTheme
    }
    var resumeWorkoutDialogOpen by remember {
        mutableStateOf(false)
    }
    ResumeWorkout(dialogueIsOpen = resumeWorkoutDialogOpen,
        discardWorkout = {
            viewModel.onEvent(HomeEvent.ResetCurrentWorkout)
            resumeWorkoutDialogOpen = false
        }) {
        resumeWorkoutDialogOpen = false
        navigator.navigate(
            WorkoutDestination(
                programId = 0L,
                resumeWorkout = true
            ),
            onlyIfResumed = true
        )
    }

    LaunchedEffect(viewModel.state.value.currentWorkout){
        delay(200)  // FIXME: done in order to avoid double dialog showing
        resumeWorkoutDialogOpen = viewModel.state.value.currentWorkout != null
    }


    if (viewModel.state.value.currentPlan == null) {
        Scaffold(
            floatingActionButton = {
                LargeFloatingActionButton(
                    onClick = { navigator.navigate(
                        AddWorkoutPlanDestination(openDialogNow = true),
                        onlyIfResumed = true
                    ) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize)
                    )
                }
            }) {
            Column(modifier = Modifier.padding(it)) {
                Text(
                    stringResource(id = R.string.empty_home),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    } else if (viewModel.state.value.programs?.isEmpty() == true) {
        Column (horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = "",
                modifier = Modifier.size(160.dp)
            )
            Text(
                stringResource(id = R.string.empty_home_program),
                modifier = Modifier.padding(16.dp)
            )
            Button(onClick = {
                navigator.navigate(
                    AddProgramDestination(
                        planName = "", // FIXME: empty plan name
                        planId = viewModel.state.value.currentPlan!!,
                        openDialogNow = true
                    ),
                    onlyIfResumed = true
                )
            }) {
                Text(stringResource(id = R.string.add_program))
            }
            TextButton(
                onClick = {
                    navigator.navigate(
                        AddWorkoutPlanDestination(),
                        onlyIfResumed = true
                    ) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text(stringResource(R.string.change_workout_plan)) }
            Spacer(modifier = Modifier.height(8.dp))

        }
    } else if (viewModel.state.value.programs?.isNotEmpty() == true
        && viewModel.state.value.currentProgram != null
    ) {
        LazyColumn(Modifier.padding(horizontal = 16.dp)){
            val currentProgram = viewModel.state.value.programs!![viewModel.state.value.currentProgram!!]
            val currentExercises =
                viewModel.state.value.exercisesAndInfo[currentProgram.programId]?.sortedBy {
                    it.programExerciseId
                } ?: emptyList()
            item {
                // Coming next
                Text(text = stringResource(id = R.string.coming_next), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                WorkoutCard(
                    program = currentProgram,
                    exercises = currentExercises,
                    // TODO: add message when no exercises in the program
                    onCardClick = {
                        navigator.navigate(
                            WorkoutDestination(
                                programId = currentProgram.programId
                            ),
                            onlyIfResumed = true
                        )},
                    navigator = navigator
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (viewModel.state.value.programs!!.size > 1) {
                    Text(
                        text = stringResource(id = R.string.other_programs),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            items(items = viewModel.state.value.programs!!.minus(currentProgram), key = { it }){
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen.card_space_between) / 2)
                        .combinedClickable(onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            navigator.navigate(
                                AddProgramExerciseDestination(
                                    programName = it.name,
                                    programId = it.programId
                                ),
                                onlyIfResumed = true
                            )
                        }) {
                            navigator.navigate(
                                WorkoutDestination(
                                    programId = it.programId
                                ),
                                onlyIfResumed = true
                            )
                        }
                ){
                    val exs =
                        viewModel.state.value.exercisesAndInfo[it.programId]?.sortedBy {
                            it.programExerciseId
                        } ?: emptyList()
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.card_inner_padding))
                    ) {
                        val pagerState = rememberPagerState()
                        Column(Modifier.weight(1.6f).fillMaxHeight()) {
                            Text(
                                text = it.name,
                                style = MaterialTheme.typography.titleLarge
                            )
                            exs.forEach { // TODO: mark supersets
                                Text(text = it.name + it.variation)
                            }
                        }
                        Column (verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ){
                            if (exs.isNotEmpty()) {
                                HorizontalPager(
                                    pageCount = exs.size, state = pagerState,
                                    modifier = Modifier.width(150.dp)
                                        .height(150.dp / 3 * 2)
                                        .clip(AbsoluteRoundedCornerShape(12.dp))
                                        .align(Alignment.End)
                                ) { page ->
                                    Box(Modifier.wrapContentSize()) {
                                        AsyncImage(
                                            model = exs[page].image,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .height(150.dp / 3 * 2)
                                                .width(150.dp)
                                        )
                                    }
                                }
                                LaunchedEffect(viewModel.state.value.animationTick){
                                    pagerState.animateScrollToPage(
                                        (pagerState.currentPage + 1) %
                                                exs.size
                                    )
                                }
                            }
                        }
                    }
                    Row (horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth()
                    ){
                        if (exs.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    navigator.navigate(
                                        WorkoutDestination(
                                            programId = it.programId,
                                            quickStart = true
                                        ),
                                        onlyIfResumed = true
                                    )
                                }) {
                                Icon(Icons.Default.RocketLaunch, null)
                            }
                        }
                        IconButton(
                            onClick = {
                                navigator.navigate(
                                    AddProgramExerciseDestination(
                                        programName = it.name,
                                        programId = it.programId
                                    ),
                                    onlyIfResumed = true
                                )
                            }) {
                            Icon(Icons.Outlined.Edit, null)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            item {
                Column (horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()){
                    TextButton(
                        onClick = {
                            navigator.navigate(
                                AddWorkoutPlanDestination(),
                                onlyIfResumed = true
                            )
                        }, modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text(stringResource(R.string.change_workout_plan)) }
                    TextButton(onClick = {
                        navigator.navigate(
                            AddProgramDestination(
                                planName = "", // FIXME: empty plan name
                                planId = viewModel.state.value.currentPlan!!
                            ),
                            onlyIfResumed = true
                        )
                    }) {
                        Text("Change programs")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}