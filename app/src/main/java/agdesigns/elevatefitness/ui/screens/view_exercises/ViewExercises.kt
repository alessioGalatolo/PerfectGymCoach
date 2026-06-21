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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Exercise
import agdesigns.elevatefitness.navigation.AddExerciseDialogDestination
import agdesigns.elevatefitness.navigation.CreateExerciseDialogDestination
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import agdesigns.elevatefitness.ui.common.GroupedCard
import agdesigns.elevatefitness.ui.common.SharedElementKey
import agdesigns.elevatefitness.ui.common.SharedElementType
import agdesigns.elevatefitness.ui.screens.create_exercise.getEquipmentIcon
import agdesigns.elevatefitness.ui.screens.create_exercise.getEquipmentImage
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.delete
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import agdesigns.elevatefitness.shared.Equipment
import agdesigns.elevatefitness.ui.common.lazyGroupedCard
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.ceil
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun SharedTransitionScope.ViewExercises(
    animatedVisibilityScope: AnimatedVisibilityScope,
    navigator: DestinationsNavigator,
    programId: Long = 0L,
    workoutId: Long = 0L,
    muscleOrdinal: Int,
    focusSearch: Boolean = false,
    programName: String = "",
    returnAfterAdding: Boolean = false,
    insertAtPosition: Int? = null,
    viewModel: ExercisesViewModel = hiltViewModel()
) {
    val sharedTransitionScope: SharedTransitionScope = this
    val exercisesState by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    // i.e., when use hits "search"
    var showSearchResultOnMainScreen by rememberSaveable { mutableStateOf(false) }
    val exercisesOnMainScreen = if (showSearchResultOnMainScreen)
        exercisesState.searchResults?.map{ it.exercise }
    else
        exercisesState.exercisesFilterEquip

    val searchBarState = rememberSearchBarState()

    var backProgress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler(
        enabled = exercisesState.searchQuery.isNotBlank(),
    ) { backFlow ->
        try {
            backFlow.collect { back ->
                backProgress = (back.progress * 2f).coerceIn(0f, 1f)
            }
            showSearchResultOnMainScreen = false
            viewModel.searchFieldState.clearText()
            backProgress = 0f
        } catch (e: CancellationException) {
            backProgress = 0f
        }
    }

    /*
    If user is coming back from a screen with a transition and tries to go back rapidly
    the old screen will flash. This feels like a bug for compose to solve but until then,
    we disallow going back until the transition is finished

    EDIT: seems to be better with nav3
     */
//    val running = this@ViewExercises.isTransitionActive
//    BackHandler(enabled = running) { }

    val toFocus = rememberSaveable { mutableStateOf(focusSearch) }
    LaunchedEffect(muscleOrdinal) {
        viewModel.onEvent(ExercisesEvent.GetExercises(Exercise.Muscle.entries[muscleOrdinal]))
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = { Text(stringResource(Exercise.Muscle.entries[muscleOrdinal].muscleNameResource)) },
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
                },
                scrollBehavior = scrollBehavior
            )
        }, content = { innerPadding ->
            var isLongPressing = remember { mutableStateOf(false) }
            var longPressImage = remember { mutableIntStateOf(R.drawable.finish_workout) }

            Box (contentAlignment = Center) {
                LazyColumn(
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item{

                        val inputField =
                            @Composable {
                                SearchBarDefaults.InputField(
                                    // TODO: capitalize first char
//                                    inputTransformation = InputTransformation {
//
//                                    },
                                    colors = SearchBarDefaults.inputFieldColors(
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    outputTransformation = PredictiveBackOutputTransformation(
                                        backProgress
                                    ),
                                    searchBarState = searchBarState,
                                    textFieldState = viewModel.searchFieldState,
                                    onSearch = {
                                        scope.launch {
                                            searchBarState.animateToCollapsed()
                                            showSearchResultOnMainScreen = true
                                            viewModel.onEvent(
                                                ExercisesEvent.AddRecentSearch(
                                                    viewModel.searchFieldState.text.toString()
                                                )
                                            )
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
                                        if (viewModel.searchFieldState.text.isNotEmpty()) {
                                            IconButton(onClick = {
                                                viewModel.searchFieldState.clearText()
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
                        SearchBar(
                            state = searchBarState,
                            inputField = inputField,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        ExpandedFullScreenSearchBar(
                            colors = SearchBarDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            state = searchBarState,
                            inputField = inputField
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                EquipmentFilterChips(
                                    exercisesState = exercisesState,
                                    filterExerciseEquipment = { equipment ->
                                        viewModel.onEvent(
                                            ExercisesEvent.FilterExerciseEquipment(
                                                equipment
                                            )
                                        )
                                    }
                                )
                                val lazyListState = rememberLazyListState()
                                LaunchedEffect(exercisesState.searchResults) {
                                    scope.launch {
                                        lazyListState.animateScrollToItem(0)
                                    }
                                }
                                val recentSearchesCardColor = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                                LazyColumn(
                                    state = lazyListState,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (exercisesState.recentSearches.isNotEmpty()) {
                                        item {
                                            // show recent searches
                                            Text(
                                                stringResource(R.string.recent_searches),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .padding(horizontal = 16.dp)
                                                    .padding(top = 16.dp)
                                            )
                                        }
                                        item {
                                            GroupedCard(
                                                colors = recentSearchesCardColor,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            ) {
                                                val maxSuggestions =
                                                    if (exercisesState.searchResults != null && exercisesState.searchResults!!.isNotEmpty()) {
                                                        min(2, exercisesState.recentSearches.size)
                                                    } else {
                                                        exercisesState.recentSearches.size
                                                    }
                                                exercisesState.recentSearches.subList(
                                                    0,
                                                    maxSuggestions
                                                ).forEach {
                                                    subCard(onClick = {
                                                        viewModel.searchFieldState.edit {
                                                            replace(
                                                                0,
                                                                viewModel.searchFieldState.text.length,
                                                                it
                                                            )
                                                        }
                                                        showSearchResultOnMainScreen = true
                                                        scope.launch {
                                                            searchBarState.animateToCollapsed()
                                                        }
                                                    }) {
                                                        Row(Modifier.fillMaxWidth()) {
                                                            Icon(
                                                                Icons.Default.History,
                                                                stringResource(R.string.history_icon_recent_search)
                                                            )
                                                            Spacer(Modifier.width(8.dp))
                                                            Text(it)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (exercisesState.searchResults != null && exercisesState.searchResults!!.isNotEmpty()) {
                                        item {
                                            Text(
                                                stringResource(R.string.results),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                    items(
                                        exercisesState.searchResults ?: emptyList(),
                                        key = { it.exercise.exerciseId }
                                    ) { result ->
                                        ExerciseCard(
                                            result.exercise,
                                            result,
                                            longPressImage,
                                            isLongPressing,
                                        ) {
                                            scope.launch {
                                                // Going directly to other screen from expanded search
                                                // can create problems with back and transitions, go back first
                                                searchBarState.animateToCollapsed()
                                                showSearchResultOnMainScreen = true
                                                viewModel.onEvent(
                                                    ExercisesEvent.AddRecentSearch(
                                                        viewModel.searchFieldState.text.toString()
                                                    )
                                                )
                                                navigator.navigate(
                                                    AddExerciseDialogDestination(
                                                        previewExercise = result.exercise,
                                                        programId = programId,
                                                        workoutId = workoutId,
                                                        insertAtPosition = insertAtPosition,
                                                        returnAfterAdding = returnAfterAdding,
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    item {
                                        CreateNewExerciseButton {
                                            navigator.navigate(
                                                CreateExerciseDialogDestination(
                                                    muscleOrdinal = muscleOrdinal,
                                                    filterEquipment = exercisesState.equipToFiler
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
                    itemsIndexed(exercisesOnMainScreen ?: emptyList(),
                        key = { _, it -> it.exerciseId }
                    ) { index, exercise ->
                        val result = if (showSearchResultOnMainScreen) exercisesState.searchResults!![index] else null
                        ExerciseCard(
                            exercise,
                            result,
                            longPressImage,
                            isLongPressing,
                            sharedTransitionScope,
                            animatedVisibilityScope
                        ) {
                            navigator.navigate(
                                AddExerciseDialogDestination(
                                    previewExercise = exercise,
                                    programId = programId,
                                    workoutId = workoutId,
                                    insertAtPosition = insertAtPosition,
                                    returnAfterAdding = returnAfterAdding,
                                )
                            )
                        }
                    }
                    item {
                        CreateNewExerciseButton {
                            navigator.navigate(
                                CreateExerciseDialogDestination(
                                    muscleOrdinal = muscleOrdinal,
                                    filterEquipment = exercisesState.equipToFiler
                                )
                            )
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
            val equipmentIcon = getEquipmentIcon(equipment)
            val equipmentImage = getEquipmentImage(equipment)
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
                    if (equipmentIcon != null) {
                        {
                            Icon(
                                equipmentIcon,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        {
                            AsyncImage(
                                model = equipmentImage,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            )
        }
        item {
            Spacer(Modifier.width(8.dp))
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Suppress("SimplifiableCallChain")
@Composable
fun LazyItemScope.ExerciseCard(
    exercise: Exercise,
    result: ExerciseSearchResult?, // if user is searching, highlight this text
    longPressImage: MutableIntState,
    isLongPressing: MutableState<Boolean>,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onExerciseClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressing by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressing) {
        longPressImage.intValue = exercise.image
        isLongPressing.value = isPressing
    }
    with (sharedTransitionScope) {
        val sharedCardModifier = this?.let {
            Modifier.sharedBounds(
                rememberSharedContentState(
                    SharedElementKey(
                        "AddExerciseDialog",
                        SharedElementType.Bounds,
                        idLong = exercise.exerciseId
                    )
                ),
                animatedVisibilityScope!!,
                boundsTransform = BoundsTransform { _, _ ->
                    MotionScheme.expressive().slowSpatialSpec()
                }
            )
        } ?: Modifier
        val sharedTextModifier = this?.let {
            Modifier.sharedElement(
                rememberSharedContentState(
                    SharedElementKey(
                        "AddExerciseDialog",
                        SharedElementType.Title,
                        idLong = exercise.exerciseId
                    )
                ),
                animatedVisibilityScope!!,
                boundsTransform = { _, _ ->
                    MotionScheme.expressive().slowSpatialSpec()
                }
            )
        } ?: Modifier
        val sharedImageModifier = this?.let {
            Modifier.sharedElement(
                rememberSharedContentState(
                    SharedElementKey(
                        "AddExerciseDialog",
                        SharedElementType.Image,
                        idLong = exercise.exerciseId
                    )
                ),
                animatedVisibilityScope!!,
                boundsTransform = { _, _ ->
                    MotionScheme.expressive().slowSpatialSpec()
                }
            )
        } ?: Modifier



        ElevatedCard(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .animateItem()
                // when clipping we remove elevation, need to add shadow again
                .shadow(1.dp, CardDefaults.shape)
                .clip(CardDefaults.shape)
                .then(sharedCardModifier)
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
                    .then(sharedImageModifier)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(Modifier.padding(8.dp)) {
                val nameRanges = result?.highlights
                    ?.filter { it.field == SearchField.Name && it.index == null }
                    ?.flatMap { it.ranges }
                Text(
                    text = getTextWithSearchHighlight(exercise.name, nameRanges),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = sharedTextModifier
                )
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
                if (exercise.variations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.i_variations_available,
                            exercise.variations.size
                        ) + ": ",
                        fontStyle = FontStyle.Italic
                    )
                    exercise.variations.forEachIndexed { index, variation ->
                        val vRanges = result?.highlights
                            ?.filter { it.field == SearchField.Variation && it.index == index }
                            ?.flatMap { it.ranges }
                        Text(text = getTextWithSearchHighlight(variation, vRanges))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateNewExerciseButton(onCreateClick: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Button(
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.align(Center),
            onClick = onCreateClick
        ) {
            Icon(Icons.Default.Add, stringResource(R.string.create_a_new_exercise))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.create_a_new_exercise))
        }
    }
}

@Composable
fun getTextWithSearchHighlight(
    text: String,
    ranges: List<IntRange>?,
    highlightStyle: SpanStyle = SpanStyle(
        fontWeight = FontWeight.SemiBold,
        background = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        color = MaterialTheme.colorScheme.onSecondaryContainer
    )
): AnnotatedString {
    if (text.isEmpty() || ranges?.isEmpty() ?: true) return AnnotatedString(text)

    val max = text.length

    // 1) Clamp to [0, length] and convert inclusive IntRange to [start, endExclusive)
    val intervals = ranges.mapNotNull { r ->
        val start = r.first.coerceAtLeast(0)
        val endExclusive = (r.last + 1).coerceAtMost(max) // +1 because IntRange is inclusive
        if (start >= endExclusive) null else start to endExclusive
    }.sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })

    if (intervals.isEmpty()) return AnnotatedString(text)

    // 2) Merge overlapping/adjacent intervals so we never double-style or step backward
    val merged = mutableListOf<Pair<Int, Int>>()
    for ((s, e) in intervals) {
        if (merged.isEmpty() || s > merged.last().second) {
            merged += s to e
        } else {
            val last = merged.removeAt(merged.lastIndex)
            merged += last.first to maxOf(last.second, e)
        }
    }

    // 3) Build the annotated string safely
    val b = AnnotatedString.Builder()
    var cursor = 0
    for ((s, e) in merged) {
        if (cursor < s) b.append(text.substring(cursor, s))
        b.pushStyle(highlightStyle)
        b.append(text.substring(s, e))
        b.pop()
        cursor = e
    }
    if (cursor < max) b.append(text.substring(cursor, max))
    return b.toAnnotatedString()
}



data class PredictiveBackOutputTransformation(private val backProgress: Float) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        val removeLength = ceil(length * backProgress).toInt()
        if (removeLength > 0) {
            delete(length - removeLength, length)
        }
    }
}
