package agdesigns.elevatefitness.ui.screens

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
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.ui.FadeTransition
import agdesigns.elevatefitness.ui.components.ResumeWorkout
import agdesigns.elevatefitness.ui.components.WorkoutCard
import agdesigns.elevatefitness.viewmodels.HomeEvent
import agdesigns.elevatefitness.viewmodels.HomeViewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import agdesigns.elevatefitness.ui.BottomNavigationGraph
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import coil3.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddProgramDestination
import com.ramcosta.composedestinations.generated.destinations.AddProgramExerciseDestination
import com.ramcosta.composedestinations.generated.destinations.AddWorkoutPlanDestination
import com.ramcosta.composedestinations.generated.destinations.WorkoutDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay


@Destination<BottomNavigationGraph>(start = true, style = FadeTransition::class)
@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
fun SharedTransitionScope.Home(
    animatedVisibilityScope: AnimatedVisibilityScope,
    navigator: DestinationsNavigator,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current

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
            )
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
                        AddWorkoutPlanDestination(openDialogNow = true)
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
                GeneratePlanButton(navigator)
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
                    )
                )
            }) {
                Text(stringResource(id = R.string.add_program))
            }
            TextButton(
                onClick = {
                    navigator.navigate(
                        AddWorkoutPlanDestination()
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
                // animate card, image and text separately
                val cardKey = rememberSharedContentState("card_${currentProgram.programId}")
                val imageKey = rememberSharedContentState("img_${currentProgram.programId}")
                val exerciseNameKey = rememberSharedContentState("exName_${currentProgram.programId}")
                val roundedCornersShape = CardDefaults.shape
                WorkoutCard(
                    program = currentProgram,
                    exercises = currentExercises,
                    // TODO: add message when no exercises in the program
                    onCardClick = { previewExercise ->
                        navigator.navigate(
                            WorkoutDestination(
                                programId = currentProgram.programId,
                                previewExercise = previewExercise
                            ),
                        )
                    },
                    navigator = navigator,
                    // FIXME: suboptimal solution
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = cardKey,
                            animatedVisibilityScope = animatedVisibilityScope,
//                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                        )
                    ,
                    imageModifier = Modifier
                        .sharedBounds(
                            sharedContentState = imageKey,
                            animatedVisibilityScope = animatedVisibilityScope,
                            clipInOverlayDuringTransition = OverlayClip(roundedCornersShape)
                        ),
                    exerciseModifier = Modifier
                        .sharedBounds(
                            sharedContentState = exerciseNameKey,
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
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
                val exs =
                    viewModel.state.value.exercisesAndInfo[it.programId]?.sortedBy {
                        it.programExerciseId
                    } ?: emptyList()
                val pagerState = rememberPagerState(pageCount = { exs.size })
                Card(
                    modifier = Modifier
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState("card_${it.programId}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen.card_space_between) / 2)
                        .combinedClickable(onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            navigator.navigate(
                                AddProgramExerciseDestination(
                                    programName = it.name,
                                    programId = it.programId
                                )
                            )
                        }) {
                            navigator.navigate(
                                WorkoutDestination(
                                    programId = it.programId,
                                    previewExercise = exs[pagerState.currentPage]
                                )
                            )
                        }
                ){
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(R.dimen.card_inner_padding))
                    ) {
                        Column(
                            Modifier
                                .weight(1.6f)
                                .fillMaxHeight()) {
                            Text(
                                text = it.name,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            exs.forEach { exercise -> // TODO: mark supersets
                                val exerciseName = exercise.name + exercise.variation
                                val modifier = if (exercise.orderInProgram == pagerState.currentPage) Modifier.sharedBounds(
                                        sharedContentState = rememberSharedContentState("exName_${it.programId}"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                    ) else Modifier
                                Text(text = exerciseName, modifier = modifier)
                            }
                        }
                        Column (verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ){
                            if (exs.isNotEmpty()) {
                                HorizontalPager(
                                    state = pagerState,
                                    userScrollEnabled = false,
                                    modifier = Modifier
                                        .sharedBounds(
                                            sharedContentState = rememberSharedContentState("img_${it.programId}"),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                        )
                                        .width(150.dp)
                                        .height(150.dp / 3 * 2)
                                        .clip(AbsoluteRoundedCornerShape(12.dp))
                                        .align(Alignment.End)
                                ) { page ->
                                    Box(Modifier.wrapContentSize()) {
                                        AsyncImage(
                                            model = exs[page].image,
                                            contentDescription = "Exercise image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .height(150.dp / 3 * 2)
                                                .width(150.dp)
                                        )
                                    }
                                }
                                LaunchedEffect(viewModel.state.value.animationTick){
                                    if (!animatedVisibilityScope.transition.isRunning) {
                                        pagerState.animateScrollToPage(
                                            (pagerState.currentPage + 1) %
                                                    exs.size
                                        )
                                    }
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
                                            previewExercise = exs[pagerState.currentPage],
                                            quickStart = true
                                        )
                                    )
                                }) {
                                Icon(Icons.Default.RocketLaunch, "Quick start workout")
                            }
                        }
                        IconButton(
                            onClick = {
                                navigator.navigate(
                                    AddProgramExerciseDestination(
                                        programName = it.name,
                                        programId = it.programId
                                    )
                                )
                            }) {
                            Icon(Icons.Outlined.Edit, "Edit program")
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
                                AddWorkoutPlanDestination()
                            )
                        }, modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text(stringResource(R.string.change_workout_plan)) }
                    TextButton(onClick = {
                        navigator.navigate(
                            AddProgramDestination(
                                planName = "", // FIXME: empty plan name
                                planId = viewModel.state.value.currentPlan!!
                            )
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