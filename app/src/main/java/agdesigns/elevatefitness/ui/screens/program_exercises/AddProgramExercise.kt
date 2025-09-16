package agdesigns.elevatefitness.ui.screens.program_exercises

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseReorder
import agdesigns.elevatefitness.data.db.entity.getProgramDisplayName
import agdesigns.elevatefitness.navigation.ChangePlanGraph
import agdesigns.elevatefitness.navigation.SlideTransition
import agdesigns.elevatefitness.ui.common.EmptyScreenInfo
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddExerciseDialogDestination
import com.ramcosta.composedestinations.generated.destinations.ExercisesByMuscleDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination<ChangePlanGraph>(style = SlideTransition::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class
)
@Composable
fun SharedTransitionScope.AddProgramExercise(
    animatedVisibilityScope: AnimatedVisibilityScope,
    navigator: DestinationsNavigator,
    programName: String,
    programId: Long,
    viewModel: ProgramExercisesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    viewModel.onEvent(ProgramExercisesEvent.GetProgramExercises(programId))
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val expandedFab by remember { derivedStateOf { !listState.isScrollInProgress } }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = { Text(getProgramDisplayName(programName)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navigator.navigateUp() },
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back_icon)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = {
                    Text(stringResource(R.string.search_and_add_exercise))
                },
                icon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.add_exercise),
                    )
                },
                expanded = expandedFab,
                onClick = {
                    navigator.navigate(
                        ExercisesByMuscleDestination(
                            programName = programName,
                            programId = programId
                        )
                    )
                },
                modifier = Modifier.sharedBounds(
                    rememberSharedContentState("fab2view"),
                    animatedVisibilityScope
                )
            )
        }, content = { innerPadding ->
            if (state.programExercises.isEmpty()) {
                // if you have no exercises
                EmptyScreenInfo(
                    Icons.Filled.FitnessCenter,
                    R.string.empty_exercises,
                    titleRes = R.string.empty_exercises,
                    subtitleRes = R.string.empty_exercises_desc
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = listState,
                    contentPadding = innerPadding,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(items = state.programExercises,
                        key = { _, it -> it.programExerciseId }) { index, programExercise ->
                        val exercise = remember(index) { state.exercises[index] }
                        val brightImage = remember { mutableStateOf(false) }
                        var expanded by remember { mutableStateOf(false) }
                        if (index != 0){
                            Row (  // row with button for superset
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .clip(CardDefaults.shape)
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            viewModel.onEvent(
                                                ProgramExercisesEvent.UpdateSuperset(
                                                    index,
                                                    index - 1
                                                )
                                            )
                                        }, onLongClick = {})
                                    .wrapContentHeight()
                            ){
                                val linked = programExercise.supersetExercise == state.programExercises[index-1].programExerciseId
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
                                    stringResource(if (linked) R.string.superset else R.string.superset_off),
                                    Modifier
                                        .scale(scale.value)
                                        .rotate(orientation.value)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.superset),
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
                                                programId = programExercise.extProgramId,
                                                exerciseId = programExercise.extExerciseId,
                                                programExerciseId = programExercise.programExerciseId,
                                                continueAdding = false
                                            )
                                        )
                                    }
                                )
                                .padding(
                                    horizontal = dimensionResource(R.dimen.card_outside_padding),
                                    vertical = dimensionResource(R.dimen.card_space_between) / 2
                                )
                        ) {
                            Box (Modifier.fillMaxWidth()){
                                AsyncImage(
                                    ImageRequest.Builder(context)
                                        .allowHardware(false)
                                        .data(exercise?.image ?: R.drawable.finish_workout)
                                        .crossfade(true)
                                        .listener { _, result ->
                                            val image = result.image.toBitmap()
                                            Palette.from(image).maximumColorCount(3)
                                                .clearFilters()
                                                .setRegion(image.width-50, 0, image.width,50)
                                                .generate {
                                                    brightImage.value = (ColorUtils.calculateLuminance(it?.getDominantColor(
                                                        Color.Black.toArgb()) ?: 0)) > 0.5
                                                }
                                        }
                                        .build(),
                                    stringResource(R.string.exercise_image),
                                    Modifier
                                        .fillMaxWidth()
                                        .height(with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } / 3)
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
                                            contentDescription = stringResource(R.string.morevert_icon_options),
                                            tint = if (brightImage.value) Color.Black else Color.White
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.move_up)) },
                                            onClick = {
                                                viewModel.onEvent(ProgramExercisesEvent.ReorderExercises(
                                                    listOf(
                                                        ProgramExerciseReorder(
                                                            programExercise.programExerciseId,
                                                            programExercise.orderInProgram-1
                                                        ),
                                                        ProgramExerciseReorder(
                                                            state.programExercises[index-1].programExerciseId,
                                                            programExercise.orderInProgram
                                                        )
                                                )))
                                                expanded = false
                                            },
                                            enabled = index > 0,
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.ArrowUpward,
                                                    contentDescription = stringResource(R.string.move_up)
                                                )
                                            })
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.move_down)) },
                                            onClick = {
                                                viewModel.onEvent(ProgramExercisesEvent.ReorderExercises(
                                                    listOf(
                                                        ProgramExerciseReorder(
                                                            programExercise.programExerciseId,
                                                            programExercise.orderInProgram+1
                                                        ),
                                                        ProgramExerciseReorder(
                                                            state.programExercises[index+1].programExerciseId,
                                                            programExercise.orderInProgram
                                                        )
                                                )))
                                                expanded = false
                                            },
                                            enabled = index+1 < state.programExercises.size,
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.ArrowDownward,
                                                    contentDescription = stringResource(R.string.move_down)
                                                )
                                            })
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.edit)) },
                                            onClick = {
                                                navigator.navigate(
                                                    AddExerciseDialogDestination(
                                                        programId = programExercise.extProgramId,
                                                        exerciseId = programExercise.extExerciseId,
                                                        programExerciseId = programExercise.programExerciseId,
                                                        continueAdding = false
                                                    )
                                                )
                                                expanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Edit,
                                                    contentDescription = stringResource(R.string.edit)
                                                )
                                            })
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.remove)) },
                                            onClick = {
                                                viewModel.onEvent(ProgramExercisesEvent.DeleteExercise(
                                                    programExercise.programExerciseId
                                                ))
                                                expanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Outlined.Delete,
                                                    contentDescription = stringResource(R.string.delete)
                                                )
                                            })
                                    }
                                }
                            }
                            Column(Modifier.padding(dimensionResource(R.dimen.card_inner_padding))) {
                                val variation = if (programExercise.variation.isBlank()) "" else " (${programExercise.variation})"
                                Text(
                                    text = (exercise?.name ?: "") + variation,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                        append(stringResource(R.string.sets))
                                        append(": ")
                                    }
                                    append(programExercise.reps.size.toString())
                                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                        append(" • ")
                                        append(stringResource(R.string.reps))
                                        append(": ")
                                    }
                                    append(programExercise.reps.joinToString(", "))
                                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                        append(" • ")
                                        append(stringResource(R.string.rest))
                                        append(": ")
                                    }
                                    append(programExercise.rest.joinToString("s, ") + "s")
                                })
                                if (programExercise.note.isNotBlank())
                                    Text(text = buildAnnotatedString {
                                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                            append(stringResource(R.string.note))
                                        }
                                        append(programExercise.note)
                                    })
                            }
                        }
//                        }
                    }
                    item{
                        var finalSpacerSize = 56.dp + 8.dp// large fab size + its padding FIXME: not hardcode
                        finalSpacerSize += 16.dp
                        Spacer(Modifier.height(finalSpacerSize))
                    }
                }
            }
        })
}
