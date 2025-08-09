package agdesigns.elevatefitness.ui.screens.view_exercises

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.data.db.entity.Exercise.Equipment
import agdesigns.elevatefitness.navigation.ChangePlanGraph
import agdesigns.elevatefitness.navigation.SlideTransition
import agdesigns.elevatefitness.ui.common.ExercisesEvent
import agdesigns.elevatefitness.ui.common.ExercisesState
import agdesigns.elevatefitness.ui.common.ExercisesViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddExerciseDialogDestination
import com.ramcosta.composedestinations.generated.destinations.CreateExerciseDialogDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch

@Destination<ChangePlanGraph>(style = SlideTransition::class)
@OptIn(ExperimentalMaterial3Api::class
)
@Composable
fun ViewExercises(
    navigator: DestinationsNavigator,
    programId: Long = 0L,
    workoutId: Long = 0L,
    muscleOrdinal: Int,
    focusSearch: Boolean = false,
    programName: String = "",
    returnAfterAdding: Boolean = false,
    viewModel: ExercisesViewModel = hiltViewModel()
) {
    val exercisesState by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    // i.e., when use hits "search"
    var showSearchResultOnMainScreen by remember { mutableStateOf(false) }
    val exercisesOnMainScreen by remember {
        derivedStateOf {
            if (showSearchResultOnMainScreen)
                exercisesState.exercisesToDisplay
            else
                exercisesState.exercisesFilterEquip
        }
    }
    var searchText by rememberSaveable { mutableStateOf("") }
    // TODO: it would be really nice to have this as the predictivebackhandler
    BackHandler (exercisesState.searchQuery.isNotBlank()) {
        viewModel.onEvent(ExercisesEvent.FilterExercise(""))
        searchText = ""
    }

    val toFocus = rememberSaveable { mutableStateOf(focusSearch) }

    viewModel.onEvent(ExercisesEvent.GetExercises(Exercise.Muscle.entries[muscleOrdinal]))

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(Exercise.Muscle.entries[muscleOrdinal].muscleName) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }, content = { innerPadding ->
            var isLongPressing = remember { mutableStateOf(false) }
            var longPressImage = remember { mutableIntStateOf(R.drawable.finish_workout) }

            // fixme: padding should be of box but items do not go under the navigation bar in that case
            Box (contentAlignment = Center) {
                LazyColumn(
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item{
                        val searchBarState = rememberSearchBarState()
                        val textFieldState = rememberTextFieldState()

                        LaunchedEffect(textFieldState.text) {
                            searchText = textFieldState.text.toString()
                            viewModel.onEvent(ExercisesEvent.FilterExercise(searchText))
                        }

                        val inputField =
                            @Composable {
                                SearchBarDefaults.InputField(
                                    modifier = Modifier,
                                    searchBarState = searchBarState,
                                    textFieldState = textFieldState,
                                    onSearch = {
                                        scope.launch {
                                            searchBarState.animateToCollapsed()
                                            showSearchResultOnMainScreen = true
                                        }
                                    },
                                    placeholder = { Text("Search exercise...") },
                                    leadingIcon = {
                                        if (searchBarState.currentValue == SearchBarValue.Expanded) {
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        searchBarState.animateToCollapsed()
                                                        showSearchResultOnMainScreen = false
                                                } }
                                            ) {
                                                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                                            }
                                        } else {
                                            Icon(Icons.Default.Search, contentDescription = null)
                                        }
                                    },
                                    trailingIcon = {
                                        if (textFieldState.text.isNotEmpty()) {
                                            IconButton(onClick = {
                                                textFieldState.clearText()
                                                showSearchResultOnMainScreen = false
                                            }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear")
                                            }
                                        }
                                    }
                                )
                            }
                        // TODO: store recent searches
                        SearchBar(state = searchBarState, inputField = inputField, modifier = Modifier.padding(horizontal = 16.dp))
                        ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
                            EquipmentFilterChips(
                                exercisesState = exercisesState,
                                filterExerciseEquipment = { equipment ->
                                    viewModel.onEvent(ExercisesEvent.FilterExerciseEquipment(equipment))
                                }
                            )
                            val lazyListState = rememberLazyListState()
                            LaunchedEffect(exercisesState.exercisesToDisplay) {
                                scope.launch {
                                    lazyListState.animateScrollToItem(0)
                                }
                            }
                            if (textFieldState.text.isNotEmpty()) {
                                LazyColumn(state = lazyListState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(
                                        exercisesState.exercisesToDisplay ?: emptyList(),
                                        key = { it.name }
                                    ) { exercise ->
                                        ExerciseCard(exercise, longPressImage, isLongPressing) {
                                            navigator.navigate(
                                                AddExerciseDialogDestination(
                                                    programId = programId,
                                                    workoutId = workoutId,
                                                    exerciseId = exercise.exerciseId,
                                                    programName = programName,
                                                    returnAfterAdding = returnAfterAdding
                                                )
                                            )
                                        }
                                    }
                                    item {
                                        Spacer(Modifier.height(16.dp))
                                    }
                                }
                            }
                        }
                        LaunchedEffect(toFocus) {
                            if (toFocus.value) {
                                toFocus.value = false
                                scope.launch {
                                    searchBarState.animateToExpanded()
                                }
                            }
                        }
                    }
                    item {
                        EquipmentFilterChips(
                            exercisesState = exercisesState,
                            filterExerciseEquipment = { equipment ->
                                viewModel.onEvent(ExercisesEvent.FilterExerciseEquipment(equipment))
                            }
                        )
                    }
                    items(exercisesOnMainScreen ?: emptyList(),
                        key = { it.name }
                    ) { exercise ->
                        ExerciseCard(exercise, longPressImage, isLongPressing) {
                            navigator.navigate(
                                AddExerciseDialogDestination(
                                    programId = programId,
                                    workoutId = workoutId,
                                    exerciseId = exercise.exerciseId,
                                    programName = programName,
                                    returnAfterAdding = returnAfterAdding
                                )
                            )
                        }
                    }
                    item {
                        Box(Modifier.fillMaxWidth()) {
                            Button(
                                modifier = Modifier.align(Center),
                                onClick = {
                                    navigator.navigate(
                                        CreateExerciseDialogDestination()
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Add, "Create exercise")
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text("Create exercise")
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.navigationBarsPadding())
                    }
                }
                AnimatedVisibility(
                    visible = isLongPressing.value,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Image(painterResource(id = longPressImage.intValue), "Bigger exercise image",
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)))
//                    AsyncImage(
//                        model = ImageRequest.Builder(LocalContext.current)
//                            .data(longPressImage)
//                            .crossfade(true)
//                            .build(),
//                        contentScale = ContentScale.FillWidth,
//                        contentDescription = null,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .clip(RoundedCornerShape(12.dp))
//                    )
                }
            }
        })
}

