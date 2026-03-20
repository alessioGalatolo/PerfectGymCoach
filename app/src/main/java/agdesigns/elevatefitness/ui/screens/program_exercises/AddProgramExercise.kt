package agdesigns.elevatefitness.ui.screens.program_exercises

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseReorder
import agdesigns.elevatefitness.data.db.entity.getProgramDisplayName
import agdesigns.elevatefitness.navigation.ChangePlanGraph
import agdesigns.elevatefitness.navigation.SlideTransition
import agdesigns.elevatefitness.ui.common.EmptyScreenInfo
import agdesigns.elevatefitness.ui.common.SharedElementGeneralKeys
import agdesigns.elevatefitness.ui.screens.program_exercises.components.ProgramExerciseCard
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.ExercisesByMuscleDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import sh.calvin.reorderable.rememberReorderableLazyListState

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
    LaunchedEffect(programId) {
        viewModel.onEvent(ProgramExercisesEvent.GetProgramExercises(programId))
    }
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
        val toIndex = state.programExercises.find { it.programExerciseId == to.key }!!.orderInProgram
        val fromIndex = state.programExercises.find { it.programExerciseId == from.key }!!.orderInProgram
        Log.d("AddProgramExercise", "Reorder from: $fromIndex to $toIndex")
        while (viewModel.reorderCompleted.tryReceive().isSuccess);
        viewModel.onEvent(
            ProgramExercisesEvent.ReorderExercises(
                listOf(
                    ProgramExerciseReorder(
                        from.key as Long,
                        toIndex
                    ),
                    ProgramExerciseReorder(
                        to.key as Long,
                        fromIndex
                    )
                )
            )
        )
        haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        while (viewModel.reorderCompleted.receive()) {
            // check reorder completed
            if (state.programExercises.find { it.programExerciseId == from.key }!!.orderInProgram == toIndex &&
                state.programExercises.find { it.programExerciseId == to.key }!!.orderInProgram == fromIndex
            )
                break
        }
    }
    val expandedFab by remember { derivedStateOf { !listState.isScrollInProgress } }
    val dragStarted = rememberSaveable { mutableStateOf(false) }

    /*
    If user is coming back from a screen with a transition and tries to go back rapidly
    the old screen will flash. This feels like a bug for compose to solve but until then,
    we disallow going back until the transition is finished
     */
    val running = this@AddProgramExercise.isTransitionActive
    BackHandler(enabled = running) { }
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
                    rememberSharedContentState(
                        SharedElementGeneralKeys.FAP_TO_VIEW
                    ),
                    animatedVisibilityScope,
                    boundsTransform = BoundsTransform { _, _ ->
                        MotionScheme.expressive().slowSpatialSpec()
                    }
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
                    itemsIndexed(
                        items = state.programExercises,
                        key = { _, it -> it.programExerciseId }
                    ) { index, programExercise ->
                        val exercise = remember(index) { state.exercises[index] }
                        val brightImage = remember { mutableStateOf(false) }
                        if (index != 0){
                            AnimatedVisibility(
                                !dragStarted.value,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                Row(  // row with button for superset
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
                                ) {
                                    val linked =
                                        programExercise.supersetExercise == state.programExercises[index - 1].programExerciseId
                                    val orientation = remember { Animatable(0f) }
                                    val scale = remember { Animatable(1f) }
                                    LaunchedEffect(linked) {
                                        orientation.animateTo(if (linked) 90f else 0f)
                                    }
                                    LaunchedEffect(linked) {
                                        scale.animateTo(if (linked) 1.1f else 1f)
                                    }
                                    Icon(
                                        if (linked)
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
                        }
                        ProgramExerciseCard(
                            animatedVisibilityScope = animatedVisibilityScope,
                            navigator = navigator,
                            reorderableListState = reorderableLazyListState,
                            exercise = exercise,
                            programExercise = programExercise,
                            brightImage = brightImage,
                            dragStarted = dragStarted,
                            canMoveUp = index > 0,
                            canMoveDown = index + 1 < state.programExercises.size,
                            moveUp = {
                                viewModel.onEvent(
                                    ProgramExercisesEvent.ReorderExercises(
                                        listOf(
                                            ProgramExerciseReorder(
                                                programExercise.programExerciseId,
                                                programExercise.orderInProgram - 1
                                            ),
                                            ProgramExerciseReorder(
                                                state.programExercises[index - 1].programExerciseId,
                                                programExercise.orderInProgram
                                            )
                                        )
                                    )
                                )
                            },
                            moveDown = {
                                viewModel.onEvent(
                                    ProgramExercisesEvent.ReorderExercises(
                                        listOf(
                                            ProgramExerciseReorder(
                                                programExercise.programExerciseId,
                                                programExercise.orderInProgram + 1
                                            ),
                                            ProgramExerciseReorder(
                                                state.programExercises[index + 1].programExerciseId,
                                                programExercise.orderInProgram
                                            )
                                        )
                                    )
                                )
                            },
                            deleteExercise = {
                                viewModel.onEvent(
                                    ProgramExercisesEvent.DeleteExercise(
                                        programExercise.programExerciseId
                                    )
                                )
                            },
                            duplicateExercise = {
                                viewModel.onEvent(
                                    ProgramExercisesEvent.DuplicateExercise(
                                        programExercise.programExerciseId
                                    )
                                )
                            }
                        )
                    }
                    item{
                        var finalSpacerSize = 56.dp + 16.dp// large fab size + its padding FIXME: not hardcode
                        finalSpacerSize += 16.dp
                        Spacer(Modifier.height(finalSpacerSize))
                    }
                }
            }
        })
}
