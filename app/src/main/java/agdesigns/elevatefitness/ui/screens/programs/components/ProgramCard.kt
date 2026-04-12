package agdesigns.elevatefitness.ui.screens.programs.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import agdesigns.elevatefitness.data.db.entity.getProgramDisplayName
import agdesigns.elevatefitness.navigation.AddProgramExerciseDestination
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import agdesigns.elevatefitness.navigation.WorkoutDestination
import agdesigns.elevatefitness.ui.common.HorizontalPagerIndicator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LazyItemScope.ProgramCard(
    navigator: DestinationsNavigator,
    reorderableState: ReorderableLazyListState,
    isDragging: MutableState<Boolean>,
    program: WorkoutProgram,
    exercises: List<ProgramExerciseAndInfo>,
    onCardClick: () -> Unit,
    cardShape: Shape = MaterialTheme.shapes.medium,
    cardElevation: Dp = 1.dp,
    modifier: Modifier,
    onDelete: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null
){
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { exercises.size })
    val interactionSource = remember { MutableInteractionSource() }
    ReorderableItem(reorderableState, key = program.programId) {
        ElevatedCard(
            interactionSource = interactionSource,
            onClick = onCardClick,
            shape = cardShape,
            modifier = modifier
                .fillMaxWidth()
                .shadow(cardElevation, cardShape)
                .clip(cardShape)
                .longPressDraggableHandle(
                    onDragStarted = {
                        isDragging.value = true
                        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    },
                    onDragStopped = {
                        isDragging.value = false
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                    },
                    interactionSource = interactionSource
                )
        ) {
            Column {
                AnimatedVisibility(
                    exercises.isNotEmpty() && !isDragging.value
                ) {
                    Box(
                        Modifier.wrapContentHeight(Alignment.Top),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.graphicsLayer {
                                shape = cardShape
                                clip = true
                            }
                        ) { page ->
                            Box(Modifier.fillMaxWidth()) {
                                AsyncImage(
                                    model = exercises[page].image,
                                    contentDescription = stringResource(R.string.exercise_image),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(3f / 2f)
                                        .align(Alignment.TopCenter)
                                )
                            }
                        }
                        HorizontalPagerIndicator(
                            pagerState = pagerState,
                            pageCount = exercises.size,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                        )
                    }
                }
                Row {
                    Text(
                        text = getProgramDisplayName(program.name),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedVisibility(
                    !isDragging.value
                ) {
                    Column {
                        exercises.forEachIndexed { index, it ->
                            val variation =
                                if (it.variation.isNotBlank()) " (${it.variation})" else ""
                            val exerciseText = it.name + variation
                            Text(
                                text = exerciseText,
                                // exerciseModifier needs to go after because we're adding padding
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                        append(stringResource(R.string.sets))
                                        append(": ")
                                    }
                                    append(it.reps.size.toString())
                                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                        append(" • ")
                                        if (it.overriddenDurationBased) {
                                            append(stringResource(R.string.exercise_hold))
                                            append(" (s)")
                                        } else
                                            append(stringResource(R.string.reps))
                                        append(": ")
                                    }
                                    append(it.reps.joinToString(", "))
                                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                        append(" • ")
                                        append(stringResource(R.string.rest))
                                        append(": ")
                                    }
                                    append(it.rest.joinToString("s, ") + "s")
                                    if (it.note.isNotBlank()) {
                                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                            append(" • ")
                                            append(stringResource(R.string.note))
                                        }
                                        append(it.note)
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (exercises.isNotEmpty()) {
                                // FIXME: doesn't animate when quickstart
                                Button(
                                    shapes = ButtonDefaults.shapes(),
                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                    onClick = {
                                        navigator.navigate(
                                            WorkoutDestination(
                                                programId = program.programId,
                                                quickStart = true,
                                                previewExercise = exercises.getOrNull(pagerState.currentPage)
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.RocketLaunch,
                                        stringResource(R.string.quick_start_icon)
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.quick_start))
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (onRename == null && onDelete == null) {
                                    IconButton(onClick = {
                                        navigator.navigate(
                                            AddProgramExerciseDestination(
                                                programName = program.name,
                                                programId = program.programId
                                            )
                                        )
                                    }) {
                                        Icon(
                                            Icons.Outlined.Edit,
                                            stringResource(R.string.edit_icon_program)
                                        )
                                    }
                                } else {
                                    IconButton(onClick = {
                                        navigator.navigate(
                                            WorkoutDestination(
                                                programId = program.programId
                                            )
                                        )
                                    }) {
                                        Icon(
                                            Icons.Outlined.PlayCircle,
                                            stringResource(R.string.start_workout)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.wrapContentSize()
                                    ) {
                                        IconButton(onClick = { expanded = true }) {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                contentDescription = stringResource(R.string.morevert_icon_options),
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.edit)) },
                                                onClick = {
                                                    navigator.navigate(
                                                        AddProgramExerciseDestination(
                                                            programName = program.name,
                                                            programId = program.programId
                                                        )
                                                    )
                                                    expanded = false
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Outlined.Edit,
                                                        contentDescription = stringResource(R.string.edit_icon_program)
                                                    )
                                                })
                                            if (onRename != null) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.rename)) },
                                                    onClick = {
                                                        onRename()
                                                        expanded = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Outlined.DriveFileRenameOutline,
                                                            contentDescription = stringResource(R.string.rename_icon_program)
                                                        )
                                                    }
                                                )
                                            }
                                            if (onDuplicate != null) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.duplicate)) },
                                                    onClick = {
                                                        onDuplicate()
                                                        expanded = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Default.ContentCopy,
                                                            contentDescription = null // TODO
                                                        )
                                                    }
                                                )
                                            }
                                            if (onDelete != null) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.delete)) },
                                                    onClick = {
                                                        onDelete()
                                                        expanded = false
                                                    },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Outlined.Delete,
                                                            contentDescription = stringResource(R.string.delete_icon_program)
                                                        )
                                                    }
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
}