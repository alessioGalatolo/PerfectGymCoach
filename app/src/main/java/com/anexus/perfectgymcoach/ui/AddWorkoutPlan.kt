package com.anexus.perfectgymcoach.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlan
import androidx.hilt.navigation.compose.hiltViewModel
import com.anexus.perfectgymcoach.ui.components.InsertNameDialog
import com.anexus.perfectgymcoach.ui.components.PGCSmallTopBar
import com.anexus.perfectgymcoach.viewmodels.PlansEvent
import com.anexus.perfectgymcoach.viewmodels.PlansViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddWorkoutPlan(navController: NavHostController,
                   openDialogNow: Boolean,
                   viewModel: PlansViewModel = hiltViewModel()) {
    InsertNameDialog(
        prompt = "Name of the new plan",
        dialogueIsOpen = viewModel.state.value.openAddPlanDialogue,
        toggleDialogue = { viewModel.onEvent(PlansEvent.TogglePlanDialogue) },
        insertName = { planName -> viewModel.onEvent(PlansEvent.AddPlan(WorkoutPlan(name = planName))) }
    )
    val openDialog = rememberSaveable { mutableStateOf(openDialogNow) }
    if (openDialog.value){
        viewModel.onEvent(PlansEvent.TogglePlanDialogue)
        openDialog.value = false
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold (
        snackbarHost = { SnackbarHost(snackbarHostState) }, // FIXME: should be padded (navbar)
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            PGCSmallTopBar(scrollBehavior = scrollBehavior, navController = navController) {
                Text(stringResource(R.string.manage_workout_plans))
            }
        }, floatingActionButton = {
            LargeFloatingActionButton (
                modifier = Modifier.navigationBarsPadding(),
                onClick = {
                    viewModel.onEvent(PlansEvent.TogglePlanDialogue)
                },
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add workout plan",
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
                )
            }
        }) { innerPadding ->
        if (viewModel.state.value.workoutPlans.isEmpty()) {
            // if you have no plans
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentPaste,
                    contentDescription = "",
                    modifier = Modifier.size(160.dp)
                )
                Text(
                    stringResource(id = R.string.empty_plans),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            // if you have some plans
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.padding(16.dp)
            ) {
                itemsIndexed(items = viewModel.state.value.workoutPlans, key = { _, it -> it.planId })
                { index, plan ->
                    if (index == 0){
                        Text("Current plan", fontWeight = FontWeight.Bold)
                    } else if (index == 1) {
                        Text("Other plans", fontWeight = FontWeight.Bold)
                    }
                    PlanCard(
                        navController = navController, plan = plan,
                        viewModel.state.value.currentPlanId
                    ) {
                        viewModel.onEvent(PlansEvent.SetCurrentPlan(it))
                        scope.launch {
                            snackbarHostState.showSnackbar("Plan set as current")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.PlanCard(navController: NavHostController,
             plan: WorkoutPlan,
             currentPlanId: Long?,
             setAsCurrent: (Long) -> Unit
){
    Card (
        Modifier
            .animateItemPlacement()
            .clickable {
                navController.navigate(
                    "${MainScreen.AddProgram.route}/${plan.name}/${plan.planId}/${false}"
                )
            }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.full_body),
                contentDescription = "Contact profile picture",
                modifier = Modifier
                    // Set image size to 40 dp
                    .size(40.dp)
                    .padding(all = 4.dp)
                    // Clip image to be shaped as a circle
                    .clip(CircleShape)
            )

            // Add a horizontal space between the image and the column
//                Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(text = plan.name)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Some program names...") // TODO
            }

            IconToggleButton(
                checked = plan.planId == currentPlanId,
                onCheckedChange = { setAsCurrent(plan.planId) }
            ) {
                val transition = updateTransition(
                    plan.planId == currentPlanId,
                    label = "Checked indicator"
                )

//            val tint by transition.animateColor(
//                label = "Tint"
//            ) { isChecked ->
//                if (isChecked) Color.Red else Color.Black
//            }
                val default_icon_size = 24.dp
                val size by transition.animateDp(
                    transitionSpec = {
                        if (false isTransitioningTo true) {
                            keyframes {
                                durationMillis = 250
                                default_icon_size + 5.dp at 0 with LinearOutSlowInEasing // for 0-15 ms
                                default_icon_size + 10.dp at 15 with FastOutLinearInEasing // for 15-75 ms
                                default_icon_size + 15.dp at 75 // ms
                                default_icon_size + 10.dp at 150 // ms
                            }
                        } else {
                            spring(stiffness = Spring.StiffnessVeryLow)
                        }
                    },
                    label = "Size"
                ) { 24.dp }

                Icon(
                    imageVector = if (plan.planId == currentPlanId) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
//                tint = tint,
                    modifier = Modifier.size(size)
                )
            }
        }
    }
}
