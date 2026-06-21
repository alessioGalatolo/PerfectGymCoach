package agdesigns.elevatefitness.ui.screens.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndInfo
import agdesigns.elevatefitness.data.db.entity.getProgramDisplayName
import agdesigns.elevatefitness.ui.common.WorkoutCard
import agdesigns.elevatefitness.navigation.AddProgramExerciseDestination
import agdesigns.elevatefitness.navigation.AddProgramDestination
import agdesigns.elevatefitness.navigation.AddWorkoutPlanDestination
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import agdesigns.elevatefitness.navigation.CustomizePlanGenerationDestination
import agdesigns.elevatefitness.navigation.PrimaryActionContent
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import agdesigns.elevatefitness.navigation.HomeDestination
import agdesigns.elevatefitness.navigation.WorkoutDestination
import agdesigns.elevatefitness.ui.common.EmptyScreenInfo
import agdesigns.elevatefitness.ui.common.SharedElementKey
import agdesigns.elevatefitness.ui.common.SharedElementType
import agdesigns.elevatefitness.ui.common.lazyGroupedCard
import agdesigns.elevatefitness.ui.screens.plans.GeneratePlanButton
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.min


@Composable
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class
)
fun SharedTransitionScope.Home(
    animatedVisibilityScope: AnimatedVisibilityScope,
    navigator: DestinationsNavigator,
    refreshContentRequest: SharedFlow<Any>,
    changePrimaryActionContent: (PrimaryActionContent) -> Unit,
    fabHeight: () -> Dp,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()

    LaunchedEffect(refreshContentRequest) {
        refreshContentRequest.collect {
            if (it == HomeDestination) {
                // this refresh is for us
                lazyListState.animateScrollToItem(0)
            }
        }
    }

    // FABs are placed by parent, notify what to put
    LaunchedEffect(state.currentPlan) {
        changePrimaryActionContent (
            if (state.currentPlan == null)
                PrimaryActionContent(
                    icon = Icons.Default.Add,
                    labelId = R.string.home_fap_create_plan,
                    onClick = {
                        navigator.navigate(
                            AddWorkoutPlanDestination(openDialogNow = true)
                        )
                    }
                )
            else {
                PrimaryActionContent(
                    icon = Icons.Default.ContentPaste,
                    labelId = R.string.change_workout_plan,
                    showLabel = true,
                    onClick = {
                        navigator.navigate(
                            AddWorkoutPlanDestination(openDialogNow = false)
                        )
                    }
                )
            }
        )
    }

    var resumeWorkoutPossible by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current

    LaunchedEffect(state.currentWorkout){
        // done in order to avoid double dialog showing especially when slow transitioning into workout
        delay(500)
        resumeWorkoutPossible = state.currentWorkout != null
    }
    // add dynamic launcher shortcuts based on current plan
    LaunchedEffect(state.currentProgram, state.programs) {
        val shortcuts = state.programs?.filterIndexed {
            index, program -> index != state.currentProgram
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

        if (state.currentProgram != null) {
            val program = state.programs?.getOrNull(state.currentProgram!!)
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
    ) { innerPadding ->
        if (state.currentPlan == null) {
            EmptyScreenInfo(
                icon = Icons.Default.Home,
                iconDescriptionRes = R.string.home,
                titleRes = R.string.empty_home,
                subtitleRes = R.string.empty_home_subtitle
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                GeneratePlanButton(navigator)
            }
        } else if (state.programs?.isEmpty() == true) {
            EmptyScreenInfo(
                icon = Icons.Outlined.Description,
                iconDescriptionRes = R.string.empty_home_program,
                titleRes = R.string.empty_no_programs,
                subtitleRes = R.string.empty_home_program
            ) {
                Button(onClick = {
                    navigator.navigate(
                        AddProgramDestination(
                            planId = state.currentPlan!!,
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
                        )
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text(stringResource(R.string.change_workout_plan)) }
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else if (state.programs?.isNotEmpty() == true
            && state.currentProgram != null
        ) {
            var currentProgram = state.programs?.getOrNull(state.currentProgram!!)
            // the check below should not be necessary anymore, the bug was fixed elsewhere
            if (currentProgram == null) {
                currentProgram = state.programs?.get(0)!!
            }
            val currentExercises =
                state.exercisesAndInfo[currentProgram.programId]?.sortedBy {
                    it.orderInProgram
                } ?: emptyList()
            // now that we got current program, roll homeState.programs so that currentProgram.orderInWorkoutPlan+1 is first
            val otherPrograms = remember(state.programs, currentProgram) {
                state.programs!!.minus(currentProgram).sortedBy {
                    (it.orderInWorkoutPlan - currentProgram.orderInWorkoutPlan).mod(state.programs!!.size)
                }
            }
            val otherProgramsCardColors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
            val otherProgramsSharedContentState = otherPrograms.map { program ->
                rememberSharedContentState(
                    SharedElementKey(
                        "Workout",
                        SharedElementType.Bounds,
                        idLong = program.programId
                    )
                )
            }
            LazyColumn(
                state = lazyListState,
                contentPadding = innerPadding
            ) {
                // Resume workout if available
                item {
                    AnimatedVisibility(
                        resumeWorkoutPossible,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        ResumeWorkoutCard(
                            resumeExercises = state.resumedWorkoutExercises,
                            modifier = Modifier
                                .padding(horizontal = dimensionResource(R.dimen.screen_edge_padding))
                                .padding(top = 16.dp),
                            onClose = {
                                viewModel.onEvent(HomeEvent.ResetCurrentWorkout)
                                resumeWorkoutPossible = false
                            },
                            onResume = {
                                navigator.navigate(
                                    WorkoutDestination(
                                        programId = 0L,
                                        resumeWorkout = true
                                    )
                                )
                            }
                        )
                    }
                }
                // Plan change reminder
                item {
                    AnimatedVisibility(
                        visible = state.showPlanChangeReminder,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        PlanChangeReminder(
                            cycleCount = state.planCycleCount,
                            navigator = navigator,
                            onDismiss = { viewModel.onEvent(HomeEvent.DismissPlanChangeReminder) },
                            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.screen_edge_padding))
                        )
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
                            )
                            .graphicsLayer(
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
                            ),
                        trailingIcons = {
                            IconButton(onClick = {
                                navigator.navigate(
                                    AddProgramExerciseDestination(
                                        programName = currentProgram.name,
                                        programId = currentProgram.programId
                                    )
                                )
                            }) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    stringResource(R.string.edit_icon_program)
                                )
                            }
                        }
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
                    lazyGroupedCard(
                        colors = otherProgramsCardColors,
                        innerCardPadding = 0.dp
                    ) {
                        otherPrograms.zip(otherProgramsSharedContentState).forEach { (program, sharedState) ->
                            val exs = state.exercisesAndInfo[program.programId]?.sortedBy {
                                it.orderInProgram
                            } ?: emptyList()
                            subCard(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .sharedBounds(
                                        sharedContentState = sharedState,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        boundsTransform = { _, _ ->
                                            MotionScheme.expressive().slowSpatialSpec()
                                        }
                                    )
                                    .fillMaxWidth()
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
                                val pagerState = rememberPagerState(pageCount = { exs.size })
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

                                            LaunchedEffect(state.animationTick) {
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
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(fabHeight() + 16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanChangeReminder(
    cycleCount: Int,
    navigator: DestinationsNavigator,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.time_to_change_plan),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = if (cycleCount >= 8) {
                            stringResource(R.string.plan_change_reason_cycles, cycleCount)
                        } else {
                            stringResource(R.string.plan_change_reason_diminishing)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_icon),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        navigator.navigate(
                            AddWorkoutPlanDestination(openDialogNow = true)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onTertiaryContainer)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.create_plan))
                }

                Button(
                    onClick = {
                        navigator.navigate(
                            CustomizePlanGenerationDestination
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.generate_plan))
                }
            }
        }
    }
}

@Composable
fun ResumeWorkoutCard(
    resumeExercises: List<ExerciseRecordAndInfo>,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onResume: () -> Unit,
) {
    Card(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.resume_unfinished_workout),
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f),
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(
                onClose,
                modifier = Modifier.padding(8.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.close_icon)
                )
            }
        }
        Text(
            text = stringResource(R.string.resume_workout_info),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (resumeExercises.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Text(stringResource(R.string.exercises_completed), style = MaterialTheme.typography.titleMedium, modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp))
        }
        for (ex in resumeExercises) {
            Text(
                fontStyle = FontStyle.Italic,
                text = "${ex.name} • ${ex.reps.size} ${stringResource(R.string.sets)}",
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(start = 8.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onClose) {
                Text(text = stringResource(R.string.discard_workout))
            }
            Button(onClick = onResume) {
                Text(text = stringResource(R.string.resume))
            }
        }
    }
}