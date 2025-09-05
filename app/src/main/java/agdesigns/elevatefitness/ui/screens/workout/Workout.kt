package agdesigns.elevatefitness.ui.screens.workout

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.Top
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.WorkoutExercise
import com.agdesignes.shared.maybeKgToLb
import com.agdesignes.shared.maybeLbToKg
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import agdesigns.elevatefitness.data.db.entity.Theme
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndEquipment
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.service.NotificationListener
import agdesigns.elevatefitness.navigation.FadeTransition
import agdesigns.elevatefitness.navigation.WorkoutOnlyGraph
import agdesigns.elevatefitness.ui.common.CancelWorkoutDialog
import agdesigns.elevatefitness.ui.common.EmptyScreenInfo
import agdesigns.elevatefitness.ui.common.FullScreenImageCard
import agdesigns.elevatefitness.ui.common.HorizontalPagerIndicator
import agdesigns.elevatefitness.ui.common.InputOtherEquipmentDialog
import agdesigns.elevatefitness.ui.common.RequestNotificationAccessDialog
import agdesigns.elevatefitness.ui.screens.workout.components.EnterIntensityAndFinishDialog
import agdesigns.elevatefitness.ui.screens.workout.components.ExercisePage
import agdesigns.elevatefitness.ui.screens.workout.components.WorkoutBottomBar
import agdesigns.elevatefitness.utils.hasNotificationAccess
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.media.session.PlaybackState.STATE_PLAYING
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.net.toUri
import androidx.wear.remote.interactions.RemoteActivityHelper
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.parameters.DeepLink
import com.ramcosta.composedestinations.generated.destinations.ExercisesByMuscleDestination
import com.ramcosta.composedestinations.generated.destinations.WorkoutRecapDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.math.max

