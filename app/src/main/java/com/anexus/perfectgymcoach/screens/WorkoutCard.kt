package com.anexus.perfectgymcoach.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutCard(program: WorkoutProgram,
                exercises: List<WorkoutExercise>,
                onCardClick: () -> Unit){
    ElevatedCard(
        onClick = onCardClick,
        modifier = Modifier
            .fillMaxWidth()
    ) {
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
                Text(text = "Sets: ${it.sets} Reps: ${it.reps} Rest: ${it.rest}s",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp))
            }// TODO
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Row (verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { /*TODO*/ }) {
                    Text(text = "See all")
                }
                OutlinedButton(onClick = { /*TODO*/ },
                    modifier = Modifier
                        .padding(8.dp)
                    /*.align(Alignment.End)*/) {
                    Text("Start workout")
                }
            }



        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}