package agdesigns.elevatefitness.presentation.screens.home.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.presentation.screens.SCALING_LIST_PADDING_VALUES
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import com.google.android.horologist.annotations.ExperimentalHorologistApi

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun PermissionRequiredScreen(
    listState: ScalingLazyListState,
    onPermissionClick: () -> Unit,
    onNotNowClick: () -> Unit,
    @StringRes buttonLabelResId: Int
) {
    ScalingLazyColumn(
        contentPadding = SCALING_LIST_PADDING_VALUES,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f)
            .background(MaterialTheme.colorScheme.background),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround,
        autoCentering = null
    ) {
        item {
            Text(
                text = stringResource(R.string.notification_permission_explanation),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                onClick = onPermissionClick,
            ) {
                Text(
                    text = stringResource(buttonLabelResId),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
        item {
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNotNowClick
            ) {
                Text(
                    text = stringResource(R.string.not_now),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}