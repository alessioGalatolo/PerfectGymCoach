package agdesigns.elevatefitness.presentation.screens.home

import com.agdesignes.shared.R as sharedR
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.datastore.ShownRationaleStatus
import agdesigns.elevatefitness.presentation.screens.home.components.PermissionRequiredScreen
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.OpenOnPhoneDialog
import androidx.wear.compose.material3.OpenOnPhoneDialogDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.openOnPhoneDialogCurvedText
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.ramcosta.composedestinations.generated.destinations.WorkoutDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalPermissionsApi::class)
@Destination<RootGraph>(start = true)
@Composable
fun Home(
    navigator: DestinationsNavigator,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val homeState by viewModel.state.collectAsState()
    val activeWorkout by viewModel.activeWorkout.collectAsState(false)

    LaunchedEffect(activeWorkout) {
        if (activeWorkout) {
            navigator.navigate(WorkoutDestination())
        }
    }

    // On T and upwards, POST_NOTIFICATIONS must be requested at runtime.
    val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else {
        // Below T, POST_NOTIFICATIONS does not need to be requested at runtime but must still be
        // specified in the Manifest. Therefore, permissionState is created such that it is already
        // in the granted state.
        object : PermissionState {
            override val permission = "no_runtime_permission_required"
            override val status = PermissionStatus.Granted
            override fun launchPermissionRequest() {}
        }
    }
    val context = LocalContext.current

    if (permissionState.status == PermissionStatus.Granted) {
        LaunchedEffect(Unit) {
            // Reset the status of having shown permission rationale.
            viewModel.permissionStateDataStore.setHasPreviouslyShownRationale(ShownRationaleStatus.UNKNOWN)
        }
    } else if (permissionState.status is PermissionStatus.Denied) {
        val denied = permissionState.status as PermissionStatus.Denied
        val hasPreviouslyShown by viewModel.permissionStateDataStore
            .hasPreviouslyShownRationaleFlow
            .collectAsStateWithLifecycle(initialValue = ShownRationaleStatus.UNKNOWN)

        if (denied.shouldShowRationale) {
            LaunchedEffect(Unit) {
                viewModel.permissionStateDataStore.setHasPreviouslyShownRationale(
                    ShownRationaleStatus.HAS_SHOWN
                )
            }
            // ShouldShowRationale returns true if:
            // - A request has previously been denied
            // - The app permission was set to denied in settings
            // At this point, the app stores the state that the rationale has been shown, as if
            // subsequently false is returned, this means that the permission cannot be requested
            // now, as opposed to the false seen from shouldShowRationale on first ever launch
            PermissionRequiredScreen(
                onPermissionClick = { permissionState.launchPermissionRequest() },
                buttonLabelResId = R.string.show_permission
            )
        } else if (hasPreviouslyShown == ShownRationaleStatus.HAS_SHOWN) {
            // Rationale has been shown previously, but the user has decided not to grant permission
            // Offer the user the option to go to permission settings.
            PermissionRequiredScreen(
                onPermissionClick = { launchPermissionsSettings(context) },
                buttonLabelResId = R.string.show_settings
            )
        } else if (hasPreviouslyShown == ShownRationaleStatus.HAS_NOT_SHOWN) {
            // First launch of permissions, show the permission request without any rationale.
            LaunchedEffect(Unit) {
                permissionState.launchPermissionRequest()
            }
        }
    }
    val remoteActivityHelper = RemoteActivityHelper(context)
    // TODO: this is nice but would be nicer if it opened next workout on phone
    // TODO: should check if app is installed, then only show one button
    val getAppIntent = Intent(Intent.ACTION_VIEW).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        setData("market://details?id=agdesigns.elevatefitness".toUri())
    }
    val openAppIntent = Intent(Intent.ACTION_VIEW).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        setData("elevatefitness://autoopenworkout".toUri())
    }
    var showConfirmation by remember { mutableStateOf(false) }
    val text = OpenOnPhoneDialogDefaults.text
    val style = OpenOnPhoneDialogDefaults.curvedTextStyle
    val listState = rememberScalingLazyListState()
    // FIXME: nullpointerexception
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AppScaffold(Modifier.background(Color.Transparent)) {
            ScreenScaffold (listState) {
                OpenOnPhoneDialog(
                    visible = showConfirmation,
                    onDismissRequest = { showConfirmation = false },
                    curvedText = { openOnPhoneDialogCurvedText(text = text, style = style) }
                )
                ScalingLazyColumn(state = listState) {
                    if (!homeState.workoutRunningFromPhone) {
                        item {
                            Spacer(Modifier.height(8.dp))
                        }
                        item {
                            Text(
                                text = stringResource(R.string.no_workout_detected),
                                textAlign = TextAlign.Center
                            )
                        }
                        item {
                            Spacer(Modifier.height(16.dp))
                        }
                        item {
                            Button(onClick = {
                                remoteActivityHelper.startRemoteActivity(openAppIntent)
                                showConfirmation = true
                            }) {
                                Icon(Icons.Default.PhoneAndroid,
                                    stringResource(R.string.phone_icon)
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.open_phone_app))
                            }
                        }
                        item {
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    remoteActivityHelper.startRemoteActivity(getAppIntent)
                                    showConfirmation = true
                                }
                            ) {
                                Text(stringResource(R.string.get_phone_app), maxLines = 1)
                            }
                        }
                    } else {
                        item {
                            Spacer(Modifier.height(8.dp))
                        }
                        item {
                            Text(
                                stringResource(R.string.workout_detected),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        item {
                            Spacer(Modifier.height(16.dp))
                        }
                        item {
                            Button(onClick = {
                                navigator.navigate(WorkoutDestination())
                            }) {
                                Icon(Icons.Default.FitnessCenter,
                                    stringResource(R.string.fitness_centre)
                                )
                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.switch_to_workout_view))
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
            contentDescription = stringResource(R.string.exercise_image),
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
                        contentDescription = stringResource(R.string.arrowback_icon_previous)
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
                    Modifier
                        .fillMaxWidth()
                        .weight(1.5f), contentAlignment = Alignment.Center
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
                        contentDescription = stringResource(R.string.arrowforward_icon_next)
                    )
                }
            }
        }
    }
}

private fun launchPermissionsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", context.packageName, null)
    intent.setData(uri)
    context.startActivity(intent)
}