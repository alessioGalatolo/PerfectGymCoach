package agdesigns.elevatefitness.ui.common

import agdesigns.elevatefitness.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import agdesigns.elevatefitness.data.db.entity.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.db.entity.WorkoutProgram
import agdesigns.elevatefitness.data.db.entity.getProgramDisplayName
import agdesigns.elevatefitness.navigation.AddProgramExerciseDestination
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import agdesigns.elevatefitness.navigation.WorkoutDestination
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkoutCard(
    navigator: DestinationsNavigator,
    program: WorkoutProgram,
    exercises: List<ProgramExerciseAndInfo>,
    onCardClick: (ProgramExerciseAndInfo?) -> Unit,
    cardModifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    exerciseModifier: Modifier = Modifier,
    cardShape: Shape = MaterialTheme.shapes.medium,
    cardElevation: Dp = 1.dp,
    onDelete: (() -> Unit)? = null,  // FIXME: this is always null
    onRename: (() -> Unit)? = null,  // FIXME: this is always null
){
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { exercises.size })
    ElevatedCard(
        shape = cardShape,
        modifier = cardModifier
            .fillMaxWidth()
            .shadow(cardElevation, cardShape)
            .clip(cardShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = {
                    onCardClick(
                        exercises.getOrNull(pagerState.currentPage)
                    )
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    expanded = true
                }
            )
    )
    {
        Column {
            if (exercises.isNotEmpty()) {
                Box(
                    Modifier.wrapContentHeight(Alignment.Top),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val roundedCornersShape = cardShape
                    HorizontalPager(state = pagerState,
                        modifier = Modifier.graphicsLayer {
                            shape = roundedCornersShape
                            clip = true
                        }
                        .then(imageModifier)) { page ->
                        Box (Modifier.fillMaxWidth()) {
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
            Row{
                Text(
                    text = getProgramDisplayName(program.name),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            exercises.forEachIndexed { index, it ->
                // breaks if more than one exercise has the same name
                val modifier = if (index == pagerState.currentPage)
                    exerciseModifier
                else Modifier
                val variation = if (it.variation.isNotBlank()) " (${it.variation})" else ""
                val exerciseText = it.name + variation
                Text(text = exerciseText,
                    // exerciseModifier needs to go after because we're adding padding
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .then(modifier))
                Text(text = buildAnnotatedString {
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
                    if (it.reps.all { rep -> rep == it.reps[0] })
                        append(it.reps[0].toString())
                    else
                        append(it.reps.joinToString(", "))
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(" • ")
                        append(stringResource(R.string.rest))
                        append(": ")
                    }
                    if (it.rest.all { rest -> rest == it.rest[0] })
                        append(it.rest[0].toString() + "s")
                    else
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
                modifier = Modifier.padding(horizontal = 8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row (
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (exercises.isNotEmpty()) {
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
                        Icon(Icons.Default.RocketLaunch, stringResource(R.string.quick_start_icon))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.quick_start))
                    }
                }
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ){
                    if (onRename == null && onDelete == null){
                        IconButton(onClick = {
                            navigator.navigate(
                                AddProgramExerciseDestination(
                                    programName = program.name,
                                    programId = program.programId
                                )
                            )
                        }) {
                            Icon(Icons.Outlined.Edit, stringResource(R.string.edit_icon_program))
                        }
                    } else {
                        IconButton(onClick = {
                            navigator.navigate(
                                WorkoutDestination(
                                    programId = program.programId
                                )
                            )
                        }) {
                            Icon(Icons.Outlined.PlayCircle, stringResource(R.string.start_workout))
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
//                                DropdownMenuItem(
//                                    text = { Text("Start workout") },
//                                    onClick = {
//                                        navigator.navigate(
//                                            WorkoutDestination(
//                                                programId = program.programId
//                                            ),
//                                            onlyIfResumed = true
//                                        )
//                                        expanded = false
//                                    },
//                                    leadingIcon = {
//                                        Icon(
//                                            Icons.Outlined.PlayCircle,
//                                            contentDescription = null
//                                        )
//                                    })
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