package agdesigns.elevatefitness.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import agdesigns.elevatefitness.data.exercise.ProgramExerciseAndInfo
import agdesigns.elevatefitness.data.workout_program.WorkoutProgram
import com.google.accompanist.pager.HorizontalPagerIndicator
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import coil3.compose.AsyncImage
import com.ramcosta.composedestinations.generated.destinations.AddProgramExerciseDestination
import com.ramcosta.composedestinations.generated.destinations.WorkoutDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutCard(
    navigator: DestinationsNavigator,
    program: WorkoutProgram,
    exercises: List<ProgramExerciseAndInfo>,
    onCardClick: (ProgramExerciseAndInfo) -> Unit,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    exerciseModifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null
){
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { exercises.size })
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = {
                    onCardClick(
                        exercises[pagerState.currentPage]
                    ) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    expanded = true
                }))
    {
        Column {
            if (exercises.isNotEmpty()) {
                Box(
                    Modifier.wrapContentHeight(Alignment.Top),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val imageWidth = with (LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } // - 32.dp // 2*padding
                    val imageHeight = imageWidth/3*2

                    val roundedCornersShape = CardDefaults.shape
                    HorizontalPager(state = pagerState,
                        modifier = imageModifier.graphicsLayer {
                            shape = roundedCornersShape
                            clip = true // <- this ensures clipping is applied during transition
                        }) { page ->
                        Box (Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = exercises[page].image, // FIXME: topbottom bars with 16:9 image as first exercise
                                contentDescription = "Image of the exercise",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(imageHeight)
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
                    text = program.name,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            exercises.forEach {
                val modifier = if (it.name == exercises[pagerState.currentPage].name)
                    exerciseModifier
                else Modifier
                val exerciseText = it.name + it.variation
                Text(text = exerciseText,
                    modifier = modifier.padding(horizontal = 8.dp))
                Text(text = buildAnnotatedString {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append("Sets: ")
                    }
                    append(it.reps.size.toString())
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(" • Reps: ")
                    }
                    append(it.reps.joinToString(", "))
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(" • Rest: ")
                    }
                    append(it.rest.joinToString("s, ") + "s")
                    if (it.note.isNotBlank()) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(" • Note: ")
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
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        onClick = {
                            navigator.navigate(
                                WorkoutDestination(
                                    programId = program.programId,
                                    quickStart = true
                                )
                            )
                        },
                        modifier = Modifier
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.RocketLaunch, "Quick start workout")
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Quick start")
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
                            Icon(Icons.Outlined.Edit, "Edit program")
                        }
                    } else {
                        IconButton(onClick = {
                            navigator.navigate(
                                WorkoutDestination(
                                    programId = program.programId
                                )
                            )
                        }) {
                            Icon(Icons.Outlined.PlayCircle, "Start workout")
                        }
                        Box(
                            modifier = Modifier.wrapContentSize()
                        ) {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More options",
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
                                    text = { Text("Edit") },
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
                                            contentDescription = "Edit program"
                                        )
                                    })
                                if (onRename != null) {
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        onClick = {
                                            onRename()
                                            expanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.DriveFileRenameOutline,
                                                contentDescription = "Rename"
                                            )
                                        }
                                    )
                                }
                                if (onDelete != null) {
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            onDelete()
                                            expanded = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = "Delete program"
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