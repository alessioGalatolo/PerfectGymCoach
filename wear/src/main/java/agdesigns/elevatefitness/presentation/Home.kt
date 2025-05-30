package agdesigns.elevatefitness.presentation

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.OpenOnPhoneDialog
import androidx.wear.compose.material3.OpenOnPhoneDialogDefaults
import androidx.wear.compose.material3.OutlinedIconButton
import androidx.wear.compose.material3.PagerScaffoldDefaults
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
    // FIXME: nullpointerexception
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {


        // Declare just one [AppScaffold] per app such as in the activity.
        // [AppScaffold] allows static screen elements (i.e. [TimeText]) to remain visible
        // during in-app transitions such as swipe-to-dismiss.
        AppScaffold(Modifier.background(Color.Transparent)) {
            // Define the navigation hierarchy within the AppScaffold,
            // such as using SwipeDismissableNavHost.
            // For this sample, we will define a single screen inline.
            val listState = rememberScalingLazyListState()

            val currentRest by remember {
                derivedStateOf {
                    if (viewModel.state.value.restTimestampDec != null && viewModel.state.value.timeDec != null)
                        max(
                            0L,
                            viewModel.state.value.restTimestampDec!! - viewModel.state.value.timeDec!!
                        )
                    else
                        null
                }
            }
            val restProgression by remember {
                derivedStateOf {
                    if (viewModel.state.value.setsDone < viewModel.state.value.rest.size)
                        currentRest?.toFloat()
                            ?.div(viewModel.state.value.rest[viewModel.state.value.setsDone] * 10)
                    else
                        null
                }
            }
            if (viewModel.state.value.imageBitmap != null) {
                ColorBackground(viewModel.state.value.imageBitmap!!.asImageBitmap())
                Vignette(
                    vignettePosition = VignettePosition.TopAndBottom
                )
            }
            if (restProgression != null && restProgression!! > 0) {
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
                    // FIXME: text goes out of bounds
                    Text(
                        // FIXME: if more sets should say current exercise
                        text = "Next up:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                    val nextUp by remember {
                        derivedStateOf {
                            if (viewModel.state.value.setsDone <= viewModel.state.value.rest.size)
                                viewModel.state.value.exerciseName
                            else
                                viewModel.state.value.nextExerciseName
                        }
                    }
                    Text(
                        text = nextUp,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
                    )
                    Text(
                        text = (currentRest!! / 10).toInt().toString(),
                        style = MaterialTheme.typography.displayLarge
                    )
                    TextButton({
                        viewModel.onEvent(HomeEvent.ResetRest)
                    }, Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Text(text = "Skip rest", textAlign = TextAlign.Center)
                    }
                }
            } else {
                if (viewModel.state.value.exerciseName.isEmpty()) {
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
                            Text("Open Phone App")
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
                                    text = viewModel.state.value.exerciseName + " (${viewModel.state.value.setsDone+1}/${viewModel.state.value.rest.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (viewModel.state.value.note.isNotEmpty()) {
                                item {
                                    Text(
                                        text = viewModel.state.value.note,
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            item {
                                Text(
                                    "Reps",
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    FilledTonalIconButton({
                                        viewModel.onEvent(HomeEvent.ChangeReps(-1))
                                    }) {
                                        Icon(
                                            Icons.Filled.Remove,
                                            contentDescription = "Remove"
                                        )
                                    }
                                    Text(
                                        text = viewModel.state.value.currentReps.toString(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    FilledTonalIconButton({
                                        viewModel.onEvent(HomeEvent.ChangeReps(1))
                                    }) {
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = "Add"
                                        )
                                    }
                                }
                            }
                            item {
                                Text(
                                    "Weight",
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    FilledTonalIconButton({
                                        viewModel.onEvent(HomeEvent.ChangeWeight(-1))
                                    }) {
                                        Icon(
                                            Icons.Filled.Remove,
                                            contentDescription = "Remove"
                                        )
                                    }
                                    Text(
                                        text = viewModel.state.value.weight.toString(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    FilledTonalIconButton({
                                        viewModel.onEvent(HomeEvent.ChangeWeight(1))
                                    }) {
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = "Add"
                                        )
                                    }
                                }
                            }
                            // TODO: if barbell should show selection
                            item {
                                Text(
                                    "Next exercise: ",
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                            item {
                                Text(viewModel.state.value.nextExerciseName, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
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
fun ColorBackground(
    imageBitmap: ImageBitmap,
    modifier: Modifier = Modifier,
    color: Color = Color.Transparent,
    background: Color = MaterialTheme.colorScheme.background,
) {
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