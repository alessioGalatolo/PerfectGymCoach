package agdesigns.elevatefitness.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.exercise.Exercise
import agdesigns.elevatefitness.ui.ChangePlanGraph
import agdesigns.elevatefitness.ui.SlideTransition
import agdesigns.elevatefitness.viewmodels.ExercisesEvent
import agdesigns.elevatefitness.viewmodels.ExercisesViewModel
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.core.content.ContextCompat.startActivity
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.AddExerciseDialogDestination
import com.ramcosta.composedestinations.generated.destinations.CreateExerciseDialogDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.android.awaitFrame
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

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
    var searchText by rememberSaveable { mutableStateOf("") }
    // TODO: it would be really nice to have this as the predictivebackhandler
    BackHandler (exercisesState.searchQuery.isNotBlank()) {
        viewModel.onEvent(ExercisesEvent.FilterExercise(""))
        searchText = ""
    }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val toFocus = rememberSaveable { mutableStateOf(focusSearch) }

    val context = LocalContext.current

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
            val haptic = LocalHapticFeedback.current
            var isLongPressing by remember { mutableStateOf(false) }
            var longPressImage by remember { mutableIntStateOf(R.drawable.finish_workout) }

            // fixme: padding should be of box but items do not go under the navigation bar in that case
            Box (contentAlignment = Center) {
                LazyColumn(
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item{
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            // FIXME: doesn't look good right now
                            tonalElevation = NavigationBarDefaults.Elevation,  // should use card elevation but it is private
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .padding(horizontal = 16.dp)
                        ){
                            Row(verticalAlignment = CenterVertically,
                                modifier = Modifier.fillMaxWidth()) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                TextField(
                                    value = searchText,
                                    onValueChange = {
                                        searchText = it
                                        viewModel.onEvent(ExercisesEvent.FilterExercise(searchText))
                                    },
                                    placeholder = { Text("Search exercise") },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Search
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            viewModel.onEvent(ExercisesEvent.FilterExercise(searchText))
                                            keyboardController?.hide()
                                        }
                                    ), modifier = Modifier
                                        .focusRequester(focusRequester)
                                        .onFocusChanged {
                                            if (it.isFocused) {
                                                keyboardController?.show()
                                            }
                                        }
                                        .weight(0.8f)
                                )
                                IconButton(onClick = {
                                    searchText = ""
                                    viewModel.onEvent(ExercisesEvent.FilterExercise(searchText))
                                }){
                                    Icon(Icons.Default.Close, "Delete current text in search",
                                        tint = if (searchText.isBlank()) Color.Transparent else LocalContentColor.current,
                                        modifier = Modifier
                                            .padding(end = 16.dp, top = 8.dp, bottom = 8.dp)
                                            .weight(0.1f)
                                    )
                                }
                            }
                            if (toFocus.value){
                                LaunchedEffect(focusRequester) {
                                    // FIXME
                                    awaitFrame()
                                    awaitFrame()
                                    awaitFrame()
                                    awaitFrame()
                                    focusRequester.requestFocus()
                                    toFocus.value = false
                                }
                            }
                        }
                    }
                    item {
                        LazyRow (horizontalArrangement = Arrangement.spacedBy(8.dp)){
                            item{
                                Spacer(Modifier.width(8.dp))
                            }
                            itemsIndexed(items = Exercise.Equipment.entries.drop(1), { _, it -> it.ordinal }){ index, equipment ->
                                FilterChip(
                                    selected = equipment == exercisesState.equipToFiler,
                                    onClick = {
                                        if (equipment != exercisesState.equipToFiler) {
                                            viewModel.onEvent(ExercisesEvent.FilterExerciseEquipment(equipment))
                                        } else {
                                            viewModel.onEvent(ExercisesEvent.FilterExerciseEquipment(Exercise.Equipment.EVERYTHING))
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
                            item{
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                    }
                    items(exercisesState.exercisesToDisplay ?: emptyList(),
                        key = { it.name }
                    ) { exercise ->
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressing by interactionSource.collectIsPressedAsState()
                        LaunchedEffect(isPressing) {
                            longPressImage = exercise.image
                            isLongPressing = isPressing
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
                                        if (exercise.name == "Can you hear the silence?") {
                                            startActivity(
                                                context,
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    "https://www.tiktok.com/@poet_jenix/video/7111621457775561989".toUri()  // FIXME
                                                ),
                                                null
                                            )
                                        } else {
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

    //                                    // TODO: on click show variations with an add button on their right
    //                                    IconButton(onClick = { /*TODO*/ }) {
    //                                        Icon(Icons.Default.ArrowDownward, null)
    //                                    }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
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
                    visible = isLongPressing,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Image(painterResource(id = longPressImage), "Bigger exercise image",
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
