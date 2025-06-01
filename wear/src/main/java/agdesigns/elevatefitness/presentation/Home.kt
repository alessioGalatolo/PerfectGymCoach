package agdesigns.elevatefitness.presentation

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.OpenOnPhoneDialog
import androidx.wear.compose.material3.OpenOnPhoneDialogDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.openOnPhoneDialogCurvedText
import androidx.wear.remote.interactions.RemoteActivityHelper
import kotlin.math.max

@Destination<RootGraph>(start = true)
@Composable
fun Home(
    viewModel: HomeViewModel = hiltViewModel()
){
    val homeState by viewModel.state.collectAsState()
    // FIXME: nullpointerexception
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AppScaffold(Modifier.background(Color.Transparent)) {
            val listState = rememberScalingLazyListState()

            val currentRestMillis: Long? by remember { derivedStateOf {
                if (homeState.restTimestamp != null)
                    max(0L,
                        homeState.restTimestamp?.toInstant()?.toEpochMilli()?.minus(
                            homeState.currentTime.toInstant().toEpochMilli()
                        ) ?: 0L
                    )
                else null
            }}
            // FIXME: if exercise is changed then rests change then progression is weird
            val restProgression by remember {
                derivedStateOf {
                    if (homeState.setsDone <= homeState.rest.size && homeState.rest.isNotEmpty()) {
                        // rest can be 0, avoid div by 0
                        if (homeState.rest[max(0, homeState.setsDone - 1)] > 0) {
                            currentRestMillis?.toFloat()
                                ?.div(homeState.rest[max(0, homeState.setsDone - 1)] * 1000)
                        } else
                            null
                    } else
                        null
                }
            }
            if (homeState.imageBitmap != null) {
                VignetteImage(homeState.imageBitmap!!.asImageBitmap())
            }
            if ((restProgression ?: 0f) > 0f) {
                // TODO: add vibration on counter finish
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    progress = { restProgression ?: 1f  }, // should not happen but sometimes is null
                    startAngle = CircularProgressIndicatorDefaults.StartAngle + 20f,  // allow for clock in up center
                    endAngle = CircularProgressIndicatorDefaults.StartAngle - 20f,
                    strokeWidth = CircularProgressIndicatorDefaults.smallStrokeWidth

                )
                Column(
                    Modifier.fillMaxSize().background(Color.Transparent),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val nextUp by remember {
                        derivedStateOf {
                            if (homeState.setsDone < homeState.rest.size)
                                homeState.exerciseName
                            else
                                homeState.nextExerciseName
                        }
                    }
                    if (nextUp.isNotBlank()) {
                        Text(
                            text = "Next up:",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = nextUp,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 16.dp
                            )
                        )
                    }
                    Text(
                        text = ((currentRestMillis ?: 0L) / 1000).toInt().toString(),
                        style = MaterialTheme.typography.displayLarge
                    )
                    TextButton({
                        viewModel.onEvent(HomeEvent.ResetRest)
                    }, Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Text(text = "Skip rest", textAlign = TextAlign.Center)
                    }
                }
            } else {
                if (homeState.exerciseName.isEmpty()) {
                    val context = LocalContext.current
                    val remoteActivityHelper = RemoteActivityHelper(context)
                    // TODO: this is nice but would be nicer if it opened next workout on phone
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        addCategory(Intent.CATEGORY_BROWSABLE)
                        data = "myapp://openapp".toUri()
                    }
                    var showConfirmation by remember { mutableStateOf(false) }
                    val text = OpenOnPhoneDialogDefaults.text
                    val style = OpenOnPhoneDialogDefaults.curvedTextStyle
                    OpenOnPhoneDialog(
                        visible = showConfirmation,
                        onDismissRequest = { showConfirmation = false },
                        curvedText = { openOnPhoneDialogCurvedText(text = text, style = style) }
                    )
                    Column (Modifier.fillMaxSize().background(Color.Transparent), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        // TODO: replace with swipe to refresh
                        IconButton({
                            viewModel.onEvent(HomeEvent.ForceSync)
                        }) {
                            Icon(Icons.Default.Sync, "Force sync")
                        }
                        Text(
                            text = "Please start a workout on your phone to begin",
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            val result = remoteActivityHelper.startRemoteActivity(intent)
                            Log.d("Home", "Result: $result")
                            showConfirmation = true
                        }) {
                            Icon(Icons.Default.PhoneAndroid, "Phone")
                            Text("Open phone app")
                        }
                    }
                } else {
                    ScreenScaffold(
                        modifier = Modifier.background(Color.Transparent),
                        scrollState = listState,
                        timeText = { TimeText() },
                        // Define custom spacing between [EdgeButton] and [ScalingLazyColumn].
                        edgeButtonSpacing = 4.dp,
                        edgeButton = {
                            EdgeButton(
                                onClick = {
                                    viewModel.onEvent(HomeEvent.CompleteSet)
                                },
                                modifier =
                                    // In case user starts scrolling from the EdgeButton.
                                    Modifier.scrollable(
                                        listState,
                                        orientation = Orientation.Vertical,
                                        reverseDirection = true,
                                        // An overscroll effect should be applied to the EdgeButton for proper
                                        // scrolling behavior.
                                        overscrollEffect = rememberOverscrollEffect()
                                    )
                            ) {
                                Icon(Icons.Filled.Done, contentDescription = "Done")
                            }
                        },
                    ) { contentPadding ->
                        ScalingLazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            // Bottom spacing is derived from [ScreenScaffold.edgeButtonSpacing].
                            contentPadding = contentPadding,
                        ) {
                            item {
                                Text(
                                    text = homeState.exerciseName + " (${homeState.setsDone+1}/${homeState.rest.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (homeState.note.isNotEmpty()) {
                                item {
                                    Text(
                                        text = homeState.note,
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                            item {
                                ListHeader {
                                    Text("Reps")
                                }
                            }
                            item {
                                val interactionSource1 = remember { MutableInteractionSource() }
                                val interactionSource3 = remember { MutableInteractionSource() }
                                Box(contentAlignment = Alignment.Center) {
                                    ButtonGroup(Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = { viewModel.onEvent(HomeEvent.ChangeReps(-1)) },
                                            modifier = Modifier.animateWidth(interactionSource1),
                                            interactionSource = interactionSource1
                                        ) {
                                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Remove,
                                                    contentDescription = "Decrease reps"
                                                )
                                            }
                                        }
                                        Box(
                                            Modifier.fillMaxWidth().weight(1.5f), contentAlignment = Alignment.Center
                                        ){
                                            Text(
                                                homeState.currentReps.toString(),
                                            )}
                                        Button(
                                            onClick = {
                                                viewModel.onEvent(HomeEvent.ChangeReps(1))
                                            },
                                            modifier = Modifier.animateWidth(interactionSource3),
                                            interactionSource = interactionSource3
                                        ) {
                                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = "Increase reps"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                ListHeader {
                                    Text("Weight")
                                }
                            }
                            item {
                                val interactionSource1 = remember { MutableInteractionSource() }
                                val interactionSource3 = remember { MutableInteractionSource() }
                                Box(contentAlignment = Alignment.Center) {
                                    ButtonGroup {
                                        Button(
                                            onClick = { viewModel.onEvent(HomeEvent.ChangeWeight(-1)) },
                                            modifier = Modifier.animateWidth(interactionSource1),
                                            interactionSource = interactionSource1
                                        ) {
                                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Remove,
                                                    contentDescription = "Decrease weight"
                                                )
                                            }
                                        }
                                        Box(
                                            Modifier.fillMaxWidth().weight(1.5f), contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                homeState.weight.toString(),
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.onEvent(HomeEvent.ChangeWeight(1))
                                            },
                                            modifier = Modifier.animateWidth(interactionSource3),
                                            interactionSource = interactionSource3
                                        ) {
                                            Box(
                                                Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = "Increase weight"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (homeState.equipment.lowercase().contains("barbell")) {
                                item {
                                    ListHeader {
                                        Text("Barbell")
                                    }
                                }
                                item {
                                    ArrowSwitcher(
                                        // FIXME: should get imperial system
                                        items = List(homeState.barbellNames.size) { i -> "${homeState.barbellNames[i]} (${homeState.barbellSizes[i]} ${if (homeState.imperialSystem) "lb" else "kg"})"},
                                        currentIndex = homeState.tareIndex,
                                        onIndexChanged = { index ->
                                            viewModel.onEvent(HomeEvent.ChangeTare(index))
                                        }
                                    )
                                }
                            }
                            if (homeState.nextExerciseName.isNotBlank()) {
                                item {
                                    ListHeader {
                                        Text(
                                            "Next exercise: ",
                                            style = MaterialTheme.typography.labelMedium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                item {
                                    Text(
                                        homeState.nextExerciseName,
                                        style = MaterialTheme.typography.titleLarge,
                                        textAlign = TextAlign.Center
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


// Credits: Horologist library
@Composable
fun VignetteImage(
    imageBitmap: ImageBitmap,
    modifier: Modifier = Modifier,
    color: Color = Color.Transparent,
    background: Color = MaterialTheme.colorScheme.background,
) {
    // Image with radial gradient
    val animatedBackgroundColor = animateColorAsState(
        targetValue = color,
        animationSpec = tween(450, 0, LinearEasing),
        label = "ColorBackground",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                // pre-compute your brush or shader once per size change
                val brush = Brush.radialGradient(
                    colors = listOf(
                        animatedBackgroundColor.value.copy(alpha = 0.4f),
                        background,
                    ),
                    center = size.center,
                    radius = size.minDimension / 2
                )
                onDrawWithContent {
                    drawContent()                // 1) draw children (your Image)
                    drawRect(brush = brush)     // 2) overlay the radial gradient
                }
            },
    ) {
        Image(
            imageBitmap,
            contentDescription = "Image",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ArrowSwitcher(
//    modifier: Modifier = Modifier,
    items: List<String>,
    onIndexChanged: (Int) -> Unit,
    currentIndex: Int
) {
    val currentItem = items[currentIndex]

    Box (contentAlignment = Alignment.Center) {
        ButtonGroup {
            IconButton(onClick = {
                onIndexChanged((currentIndex - 1 + items.size) % items.size)
            }
                ) {
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous"
                    )
                }
            }

            AnimatedContent(
                targetState = currentItem,
                transitionSpec = {
                    if (targetState != initialState) {
                        slideInHorizontally(initialOffsetX = { it }) + fadeIn() with
                                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                    } else {
                        EnterTransition.None with ExitTransition.None
                    }
                },
                label = "Text Switch"
            ) { text ->
                Box(
                    Modifier.fillMaxWidth().weight(1.5f), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            IconButton(onClick = {
                onIndexChanged((currentIndex + 1) % items.size)
            }) {
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next"
                    )
                }
            }
        }
    }
}