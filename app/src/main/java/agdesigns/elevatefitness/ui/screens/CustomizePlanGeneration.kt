package agdesigns.elevatefitness.ui.screens

import agdesigns.elevatefitness.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanDifficulty
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanGoal
import agdesigns.elevatefitness.data.workout_plan.WorkoutPlanSplit
import agdesigns.elevatefitness.ui.GeneratePlanGraph
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.ViewGeneratedPlanDestination
import kotlinx.coroutines.launch

@Destination<GeneratePlanGraph>(start = true)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomizePlanGeneration(
    navigator: DestinationsNavigator
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val totalPageCount = 3
    val pagerState = rememberPagerState(pageCount = { totalPageCount })
    val scope = rememberCoroutineScope()
    val goalChoice = rememberSaveable { mutableStateOf("") }
    val expertiseLevel = rememberSaveable { mutableStateOf("") }
    val workoutSplit = rememberSaveable { mutableStateOf("") }

    // FIXME: maybe replace with predictivebackhandler?
    BackHandler(pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
        topBar = {
            TopAppBar(title = { Text("Generate a new plan") },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
                        )
                    }
                })
        }, content = { innerPadding ->
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    when (it) {
                        0 -> goalChoice { choice ->
                            goalChoice.value = choice
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }

                        1 -> expertiseLevel { choice ->
                            expertiseLevel.value = choice
                            scope.launch { pagerState.animateScrollToPage(2) }
                        }

                        2 -> workoutSplit { choice ->
                            workoutSplit.value = choice
                            navigator.navigateUp()
                            navigator.navigate(
                                ViewGeneratedPlanDestination(
                                    goalChoice.value,
                                    expertiseLevel.value,
                                    workoutSplit.value
                                )
                            )
                        }
                    }
                }
            }
        }
    )
}


@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.goalChoice(completeGoal: (String) -> Unit){
    val goalImages = mapOf(
        WorkoutPlanGoal.HYPERTROPHY.goal to R.drawable.cable_curl,
        WorkoutPlanGoal.STRENGTH.goal to R.drawable.headstand_push_up,
        WorkoutPlanGoal.ENDURANCE.goal to R.drawable.plank,
        WorkoutPlanGoal.CARDIO.goal to R.drawable.sit_ups
    )
    // fixme: would like this to be a stickyHeader but it is currently bugged
    item {
        Text(
            "What is your goal when training?",
            style = MaterialTheme.typography.titleLarge
        )
    }
    items(goalImages.size) { index ->
        val goal = goalImages.keys.elementAt(index)
        val image = goalImages.values.elementAt(index)
        if (goal == WorkoutPlanGoal.CARDIO.goal || goal == WorkoutPlanGoal.ENDURANCE.goal) {
            Card {
                AsyncImage(
                    model = image,
                    contentDescription = "$goal image",
                    contentScale = ContentScale.Inside,
                    colorFilter = ColorFilter.tint(Color.Black, BlendMode.Color),
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Text(
                    text = "$goal. Not currently available.",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        else {
            ElevatedCard(Modifier.clickable {
                completeGoal(goal)
            }) {
                AsyncImage(
                    model = image,
                    contentDescription = "$goal image",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                )
                Text(
                    text = goal,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.expertiseLevel(completeExpertise: (String) -> Unit) {
    val expertiseImages = mapOf(
        WorkoutPlanDifficulty.BEGINNER.expertiseLevel to R.drawable.chest_press,
        WorkoutPlanDifficulty.INTERMEDIATE.expertiseLevel to R.drawable.deadlift,
        WorkoutPlanDifficulty.ADVANCED.expertiseLevel to R.drawable.muscle_up
    )
    stickyHeader {
        Text(
            "What is your expertise level?",
            style = MaterialTheme.typography.titleLarge
        )
    }
    items(expertiseImages.size) { index ->
        val level = expertiseImages.keys.elementAt(index)
        val image = expertiseImages.values.elementAt(index)
        ElevatedCard(Modifier.clickable {
            completeExpertise(level)
        }) {
            AsyncImage(
                model = image,
                contentDescription = "$level image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
            )
            Text(
                text = level,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.workoutSplit(completeSplit: (String) -> Unit) {
    val workoutImages = mapOf(
        WorkoutPlanSplit.BRO.split to R.drawable.bench_press,
        WorkoutPlanSplit.GAINZ.split to R.drawable.generic_barbell,
        WorkoutPlanSplit.FULL_BODY.split to R.drawable.generic_machine,
        WorkoutPlanSplit.UPPER_LOWER.split to R.drawable.chest_dip,
    )

    stickyHeader {
        Text(
            "How many times per week do you want to exercise?",
            style = MaterialTheme.typography.titleLarge
        )
    }

    items(workoutImages.size) { index ->
        val split = workoutImages.keys.elementAt(index)
        val image = workoutImages.values.elementAt(index)
        ElevatedCard(Modifier.clickable {
            completeSplit(split)
        }) {
            AsyncImage(
                model = image,
                contentDescription = "$split image",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
            )
            Text(
                text = split,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}