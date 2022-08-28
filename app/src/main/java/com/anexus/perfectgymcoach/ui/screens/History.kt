package com.anexus.perfectgymcoach.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.viewmodels.HistoryViewModel
import java.text.SimpleDateFormat


@Composable
fun History(onNavigate: NavHostController, viewModel: HistoryViewModel = hiltViewModel()) {
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)) {
        items(items = viewModel.state.value.workoutRecords, key = { it }){
            Card(Modifier.fillMaxWidth()) {
                Column (Modifier.padding(8.dp)) {
                    val dateFormat = SimpleDateFormat("d MMM (yyyy) - HH:mm")
                    val date: String = dateFormat.format(it.startDate.time)
                    Text("Started at: $date")
                    Text("Duration: ${DateUtils.formatElapsedTime(it.duration)}")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}