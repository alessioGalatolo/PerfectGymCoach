package com.anexus.perfectgymcoach.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.viewmodels.PlansEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Program(navController: NavHostController, name: String?) {
    val programName = name ?: ""
    Scaffold(
        topBar = {
            SmallTopAppBar(title = { Text(programName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                })
        }, floatingActionButton = {
            LargeFloatingActionButton (
                onClick = {
//                    viewModel.onEvent(PlansEvent.TogglePlanDialogue)
                },
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add program",
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                )
            }
        }, content = { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)){
//                if (viewModel.state.value.workoutPlans.isEmpty()) {
//                    // if you have no plans
//                    Column(
//                        modifier = Modifier.fillMaxSize(),
//                        verticalArrangement = Arrangement.Center,
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Icon(
//                            imageVector = Icons.Filled.Inventory, // TODO: not the right icon
//                            contentDescription = "",
//                            modifier = Modifier.size(160.dp)
//                        )
//                        Text(
//                            stringResource(id = R.string.empty_plans),
//                            modifier = Modifier.padding(16.dp)
//                        )
//                    }
//                } else {
//                    // if you have some plans
//                    LazyColumn(
//                        modifier = Modifier.fillMaxWidth(),
//                        contentPadding = PaddingValues(16.dp)
//                    ) {
//                        items(items = viewModel.state.value.workoutPlans, key = { it }) { plan ->
//                            Card(
//                                onClick = {
//                                    navController.navigate("${MainScreen.Program.route}/${plan.name}")
//                                },
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(horizontal = 4.dp, vertical = 2.dp)
//                            ) {
//                                Row {
//                                    Image(
//                                        painter = painterResource(R.drawable.full_body),
//                                        contentDescription = "Contact profile picture",
//                                        modifier = Modifier
//                                            // Set image size to 40 dp
//                                            .size(40.dp)
//                                            .padding(all = 4.dp)
//                                            // Clip image to be shaped as a circle
//                                            .clip(CircleShape)
//                                    )
//
//                                    // Add a horizontal space between the image and the column
////                Spacer(modifier = Modifier.width(8.dp))
//
//                                    Column {
//                                        Text(text = plan.name)
//                                        Spacer(modifier = Modifier.height(4.dp))
//                                        Text(text = "Some program names...") // TODO
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
            }
        })
}