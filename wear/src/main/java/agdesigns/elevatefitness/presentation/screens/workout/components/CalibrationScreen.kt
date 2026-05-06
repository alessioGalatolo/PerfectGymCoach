package agdesigns.elevatefitness.presentation.screens.workout.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.presentation.screens.common.TextHeaderWithMarquee
import agdesigns.elevatefitness.presentation.screens.workout.WorkoutViewModel
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.ui.screens.player.PlayerScreen
import kotlinx.coroutines.delay

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun CalibrationScreen(
    isCalibrating: Boolean,
    progress: Float,
    isComplete: Boolean,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            WorkoutViewModel.TIME_REFRESH_DELAY_MILLIS.toInt(),
            easing = LinearEasing
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isComplete -> {
                LaunchedEffect(Unit) {
                    delay(1500L)
                    onDismiss()
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.calibration_complete),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            isCalibrating -> {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Text(
                        text = stringResource(R.string.calibration_keep_still),
                        style = MaterialTheme.typography.numeralSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            }

            else -> {
                PlayerScreen(
                    mediaDisplay = {
                        Text(
                            text = stringResource(R.string.calibration_title),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    },
                    controlButtons = {
                        Text(
                            text = stringResource(R.string.calibration_description),
                            style = MaterialTheme.typography.bodyExtraSmall,
                            textAlign = TextAlign.Center,
                        )
                    },
                    buttons = {
                        Button(onClick = onStart) {
                            Text(stringResource(R.string.calibration_start))
                        }
                    }
                )
            }
        }
    }
}