package com.anexus.perfectgymcoach.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment.TopRight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseAndInfo
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseReorder
import com.anexus.perfectgymcoach.ui.MainScreen
import com.anexus.perfectgymcoach.viewmodels.ExercisesEvent
import com.anexus.perfectgymcoach.viewmodels.ExercisesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddExercise(navController: NavHostController, programName: String, programId: Long,
                viewModel: ExercisesViewModel = hiltViewModel()) {
    viewModel.onEvent(ExercisesEvent.GetWorkoutExercises(programId))
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
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
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = innerPadding
                ) {
                    itemsIndexed(items = viewModel.state.value.workoutExercisesAndInfo,
                        key = { _, it -> it.workoutExerciseId }) { index, exercise ->
                        val brightImage = remember { mutableStateOf(false) }
                        var expanded by remember { mutableStateOf(false) }
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement()
                                .combinedClickable (
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        expanded = true
                                    },
                                    onClick = {
                                        navController.navigate(
                                        "${MainScreen.AddExerciseDialog.route}/" +
                                                "${exercise.extProgramId}/" +
                                                "${exercise.extExerciseId}/" +
                                                "${exercise.workoutExerciseId}"
                                        )
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Box (Modifier.fillMaxWidth()){
                                AsyncImage(
                                    ImageRequest.Builder(context)
                                        .allowHardware(false)
                                        .data(exercise.image)
                                        .crossfade(true)
                                        .listener { _, result ->
                                            val image = result.drawable.toBitmap()
                                            Palette.from(image).maximumColorCount(3)
                                                .clearFilters()
                                                .setRegion(image.width-50, 0, image.width,50)
                                                .generate {
                                                    brightImage.value = (ColorUtils.calculateLuminance(it?.getDominantColor(
                                                        Color.Black.toArgb()) ?: 0)) > 0.5
                                                }
                                        }
                                        .build(),
                                    null,
                                    Modifier
                                        .fillMaxWidth()
                                        .height(LocalConfiguration.current.screenWidthDp.dp / 3)
                                        .align(Alignment.TopCenter)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier.wrapContentSize().align(TopRight)
                                ) {

                                    IconButton(onClick = { expanded = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Localized description",
                                            tint = if (brightImage.value) Color.Black else Color.White
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Move up") },
                                            onClick = {
                                                viewModel.onEvent(ExercisesEvent.ReorderExercises(listOf(
                                                    WorkoutExerciseReorder(exercise.workoutExerciseId, exercise.orderInProgram-1),
                                                    WorkoutExerciseReorder(viewModel.state.value.workoutExercisesAndInfo[index-1].workoutExerciseId, exercise.orderInProgram)
                                                )))
                                                expanded = false
                                            },
                                            enabled = index > 0,
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.ArrowUpward,
                                                    contentDescription = null
                                                )
                                            })
                                        DropdownMenuItem(
                                            text = { Text("Move down") },
                                            onClick = {
                                                viewModel.onEvent(ExercisesEvent.ReorderExercises(listOf(
                                                    WorkoutExerciseReorder(exercise.workoutExerciseId, exercise.orderInProgram+1),
                                                    WorkoutExerciseReorder(viewModel.state.value.workoutExercisesAndInfo[index+1].workoutExerciseId, exercise.orderInProgram)
                                                )))
                                                expanded = false
                                            },
                                            enabled = index+1 < viewModel.state.value.workoutExercisesAndInfo.size,
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.ArrowDownward,
                                                    contentDescription = null
                                                )
                                            })
                                        DropdownMenuItem(
                                            text = { Text("Edit") },
                                            onClick = {
                                                navController.navigate(
                                                    "${MainScreen.AddExerciseDialog.route}/" +
                                                            "${exercise.extProgramId}/" +
                                                            "${exercise.extExerciseId}/" +
                                                            "${exercise.workoutExerciseId}"
                                                )
                                                expanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Edit,
                                                    contentDescription = null
                                                )
                                            })
                                        DropdownMenuItem(
                                            text = { Text("Delete") },
                                            onClick = {
                                                viewModel.onEvent(ExercisesEvent.DeleteExercise(
                                                    exercise.workoutExerciseId
                                                ))
                                                expanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Delete,
                                                    contentDescription = null
                                                )
                                            })
                                    }
                                }
                            }
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
//                        }
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
