package com.anexus.perfectgymcoach.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.anexus.perfectgymcoach.data.exercise.ProgramExerciseAndInfo
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import com.anexus.perfectgymcoach.ui.MainScreen
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState

@OptIn(ExperimentalFoundationApi::class, ExperimentalPagerApi::class)
@Composable
fun WorkoutCard(
    program: WorkoutProgram,
    exercises: List<ProgramExerciseAndInfo>,
    onCardClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    navController: NavHostController,
    modifier: Modifier = Modifier
){
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onCardClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    expanded = true
                }))
    {
        Column {
            val pagerState = rememberPagerState()
            if (exercises.isNotEmpty()) {
                Box(
                    Modifier.wrapContentHeight(Alignment.Top),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val imageWidth = LocalConfiguration.current.screenWidthDp.dp // - 32.dp // 2*padding
                    val imageHeight = imageWidth/3*2

                    HorizontalPager(count = exercises.size, state = pagerState,
                        modifier = Modifier
                        .clip(AbsoluteRoundedCornerShape(12.dp))) { page ->
                        Box (Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = exercises[page].image, // FIXME: topbottom bars with 16:9 image as first exercise
                                contentDescription = null,
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
                Text(text = it.name + it.variation,
                    modifier = Modifier.padding(horizontal = 8.dp))
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
                    append("${it.rest}s")
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
                Button(contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    onClick = {
                        navController.navigate("${MainScreen.Workout.route}/${program.programId}/${true}/${false}")
                    },
                    modifier = Modifier
                        .padding(8.dp)) {
                    Icon(Icons.Default.RocketLaunch, null)
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Quick start")
                }
                Row (
                    verticalAlignment = Alignment.CenterVertically
                ){
                    if (onRename == null && onDelete == null){
                        IconButton(onClick = {
                            navController.navigate(
                                "${MainScreen.Workout.route}/" +
                                        "${program.programId}/" +
                                        "${false}/${false}"
                            )
                        }) {
                            Icon(Icons.Outlined.PlayCircle, null)
                        }
                        IconButton(onClick = {
                            navController.navigate(
                                "${MainScreen.AddWorkoutExercise.route}/" +
                                        "${program.name}/" +
                                        "${program.programId}"
                            )
                        }) {
                            Icon(Icons.Outlined.Edit, null)
                        }
                    } else {
                        Box(
                            modifier = Modifier.wrapContentSize()
                        ) {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Localized description",
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Start workout") },
                                    onClick = {
                                        navController.navigate(
                                            "${MainScreen.Workout.route}/" +
                                                    "${program.programId}/" +
                                                    "${false}/${false}"
                                        )
                                        expanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.PlayCircle,
                                            contentDescription = null
                                        )
                                    })
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        navController.navigate(
                                            "${MainScreen.AddWorkoutExercise.route}/" +
                                                    "${program.name}/" +
                                                    "${program.programId}"
                                        )
                                        expanded = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Edit,
                                            contentDescription = null
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
                                                contentDescription = null
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
                                                contentDescription = null
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