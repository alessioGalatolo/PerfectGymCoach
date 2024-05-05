package agdesigns.elevatefitness.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.ripple.rememberRipple
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
import agdesigns.elevatefitness.data.exercise.Exercise
import agdesigns.elevatefitness.ui.ChangePlanGraph
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.ViewExercisesDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination<ChangePlanGraph>
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExercisesByMuscle(
    navigator: DestinationsNavigator,
    programName: String,
    programId: Long = 0,
    workoutId: Long = 0,
    successfulAddExercise: Boolean = false
) {
    // scroll behaviour for top bar
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    val snackbarHostState = remember { SnackbarHostState() }

    val showSnackbar = rememberSaveable { mutableStateOf(successfulAddExercise) }
    LaunchedEffect(showSnackbar){
        if (showSnackbar.value){
            snackbarHostState.showSnackbar("Exercise added successfully, you can continue adding")
            showSnackbar.value = false
        }
    }


    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(title = { Text("Add exercise to $programName") },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
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
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = NavigationBarDefaults.Elevation,  // should use card elevation but it is private
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = rememberRipple(),
                                    onClick = {
                                        navigator.navigate(
                                            ViewExercisesDestination(
                                                programId = programId,
                                                workoutId = workoutId,
                                                muscleOrdinal = Exercise.Muscle.EVERYTHING.ordinal,
                                                focusSearch = true
                                            ),
                                            onlyIfResumed = true
                                        )
                                    }
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    top = 8.dp,
                                    bottom = 8.dp
                                ),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Search exercise",
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(16.dp),
//                            style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                items(items = Exercise.Muscle.entries.toTypedArray(), key = { it.ordinal }) {
                    Card(
                        onClick = {
                            navigator.navigate(
                                ViewExercisesDestination(
                                    programId = programId,
                                    workoutId = workoutId,
                                    muscleOrdinal = it.ordinal
                                ),
                                onlyIfResumed = true
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row (Modifier.padding(dimensionResource(R.dimen.card_inner_padding))){
                            Image(
                                painter = painterResource(it.image),
                                contentDescription = "Contact profile picture",
                                modifier = Modifier
                                    // Set image size to 40 dp
                                    .size(80.dp)
                                    // Clip image to be shaped as a circle
                                    .clip(CircleShape)
                            )


                            Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                                Text(text = it.muscleName, fontWeight = FontWeight.Bold)
//                                Spacer(modifier = Modifier.height(4.dp))
//                                Text(text = "Some exercise names...") // TODO
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    )
}