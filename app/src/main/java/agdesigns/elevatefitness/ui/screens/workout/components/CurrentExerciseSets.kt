package agdesigns.elevatefitness.ui.screens.workout.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.shared.Equipment
import agdesigns.elevatefitness.shared.barbellResFromWeight
import agdesigns.elevatefitness.shared.weightAndUnit
import agdesigns.elevatefitness.ui.common.ChangeRepsWeightDialog
import agdesigns.elevatefitness.ui.screens.workout.SetDisplayRow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun CurrentExerciseSets(
    exerciseRest: Int,
    equipment: Equipment,
    tare: Float?,
    repsWeightRows: List<SetDisplayRow>,
    setsDone: Int,
    imperialSystem: Boolean,
    workoutStarted: Boolean,
    updateRowValues: (Int, Float, Int) -> Unit,
    updateTare: (Float) -> Unit,
    updateBottomBar: (Int?, Float?) -> Unit,
    toggleOtherEquipment: () -> Unit,
    addSet: () -> Unit,
    deleteSet: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(dimensionResource(R.dimen.card_inner_padding)),
            horizontalAlignment = CenterHorizontally
        ) {
            Text(stringResource(R.string.rest) +
                    ": ${exerciseRest}s", Modifier.align(Alignment.Start))

            // if barbell, allow to add barbell weight (used for volume)
            AnimatedVisibility(
                visible = workoutStarted && equipment == Equipment.BARBELL,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                // FIXME: barbellResFromWeight should be computed in ViewModel
                val barbellName: String =
                    stringResource(barbellResFromWeight(tare ?: 0f)) +
                            " " +
                            weightAndUnit(tare ?: 0f,
                                imperialSystem,
                                inParenthesis = true
                            )

                BarbellSelector(
                    selectedBarbell = barbellName,
                    toggleOtherEquipment = toggleOtherEquipment,
                    useImperialSystem = imperialSystem,
                    onBarbellSelected = { weight -> updateTare(weight) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(CenterHorizontally)
                )
            }
            repsWeightRows.forEachIndexed { setCount, (repsInRow, weightInRow, toBeDone, projectedRep, projectedWeight) ->
                var dialogIsOpen by rememberSaveable { mutableStateOf(false) }
                ChangeRepsWeightDialog(
                    dialogIsOpen = dialogIsOpen,
                    toggleDialog = { dialogIsOpen = !dialogIsOpen },
                    initialReps = repsInRow,
                    initialWeight = weightInRow,
                    updateValues = { reps, weight ->
                        updateRowValues(
                            reps,
                            weight,
                            setCount
                        )
                    },
                    deleteSet = { deleteSet(setCount) }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardDefaults.shape) // rounded bounds when clicking
                        .combinedClickable(onLongClick = {
                            if (!toBeDone) {
                                haptic.performHapticFeedback(
                                    HapticFeedbackType.LongPress
                                )
                                dialogIsOpen = true
                            }
                        }, onClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.TextHandleMove // FIXME: not right haptic
                            )
                            updateBottomBar(
                                projectedRep?.toIntOrNull() ?: repsInRow.toIntOrNull(),
                                projectedWeight?.toFloatOrNull() ?: weightInRow.toFloatOrNull()
                            )
                        })
                ) {
                    FilledIconToggleButton(
                        enabled = toBeDone,
                        checked = setsDone == setCount,
                        onCheckedChange = {}
                    ) {
                        Text((setCount + 1).toString())
                    }
                    Spacer(Modifier.width(8.dp))
                    val textColor = if (toBeDone) LocalContentColor.current else MaterialTheme.colorScheme.outline
                    val unitString = if (imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg)

                    // Reps section
                    Text(
                        // FIXME: overflow in other languages
                        text = stringResource(R.string.reps) + ": ",
                        color = textColor
                    )
                    Text(
                        text = repsInRow,
                        color = textColor,
                        textDecoration = if (projectedRep != null && repsInRow != projectedRep) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (projectedRep != null && repsInRow != projectedRep) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = textColor
                        )
                        Text(
                            text = "$projectedRep ",
                            color = textColor
                        )
                    } else {
                        Text(
                            text = " ",
                            color = textColor,
                        )
                    }

                    // Weight section
                    Text(
                        text = stringResource(R.string.weight) + ": ", // " Weight: "
                        color = textColor
                    )
                    if (projectedWeight == null || weightInRow != "...") {
                        Text(
                            text = weightInRow,
                            color = textColor,
                            textDecoration = if (projectedWeight != null && weightInRow != projectedWeight) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                    if (projectedWeight != null && weightInRow != projectedWeight) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = textColor
                        )
                        Text(
                            text = projectedWeight,
                            color = textColor
                        )
                    }

                    // Unit
                    Text(
                        text = " $unitString",
                        color = textColor
                    )
                }
            }
            AnimatedVisibility(
                visible = workoutStarted,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                TextButton(onClick = addSet) {
                    Text(stringResource(R.string.add_set))
                }
            }
        }
    }
}