package agdesigns.elevatefitness.ui.common

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndEquipment
import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndInfo
import agdesigns.elevatefitness.shared.maybeKgToLb
import agdesigns.elevatefitness.shared.weightAndUnit
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat.ID_NULL
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import agdesigns.elevatefitness.shared.Equipment
import agdesigns.elevatefitness.shared.barbellResFromWeight

// Shows a nice list of records
fun LazyListScope.ExerciseRecordsList(
    useImperialSystem: Boolean,
    exerciseRecordsWithImage: List<ExerciseRecordAndInfo> = emptyList(),
    onRecordClick: (Long) -> Unit = {},
) {
    items (items = exerciseRecordsWithImage, key = { it.recordId }) { exercise ->
        Card (onClick = {
            onRecordClick(exercise.recordId)
        }, modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)){
            if (exercise.image != ID_NULL) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(exercise.image)
                        .crossfade(true)
                        .build(),
                    contentScale = ContentScale.Crop,
                    contentDescription = stringResource(R.string.exercise_image),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } / 4)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            Column(Modifier.padding(dimensionResource(R.dimen.card_inner_padding))) {
                if (exercise.name.isNotEmpty())
                    Text(text = exercise.name + exercise.variation, style = MaterialTheme.typography.titleLarge)
                if (exercise.equipment == Equipment.BARBELL) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(
                            R.string.barbell_used,
                            stringResource(barbellResFromWeight(exercise.tare)),
                            weightAndUnit(exercise.tare, useImperialSystem, inParenthesis = true)
                        )
                    )
                } else if (exercise.equipment == Equipment.BODY_WEIGHT) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(
                            R.string.bodyweight_at_the_time,
                            weightAndUnit(exercise.tare, useImperialSystem)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                exercise.reps.forEachIndexed { index, rep ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilledIconToggleButton(checked = false, // FIXME: can use different component?
                            onCheckedChange = { }) {
                            Text((index + 1).toString())
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(
                                R.string.reps_weight,
                                rep,
                                maybeKgToLb(exercise.weights[index], useImperialSystem),
                                if (useImperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg)
                            )
                        )
                    }
                }

            }
        }
    }
}