package com.anexus.perfectgymcoach.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun History(onNavigate: NavHostController) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        repeat(30) {
            Card(Modifier.fillMaxWidth()) {
                Text("History")
                Text("Alessio")
            }
        }
    }
}