@Composable
fun EquipmentFilterChips(
    exercisesState: ExercisesState,
    filterExerciseEquipment: (Equipment) -> Unit
){
    LazyRow (horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Spacer(Modifier.width(8.dp))
        }
        itemsIndexed(
            items = Equipment.entries.drop(1),
            { _, it -> it.ordinal }) { index, equipment ->
            FilterChip(
                selected = equipment == exercisesState.equipToFiler,
                onClick = {
                    if (equipment != exercisesState.equipToFiler) {
                       filterExerciseEquipment(equipment)
                    } else {
                        filterExerciseEquipment(Equipment.EVERYTHING)
                    }
                },
                label = { Text(equipment.equipmentName) },
                leadingIcon = if (equipment == exercisesState.equipToFiler) {
                    {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Selected",
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                }
            )
        }
        item {
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
fun LazyItemScope.ExerciseCard(
    exercise: Exercise,
    longPressImage: MutableIntState,
    isLongPressing: MutableState<Boolean>,
    onExerciseClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressing by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressing) {
        longPressImage.intValue = exercise.image
        isLongPressing.value = isPressing
    }
    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .animateItem()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = {
                    onExerciseClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                })
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(exercise.image)
                .crossfade(true)
                .build(),
            contentScale = ContentScale.Crop,
            contentDescription = "Exercise image",
            modifier = Modifier
                .fillMaxWidth()
                .height(with (LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } / 4)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(12.dp))
        )
        Column (Modifier.padding(8.dp)){
            Text(text = exercise.name, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = buildAnnotatedString {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append("Primary muscle: ")
                }
                append(exercise.primaryMuscle.muscleName)
            })
            if (exercise.secondaryMuscles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = buildAnnotatedString {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append("Secondary muscles: ")
                    }
                    append(exercise.secondaryMuscles.joinToString(
                        ", "
                    ) { it.muscleName })
                })
            }
            // TODO: add option to have variations already expanded
            if (exercise.variations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row (Modifier.fillMaxWidth()){
                    Text(text = "${exercise.variations.size}+ variations available",
                        fontStyle = FontStyle.Italic
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}