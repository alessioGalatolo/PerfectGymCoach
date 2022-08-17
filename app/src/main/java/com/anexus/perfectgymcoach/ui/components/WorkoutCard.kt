package com.anexus.perfectgymcoach.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import com.anexus.perfectgymcoach.ui.MainScreen

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutCard(program: WorkoutProgram,
                exercises: List<WorkoutExercise>,
                onCardClick: () -> Unit,
                onCardLongPress: () -> Unit,
                navController: NavHostController){
    val haptic = LocalHapticFeedback.current
    ElevatedCard(
        modifier = Modifier
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
            Image(
                painter = painterResource(R.drawable.sample_image),
                contentDescription = "Contact profile picture",
                alignment = Alignment.Center,
                modifier = Modifier
                    // Set image size to 40 dp
                    .fillMaxWidth()
//                                        .size(160.dp)
                    .align(Alignment.CenterHorizontally)
                    // Clip image to be shaped as a circle
                    .clip(AbsoluteRoundedCornerShape(12.dp))
            )

            Text(text = program.name,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(modifier = Modifier.height(4.dp))
            exercises.forEach {
                Text(text = it.name,
                    modifier = Modifier.padding(horizontal = 8.dp))
                Text(text = "Sets: ${it.reps.size} • Reps: ${it.reps.joinToString(", ")} • Rest: ${it.rest}s",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp))
            }// TODO: maybe improve
            Spacer(modifier = Modifier.height(8.dp))
            Row (verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = { navController.navigate("${MainScreen.Workout.route}/${program.programId}") },
                    modifier = Modifier
                        .padding(8.dp)
                    /*.align(Alignment.End)*/) {
                    Text("Start workout")
                }
                IconButton(onClick = { navController.navigate(
                    "${MainScreen.AddExercise.route}/${program.name}/${program.programId}") }) {
                    Icon(Icons.Filled.Edit, null)
                }
            }
        }
    }
}