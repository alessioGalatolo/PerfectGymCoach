package agdesigns.elevatefitness.presentation.screens.workout.components

import agdesigns.elevatefitness.R
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

/**
 * Shown when the rest timer is over but the heart rate has not recovered yet.
 * Disappears on its own once the heart rate reaches [targetHeartRate].
 */
@Composable
fun RecoveryWarningScreen(
    heartRate: Int?,
    targetHeartRate: Int,
    // 0 at the set's peak heart rate, 1 at target
    recoveryProgress: Float,
    ambientMode: AmbientMode,
    onDismiss: () -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = recoveryProgress,
        animationSpec = tween(1000),
        label = "recoveryProgress"
    )
    val animation = rememberInfiniteTransition(label = "heartbeat")
    val heartSize by animation.animateFloat(
        initialValue = 20f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "heartSize"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // block touches from reaching the workout pages below
            .pointerInput(Unit) {},
        contentAlignment = Alignment.Center,
    ) {
        if (ambientMode is AmbientMode.Interactive) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                startAngle = CircularProgressIndicatorDefaults.StartAngle + 20f,  // allow for clock in up center
                endAngle = CircularProgressIndicatorDefaults.StartAngle - 20f,
                strokeWidth = CircularProgressIndicatorDefaults.smallStrokeWidth,
                colors = ProgressIndicatorDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.error
                )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.recovery_warning_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(26.dp)) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(
                            if (ambientMode is AmbientMode.Interactive) heartSize.dp else 20.dp
                        )
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (ambientMode is AmbientMode.Interactive && heartRate != null)
                        heartRate.toString()
                    else
                        "--",
                    style = MaterialTheme.typography.numeralMedium,
                )
            }
            Text(
                text = stringResource(R.string.recovery_target, targetHeartRate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
            )
        }
        EdgeButton(
            onClick = onDismiss,
            buttonSize = EdgeButtonSize.ExtraSmall,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = CircularProgressIndicatorDefaults.smallStrokeWidth),
            colors = if (ambientMode is AmbientMode.Interactive)
                ButtonDefaults.filledTonalButtonColors()
            else
                ButtonDefaults.outlinedButtonColors(),
            border = if (ambientMode is AmbientMode.Interactive)
                null
            else
                ButtonDefaults.outlinedButtonBorder(true)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.close)
            )
        }
    }
}
