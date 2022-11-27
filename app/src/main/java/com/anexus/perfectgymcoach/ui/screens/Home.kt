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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.ui.MainScreen
import com.anexus.perfectgymcoach.ui.components.ResumeWorkout
import com.anexus.perfectgymcoach.ui.components.WorkoutCard
import com.anexus.perfectgymcoach.viewmodels.HomeEvent
import com.anexus.perfectgymcoach.viewmodels.HomeViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalPagerApi::class
)
@Composable
fun Home(navController: NavHostController,
         contentPadding: PaddingValues,
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
        navController.navigate("${MainScreen.Workout.route}/${0L}/${false}/${true}")
    }

    LaunchedEffect(viewModel.state.value.currentWorkout){
        delay(1000)  // FIXME: done in order to avoid double dialog showing
        resumeWorkoutDialogOpen = viewModel.state.value.currentWorkout != null
    }


    if (viewModel.state.value.currentPlan == null) {
        Scaffold(
            modifier = Modifier.padding(contentPadding),
            floatingActionButton = {
                LargeFloatingActionButton(
                    onClick = { navController.navigate("${MainScreen.ChangePlan.route}/${true}") }
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
        Column (horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(contentPadding)) {
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
                navController.navigate( // FIXME: empty plan name
                    "${MainScreen.AddProgram.route}/ /${viewModel.state.value.currentPlan!!}/${true}")
            }) {
                Text(stringResource(id = R.string.add_program))
            }
            TextButton(
                onClick = { navController.navigate("${MainScreen.ChangePlan.route}/${false}" ) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text(stringResource(R.string.change_workout_plan)) }
            Spacer(modifier = Modifier.height(8.dp))

        }
    } else if (viewModel.state.value.programs?.isNotEmpty() == true
        && viewModel.state.value.currentProgram != null
    ) {
        LazyColumn(Modifier.padding(horizontal = 16.dp), contentPadding = contentPadding){
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
                    onCardClick = { navController.navigate("${MainScreen.Workout.route}/" +
                            "${currentProgram.programId}/${false}/${false}") },
                    navController = navController
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
                        .padding(vertical = 4.dp)
                        .combinedClickable(onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            navController.navigate(
                                "${MainScreen.AddProgramExercise.route}/" +
                                        "${it.name}/" +
                                        "${it.programId}"
                            )
                        }) {
                            navController.navigate("${MainScreen.Workout.route}/" +
                                    "${it.programId}/${false}/${false}")
                        }
                ){
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val pagerState = rememberPagerState()
                        val exs =
                            viewModel.state.value.exercisesAndInfo[it.programId]?.sortedBy {
                                it.programExerciseId
                            } ?: emptyList()
                        Column(Modifier.weight(1.6f).fillMaxHeight()) {
                            Text(
                                text = it.name,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.titleLarge
                            )
                            exs.forEach { // TODO: mark supersets
                                Text(text = it.name + it.variation,
                                    modifier = Modifier.padding(horizontal = 8.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Column (verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ){
                            if (exs.isNotEmpty()) {
                                HorizontalPager(
                                    count = exs.size, state = pagerState,
                                    modifier = Modifier.width(150.dp)
                                        .height(150.dp / 3 * 2)
                                        .padding(8.dp)
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
                                                pagerState.pageCount
                                    )
                                }
                            }
                            // FIXME: should go to the bottom but does not
                            Row (horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.fillMaxWidth()
                            ){
                                IconButton(
                                    onClick = {
                                        navController.navigate("${MainScreen.Workout.route}/" +
                                                "${it.programId}/${true}/${false}")
                                    }) {
                                    Icon(Icons.Default.RocketLaunch, null)
                                }
                                IconButton(
                                    onClick = {
                                        navController.navigate(
                                            "${MainScreen.AddProgramExercise.route}/" +
                                                    "${it.name}/" +
                                                    "${it.programId}"
                                        )
                                    }) {
                                    Icon(Icons.Outlined.Edit, null)
                                }
                            }
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
                            navController.navigate("${MainScreen.ChangePlan.route}/${false}")
                                  },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text(stringResource(R.string.change_workout_plan)) }
                    TextButton(onClick = {
                        navController.navigate( // FIXME: empty plan name
                            "${MainScreen.AddProgram.route}/ /" +
                                    "${viewModel.state.value.currentPlan!!}/${false}")
                    }) {
                        Text("Change programs")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}