package com.anexus.perfectgymcoach.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.ui.MainScreen
import com.anexus.perfectgymcoach.viewmodels.HistoryViewModel
import java.text.SimpleDateFormat


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun History(navController: NavHostController,
            contentPadding: PaddingValues,
            viewModel: HistoryViewModel = hiltViewModel()
) {
    val records = viewModel.state.value.workoutRecords.filter { it.duration > 0 }
    if (records.isEmpty())
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "",
                modifier = Modifier.size(160.dp)
            )
            Text(
                stringResource(id = R.string.empty_history),
                modifier = Modifier.padding(16.dp)
            )
        }
    else
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
            contentPadding = contentPadding
        ) {
            items(items = records, key = { it }){
                Card(modifier = Modifier.fillMaxWidth(), onClick = {
                    navController.navigate("${MainScreen.WorkoutRecap.route}/${it.workoutId}")
                }) {
                    Row(Modifier.padding(8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            val dateFormat = SimpleDateFormat("d MMM (yyyy) - HH:mm")
                            val date: String = dateFormat.format(it.startDate.time)
                            Text(it.name, style = MaterialTheme.typography.titleLarge)
                            Text("Started at: $date")
                            Text("Duration: ${DateUtils.formatElapsedTime(it.duration)}")
                        }
//                        IconButton(onClick = {
//                            navController.navigate("${MainScreen.WorkoutRecap.route}/${it.workoutId}")
//                        }) {
//                            Icon(Icons.Default.Info, null)
//                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
}