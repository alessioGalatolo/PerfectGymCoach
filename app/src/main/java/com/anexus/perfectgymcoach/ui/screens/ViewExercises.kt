package com.anexus.perfectgymcoach.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.Exercise
import com.anexus.perfectgymcoach.ui.ChangePlanNavGraph
import com.anexus.perfectgymcoach.ui.destinations.AddExerciseDialogDestination
import com.anexus.perfectgymcoach.ui.destinations.CreateExerciseDialogDestination
import com.anexus.perfectgymcoach.viewmodels.ExercisesEvent
import com.anexus.perfectgymcoach.viewmodels.ExercisesViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay

@ChangePlanNavGraph
@Destination
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class
)
@Composable
fun ViewExercises(
    navigator: DestinationsNavigator,
    programId: Long = 0L,
    workoutId: Long = 0L,
    muscleOrdinal: Int,
    focusSearch: Boolean = false,
    viewModel: ExercisesViewModel = hiltViewModel()
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val toFocus = rememberSaveable { mutableStateOf(focusSearch) }

    val context = LocalContext.current

    viewModel.onEvent(ExercisesEvent.GetExercises(Exercise.Muscle.values()[muscleOrdinal]))

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(Exercise.Muscle.values()[muscleOrdinal].muscleName) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }, content = { innerPadding ->
            val haptic = LocalHapticFeedback.current
            var isLongPressing by remember { mutableStateOf(false) }
            var longPressImage by remember { mutableStateOf(R.drawable.finish_workout) }

            // fixme: this screen does not go under the navigation bar
            Box (Modifier.padding(innerPadding), contentAlignment = Center) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item{
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = NavigationBarDefaults.Elevation,  // should use card elevation but it is private
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .padding(horizontal = 16.dp)
                        ){
                            Row(verticalAlignment = CenterVertically,
                                modifier = Modifier.fillMaxWidth()) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                var searchText by rememberSaveable { mutableStateOf("") }
                                TextField(
                                    value = searchText,
                                    onValueChange = {
                                        searchText = it
                                        viewModel.onEvent(ExercisesEvent.FilterExercise(searchText))
                                    },
                                    placeholder = { Text("Search exercise") },
                                    singleLine = true,
                                    colors = TextFieldDefaults.textFieldColors(
                                        containerColor = Color.Transparent,
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
                                    Icon(Icons.Default.Close, null,
                                        tint = if (searchText.isBlank()) Color.Transparent else LocalContentColor.current,
                                        modifier = Modifier
                                            .padding(end = 16.dp, top = 8.dp, bottom = 8.dp)
                                            .weight(0.1f)
                                    )
                                }
                            }
                            if (toFocus.value){
                                LaunchedEffect(focusRequester) {
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
                        var selectedFilter by remember { mutableStateOf(-1) }
                        LazyRow (horizontalArrangement = Arrangement.spacedBy(8.dp)){
                            item{
                                Spacer(Modifier.width(8.dp))
                            }
                            itemsIndexed(items = Exercise.Equipment.values().drop(1), { _, it -> it.ordinal }){ index, equipment ->
                                FilterChip(
                                    selected = selectedFilter == index,
                                    onClick = {
                                        if (selectedFilter != index) {
                                            selectedFilter = index
                                            viewModel.onEvent(ExercisesEvent.FilterExerciseEquipment(equipment))
                                        } else {
                                            selectedFilter = -1
                                            viewModel.onEvent(ExercisesEvent.FilterExerciseEquipment(Exercise.Equipment.EVERYTHING))
                                        }
                                    },
                                    label = { Text(equipment.equipmentName) },
                                    leadingIcon = if (selectedFilter == index) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Done,
                                                contentDescription = "Localized Description",
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
                    items(viewModel.state.value.exercisesToDisplay ?: emptyList(),
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
                                .animateItemPlacement()
                                .combinedClickable(
                                    interactionSource = interactionSource,
                                    indication = rememberRipple(),
                                    onClick = {
                                        if (exercise.name == "Can you hear the silence?") {
                                            ContextCompat.startActivity(
                                                context,
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://www.tiktok.com/@poet_jenix/video/7111621457775561989")  // FIXME
                                                ),
                                                null
                                            )
                                        } else {
                                            navigator.navigate(
                                                AddExerciseDialogDestination(
                                                    programId = programId,
                                                    workoutId = workoutId,
                                                    exerciseId = exercise.exerciseId
                                                ),
                                                onlyIfResumed = true
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
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(LocalConfiguration.current.screenWidthDp.dp / 4)
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
                                        CreateExerciseDialogDestination(),
                                        onlyIfResumed = true
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Add, null)
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
                    Image(painterResource(id = longPressImage), null,
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
