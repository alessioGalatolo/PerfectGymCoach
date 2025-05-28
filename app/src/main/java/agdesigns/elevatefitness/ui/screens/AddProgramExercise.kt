package agdesigns.elevatefitness.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment.TopRight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.exercise.ProgramExerciseReorder
import agdesigns.elevatefitness.ui.ChangePlanGraph
import agdesigns.elevatefitness.viewmodels.ExercisesEvent
import agdesigns.elevatefitness.viewmodels.ExercisesViewModel
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddExerciseDialogDestination
import com.ramcosta.composedestinations.generated.destinations.ExercisesByMuscleDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination<ChangePlanGraph>
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddProgramExercise(
    navigator: DestinationsNavigator,
    programName: String,
    programId: Long,
    viewModel: ExercisesViewModel = hiltViewModel()
) {
    viewModel.onEvent(ExercisesEvent.GetProgramExercises(programId))
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(programName) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    navigator.navigate(
                        ExercisesByMuscleDestination(
                            programName = programName,
                            programId = programId
                        ),
                        onlyIfResumed = true
                    )
                },
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add exercise",
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                )
            }
        }, content = { innerPadding ->
            if (viewModel.state.value.programExercisesAndInfo.isEmpty()) {
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
                    itemsIndexed(items = viewModel.state.value.programExercisesAndInfo,
                        key = { _, it -> it.programExerciseId }) { index, exercise ->
                        val brightImage = remember { mutableStateOf(false) }
                        var expanded by remember { mutableStateOf(false) }
                        if (index != 0){
                            Row (  // row with button for superset
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.onEvent(
                                            ExercisesEvent.UpdateSuperset(
                                                index,
                                                index - 1
                                            )
                                        )
                                    }
                                    .wrapContentHeight()
                                    .fillMaxWidth()
                            ){
                                val linked = exercise.supersetExercise == viewModel.state.value.programExercisesAndInfo[index-1].programExerciseId
                                val orientation = remember { Animatable(0f) }
                                val scale = remember { Animatable(1f) }
                                LaunchedEffect(linked) {
                                    orientation.animateTo(if (linked) 90f else 0f)
                                }
                                LaunchedEffect(linked){
                                    scale.animateTo(if (linked) 1.1f else 1f)
                                }
                                Icon(if (linked)
                                    Icons.Default.Link
                                else
                                    Icons.Default.LinkOff,
                                    if (linked) "Superset" else "Superset off",
                                    Modifier.scale(scale.value)
                                        .rotate(orientation.value)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Superset",
                                    fontStyle = FontStyle.Italic,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .combinedClickable(
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        expanded = true
                                    },
                                    onClick = {
                                        navigator.navigate(
                                            AddExerciseDialogDestination(
                                                programId = exercise.extProgramId,
                                                exerciseId = exercise.extExerciseId,
                                                programExerciseId = exercise.programExerciseId
                                            ),
                                            onlyIfResumed = true
                                        )
                                    }
                                )
                                .padding(
                                    horizontal = dimensionResource(R.dimen.card_outside_padding),
                                    vertical = dimensionResource(R.dimen.card_space_between)/2
                                )
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
                                    "Exercise image",
                                    Modifier
                                        .fillMaxWidth()
                                        .height(with (LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } / 3)
                                        .align(Alignment.TopCenter)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .wrapContentSize()
                                        .align(TopRight)
                                ) {

                                    IconButton(onClick = { expanded = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "More options",
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
                                                    ProgramExerciseReorder(exercise.programExerciseId, exercise.orderInProgram-1),
                                                    ProgramExerciseReorder(viewModel.state.value.programExercisesAndInfo[index-1].programExerciseId, exercise.orderInProgram)
                                                )))
                                                expanded = false
                                            },
                                            enabled = index > 0,
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.ArrowUpward,
                                                    contentDescription = "Move up"
                                                )
                                            })
                                        DropdownMenuItem(
                                            text = { Text("Move down") },
                                            onClick = {
                                                viewModel.onEvent(ExercisesEvent.ReorderExercises(listOf(
                                                    ProgramExerciseReorder(exercise.programExerciseId, exercise.orderInProgram+1),
                                                    ProgramExerciseReorder(viewModel.state.value.programExercisesAndInfo[index+1].programExerciseId, exercise.orderInProgram)
                                                )))
                                                expanded = false
                                            },
                                            enabled = index+1 < viewModel.state.value.programExercisesAndInfo.size,
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.ArrowDownward,
                                                    contentDescription = "Move down"
                                                )
                                            })
                                        DropdownMenuItem(
                                            text = { Text("Edit") },
                                            onClick = {
                                                navigator.navigate(
                                                    AddExerciseDialogDestination(
                                                        programId = exercise.extProgramId,
                                                        exerciseId = exercise.extExerciseId,
                                                        programExerciseId = exercise.programExerciseId
                                                    ),
                                                    onlyIfResumed = true
                                                )
                                                expanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Edit,
                                                    contentDescription = "Edit"
                                                )
                                            })
                                        DropdownMenuItem(
                                            text = { Text("Delete") },
                                            onClick = {
                                                viewModel.onEvent(ExercisesEvent.DeleteExercise(
                                                    exercise.programExerciseId
                                                ))
                                                expanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Delete,
                                                    contentDescription = "Delete"
                                                )
                                            })
                                    }
                                }
                            }
                            Column(Modifier.padding(dimensionResource(R.dimen.card_inner_padding))) {
                                Text(
                                    text = exercise.name + exercise.variation,
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
                                    append(exercise.rest.joinToString("s, ") + "s")
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
                        var finalSpacerSize = 96.dp + 8.dp// large fab size + its padding FIXME: not hardcode
                        finalSpacerSize += 16.dp
                        Spacer(modifier = Modifier.navigationBarsPadding())
                        Spacer(Modifier.height(finalSpacerSize))
                    }
                }
            }
        })
}
