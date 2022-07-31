package com.anexus.perfectgymcoach.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(navController: NavHostController) {
    Column(modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(8.dp)) {
        // Coming next
        Text(text = stringResource(id = R.string.coming_next), fontWeight = FontWeight.Bold)
        ElevatedCard(modifier = Modifier
            .fillMaxWidth()
            .padding(all = 4.dp),
            onClick = {
                navController.navigate(MainScreen.Workout.route)
            }) {
            Row {
                Image(
                    painter = painterResource(R.drawable.full_body),
                    contentDescription = "Contact profile picture",
                    modifier = Modifier
                        // Set image size to 40 dp
                        .size(160.dp)
                        .padding(all = 4.dp)
                        // Clip image to be shaped as a circle
                        .clip(CircleShape)
                )

                // Add a horizontal space between the image and the column
//                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(text = "msg.author")
                    // Add a vertical space between the author and message texts
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "msg.body")
                }
            }

        }
        Text(text = stringResource(id = R.string.other_programs), fontWeight = FontWeight.Bold)
        repeat(6) {
            Row (modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)) {
                Image(
                    painter = painterResource(R.drawable.full_body),
                    contentDescription = "Contact profile picture",
                    modifier = Modifier
                        // Set image size to 40 dp
                        .size(60.dp)
                        .padding(all = 4.dp)
                        // Clip image to be shaped as a circle
                        .clip(CircleShape)
                )

                // Add a horizontal space between the image and the column
                Spacer(modifier = Modifier.width(8.dp))

                Column (modifier = Modifier.align(Alignment.CenterVertically)){
                    Text(text = "msg.author")
                    // Add a vertical space between the author and message texts
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "msg.body")
                }
            }
//            Divider()

            Spacer(modifier = Modifier.height(4.dp))
        }
        TextButton(onClick = { navController.navigate(MainScreen.ChangePlan.route) },
            modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Change workout plan") }
        Spacer(modifier = Modifier.height(8.dp))
    }
}