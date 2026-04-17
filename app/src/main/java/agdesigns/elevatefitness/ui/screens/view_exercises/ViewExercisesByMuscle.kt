package agdesigns.elevatefitness.ui.screens.view_exercises

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.entity.getProgramDisplayName
import agdesigns.elevatefitness.navigation.AddExerciseDialogDestination
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import agdesigns.elevatefitness.navigation.ViewExercisesDestination
import agdesigns.elevatefitness.ui.common.SharedElementGeneralKeys
import agdesigns.elevatefitness.ui.common.lazyGroupedCard
import agdesigns.elevatefitness.utils.plus
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class
)
@Composable
fun SharedTransitionScope.ExercisesByMuscle(
    animatedVisibilityScope: AnimatedVisibilityScope,
    navigator: DestinationsNavigator,
    programName: String,
    programId: Long = 0L,
    workoutId: Long = 0L,
    returnAfterAdding: Boolean = false, // if adding a single exercise to workout, return to workout instead of program
    insertAtPosition: Int? = null,
) {
    // scroll behaviour for top bar
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    val muscleCardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarText = stringResource(R.string.snackbar_exercise_added)
    LaunchedEffect(Unit) {
        val maybeSuccessfulAdd = navigator.consumeResult(
            AddExerciseDialogDestination.ADDITION_OUTCOME_KEY
        )
        if (maybeSuccessfulAdd == true) {
            snackbarHostState.showSnackbar(snackbarText)
        }
    }


    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .sharedBounds(
                rememberSharedContentState(
                    SharedElementGeneralKeys.FAB_TO_VIEW
                ),
                animatedVisibilityScope
            ),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = {
                    Text(stringResource(
                R.string.add_exercise_to_i,
                getProgramDisplayName(programName)
            )) },
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back_icon)
                        )
                    }
                }
            )
        }, content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding + PaddingValues(
                    bottom = dimensionResource(R.dimen.screen_edge_padding)
                ),
            ) {
                item {
                    // search bar
                    val searchBarState = rememberSearchBarState()
                    val textFieldState = rememberTextFieldState()
                    LaunchedEffect(searchBarState.currentValue) {
                        if (searchBarState.currentValue == SearchBarValue.Expanded) {
                            searchBarState.snapTo(0f)
                            navigator.navigate(
                                ViewExercisesDestination(
                                    programId = programId,
                                    workoutId = workoutId,
                                    insertAtPosition = insertAtPosition,
                                    muscleOrdinal = Exercise.Muscle.EVERYTHING.ordinal,
                                    focusSearch = true,
                                    programName = programName,
                                    returnAfterAdding = returnAfterAdding,
                                )
                            )
                        }
                    }
                    SearchBarDefaults.InputField(
                        colors = SearchBarDefaults.inputFieldColors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.padding(dimensionResource(R.dimen.screen_edge_padding)),
                        searchBarState = searchBarState,
                        textFieldState = textFieldState,
                        readOnly = true,
                        onSearch = { navigator.navigate(
                            ViewExercisesDestination(
                                programId = programId,
                                workoutId = workoutId,
                                insertAtPosition = insertAtPosition,
                                muscleOrdinal = Exercise.Muscle.EVERYTHING.ordinal,
                                focusSearch = true,
                                programName = programName,
                                returnAfterAdding = returnAfterAdding,
                            )
                        ) },
                        placeholder = { Text(stringResource(R.string.search_exercise)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                lazyGroupedCard(
                    colors = muscleCardColors,
                    innerCardPadding = 0.dp,
                ) {
                    Exercise.Muscle.entries.forEach { muscle ->
                        subCard(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            onClick = {
                                navigator.navigate(
                                    ViewExercisesDestination(
                                        programId = programId,
                                        workoutId = workoutId,
                                        insertAtPosition = insertAtPosition,
                                        muscleOrdinal = muscle.ordinal,
                                        programName = programName,
                                        returnAfterAdding = returnAfterAdding,
                                    )
                                )
                            }
                        ) {
                            Row (Modifier.padding(dimensionResource(R.dimen.card_inner_padding))){
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surfaceBright
                                ) {
                                    Image(
                                        painter = painterResource(muscle.imageRes),
                                        contentDescription = stringResource(R.string.image_highlighting_the_muscle),
                                        modifier = Modifier
                                            // Set image size to 40 dp
                                            .size(80.dp)
                                            // Clip image to be shaped as a circle
                                            .clip(CircleShape)
                                            .padding(4.dp)
                                    )
                                }

                                Column(modifier = Modifier.align(Alignment.CenterVertically).padding(8.dp)) {
                                    Text(text = stringResource(muscle.muscleNameResource), fontWeight = FontWeight.Bold)
//                                Spacer(modifier = Modifier.height(4.dp))
//                                Text(text = "Some exercise names...") // TODO
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}