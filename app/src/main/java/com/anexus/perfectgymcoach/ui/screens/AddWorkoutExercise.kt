package com.anexus.perfectgymcoach.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseAndInfo
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseReorder
import com.anexus.perfectgymcoach.ui.MainScreen
import com.anexus.perfectgymcoach.viewmodels.ExercisesEvent
import com.anexus.perfectgymcoach.viewmodels.ExercisesViewModel
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddExercise(navController: NavHostController, programName: String, programId: Long,
                viewModel: ExercisesViewModel = hiltViewModel()) {
    viewModel.onEvent(ExercisesEvent.GetWorkoutExercises(programId))
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SmallTopAppBar(
                title = { Text(programName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }, floatingActionButton = {
            LargeFloatingActionButton (
                modifier = Modifier.navigationBarsPadding(),
                onClick = {
                    navController.navigate("${MainScreen.ExercisesByMuscle.route}/$programName/$programId")
                },
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add exercise",
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                )
            }
        }, content = { innerPadding ->
            if (viewModel.state.value.workoutExercisesAndInfo.isEmpty()) {
                // if you have no exercises
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = "",
                        modifier = Modifier.size(160.dp)
                    )
                    Text(
                        stringResource(id = R.string.empty_exercises),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                val state = rememberReorderableLazyListState(
                    onDragEnd = { from, to ->

                    },
                    canDragOver = { pos -> viewModel.state.value.workoutExercisesAndInfo.any { it == pos.key } },
                    onMove = { from, to ->
                    viewModel.onEvent(ExercisesEvent.ReorderExercises(listOf(
                        WorkoutExerciseReorder(
                            (from.key as WorkoutExerciseAndInfo).workoutExerciseId,
                            (to.key as WorkoutExerciseAndInfo).orderInProgram
                        ),
                        WorkoutExerciseReorder(
                            (to.key as WorkoutExerciseAndInfo).workoutExerciseId,
                            (from.key as WorkoutExerciseAndInfo).orderInProgram
                        )
                    )))
                })
                LazyColumn(
                    state = state.listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .reorderable(state),
                    contentPadding = innerPadding
                ) {
                    items(items = viewModel.state.value.workoutExercisesAndInfo, key = { it }) { exercise ->
                        ReorderableItem(
                            reorderableState = state,
                            key = exercise
                        ) { isDragging ->
                            val elevation = animateDpAsState(if (isDragging) 16.dp else 0.dp)
                            val scale = animateFloatAsState(if (isDragging) 1.05f else 1.0f)
                            ElevatedCard(
                                onClick = {
                                    navController.navigate(
                                        "${MainScreen.AddExerciseDialog.route}/" +
                                                "${exercise.extProgramId}/" +
                                                "${exercise.extExerciseId}/" +
                                                "${exercise.workoutExerciseId}"
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(elevation.value, RoundedCornerShape(12.dp))
                                    .scale(scale.value)
                                    .detectReorderAfterLongPress(state)
                                    .animateItemPlacement()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                AsyncImage(
                                    model = exercise.image,
                                    contentScale = ContentScale.Crop,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(LocalConfiguration.current.screenWidthDp.dp / 3)
                                        .align(Alignment.CenterHorizontally)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Column(Modifier.padding(8.dp)) {
                                    Text(
                                        text = exercise.name,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = buildAnnotatedString {
                                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                            append("Sets: ")
                                        }
                                        append(exercise.reps.size.toString())
                                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                            append(" • Reps: ")
                                        }
                                        append(exercise.reps.joinToString(", "))
                                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                            append(" • Rest: ")
                                        }
                                        append("${exercise.rest}s")
                                    })
                                    if (exercise.note.isNotBlank())
                                        Text(text = buildAnnotatedString {
                                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                                append("Note: ")
                                            }
                                            append(exercise.note)
                                        })
                                }
                            }
                        }
                    }
                    item{
                        var finalSpacerSize = 96.dp + 8.dp // large fab size + its padding FIXME: not hardcode
                        finalSpacerSize += 8.dp
                        Spacer(modifier = Modifier.navigationBarsPadding())
                        Spacer(Modifier.height(finalSpacerSize))
                    }
                }
            }
        })
}
