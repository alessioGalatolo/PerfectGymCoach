package agdesigns.elevatefitness.ui.screens.workout.components

import agdesigns.elevatefitness.R
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermission(
    hasPromotedNotificationAccess: Boolean,
    canAsk: Boolean,
    onDontAskAgain: () -> Unit,
    refreshPromotedNotificationAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    @SuppressLint("InlinedApi") // Granted at install time on API < 33.
    val notificationPermissionState = rememberPermissionState(
        android.Manifest.permission.POST_NOTIFICATIONS,
    )
    val initialStateIsGranted = rememberSaveable {
        notificationPermissionState.status.isGranted
    }
    var hasNormalBeenClosed by rememberSaveable {
        mutableStateOf(false)
    }
    var hasPromotedBeenClosed by rememberSaveable {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshPromotedNotificationAccess()
    }
    if (!notificationPermissionState.status.isGranted && canAsk && !hasNormalBeenClosed) {
        NotificationPermissionCard(
            onGrantClick = {
                notificationPermissionState.launchPermissionRequest()
            },
            onClose = {
                hasNormalBeenClosed = true
            },
            onDontAskAgain = onDontAskAgain,
            titleText = stringResource(R.string.enable_ongoing_workout_notifications_title),
            infoText = stringResource(R.string.enable_ongoing_workout_notifications_info),
            modifier = modifier
                .fillMaxWidth()
        )
    }
    if (
        !hasPromotedBeenClosed &&
        initialStateIsGranted != true &&
        notificationPermissionState.status.isGranted &&
        !hasPromotedNotificationAccess &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
    ) {
        NotificationPermissionCard(
            onGrantClick = {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                })
            },
            onClose = {
                hasPromotedBeenClosed = true
            },
            onDontAskAgain = null,
            titleText = stringResource(R.string.enable_live_notification_title),
            infoText = stringResource(R.string.enable_live_notification_info),
            modifier = modifier
                .fillMaxWidth()
        )
    }
}

@Composable
private fun NotificationPermissionCard(
    onGrantClick: () -> Unit,
    onClose: () -> Unit,
    titleText: String,
    infoText: String,
    modifier: Modifier = Modifier,
    onDontAskAgain: (() -> Unit)? = null
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
                text = titleText,
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
            text = infoText,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.End
        ) {
            if (onDontAskAgain != null) {
                TextButton(onClick = { onDontAskAgain() }) {
                    Text(text = stringResource(R.string.don_t_ask_again))
                }
            }
            Button(onClick = onGrantClick) {
                Text(text = stringResource(R.string.request_notification_access_confirm))
            }
        }
    }
}