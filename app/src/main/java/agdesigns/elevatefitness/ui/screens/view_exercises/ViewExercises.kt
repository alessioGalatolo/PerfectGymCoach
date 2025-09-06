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
import agdesigns.elevatefitness.navigation.ChangePlanGraph
import agdesigns.elevatefitness.navigation.SlideTransition
import agdesigns.elevatefitness.ui.common.ExercisesEvent
import agdesigns.elevatefitness.ui.common.ExercisesState
import agdesigns.elevatefitness.ui.common.ExercisesViewModel
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddExerciseDialogDestination
import com.ramcosta.composedestinations.generated.destinations.CreateExerciseDialogDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.agdesignes.shared.Equipment
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.ceil

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

    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    LaunchedEffect(textFieldState.text) {
        viewModel.onEvent(ExercisesEvent.FilterExercise(
            textFieldState.text.toString()
        ))
    }
    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(
        enabled = exercisesState.searchQuery.isNotBlank(),
    ) { backFlow ->
        try {
            backFlow.collect { back ->
                backProgress = back.progress
            }
            textFieldState.clearText()
            backProgress = 0f
        } catch (e: CancellationException) {
            backProgress = 0f
        }

    }

    val toFocus = rememberSaveable { mutableStateOf(focusSearch) }

    viewModel.onEvent(ExercisesEvent.GetExercises(Exercise.Muscle.entries[muscleOrdinal]))

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Exercise.Muscle.entries[muscleOrdinal].muscleNameResource)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back_icon)
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

                        val inputField =
                            @Composable {
                                SearchBarDefaults.InputField(
                                    outputTransformation = PredictiveBackOutputTransformation(
                                        backProgress
                                    ),
                                    searchBarState = searchBarState,
                                    textFieldState = textFieldState,
                                    onSearch = {
                                        scope.launch {
                                            searchBarState.animateToCollapsed()
                                            showSearchResultOnMainScreen = true
                                        }
                                    },
                                    placeholder = { Text(stringResource(R.string.search_exercise)) },
                                    leadingIcon = {
                                        if (searchBarState.currentValue == SearchBarValue.Expanded) {
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        searchBarState.animateToCollapsed()
                                                        showSearchResultOnMainScreen = false
                                                } }
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Default.ArrowBack,
                                                    contentDescription = stringResource(R.string.go_back_icon)
                                                )
                                            }
                                        } else {
                                            Icon(Icons.Default.Search, contentDescription = stringResource(
                                                R.string.search_icon
                                            ))
                                        }
                                    },
                                    trailingIcon = {
                                        if (textFieldState.text.isNotEmpty()) {
                                            IconButton(onClick = {
                                                textFieldState.clearText()
                                                showSearchResultOnMainScreen = false
                                            }) {
                                                Icon(Icons.Default.Close, contentDescription = stringResource(
                                                    R.string.close_icon_clear_textfield
                                                ))
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
                                Icon(Icons.Default.Add, stringResource(R.string.create_a_new_exercise))
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.create_a_new_exercise))
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                    }
                }
                AnimatedVisibility(
                    visible = isLongPressing.value,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Image(painterResource(id = longPressImage.intValue),
                        stringResource(R.string.bigger_exercise_image),
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
                label = { Text(stringResource(equipment.equipmentNameResource)) },
                leadingIcon = if (equipment == exercisesState.equipToFiler) {
                    {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = stringResource(R.string.done_icon_item_selected),
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
            contentDescription = stringResource(R.string.exercise_image),
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } / 4)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(12.dp))
        )
        Column (Modifier.padding(8.dp)){
            Text(text = exercise.name, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = buildAnnotatedString {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(stringResource(R.string.primary_muscle))
                }
                append(stringResource(exercise.primaryMuscle.muscleNameResource))
            })
            if (exercise.secondaryMuscles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                // don't simplify call chain, won't be able to use stringResource
                val secondaryMuscles = exercise.secondaryMuscles.map {
                    stringResource(it.muscleNameResource)
                }.joinToString(
                    ", "
                )
                Text(text = buildAnnotatedString {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(stringResource(R.string.secondary_muscles))
                    }
                    append(secondaryMuscles)
                })
            }
            // TODO: add option to have variations already expanded
            if (exercise.variations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row (Modifier.fillMaxWidth()){
                    Text(text = stringResource(
                        R.string.i_variations_available,
                        exercise.variations.size
                    ),
                        fontStyle = FontStyle.Italic
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

data class PredictiveBackOutputTransformation(private val backProgress: Float) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        val removeLength = ceil(length * backProgress).toInt()
        if (removeLength > 0) {
            delete(length - removeLength, length)
        }
    }
}