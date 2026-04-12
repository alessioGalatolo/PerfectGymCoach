package agdesigns.elevatefitness.ui.screens.plans

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
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanDifficulty
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanGoal
import agdesigns.elevatefitness.data.db.entity.WorkoutPlanSplit
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import agdesigns.elevatefitness.navigation.ViewGeneratedPlanDestination
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.max
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun CustomizePlanGeneration(
    navigator: DestinationsNavigator
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState(), { false })
    val snackbarHostState = remember { SnackbarHostState() }
    val totalPageCount = 3
    val pagerState = rememberPagerState(pageCount = { totalPageCount })
    val scope = rememberCoroutineScope()
    val goalChoice = rememberSaveable { mutableStateOf(WorkoutPlanGoal.HYPERTROPHY) }
    val expertiseLevel = rememberSaveable { mutableStateOf(WorkoutPlanDifficulty.BEGINNER) }
    val workoutSplit = rememberSaveable { mutableStateOf(WorkoutPlanSplit.BRO) }

    PredictiveBackHandler(enabled = pagerState.currentPage > 0 || pagerState.isScrollInProgress) { progress ->
        // This block is executed when the back gesture begins.
        try {
            progress.collect { backEvent ->
                pagerState.scrollToPage(
                    pagerState.currentPage,
                    -backEvent.progress.coerceIn(-0.49f, 0.49f)
                )
            }
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
            // This block is executed if the gesture completes successfully.
        } catch (e: CancellationException) {
            pagerState.animateScrollToPage(pagerState.currentPage)
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = { Text(stringResource(R.string.generate_a_new_plan)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(
                        onClick = { navigator.navigateUp() },
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.close_icon)
                        )
                    }
                })
        }, content = { innerPadding ->
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    when (it) {
                        0 -> goalChoicePage { choice ->
                            goalChoice.value = choice
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }

                        1 -> expertiseLevelPage { choice ->
                            expertiseLevel.value = choice
                            scope.launch { pagerState.animateScrollToPage(2) }
                        }

                        2 -> workoutSplitPage { choice ->
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
                    item {
                        Spacer(Modifier.height(max(0.dp,innerPadding.calculateBottomPadding()-16.dp)))
                    }
                }
            }
        }
    )
}


@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.goalChoicePage(completeGoal: (WorkoutPlanGoal) -> Unit){
    val goalImages = mapOf(
        WorkoutPlanGoal.HYPERTROPHY to R.drawable.cable_curl,
        WorkoutPlanGoal.STRENGTH to R.drawable.headstand_push_up,
        WorkoutPlanGoal.ENDURANCE to R.drawable.plank,
        WorkoutPlanGoal.CARDIO to R.drawable.sit_ups
    )
    stickyHeader {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.what_is_your_goal_when_training),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    items(goalImages.size) { index ->
        val goal = goalImages.keys.elementAt(index)
        val image = goalImages.values.elementAt(index)
        ElevatedCard(
            onClick = {
                completeGoal(goal)
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            AsyncImage(
                model = image,
                contentDescription = stringResource(R.string.goal_i_image, goal),
//                contentScale = ContentScale.FillWidth,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Text(
                text = stringResource(goal.descResource),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    item {
        Spacer(Modifier.height(0.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.expertiseLevelPage(completeExpertise: (WorkoutPlanDifficulty) -> Unit) {
    val expertiseImages = mapOf(
        WorkoutPlanDifficulty.BEGINNER to R.drawable.chest_press,
        WorkoutPlanDifficulty.INTERMEDIATE to R.drawable.deadlift,
        WorkoutPlanDifficulty.ADVANCED to R.drawable.muscle_up
    )
    stickyHeader {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.what_is_your_expertise_level),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    items(expertiseImages.size) { index ->
        val level = expertiseImages.keys.elementAt(index)
        val image = expertiseImages.values.elementAt(index)
        ElevatedCard(
            onClick = {
                completeExpertise(level)
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            AsyncImage(
                model = image,
                contentDescription = stringResource(R.string.goal_i_image, level),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Text(
                text = stringResource(level.expertiseResource),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    item {
        Spacer(Modifier.height(0.dp))
    }
}


@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.workoutSplitPage(completeSplit: (WorkoutPlanSplit) -> Unit) {
    val workoutImages = mapOf(
        WorkoutPlanSplit.BRO to R.drawable.bench_press,
        WorkoutPlanSplit.GAINZ to R.drawable.generic_barbell,
        WorkoutPlanSplit.FULL_BODY to R.drawable.generic_machine,
        WorkoutPlanSplit.UPPER_LOWER to R.drawable.chest_dip,
    )

    stickyHeader {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.how_many_times_per_week_do_you_want_to_exercise),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    items(workoutImages.size) { index ->
        val split = workoutImages.keys.elementAt(index)
        val image = workoutImages.values.elementAt(index)
        ElevatedCard(
            onClick = {
                completeSplit(split)
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            AsyncImage(
                model = image,
                contentDescription = stringResource(R.string.goal_i_image, split),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Text(
                text = stringResource(split.splitResource),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    item {
        Spacer(Modifier.height(0.dp))
    }
}