@Destination<WorkoutOnlyGraph>(
    start = true,
    style = FadeTransition::class,
    deepLinks = [
        DeepLink(uriPattern="elevatefitness://autoopenworkout"),
        DeepLink(uriPattern="elevatefitness://workout/{programId}"),
    ]
)
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun SharedTransitionScope.Workout(
    animatedVisibilityScope: AnimatedVisibilityScope,
    navigator: DestinationsNavigator,
    programId: Long = 0L,
    previewExercise: ProgramExerciseAndInfo? = null, // preview of the first exercise, used for transition
    quickStart: Boolean = false,
    resumeWorkout: Boolean = false,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val workoutState by viewModel.state.collectAsState()
    LaunchedEffect(workoutState.autoStartFailed) {
        if (workoutState.autoStartFailed)
            navigator.navigateUp()
    }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        // maybe open wear os app
        val openWearIntent = Intent(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            setData("elevatefitnesswear://startworkout".toUri())
        }
        val remoteActivityHelper = RemoteActivityHelper(context)
        remoteActivityHelper.startRemoteActivity(openWearIntent)
    }
    // when exiting the screen, stop wear workout
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onEvent(WorkoutEvent.InterruptWearWorkout)
        }
    }

    // for container transform animation
    val sharedStateCard = rememberSharedContentState("card_$programId")
    val sharedStateImg = rememberSharedContentState("img_$programId")
    val sharedStateTitle = rememberSharedContentState("exName_$programId")

    val snackbarHostState = remember { SnackbarHostState() }
    if (resumeWorkout)
        viewModel.onEvent(WorkoutEvent.ResumeWorkout)
    else if (programId == 0L)
        viewModel.onEvent(WorkoutEvent.AutoStartWorkout)
    else
        viewModel.onEvent(WorkoutEvent.InitWorkout(programId))

    val scope = rememberCoroutineScope()

    val startWorkout = rememberSaveable { mutableStateOf(quickStart) }

    // request to have notification access to show music playing
    var alreadyRequestedPermission by rememberSaveable { mutableStateOf(false) }

    var retrieveMediaJob: Job? by remember {
        mutableStateOf(null)
    }
    var session: MediaController? by remember { mutableStateOf(null) }
    var mediaTitle: String? by remember { mutableStateOf(null) }
    val artistNotAvailableString = stringResource(R.string.artist_not_available)
    var mediaArtist: String by remember { mutableStateOf(artistNotAvailableString) }
    var isPlaying: Boolean by remember { mutableStateOf(false) }
    var artworkBitmap: Bitmap? by remember { mutableStateOf(null) }
    // Show media card and ask user if they want it with actual content
    val shouldTeaseMediaAccess by remember { derivedStateOf {
        !workoutState.cantRequestNotificationAccess
                && !hasNotificationAccess(context)
                && !alreadyRequestedPermission
    } }
    val teaseMediaAccessPrompt = stringResource(R.string.tease_media_access_prompt)
    val teaseMediaAccessLearnMore = stringResource(R.string.tease_media_access_learn_more)
    val noMusicPlayingString = stringResource(R.string.no_music_playing)
    val noMusicPlayingInfo = stringResource(R.string.no_music_playing_info)
    LaunchedEffect(shouldTeaseMediaAccess) {
        if (mediaTitle == null && shouldTeaseMediaAccess) {
            mediaTitle = teaseMediaAccessPrompt
            mediaArtist = teaseMediaAccessLearnMore
        } else if (!shouldTeaseMediaAccess && mediaTitle == teaseMediaAccessPrompt) {
            // reset if we should not tease anymore (e.g., user says "do not ask again")
            // TODO: check that if user has not granted permission and says "do not ask again", music card disappears
            mediaTitle = null
            mediaArtist = artistNotAvailableString
        }
    }
    DisposableEffect(context) {
        // FIXME: this looks like it belongs in a viewModel but the problem is the context
        // TODO: perhaps move mediaTitle/Artist to viewModel and job to repository?
        retrieveMediaJob = scope.launch {
            while (true) {
                if (hasNotificationAccess(context) && session == null) {
                    val m = getSystemService(context, MediaSessionManager::class.java)!!
                    val component = ComponentName(context, NotificationListener::class.java)
                    session = m.getActiveSessions(component).filter {
                        it.metadata?.description?.title != null
                    }.getOrNull(0)
                    if (session != null) {
                        val callback = object : MediaController.Callback() {
                            override fun onPlaybackStateChanged(state: PlaybackState?) {
                                isPlaying = state?.state == STATE_PLAYING
                            }

                            override fun onMetadataChanged(metadata: MediaMetadata?) {
                                mediaTitle = metadata?.description?.title?.toString()
                                mediaArtist = metadata?.description?.subtitle?.toString() ?: artistNotAvailableString
                                val newBitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                                    ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                                if (newBitmap != null && !newBitmap.sameAs(artworkBitmap)) {
                                    artworkBitmap = newBitmap
                                }
                            }
                        }
                        session!!.registerCallback(callback)
                        mediaTitle = session!!.metadata!!.description.title.toString()
                        mediaArtist = session!!.metadata!!.description.subtitle.toString()
                        artworkBitmap = session!!.metadata!!.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                            ?: session!!.metadata!!.getBitmap(MediaMetadata.METADATA_KEY_ART)
                        isPlaying = session!!.playbackState?.state == STATE_PLAYING
                    } else {
                        mediaTitle = noMusicPlayingString
                        mediaArtist = noMusicPlayingInfo
                    }
                }
                delay(100)
            }
        }
        onDispose {
            retrieveMediaJob?.cancel()
        }
    }
    val onClose = {
        if (workoutState.startDate == null)
            // workout has not started, just go up
            navigator.navigateUp()
        else
            // ask confirmation
            viewModel.onEvent(WorkoutEvent.ToggleCancelWorkoutDialog)
        Unit
    }
    BackHandler(
        enabled = startWorkout.value || workoutState.startDate != null || resumeWorkout,
        onBack = onClose
    )
    LaunchedEffect(startWorkout.value) {
        if (startWorkout.value) {
            viewModel.onEvent(WorkoutEvent.StartWorkout)
            startWorkout.value = false
        }
    }

    LaunchedEffect (workoutState.shutDown){
        if (workoutState.shutDown) {
            navigator.navigateUp()
            navigator.navigate(
                WorkoutRecapDestination(workoutId = workoutState.workoutId)
            )
        }
    }

    RequestNotificationAccessDialog(
        dialogIsOpen = workoutState.requestNotificationAccessDialogOpen,
        toggleDialog = { viewModel.onEvent(WorkoutEvent.ToggleRequestNotificationAccessDialog) },
        openPermissionRequest = {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            context.startActivity(intent)
        },
        dontAskAgain = {
            viewModel.onEvent(WorkoutEvent.DontRequestNotificationAgain)
        }
    )
    CancelWorkoutDialog(
        dialogueIsOpen = workoutState.cancelWorkoutDialogOpen,
        toggleDialog = { viewModel.onEvent(WorkoutEvent.ToggleCancelWorkoutDialog) },
        cancelWorkout = { viewModel.onEvent(WorkoutEvent.CancelWorkout); navigator.navigateUp() },
        deleteData = { viewModel.onEvent(WorkoutEvent.DeleteCurrentRecords) },
        hasRecords = workoutState.hasRecordedExercise
    )
    InputOtherEquipmentDialog(
        dialogIsOpen = workoutState.otherEquipmentDialogOpen,
        toggleDialog = { viewModel.onEvent(WorkoutEvent.ToggleOtherEquipmentDialog) },
        weightUnit = if (workoutState.imperialSystem) stringResource(R.string.lb) else stringResource(
            R.string.kg
        ),
        updateTare = { tare ->
            viewModel.onEvent(
                WorkoutEvent.UpdateTare(
                    maybeLbToKg(
                        tare,
                        workoutState.imperialSystem
                    )
                )
            )
        }
    )

    val pagerState = rememberPagerState(
        initialPage = previewExercise?.orderInProgram ?: 0,
        pageCount = {
        if (workoutState.startDate != null)
            workoutState.workoutExercises.size+1
        else
            workoutState.workoutExercises.size
    })
    // communicate with viewModel so that it know current exercise
    // FIXME: wouldn't it be easier to use currentExercise?
    LaunchedEffect(pagerState.currentPage, workoutState.workoutExercises) {
        viewModel.onEvent(WorkoutEvent.UpdateCurrentPage(pagerState.currentPage))
    }
    val currentExercise: WorkoutExercise? by remember {
        derivedStateOf {
            if (pagerState.currentPage < workoutState.workoutExercises.size) {
                workoutState.workoutExercises[pagerState.currentPage]
            } else {
                null
            }
        }
    }

    LaunchedEffect(workoutState.autoAdvancePage) {
        if (workoutState.autoAdvancePage != null) {
            pagerState.animateScrollToPage(workoutState.autoAdvancePage!!)
            viewModel.onEvent(WorkoutEvent.ResetAutoAdvancePage)
        }
    }
    val workoutTimeMillis by remember {
        derivedStateOf {
            workoutState.startDate?.toInstant()?.toEpochMilli()?.let {
                workoutState.currentTime.toInstant().toEpochMilli() - it
            } ?: 0L
        }
    }
    val timer = {" " + if (workoutTimeMillis > 0L) DateUtils.formatElapsedTime(workoutTimeMillis / 1000) else "" }

    val variation = if ((currentExercise?.variation ?: "").isNotBlank()) " (${currentExercise?.variation})" else ""
    // title for top app bar, do not share bounds for animation
    val titleTopBar = @Composable { Text(
        currentExercise?.name?.plus(variation) ?: stringResource(R.string.end_of_workout),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    ) }
    // title below image, share bounds for animation
    val title = @Composable { Text(
        currentExercise?.name?.plus(variation) ?: stringResource(R.string.end_of_workout),
        overflow = TextOverflow.Ellipsis,
        maxLines = 3,
        modifier = Modifier.sharedBounds(
            sharedStateTitle,
            animatedVisibilityScope,
        )
    ) }

    val currentExerciseRecord by remember { derivedStateOf {
        if (pagerState.currentPage < workoutState.workoutExercises.size)
            workoutState.allRecords[
                    workoutState.workoutExercises[pagerState.currentPage].extExerciseId
            ] ?: emptyList()
        else
            emptyList()
    }}

    // record being set right now for current exercise
    val ongoingRecord by remember { derivedStateOf {
        currentExerciseRecord.find {
            it.extWorkoutId == workoutState.workoutId && it.exerciseInWorkout == pagerState.currentPage
        }
    }}

    // records for current exercise minus ongoingRecord
    val recordsToDisplay by remember { derivedStateOf {
        if (ongoingRecord != null)
            currentExerciseRecord.minus(ongoingRecord!!).sortedByDescending { it.date }
        else
            currentExerciseRecord.sortedByDescending { it.date }
    }}

    val setsDone = remember { derivedStateOf{
        ongoingRecord?.reps?.size ?: 0
    }}
    LaunchedEffect(setsDone.value, ongoingRecord){
        // This is to fix the bug when user is resume a workout with added sets
        // (before) on resume, only the original number of sets would be shown (instead of also the added ones)
        if (setsDone.value > 0 && setsDone.value > (currentExercise?.reps?.size ?: Int.MAX_VALUE)) {
            for (i in setsDone.value-1 downTo currentExercise!!.reps.size) {
                viewModel.onEvent(WorkoutEvent.AddSetToExercise(pagerState.currentPage))
            }
        }
    }

    LaunchedEffect(setsDone.value){
        // update viewModel so that it can be transmitted to wear os
        viewModel.onEvent(WorkoutEvent.UpdateSetsDone(setsDone.value))
    }
    // once we change exercise or current set, update reps count for the upcoming set
    LaunchedEffect(currentExercise, setsDone){
        if (currentExercise != null && setsDone.value < currentExercise!!.reps.size) {
            viewModel.onEvent(
                WorkoutEvent.UpdateReps(
                    currentExercise!!.reps[setsDone.value].toString()
                )
            )
        }
    }

    // set predicted weight and tare for bottom bar
    LaunchedEffect(recordsToDisplay, ongoingRecord, setsDone){
        // heuristic: tare is taken from previous set if available, otherwise from previous record
        /*
         weight is taken in this order:
         0. If first set, take from last record
         1. If not first set, check last set weight:
         1a. If == to the same set from last record, take from last record
         2. Otherwise, check whether the weight also changed between sets in last record (e.g., pyramid)
         2a. If not, keep weight from previous set
         3. Otherwise, take last record increased/decreased by same amount as previous set
         */
        // FIXME: this heuristic is not transparent to the user that might question what these
        // "random" changes in weight are. Perhaps it is better to always have ongoingRecord and
        // have the rest as a suggestion

        // this is the record of the last record before current workout
        val lastOldRecord = recordsToDisplay.firstOrNull()

        var weightCandidate: Float? = null
        var oldRecordWeightCurrentSet: Float? = null
        var oldRecordWeightPreviousSet: Float? = null
        var ongoingRecordWeightPreviousSet: Float? = null
        var tareCandidate: Float? = null
        // for the weight, try to copy from last old record
        if (lastOldRecord != null) {
            oldRecordWeightCurrentSet = lastOldRecord.weights.getOrNull(setsDone.value)
            oldRecordWeightPreviousSet = lastOldRecord.weights.getOrNull(setsDone.value-1)
            tareCandidate = lastOldRecord.tare
        }
        if (ongoingRecord != null) {
            ongoingRecordWeightPreviousSet = ongoingRecord!!.weights.getOrNull(setsDone.value-1)
            tareCandidate = ongoingRecord!!.tare
        }
        if (setsDone.value == 0) {
            weightCandidate = oldRecordWeightCurrentSet
        } else if (oldRecordWeightCurrentSet != null && oldRecordWeightPreviousSet == ongoingRecordWeightPreviousSet) {
            weightCandidate = oldRecordWeightCurrentSet
        } else if (oldRecordWeightCurrentSet != null && oldRecordWeightPreviousSet != oldRecordWeightCurrentSet) {
            val delta = oldRecordWeightPreviousSet?.let { ongoingRecordWeightPreviousSet?.minus(it) }
            weightCandidate = oldRecordWeightCurrentSet.plus(delta ?: 0f)
        } else {
            weightCandidate = ongoingRecordWeightPreviousSet
        }
        weightCandidate = weightCandidate?.let { maybeKgToLb(it, workoutState.imperialSystem) }

        viewModel.onEvent(WorkoutEvent.UpdateWeight(weightCandidate?.toString() ?: "0.0"))
        viewModel.onEvent(WorkoutEvent.UpdateTare(tareCandidate ?: 0f))
    }

    EnterIntensityAndFinishDialog(
        dialogIsOpen = workoutState.enterIntensityDialogOpen,
        lastIntensity = workoutState.lastWorkoutIntensity,
        dismissDialog = { viewModel.onEvent(WorkoutEvent.ToggleEnterIntensityDialog) },
        completeWorkout = { viewModel.onEvent(WorkoutEvent.FinishWorkout(it)) }
    )
    val completeWorkout: () -> Unit = {
        viewModel.onEvent(WorkoutEvent.ToggleEnterIntensityDialog)
    }

    val pagerPageCount by remember { derivedStateOf {
        if (workoutState.startDate != null)
            workoutState.workoutExercises.size+1
        else
            workoutState.workoutExercises.size
    }}

    var fabHeight by remember { mutableStateOf(0.dp) }

    // if bright image (i.e., white), change status bar icons to dark
    val brightImage = remember { mutableStateOf(false) }
    val imageWidth = with (LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val imageHeight = imageWidth/3*2
    val systemTheme = isSystemInDarkTheme()
    val useDarkTheme by remember { derivedStateOf {
        when (workoutState.userTheme) {
            Theme.SYSTEM -> systemTheme
            Theme.LIGHT -> false
            Theme.DARK -> true
        }
    }}
    // We are animating from the previous screen but do not have the data yet. Use placeholder screen
    // to finish the animation, then show the actual screen. We need to record whether the animation
    // has finished once as, depending on how do user is interacting, it may keep going
    var animationHasFinished by remember { mutableStateOf(false) }
    animationHasFinished = animationHasFinished || !animatedVisibilityScope.transition.isRunning
    if (workoutState.workoutExercises.isNotEmpty() && animationHasFinished) {
        val currentImageId by remember { derivedStateOf {
            if (pagerState.currentPage == workoutState.workoutExercises.size)
                R.drawable.finish_workout
            else currentExercise!!.image
        }}
        FullScreenImageCard(
            animatedVisibilityScope = animatedVisibilityScope,
            sharedState = sharedStateCard,
            snackbarHostState = snackbarHostState,
            topAppBarNavigationIcon = { appBarShown ->
                val needsDarkColor = (brightImage.value && !appBarShown) || (appBarShown && !useDarkTheme)
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close_icon),
                        tint = if (needsDarkColor)
                            Color.Black
                        else
                            Color.White
                    )
                }
            },
            topAppBarActions = { appBarShown ->
                Row(verticalAlignment = CenterVertically) {
                    val needsDarkColor = (brightImage.value && !appBarShown) ||
                            (appBarShown && !useDarkTheme)
                    Text(
                        timer(),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (needsDarkColor)
                            Color.Black
                        else
                            Color.White
                    )
                    if (workoutState.startDate != null) {
                        Spacer(Modifier.width(4.dp))
                        FilledIconButton(onClick = {
                            completeWorkout()
                        }, shapes = IconButtonDefaults.shapes(
                            MaterialTheme.shapes.small,
                            MaterialTheme.shapes.extraLarge
                        )) {
                            Icon(
                                Icons.Default.DoneAll,
                                stringResource(R.string.finish),
//                                tint = if (needsDarkColor)
//                                    Color.Black
//                                else
//                                    Color.White
                            )
                        }
                    }
                }
            },
            title = titleTopBar,
            image = {
                val roundedCornersShape = CardDefaults.shape
                Box(Modifier
                    .wrapContentHeight(Top), contentAlignment = TopCenter) { // TODO: add swipe
                    AsyncImage(
                        ImageRequest.Builder(context)
                            .allowHardware(false) // pixel access is not supported on Config#HARDWARE bitmaps
                            .data(currentImageId)
                            .crossfade(true)
                            .listener { _, result ->
                                val image = result.image.toBitmap()
                                Palette.from(image).maximumColorCount(3)
                                    .clearFilters()
                                    .setRegion(0, 0, image.width,50)
                                    .generate {
                                        brightImage.value = (ColorUtils.calculateLuminance(it?.getDominantColor(Color.Black.toArgb()) ?: 0)) > 0.5
                                    }
                            }
                            .build(),
                        stringResource(R.string.exercise_image),
                        Modifier
                            .fillMaxWidth()
                            .height(imageHeight)
                            .sharedBounds(
                                sharedStateImg,
                                animatedVisibilityScope,
                                clipInOverlayDuringTransition = OverlayClip(roundedCornersShape)
                            )
                            .graphicsLayer(
                                shape = roundedCornersShape,
                                clip = true
                            ),
                        contentScale = ContentScale.Crop
                    )

                    HorizontalPagerIndicator(
                        pagerState = pagerState,
                        pageCount = pagerPageCount,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    )
                }
            },
            imageHeight = imageHeight,
            brightImage = brightImage.value,
            darkTheme = useDarkTheme,
            content = { bottomPadding ->
                val restCounterMillis: Long? = if (workoutState.restTimestamp != null && currentExercise != null)
                    max(0L,
                        workoutState.restTimestamp?.toInstant()?.toEpochMilli()?.minus(
                            workoutState.currentTime.toInstant().toEpochMilli()
                        ) ?: 0L
                    )
                else null
                // restCounterMillis is updated infrequently, animate between value to have smooth progress
                val progressAnim = remember { Animatable(1f) }

                val targetProgress = restCounterMillis?.let {
                    it.toFloat() / (workoutState.currentExerciseRest?.times(1000L) ?: restCounterMillis).toFloat()
                } ?: 1f

                LaunchedEffect(targetProgress) {
                    progressAnim.animateTo(
                        targetValue = targetProgress,
                        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                    )
                }


                ExercisePage(
                    bottomPadding = bottomPadding,
                    pagerState = pagerState,
                    workoutTimeMillis = workoutTimeMillis,
                    workoutExercises = workoutState.workoutExercises,
                    workoutId = workoutState.workoutId,
                    navigator = navigator,
                    setsDone = setsDone,
                    ongoingRecord = ongoingRecord,
                    currentExerciseRecords = recordsToDisplay,
                    exerciseDescription = currentExercise?.description
                        ?: stringResource(R.string.exercise_description_not_available),
                    fabHeight = fabHeight,
                    title = title,
                    addSet = { viewModel.onEvent(WorkoutEvent.AddSetToExercise(pagerState.currentPage)) },
                    restCounterMillis = restCounterMillis,
                    restCounterProgress = progressAnim.value,
                    updateExerciseProbability = { probability ->
                        scope.launch {
                            // if already snackbarring, dismiss it before a new one.
                            snackbarHostState.currentSnackbarData?.dismiss()
                            if (probability > 0)
                                snackbarHostState.showSnackbar(context.getString(R.string.increasing_exercise_probability))
                            else
                                snackbarHostState.showSnackbar(context.getString(R.string.decreasing_exercise_probability))
                        }
                        viewModel.onEvent(
                            WorkoutEvent.UpdateExerciseProbability(
                                pagerState.currentPage,
                                probability
                            )
                        )
                    },
                    updateBottomBar = { rep, weight ->
                        if (rep != null)
                            viewModel.onEvent(WorkoutEvent.UpdateReps(rep.toString()))
                        else
                        // this should never happen. Log it
                            Log.e("Workout", "updateBottomBar called with null rep")
                        if (weight != null)
                            viewModel.onEvent(WorkoutEvent.UpdateWeight(weight.toString()))
                    },
                    updateValues = { a, b, c, d ->
                        viewModel.onEvent(
                            WorkoutEvent.EditSetRecord(
                                a,
                                b,
                                c,
                                d
                            )
                        )
                    },
                    updateTare = { tare -> viewModel.onEvent(WorkoutEvent.UpdateTare(tare)) },
                    useImperialSystem = workoutState.imperialSystem,
                    tare = workoutState.tare,
                    toggleOtherEquipment = { viewModel.onEvent(WorkoutEvent.ToggleOtherEquipmentDialog) },
                    changeExercise = { exerciseInWorkout, originalSize ->
                        scope.launch {
                            viewModel.onEvent(
                                WorkoutEvent.ReplaceExercise(
                                    exerciseInWorkout,
                                    originalSize
                                )
                            )
                        }
                    },
                    removeExercise = { viewModel.onEvent(WorkoutEvent.RemoveExercise(it)) }
                )
            },
            floatingActionButton = {
                if (mediaTitle != null) {
                    val visibleFabHeight = 16.dp + // top inner padding
                            16.dp + // bottom inner padding
                            48.dp + // album art size
                            16.dp // card bottom padding
                    var dismissed by remember { mutableStateOf(false) }
                    LaunchedEffect(dismissed, pagerState.isScrollInProgress) {
                        fabHeight = if (dismissed || pagerState.isScrollInProgress) 0.dp else visibleFabHeight
                    }
                    AnimatedVisibility(
                        visible = !pagerState.isScrollInProgress && !dismissed,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        SwipeToDismissBox(
                            state = rememberSwipeToDismissBoxState(),
                            onDismiss = {
                                dismissed = true
                            },
                            backgroundContent = {}
                        ) {
                            ElevatedCard(
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.inverseSurface
                                ),
                                modifier = Modifier
                                    .padding(start = 32.dp)
                                    .clickable {  // weird padding as it pretends to be a fab
                                        if (shouldTeaseMediaAccess) {
                                            viewModel.onEvent(WorkoutEvent.ToggleRequestNotificationAccessDialog)
                                        } else {
                                            // TODO: This has fixed the crash when clicking on the card
                                            //  right after granting permission but results in clicks
                                            //  that go nowhere...
                                            val packageName = session?.packageName
                                            if (packageName != null) {
                                                val intent =
                                                    context.packageManager.getLaunchIntentForPackage(
                                                        packageName
                                                    )
                                                if (intent != null) {
                                                    context.startActivity(intent)
                                                }
                                            }
                                        }
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (artworkBitmap != null) {
                                        AsyncImage(
                                            artworkBitmap,
                                            stringResource(R.string.song_artwork),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .matchParentSize()
                                                .blur(16.dp)
                                        )
                                        // Dimming scrim (dark overlay)
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(Color.Black.copy(alpha = 0.3f))
                                        )
                                    }
                                    Column (Modifier.padding(16.dp)) {
                                        Row(
                                            verticalAlignment = CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                        ) {
                                            if (artworkBitmap != null) {
                                                AsyncImage(
                                                    artworkBitmap, stringResource(R.string.song_artwork),
                                                    Modifier
                                                        .size(48.dp)
                                                        .clip(
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.MusicNote,
                                                    stringResource(R.string.no_song_artwork_available),
                                                    Modifier.size(48.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    mediaTitle!!,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                                Text(
                                                    mediaArtist,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            // if we are just teasing, gain space by removing buttons
                                            if (!shouldTeaseMediaAccess) {
                                                Spacer(Modifier.width(8.dp))
                                                FilledIconToggleButton(
                                                    checked = isPlaying,
                                                    onCheckedChange = {
                                                        if (session != null) {
                                                            if (session!!.playbackState?.state == STATE_PLAYING)
                                                                session!!.transportControls.pause()
                                                            else
                                                                session!!.transportControls.play()
                                                        }
                                                    },
                                                    shapes = IconButtonDefaults.toggleableShapes(),
                                                    modifier = Modifier.size(IconButtonDefaults.smallContainerSize(
                                                        IconButtonDefaults.IconButtonWidthOption.Wide))
                                                ) {
                                                    if (isPlaying) {
                                                        Icon(Icons.Default.Pause,
                                                            stringResource(R.string.pause_icon)
                                                        )
                                                    } else {
                                                        Icon(Icons.Default.PlayArrow,
                                                            stringResource(R.string.play_icon)
                                                        )
                                                    }
                                                }
                                                FilledTonalIconButton(
                                                    shapes = IconButtonDefaults.shapes(),
                                                    onClick = {
                                                        if (session != null) {
                                                            session!!.transportControls.skipToNext()
                                                        }
                                                    }
                                                ) {
                                                    Icon(Icons.Default.SkipNext,
                                                        stringResource(R.string.skipnext_icon_track)
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
            }
        ) { padding ->
            // FIXME: when becoming invisible, causes bottomPadding to become 0, thus removing the bottom
            // spacer in the exercises and a slight movement in the list
            AnimatedVisibility(
                visible = !pagerState.isScrollInProgress,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
            ) {
                WorkoutBottomBar(
                    contentPadding = padding,
                    workoutStarted = workoutState.startDate != null,
                    startWorkout = { viewModel.onEvent(WorkoutEvent.StartWorkout) },
                    currentExercise = currentExercise,
                    completeWorkout = completeWorkout,
                    completeSet = {
                        if (!viewModel.onEvent(
                                WorkoutEvent.TryCompleteSet(
                                    pagerState.currentPage,
                                    currentExercise!!.rest[setsDone.value].toLong()
                                )
                            )
                        ) {
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(context.getString(R.string.please_enter_valid_numbers))
                            }
                        } else if ((currentExercise?.supersetExercise ?: 0L) != 0L) {
                            val superExercise =
                                workoutState.workoutExercises.find {
                                    it.extProgramExerciseId == currentExercise!!.supersetExercise
                                }
                            if (superExercise != null) {
                                if (workoutState.workoutExercises.indexOf(
                                        superExercise
                                    ) >
                                    workoutState.workoutExercises.indexOf(
                                        currentExercise
                                    )
                                ) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                } else {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            }
                        }
                    }, setsFinished = setsDone.value >= (currentExercise?.reps?.size ?: 0),
                    addSet = { viewModel.onEvent(WorkoutEvent.AddSetToExercise(pagerState.currentPage)) },
                    goToNextExercise = {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                pagerState.currentPage + 1
                            )
                        }
                    },
                    repsToDisplay = workoutState.repsBottomBar,
                    updateReps = { value -> viewModel.onEvent(WorkoutEvent.UpdateReps(value)) },
                    weightToDisplay = workoutState.weightBottomBar,
                    updateWeight = { value ->
                        viewModel.onEvent(
                            WorkoutEvent.UpdateWeight(
                                value
                            )
                        )
                    },
                    autoStepWeight = { newValue, equipment, decrement ->
                        viewModel.onEvent(
                            WorkoutEvent.AutoStepWeight(newValue, equipment, decrement)
                        )
                    }
                )
            }
        }
    } else if (previewExercise != null) {
        // placeholder mainly used for animation
        FullScreenImageCard(
            animatedVisibilityScope = animatedVisibilityScope,
            sharedState = sharedStateCard,
            snackbarHostState = snackbarHostState,
            topAppBarNavigationIcon = { appBarShown ->
                val needsDarkColor = (brightImage.value && !appBarShown) || (appBarShown && !useDarkTheme)
                IconButton(onClick = { /* just a placeholder, won't be clicked anyway */}) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close_icon),
                        tint = if (needsDarkColor) Color.Gray else Color.White
                    )
                }
            },
            topAppBarActions = {},
            title = { },
            image = {
                val roundedCornersShape = CardDefaults.shape
                Box(Modifier
                    .wrapContentHeight(Top), contentAlignment = TopCenter) {
                    AsyncImage(
                        ImageRequest.Builder(context)
                            .allowHardware(false)
                            .data(previewExercise.image)
                            .crossfade(true)
                            .listener { _, result ->
                                val image = result.image.toBitmap()
                                Palette.from(image).maximumColorCount(3)
                                    .clearFilters()
                                    .setRegion(0, 0, image.width, 50)
                                    .generate {
                                        brightImage.value = (ColorUtils.calculateLuminance(it?.getDominantColor(Color.Black.toArgb()) ?: 0)) > 0.5
                                    }
                            }
                            .build(),
                        stringResource(R.string.exercise_image),
                        Modifier
                            .fillMaxWidth()
                            .height(imageHeight)
                            .sharedBounds(
                                sharedStateImg,
                                animatedVisibilityScope,
                                clipInOverlayDuringTransition = OverlayClip(roundedCornersShape)
                            )
                            .graphicsLayer(
                                shape = roundedCornersShape,
                                clip = true // <- this ensures clipping is applied during transition
                            ),
                        contentScale = ContentScale.Crop
                    )
                }
            },
            imageHeight = imageHeight,
            brightImage = brightImage.value,
            darkTheme = useDarkTheme,
            content = { bottomPadding ->
                val exampleRecord = ExerciseRecordAndEquipment(
                    recordId = 0L,
                    extExerciseId = 0L,
                    extWorkoutId = 0L,
                    exerciseInWorkout = previewExercise.orderInProgram,
                    date = ZonedDateTime.now(),
                    reps = previewExercise.reps,
                    weights = previewExercise.reps.map { 0f },
                    tare = 0f,
                    variation = previewExercise.variation,
                    variationResKey = previewExercise.variationResKey,
                    rest = previewExercise.rest,
                    equipment = previewExercise.equipment
                )
                val exampleExercise = WorkoutExercise(
                    workoutExerciseId = 0L,
                    extWorkoutId = 0L,
                    extProgramExerciseId = 0L,
                    extExerciseId = 0L,
                    name = previewExercise.name,
                    nameResKey = previewExercise.nameResKey,
                    image = previewExercise.image,
                    imageResKey = previewExercise.imageResKey,
                    description = previewExercise.description,
                    descriptionResKey = previewExercise.descriptionResKey,
                    equipment = previewExercise.equipment,
                    orderInProgram = previewExercise.orderInProgram,
                    reps = previewExercise.reps,
                    rest = previewExercise.rest,
                    note = previewExercise.note,
                    variation = previewExercise.variation,
                    variationResKey = previewExercise.variationResKey,
                    supersetExercise = previewExercise.supersetExercise
                )
                val workoutExercisesExample = listOf(
                    exampleExercise,
                    exampleExercise
                )
                ExercisePage(
                    bottomPadding = bottomPadding,
                    pagerState = rememberPagerState(pageCount = { 2 }),
                    workoutTimeMillis = 0L,
                    workoutExercises = workoutExercisesExample,
                    workoutId = 0L,
                    navigator = navigator,
                    setsDone = setsDone,
                    ongoingRecord = exampleRecord,
                    currentExerciseRecords = emptyList(),
                    exerciseDescription = "",
                    fabHeight = 0.dp,
                    title = {
                        Text(
                            previewExercise.name, modifier = Modifier.sharedBounds(
                                sharedStateTitle,
                                animatedVisibilityScope,
                            )
                        )
                    },
                    addSet = { },
                    updateBottomBar = { _, _ -> },
                    updateValues = { _, _, _, _ -> },
                    updateTare = { },
                    useImperialSystem = false,
                    tare = 0f,
                    toggleOtherEquipment = { },
                    changeExercise = { _, _ -> },
                    removeExercise = { },
                    restCounterMillis = null,
                    restCounterProgress = null,
                    updateExerciseProbability = { _ -> }
                )
            },
            floatingActionButton = {},
            bottomBar = { _ -> }
        )
    } else if (workoutState.workoutId != 0L){
        // program is empty, prompt to add an exercise
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { navigator.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.go_back_icon)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                Column(
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {  // FIXME: not really happy about the double FABs
                    SmallFloatingActionButton(onClick = {
                        navigator.navigate(
                            ExercisesByMuscleDestination(
                                programName = context.getString(R.string.current_and_future_workouts),  // FIXME: all workouts?
                                workoutId = workoutState.workoutId,
                                programId = programId
                            )
                        )
                    }, Modifier.padding(bottom = 24.dp),
                        containerColor = MaterialTheme.colorScheme.secondary) {
                        Icon(Icons.Default.Edit,
                            stringResource(R.string.add_an_exercise_to_current_and_future_workouts_of_this_program)
                        )
                    }
                    LargeFloatingActionButton(onClick = {
                        navigator.navigate(
                            ExercisesByMuscleDestination(
                                programName = context.getString(R.string.current_workout),
                                workoutId = workoutState.workoutId,
                            )
                        )
                    }) {
                        Icon(
                            Icons.Default.Add,
                            stringResource(R.string.add_an_exercise_to_current_workout),
                            Modifier.size(FloatingActionButtonDefaults.LargeIconSize)
                        )
                    }
                }
            }) { innerPadding ->
            EmptyScreenInfo(
                Icons.Default.FitnessCenter,
                R.string.empty_exercises,
                R.string.empty_exercises,
                R.string.workout_empty_exercises,
                modifier = Modifier.padding(innerPadding)
            ) {
                Text(
                    stringResource(R.string.note_empty_workout),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(40.dp)  // big padding to avoid being under fab
                )
            }
        }
    }
}
