package agdesigns.elevatefitness.presentation.screens.home

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.datastore.ShownRationaleStatus
import agdesigns.elevatefitness.presentation.screens.SCALING_LIST_PADDING_VALUES
import agdesigns.elevatefitness.presentation.screens.home.components.PermissionRequiredScreen
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhonelinkErase
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
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
import com.google.android.horologist.compose.ambient.AmbientAware
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Home(
    openWorkoutScreen: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val homeState by viewModel.state.collectAsState()
    val activeWorkout by viewModel.activeWorkout.collectAsState(false)

    // notifications permission
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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


    val remoteActivityHelper = RemoteActivityHelper(context)
    val getAppIntent = Intent(Intent.ACTION_VIEW).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        data = "market://details?id=agdesigns.elevatefitness".toUri()
    }
    val openAppIntent = Intent(Intent.ACTION_VIEW).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        data = "elevatefitness://autoopenworkout".toUri()
    }
    var showConfirmation by remember { mutableStateOf(false) }
    val text = OpenOnPhoneDialogDefaults.text
    val style = OpenOnPhoneDialogDefaults.curvedTextStyle
    val listState = rememberScalingLazyListState()

    AmbientAware { ambientState ->
        ScreenScaffold(listState) {
            var hasCheckedAlarmPermission by remember { mutableStateOf(false) }
            RequestAlarmPermission(
                listState,
                viewModel,
                setHasCheckPermission = {
                    hasCheckedAlarmPermission = true
                }
            )
            OpenOnPhoneDialog(
                visible = showConfirmation,
                onDismissRequest = { showConfirmation = false },
                curvedText = { openOnPhoneDialogCurvedText(text = text, style = style) }
            )
            if (notificationPermissionState.status == PermissionStatus.Granted) {
                LaunchedEffect(Unit) {
                    // Reset the status of having shown permission rationale.
                    viewModel.permissionStateDataStore.setHasPreviouslyShownRationale(
                        ShownRationaleStatus.UNKNOWN,
                        permission = notificationPermissionState.permission
                    )
                }
            }
            var notNow by rememberSaveable { mutableStateOf(false) }
            if (hasCheckedAlarmPermission) {
                if (notificationPermissionState.status is PermissionStatus.Denied && !notNow) {
                    val denied = notificationPermissionState.status as PermissionStatus.Denied
                    val hasPreviouslyShown by viewModel.permissionStateDataStore
                        .hasPreviouslyShownRationale(notificationPermissionState.permission)
                        .collectAsStateWithLifecycle(initialValue = ShownRationaleStatus.UNKNOWN)

                    if (denied.shouldShowRationale) {
                        LaunchedEffect(Unit) {
                            viewModel.permissionStateDataStore.setHasPreviouslyShownRationale(
                                ShownRationaleStatus.HAS_SHOWN,
                                permission = notificationPermissionState.permission
                            )
                        }
                        // ShouldShowRationale returns true if:
                        // - A request has previously been denied
                        // - The app permission was set to denied in settings
                        // At this point, the app stores the state that the rationale has been shown, as if
                        // subsequently false is returned, this means that the permission cannot be requested
                        // now, as opposed to the false seen from shouldShowRationale on first ever launch
                        PermissionRequiredScreen(
                            listState,
                            descResId = R.string.notification_permission_explanation,
                            onPermissionClick = { notificationPermissionState.launchPermissionRequest() },
                            buttonLabelResId = R.string.show_permission,
                            onNotNowClick = { notNow = true }
                        )
                    } else if (hasPreviouslyShown == ShownRationaleStatus.HAS_SHOWN) {
                        // Rationale has been shown previously, but the user has decided not to grant permission
                        // Offer the user the option to go to permission settings.
                        PermissionRequiredScreen(
                            listState,
                            descResId = R.string.notification_permission_explanation,
                            onPermissionClick = { launchPermissionsSettings(context) },
                            buttonLabelResId = R.string.show_settings,
                            onNotNowClick = { notNow = true }
                        )
                    } else if (hasPreviouslyShown == ShownRationaleStatus.HAS_NOT_SHOWN) {
                        // First launch of permissions, show the permission request without any rationale.
                        LaunchedEffect(Unit) {
                            notificationPermissionState.launchPermissionRequest()
                        }
                    }
                } else {
                    if (homeState.incompatibleVersion) {
                        AlertDialog(
                            visible = homeState.incompatibleVersion,
                            icon = {
                                Icon(
                                    Icons.Default.PhonelinkErase,
                                    stringResource(R.string.phone_app_version_incompatible),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(
                                        ButtonDefaults.LargeIconSize
                                    )
                                )
                            },
                            title = {
                                Text(
                                    stringResource(R.string.phone_app_version_incompatible),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            },
                            text = {
                                Text(stringResource(R.string.phone_app_version_incompatible_desc))
                            },
                            edgeButton = {
                                EdgeButton(onClick = {
                                    viewModel.onEvent(HomeEvent.RetryVersionCheck)
                                }) {
                                    Text(stringResource(R.string.retriable_error_retry))
                                }
                            },
                            onDismissRequest = {}
                        )
                    } else {
                        LaunchedEffect(activeWorkout) {
                            if (activeWorkout) {
                                openWorkoutScreen()
                            }
                        }
                        LaunchedEffect(Unit) {
                            // PermissionRequiredScreen shares the same state, if we begin with that screen and scroll
                            // then we should scroll back before showing the actual screen
                            listState.scrollToItem(0)
                        }
                        ScalingLazyColumn(
                            state = listState,
                            contentPadding = SCALING_LIST_PADDING_VALUES,
                            // param below will avoid having the last element scroll all the way to the center
                            // but will create problems with google's review process
                            //                autoCentering = null
                        ) {
                            if (!homeState.workoutRunningFromPhone) {
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
                                    Button(
                                        colors = if (ambientState.isInteractive)
                                            ButtonDefaults.buttonColors()
                                        else
                                            ButtonDefaults.outlinedButtonColors(),
                                        border = if (ambientState.isAmbient)
                                            ButtonDefaults.outlinedButtonBorder(true)
                                        else null,
                                        onClick = {
                                            remoteActivityHelper.startRemoteActivity(openAppIntent)
                                            showConfirmation = true
                                        }
                                    ) {
                                        Icon(
                                            if (ambientState.isInteractive)
                                                Icons.Default.PhoneAndroid
                                            else
                                                Icons.Outlined.PhoneAndroid,
                                            stringResource(R.string.phone_icon)
                                        )
                                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                        Text(stringResource(R.string.open_phone_app))
                                    }
                                }
                                if (homeState.phoneVersionInfo == null) {
                                    // if we have info about the phone app version, it must be installed
                                    // if we don't it may be either outdated or not installed
                                    item {
                                        TextButton(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = {
                                                remoteActivityHelper.startRemoteActivity(
                                                    getAppIntent
                                                )
                                                showConfirmation = true
                                            }
                                        ) {
                                            Text(
                                                stringResource(R.string.get_phone_app),
                                                maxLines = 1
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
}


private fun launchPermissionsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", context.packageName, null)
    intent.data = uri
    context.startActivity(intent)
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestAlarmPermission(
    listState: ScalingLazyListState,
    viewModel: HomeViewModel,
    setHasCheckPermission: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasExactAlarm by viewModel.hasExactAlarm.collectAsState(null)

    LaunchedEffect(Unit) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(hasExactAlarm) {
        if (hasExactAlarm == true) {
            // Reset the status of having shown permission rationale.
            @SuppressLint("InlinedApi")
            viewModel.permissionStateDataStore.setHasPreviouslyShownRationale(
                ShownRationaleStatus.UNKNOWN,
                permission = Manifest.permission.SCHEDULE_EXACT_ALARM
            )
            setHasCheckPermission()
        }
    }
    // check on build version is not really necessary
    if (hasExactAlarm == false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val hasPreviouslyShown by viewModel.permissionStateDataStore
            .hasPreviouslyShownRationale(Manifest.permission.SCHEDULE_EXACT_ALARM)
            .collectAsStateWithLifecycle(initialValue = ShownRationaleStatus.UNKNOWN)

        if (hasPreviouslyShown == ShownRationaleStatus.HAS_SHOWN) {
            // Rationale has been shown previously, but the user has decided not to grant permission
            // Right now, we just allow the user to continue using the app. One could ask for
            // permission again maybe...
            LaunchedEffect(hasPreviouslyShown) {
                setHasCheckPermission()
            }
        } else if (hasPreviouslyShown == ShownRationaleStatus.HAS_NOT_SHOWN) {
            // First launch of permissions, show the permission request without any rationale.
            PermissionRequiredScreen(
                listState,
                titleResId = R.string.permission_exact_alarm_title,
                descResId = R.string.permission_request_alarm,
                onPermissionClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    val uri = Uri.fromParts("package", context.packageName, null)
                    intent.data = uri
                    context.startActivity(intent)
                },
                buttonLabelResId = R.string.show_settings,
                onNotNowClick = { scope.launch {
                    viewModel.permissionStateDataStore.setHasPreviouslyShownRationale(
                        ShownRationaleStatus.HAS_SHOWN,
                        permission = Manifest.permission.SCHEDULE_EXACT_ALARM
                    )
                    setHasCheckPermission()
                } }
            )
        }
    }
}