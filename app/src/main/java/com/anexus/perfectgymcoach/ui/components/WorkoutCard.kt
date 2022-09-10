package com.anexus.perfectgymcoach.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.anexus.perfectgymcoach.data.exercise.WorkoutExerciseAndInfo
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
    exercises: List<WorkoutExerciseAndInfo>,
    onCardClick: () -> Unit,
    onCardLongPress: () -> Unit,
    navController: NavHostController,
    modifier: Modifier = Modifier
){
    val haptic = LocalHapticFeedback.current
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onCardClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCardLongPress()
                }))
    {
        Column {
            val pagerState = rememberPagerState()
            if (exercises.isNotEmpty()) {
                Box(
                    Modifier.wrapContentHeight(Alignment.Top),
                    contentAlignment = Alignment.TopCenter
                ) {
                    HorizontalPager(count = exercises.size, state = pagerState) { page ->
                        AsyncImage(
                            model = exercises[page].image, // FIXME: topbottom bars with 16:9 image as first exercise
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .clip(AbsoluteRoundedCornerShape(12.dp))
                        )
                    }
                    HorizontalPagerIndicator(
                        pagerState = pagerState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    )
                }
            }


            Text(text = program.name,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(modifier = Modifier.height(4.dp))
            exercises.forEach {
                Text(text = it.name,
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
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp))
            }// TODO: maybe improve
            Spacer(modifier = Modifier.height(8.dp))
            Row (
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    onClick = {
//                        navController.popBackStack(NavigationScreen.Home.route, false)
                        navController.navigate("${MainScreen.Workout.route}/${program.programId}/${true}")
                    },
                    modifier = Modifier
                        .padding(8.dp)) {
                    Icon(Icons.Default.RocketLaunch, null)
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Quick start")
                }
                IconButton(onClick = { navController.navigate(
                    "${MainScreen.AddWorkoutExercise.route}/${program.name}/${program.programId}") }) {
                    Icon(Icons.Filled.Edit, null)
                }
            }
        }
    }
}