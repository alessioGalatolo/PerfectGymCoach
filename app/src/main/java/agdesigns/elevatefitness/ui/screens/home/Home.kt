package agdesigns.elevatefitness.ui.screens.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.getProgramDisplayName
import agdesigns.elevatefitness.navigation.FadeTransition
import agdesigns.elevatefitness.ui.common.ResumeWorkout
import agdesigns.elevatefitness.ui.common.WorkoutCard
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import agdesigns.elevatefitness.navigation.BottomNavigationGraph
import agdesigns.elevatefitness.ui.common.EmptyScreenInfo
import agdesigns.elevatefitness.ui.common.SharedElementKey
import agdesigns.elevatefitness.ui.common.SharedElementType
import agdesigns.elevatefitness.ui.screens.plans.GeneratePlanButton
import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddProgramDestination
import com.ramcosta.composedestinations.generated.destinations.AddProgramExerciseDestination
import com.ramcosta.composedestinations.generated.destinations.AddWorkoutPlanDestination
import com.ramcosta.composedestinations.generated.destinations.WorkoutDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlin.math.min


@Destination<BottomNavigationGraph>(start = true, style = FadeTransition::class)
@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class
)
fun SharedTransitionScope.Home(
    animatedVisibilityScope: AnimatedVisibilityScope,
    navigator: DestinationsNavigator,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val homeState by viewModel.state.collectAsState()
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
    val context = LocalContext.current

    LaunchedEffect(homeState.currentWorkout){
        // done in order to avoid double dialog showing especially when slow transitioning into workout
        delay(2000)
        resumeWorkoutDialogOpen = homeState.currentWorkout != null
    }
    // add dynamic launcher shortcuts based on current plan
    LaunchedEffect(homeState.currentProgram, homeState.programs) {
        val shortcuts = homeState.programs?.filterIndexed {
            index, program -> index != homeState.currentProgram
        }?.map {
            ShortcutInfoCompat.Builder(context, "start_workout_dyn_${it.programId}")
                .setShortLabel(getProgramDisplayName(it.name, context))
//                .setLongLabel(context.getString(R.string.shortcut_start_workout_long))
                .setIcon(IconCompat.createWithResource(context, R.drawable.weight_icon))
                .setIntent(Intent(Intent.ACTION_VIEW, "elevatefitness://workout/${it.programId}".toUri()).apply {
                    setPackage(context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                })
                .build()
        }?.toMutableList() ?: mutableListOf()

        if (homeState.currentProgram != null) {
            val program = homeState.programs?.getOrNull(homeState.currentProgram!!)
            if (program != null) {
                shortcuts.add(0,
                    ShortcutInfoCompat.Builder(context, "start_workout_dyn_${program.programId}")
                        .setShortLabel(getProgramDisplayName(program.name, context))
                        .setIcon(IconCompat.createWithResource(context, R.drawable.icon_event_upcoming))
                        .setIntent(
                            Intent(
                                Intent.ACTION_VIEW,
                                "elevatefitness://workout/${program.programId}".toUri()
                            ).apply {
                                setPackage(context.packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            })
                        .build()
                )
            }
        }
        Log.d("Home", "shortcuts: $shortcuts")
        // Replaces existing dynamic shortcuts
        if (shortcuts.isNotEmpty()) {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts.subList(
                0, min(
                    shortcuts.size,
                    ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
                )
            ))
        }
    }
    Scaffold(
        // use a primary container to put emphasis on upcoming workout in elevated card
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        floatingActionButton = {
            if (homeState.currentPlan == null) {
                MediumFloatingActionButton(
                    onClick = {
                        navigator.navigate(
                            AddWorkoutPlanDestination(openDialogNow = true)
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(
                            FloatingActionButtonDefaults.MediumIconSize
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        if (homeState.currentPlan == null) {
            EmptyScreenInfo(
                icon = Icons.Default.Home,
                iconDescriptionRes = R.string.home,
                titleRes = R.string.empty_home,
                subtitleRes = R.string.empty_home_subtitle
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                GeneratePlanButton(navigator)
            }
        } else if (homeState.programs?.isEmpty() == true) {
            EmptyScreenInfo(
                icon = Icons.Outlined.Description,
                iconDescriptionRes = R.string.empty_home_program,
                titleRes = R.string.empty_no_programs,
                subtitleRes = R.string.empty_home_program
            ) {
                Button(onClick = {
                    navigator.navigate(
                        AddProgramDestination(
                            planId = homeState.currentPlan!!,
                            openDialogNow = true
                        )
                    )
                }, modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp)
                ) {
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
        } else if (homeState.programs?.isNotEmpty() == true
            && homeState.currentProgram != null
        ) {
            LazyColumn(
                contentPadding = innerPadding
            ) {
                var currentProgram = homeState.programs?.getOrNull(homeState.currentProgram!!)
                // the check below should not be necessary anymore, the bug was fixed elsewhere
                if (currentProgram == null) {
                    currentProgram = homeState.programs?.get(0)!!
                }
                val currentExercises =
                    homeState.exercisesAndInfo[currentProgram.programId]?.sortedBy {
                        it.orderInProgram
                    } ?: emptyList()
                // now that we got current program, roll homeState.programs so that currentProgram.orderInWorkoutPlan+1 is first
                val otherPrograms by derivedStateOf {
                    homeState.programs!!.minus(currentProgram).sortedBy {
                        (it.orderInWorkoutPlan - currentProgram.orderInWorkoutPlan).mod(homeState.programs!!.size)
                    }
                }
                item {
                    // Coming next
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.coming_next),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.header_to_content_padding)))
                    WorkoutCard(
                        program = currentProgram,
                        exercises = currentExercises,
                        cardShape = MaterialTheme.shapes.extraLarge,
                        cardElevation = 2.dp,
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
                        cardModifier = Modifier
                            .padding(horizontal = dimensionResource(R.dimen.screen_edge_padding))
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(
                                    SharedElementKey(
                                        "Workout",
                                        SharedElementType.Bounds,
                                        idLong = currentProgram.programId
                                    )
                                ),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    MotionScheme.expressive().slowSpatialSpec()
                                }
//                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                            ),
                        imageModifier = Modifier
                            .sharedBounds(
                                sharedContentState =
                                    rememberSharedContentState(
                                        SharedElementKey(
                                            "Workout",
                                            SharedElementType.Image,
                                            idLong = currentProgram.programId
                                        )
                                    ),
                                animatedVisibilityScope = animatedVisibilityScope,
                                clipInOverlayDuringTransition = OverlayClip(MaterialTheme.shapes.extraLarge),
                                boundsTransform = { _, _ ->
                                    MotionScheme.expressive().slowSpatialSpec()
                                }
                            ).graphicsLayer(
                                shape = MaterialTheme.shapes.extraLarge,
                                clip = true
                            ),
                        exerciseModifier = Modifier
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(
                                    SharedElementKey(
                                        "Workout",
                                        SharedElementType.Title,
                                        idLong = currentProgram.programId
                                    )
                                ),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    MotionScheme.expressive().slowSpatialSpec()
                                }
                            )
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.section_spacing)))
                }
                if (otherPrograms.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(id = R.string.other_programs),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.screen_edge_padding))
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.header_to_content_padding)))
                    }
                    itemsIndexed(items = otherPrograms, key = { _, it -> it.programId }) { index, program ->
                        val exs = homeState.exercisesAndInfo[program.programId]?.sortedBy {
                            it.orderInProgram
                        } ?: emptyList()

                        val pagerState = rememberPagerState(pageCount = { exs.size })

                        // TODO: use grouped card instead
                        // Softer corner radius for less emphasis
                        val cardShape = if (otherPrograms.size == 1) {
                            RoundedCornerShape(16.dp, 16.dp, 16.dp, 16.dp)
                        } else {
                            when (index) {
                                0 -> RoundedCornerShape(16.dp, 16.dp, 4.dp, 4.dp)
                                otherPrograms.size - 1 -> RoundedCornerShape(
                                    4.dp,
                                    4.dp,
                                    16.dp,
                                    16.dp
                                )

                                else -> RoundedCornerShape(4.dp)
                            }
                        }

                        Card(
                            shape = cardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(
                                        SharedElementKey(
                                            "Workout",
                                            SharedElementType.Bounds,
                                            idLong = program.programId
                                        )
                                    ),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    boundsTransform = { _, _ ->
                                        MotionScheme.expressive().slowSpatialSpec()
                                    }
                                )
                                .fillMaxWidth()
                                .clip(cardShape)
                                .combinedClickable(
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        navigator.navigate(
                                            AddProgramExerciseDestination(
                                                programName = program.name,
                                                programId = program.programId
                                            )
                                        )
                                    }
                                ) {
                                    navigator.navigate(
                                        WorkoutDestination(
                                            programId = program.programId,
                                            previewExercise = exs.getOrNull(0)
                                        )
                                    )
                                }
                        ) {
                            Column {
                                // Main content row
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp) // Slightly reduced padding
                                ) {
                                    // Exercise image pager
                                    if (exs.isNotEmpty()) {
                                        HorizontalPager(
                                            state = pagerState,
                                            userScrollEnabled = false,
                                            modifier = Modifier
                                                .sharedBounds(
                                                    sharedContentState = rememberSharedContentState(
                                                        SharedElementKey(
                                                            "Workout",
                                                            SharedElementType.Image,
                                                            idLong = program.programId
                                                        )
                                                    ),
                                                    animatedVisibilityScope = animatedVisibilityScope,
                                                    clipInOverlayDuringTransition = OverlayClip(
                                                        MaterialTheme.shapes.small
                                                    ),
                                                    boundsTransform = { _, _ ->
                                                        MotionScheme.expressive().slowSpatialSpec()
                                                    }
                                                )
                                                .size(80.dp) // Smaller image for less emphasis
                                                .clip(MaterialTheme.shapes.small)
                                        ) { page ->
                                            AsyncImage(
                                                model = exs[page].image,
                                                contentDescription = stringResource(id = R.string.exercise_image),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                colorFilter = ColorFilter.tint(
                                                    Color.Black.copy(alpha = 0.1f),
                                                    BlendMode.Darken
                                                ) // Subtle overlay for less prominence
                                            )
                                        }

                                        LaunchedEffect(homeState.animationTick) {
                                            if (!animatedVisibilityScope.transition.isRunning) {
                                                pagerState.animateScrollToPage(
                                                    (pagerState.currentPage + 1) % exs.size
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Program info column
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Text(
                                            text = getProgramDisplayName(program.name),
                                            style = MaterialTheme.typography.titleMedium, // Smaller title
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Exercise preview - show max 3 for cleaner look
                                        exs.take(3).forEach { exercise ->
                                            var exerciseName = exercise.name
                                            if (exercise.variation.isNotBlank()) {
                                                exerciseName += " (${exercise.variation})"
                                            }
                                            exerciseName += ": "

                                            var exInfo = "${exercise.reps.size}x${exercise.reps.min()}"
                                            if (exercise.reps.distinct().size > 1) {
                                                exInfo += "+"
                                            }
                                            exInfo += " • ${exercise.rest.min()}s"
                                            if (exercise.rest.distinct().size > 1) {
                                                exInfo += "+"
                                            }

                                            val modifier = if (exercise.orderInProgram == pagerState.currentPage) {
                                                Modifier.sharedBounds(
                                                    sharedContentState = rememberSharedContentState(
                                                        SharedElementKey(
                                                            "Workout",
                                                            SharedElementType.Title,
                                                            idLong = program.programId
                                                        )
                                                    ),
                                                    animatedVisibilityScope = animatedVisibilityScope,
                                                    boundsTransform = { _, _ ->
                                                        MotionScheme.expressive().slowSpatialSpec()
                                                    }
                                                )
                                            } else Modifier

                                            Text(
                                                text = exerciseName + exInfo,
                                                modifier = modifier,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = Ellipsis
                                            )
                                        }

                                        if (exs.size > 3) {
                                            Text(
                                                text = stringResource(
                                                    R.string.i_more_exercises,
                                                    exs.size - 2
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = Ellipsis
                                            )
                                        }
                                    }

                                    // Action buttons - more compact
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                navigator.navigate(
                                                    AddProgramExerciseDestination(
                                                        programName = program.name,
                                                        programId = program.programId
                                                    )
                                                )
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Edit,
                                                contentDescription = stringResource(R.string.edit_program_icon),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        if (exs.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    navigator.navigate(
                                                        WorkoutDestination(
                                                            programId = program.programId,
                                                            previewExercise = exs[pagerState.currentPage],
                                                            quickStart = true
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.size(36.dp) // Smaller buttons
                                            ) {
                                                Icon(
                                                    Icons.Default.RocketLaunch,
                                                    contentDescription = stringResource(R.string.quick_start_icon),
                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Consistent spacing
                        if (index != otherPrograms.size - 1) {
                            Spacer(modifier = Modifier.height(2.dp)) // Minimal gap for cohesion
                        }
                    }
                }
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                                    planId = homeState.currentPlan!!
                                )
                            )
                        }) {
                            Text(stringResource(R.string.change_programs))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}