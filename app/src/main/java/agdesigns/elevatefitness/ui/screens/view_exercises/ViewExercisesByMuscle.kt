package agdesigns.elevatefitness.ui.screens.view_exercises

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import agdesigns.elevatefitness.navigation.ChangePlanGraph
import agdesigns.elevatefitness.navigation.SlideTransition
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.res.stringResource
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.ViewExercisesDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination<ChangePlanGraph>(style = SlideTransition::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExercisesByMuscle(
    navigator: DestinationsNavigator,
    programName: String,
    programId: Long = 0,
    workoutId: Long = 0,
    successfulAddExercise: Boolean = false,
    returnAfterAdding: Boolean = false // if adding a single exercise to workout, return to workout instead of program
) {
    // scroll behaviour for top bar
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val showSnackbar = rememberSaveable { mutableStateOf(successfulAddExercise) }
    val snackbarText = stringResource(R.string.snackbar_exercise_added)
    LaunchedEffect(showSnackbar){
        if (showSnackbar.value){
            if (!returnAfterAdding) {
                snackbarHostState.showSnackbar(snackbarText)
                showSnackbar.value = false
            } else {
                navigator.navigateUp()
            }
        }
    }


    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(title = { Text(stringResource(
                R.string.add_exercise_to_i,
                getProgramDisplayName(programName)
            )) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back_icon)
                        )
                    }
                }
            )
        }, content = { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.card_space_between)),
                modifier = Modifier.padding(horizontal = 16.dp)) {
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
                                    muscleOrdinal = Exercise.Muscle.EVERYTHING.ordinal,
                                    focusSearch = true,
                                    programName = programName,
                                    returnAfterAdding = returnAfterAdding
                                )
                            )
                        }
                    }
                    SearchBarDefaults.InputField(
                        modifier = Modifier.padding(vertical = 16.dp),
                        searchBarState = searchBarState,
                        textFieldState = textFieldState,
                        readOnly = true,
                        onSearch = { navigator.navigate(
                            ViewExercisesDestination(
                                programId = programId,
                                workoutId = workoutId,
                                muscleOrdinal = Exercise.Muscle.EVERYTHING.ordinal,
                                focusSearch = true,
                                programName = programName,
                                returnAfterAdding = returnAfterAdding
                            )
                        ) },
                        placeholder = { Text(stringResource(R.string.search_exercise)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    )
                }
                items(items = Exercise.Muscle.entries.toTypedArray(), key = { it.ordinal }) {
                    Card(
                        onClick = {
                            navigator.navigate(
                                ViewExercisesDestination(
                                    programId = programId,
                                    workoutId = workoutId,
                                    muscleOrdinal = it.ordinal,
                                    programName = programName,
                                    returnAfterAdding = returnAfterAdding
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row (Modifier.padding(dimensionResource(R.dimen.card_inner_padding))){
                            Image(
                                painter = painterResource(it.imageRes),
                                contentDescription = stringResource(R.string.image_highlighting_the_muscle),
                                modifier = Modifier
                                    // Set image size to 40 dp
                                    .size(80.dp)
                                    // Clip image to be shaped as a circle
                                    .clip(CircleShape)
                            )


                            Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                                Text(text = stringResource(it.muscleNameResource), fontWeight = FontWeight.Bold)
//                                Spacer(modifier = Modifier.height(4.dp))
//                                Text(text = "Some exercise names...") // TODO
                            }
                        }
                    }
                }
                item {
                    // We want 16.dp at the bottom, we have 8 from here and 8 from spacedBy
                    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.card_space_between)))
                }
            }
        }
    )
}