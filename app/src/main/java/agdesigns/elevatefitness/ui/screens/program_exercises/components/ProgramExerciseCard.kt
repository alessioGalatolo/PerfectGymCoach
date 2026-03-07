package agdesigns.elevatefitness.ui.screens.program_exercises.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.entity.ProgramExercise
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseReorder
import agdesigns.elevatefitness.ui.common.SharedElementKey
import agdesigns.elevatefitness.ui.common.SharedElementType
import agdesigns.elevatefitness.ui.screens.program_exercises.ProgramExercisesEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment.TopRight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.ramcosta.composedestinations.generated.destinations.AddExerciseDialogDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.ReorderableListState


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
context(lazyItemScope: LazyItemScope)
fun SharedTransitionScope.ProgramExerciseCard(
    animatedVisibilityScope: AnimatedVisibilityScope,
    navigator: DestinationsNavigator,
    reorderableListState: ReorderableLazyListState,
    exercise: Exercise?,
    programExercise: ProgramExercise,
    brightImage: MutableState<Boolean>,
    dragStarted: MutableState<Boolean>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    moveUp: () -> Unit,
    moveDown: () -> Unit,
    deleteExercise: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    with(lazyItemScope) {
        ReorderableItem(reorderableListState, key = programExercise.programExerciseId) {
            val interactionSource = remember { MutableInteractionSource() }
            ElevatedCard(
                onClick = {
                    navigator.navigate(
                        AddExerciseDialogDestination(
                            previewExercise = exercise!!,
                            programId = programExercise.extProgramId,
                            programExerciseId = programExercise.programExerciseId,
                            continueAdding = false
                        )
                    )
                },
                interactionSource = interactionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(R.dimen.card_outside_padding),
                        vertical = dimensionResource(R.dimen.card_space_between) / 2
                    )
                    .longPressDraggableHandle(
                        onDragStarted = {
                            dragStarted.value = true
                            haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                        },
                        onDragStopped = {
                            dragStarted.value = false
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        },
                        interactionSource = interactionSource,
                    )
                    .sharedBounds(
                        rememberSharedContentState(
                            SharedElementKey(
                                "AddExerciseDialog",
                                SharedElementType.Bounds,
                                idLong = exercise?.exerciseId ?: 0L
                            )
                        ),
                        animatedVisibilityScope
                    )
                    // clip removes card elevation, we need to reapply the shadow
                    .shadow(1.dp, CardDefaults.shape)
                    .clip(CardDefaults.shape)
            ) {
                this@ElevatedCard.AnimatedVisibility(!dragStarted.value) {
                    Box(Modifier.fillMaxWidth()) {
                        AsyncImage(
                            ImageRequest.Builder(context)
                                .allowHardware(false)
                                .data(exercise?.image ?: R.drawable.finish_workout)
                                .crossfade(true)
                                .listener { _, result ->
                                    val image = result.image.toBitmap()
                                    Palette.from(image).maximumColorCount(3)
                                        .clearFilters()
                                        .setRegion(image.width - 50, 0, image.width, 50)
                                        .generate {
                                            brightImage.value = (ColorUtils.calculateLuminance(
                                                it?.getDominantColor(
                                                    Color.Black.toArgb()
                                                ) ?: 0
                                            )) > 0.5
                                        }
                                }
                                .build(),
                            stringResource(R.string.exercise_image),
                            Modifier
                                .fillMaxWidth()
                                .height(with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } / 3)
                                .align(Alignment.TopCenter)
                                .sharedElement(
                                    rememberSharedContentState(
                                        SharedElementKey(
                                            "AddExerciseDialog",
                                            SharedElementType.Image,
                                            idLong = exercise?.exerciseId ?: 0L
                                        )
                                    ),
                                    animatedVisibilityScope,
                                    boundsTransform = BoundsTransform { _, _ ->
                                        MotionScheme.expressive().slowSpatialSpec()
                                    }
                                )
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .draggableHandle(
                                    onDragStarted = {
                                        dragStarted.value = true
                                        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                    },
                                    onDragStopped = {
                                        dragStarted.value = false
                                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                    },
                                    interactionSource = interactionSource,
                                ),
                            onClick = {},

                        ) {
                            Icon(
                                Icons.Rounded.DragHandle,
                                contentDescription = stringResource(R.string.reorder_icon),
                                tint = if (brightImage.value) Color.Black else Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .align(TopRight)
                        ) {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.morevert_icon_options),
                                    tint = if (brightImage.value) Color.Black else Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.move_up)) },
                                    onClick = {
                                        moveUp()
                                        menuExpanded = false
                                    },
                                    enabled = canMoveUp,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.ArrowUpward,
                                            contentDescription = stringResource(R.string.move_up)
                                        )
                                    })
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.move_down)) },
                                    onClick = {
                                        moveDown()
                                        menuExpanded = false
                                    },
                                    enabled = canMoveDown,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.ArrowDownward,
                                            contentDescription = stringResource(R.string.move_down)
                                        )
                                    })
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit)) },
                                    onClick = {
                                        navigator.navigate(
                                            AddExerciseDialogDestination(
                                                previewExercise = exercise!!,
                                                programId = programExercise.extProgramId,
                                                programExerciseId = programExercise.programExerciseId,
                                                continueAdding = false
                                            )
                                        )
                                        menuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Edit,
                                            contentDescription = stringResource(R.string.edit)
                                        )
                                    })
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.remove)) },
                                    onClick = {
                                        deleteExercise()
                                        menuExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = stringResource(R.string.delete)
                                        )
                                    })
                            }
                        }
                    }
                }
                Column(Modifier.padding(dimensionResource(R.dimen.card_inner_padding))) {
                    val variation =
                        if (programExercise.variation.isBlank()) "" else " (${programExercise.variation})"
                    Text(
                        text = (exercise?.name ?: "") + variation,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.sharedElement(
                            rememberSharedContentState(
                                SharedElementKey(
                                    "AddExerciseDialog",
                                    SharedElementType.Title,
                                    idLong = exercise?.exerciseId ?: 0L
                                )
                            ),
                            animatedVisibilityScope,
                            boundsTransform = BoundsTransform { _, _ ->
                                MotionScheme.expressive().slowSpatialSpec()
                            }
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = buildAnnotatedString {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(stringResource(R.string.sets))
                            append(": ")
                        }
                        append(programExercise.reps.size.toString())
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(" • ")
                            append(stringResource(R.string.reps))
                            append(": ")
                        }
                        append(programExercise.reps.joinToString(", "))
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(" • ")
                            append(stringResource(R.string.rest))
                            append(": ")
                        }
                        append(programExercise.rest.joinToString("s, ") + "s")
                    })
                    if (programExercise.note.isNotBlank())
                        Text(text = buildAnnotatedString {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(stringResource(R.string.note))
                            }
                            append(programExercise.note)
                        })
                }
            }
        }
    }
}