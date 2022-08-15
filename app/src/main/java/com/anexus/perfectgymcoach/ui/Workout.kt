package com.anexus.perfectgymcoach.ui

import android.view.RoundedCorner
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialogDefaults.titleContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterStart
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.data.exercise.WorkoutExercise
import com.anexus.perfectgymcoach.viewmodels.WorkoutEvent
import com.anexus.perfectgymcoach.viewmodels.WorkoutViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.text.SimpleDateFormat
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Workout(navController: NavHostController, programId: Long,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    viewModel.onEvent(WorkoutEvent.GetWorkoutExercises(programId))
    val deviceCornerRadius = 12.dp // TODO: should be same as device (waiting compose API)

    // make status bar transparent to see image behind
    val sysUiController = rememberSystemUiController()
    SideEffect {
        sysUiController.setSystemBarsColor(
            color = Transparent,
            darkIcons = true
        )
    }
    val localDensity = LocalDensity.current
    var imageHeight by remember { // store image height to make content start at the right point
        mutableStateOf(0)
    }
    val imageHeightDp = with(localDensity) {imageHeight.toDp()}
    val statusBarsHeight = WindowInsets.Companion.statusBars.asPaddingValues().calculateTopPadding()
    val contentBelowImage = imageHeightDp - statusBarsHeight - 64.dp // top app bar height FIXME: should not be hardcoded
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        rememberTopAppBarState()
    )
    var currentExercise: WorkoutExercise? = null
    if (viewModel.state.value.currentExercise != null &&
            viewModel.state.value.workoutExercises.isNotEmpty()) {
        currentExercise =
            viewModel.state.value.workoutExercises[viewModel.state.value.currentExercise!!]
    }
    var timer = ""
    if (viewModel.state.value.workoutStarted != null){
        timer = " " + viewModel.state.value.workoutStarted!!.toDuration(DurationUnit.SECONDS).toString()
    }

    val haptic = LocalHapticFeedback.current
    Box (
        Modifier
            .background(Transparent)
            .fillMaxSize()){
        Image(
            painterResource(id = R.drawable.sample_image),
            null,
            modifier = Modifier
                .fillMaxWidth()
//                .clip(AbsoluteRoundedCornerShape(deviceCornerRadius))
                .onGloballyPositioned { coordinates ->
                    imageHeight = coordinates.size.height
                }
        ) // TODO: image of exercise

        Scaffold (
            containerColor = Transparent,
//            floatingActionButton = ,
            topBar = {
                val backgroundColors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Transparent,
                    scrolledContainerColor = TopAppBarDefaults.smallTopAppBarColors().containerColor(
                        colorTransitionFraction = 1f
                    ).value)
                val s = scrollBehavior.state
                val belowImageFloat = with(localDensity) { contentBelowImage.toPx() }
                val transition = 1 - ((s.heightOffsetLimit - s.contentOffset - belowImageFloat).coerceIn(
                    minimumValue = s.heightOffsetLimit,
                    maximumValue = 0f
                ) / s.heightOffsetLimit)
                val backgroundColor = backgroundColors.containerColor(
                    colorTransitionFraction = transition
                ).value
                Box(Modifier.background(backgroundColor)) {
                    SmallTopAppBar(
                        title = {
                            // animate text alpha with scrolling
                            ProvideTextStyle(value = MaterialTheme.typography.titleLarge) {
                                CompositionLocalProvider(
                                    LocalContentColor provides titleContentColor.copy(alpha = transition),
                                    content = { Text(currentExercise?.name ?: "") }
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.smallTopAppBarColors(
                            containerColor = Transparent,
                            scrolledContainerColor = Transparent
                        ),
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Go back" // TODO: cancel workout?
                                )
                            }
                        },
                        actions = {
                            Row (verticalAlignment = CenterVertically) {
                                Text(timer, style = MaterialTheme.typography.titleLarge)
                                if (timer.isNotEmpty()){
                                    IconButton(onClick = { /*TODO*/ }) {
                                        Icon(Icons.Default.Done, null)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.statusBarsPadding()
                    )
                }
        }, content = {
            Box(Modifier.padding(it)) {
                var bottomBarHeight by remember { mutableStateOf(0) }
                Column(
                    Modifier
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(bottom = with(localDensity) { bottomBarHeight.toDp() })
                        .fillMaxSize()
                ) {
                    // space the same as the image height
                    Spacer(modifier = Modifier
                        .height(contentBelowImage)
                        .zIndex(1f))
                    Surface(
                        shape = ReversedCornersShape(deviceCornerRadius),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp)) {
                            Row(Modifier.fillMaxWidth(),
                                verticalAlignment = CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ){
                                IconButton(onClick = { viewModel.onEvent(WorkoutEvent.PreviousExercise) },
                                    enabled = (viewModel.state.value.currentExercise ?: 0) > 0
                                ) {
                                    Icon(Icons.Outlined.ArrowBack, null)
                                }
                                Row (verticalAlignment = CenterVertically,
                                    horizontalArrangement = Arrangement.Center){
                                    ProvideTextStyle(value = MaterialTheme.typography.headlineMedium) {
                                        CompositionLocalProvider(
                                            content = { Text(currentExercise?.name ?: "") }
                                        )
                                    }
//                                    ExerciseSettingsMenu()
                                }
                                IconButton(onClick = { viewModel.onEvent(WorkoutEvent.NextExercise) },
                                    enabled =  (viewModel.state.value.currentExercise ?: Int.MAX_VALUE) < viewModel.state.value.workoutExercises.size-1) {
                                    Icon(Icons.Outlined.ArrowForward, null)
                                }
                            }

                            // content
                            if (currentExercise != null) {
                                Row (verticalAlignment = CenterVertically) {
                                    Text(
                                        "Current", Modifier.padding(vertical = 8.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    ExerciseSettingsMenu()
//                                    TextButton(onClick = { /*TODO*/ }) {
//                                        Text("Change exercise")
//                                    }
                                }
                                ElevatedCard (Modifier.fillMaxWidth()){
                                    var checkedNumber by remember { mutableStateOf(0) }
                                    Column (Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally){
                                        currentExercise.reps.forEachIndexed() {setCount, repsCount ->
                                            Row(
                                                verticalAlignment = CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .combinedClickable(onLongClick = {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                        // TODO: open dialogue to modify
                                                    }, onClick = {
                                                        checkedNumber = setCount
                                                    })
                                            ) {
                                                FilledIconToggleButton(checked = checkedNumber == setCount,
                                                    onCheckedChange = { checkedNumber = setCount }) {
                                                    Text((setCount+1).toString())
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Text("Reps: $repsCount Weight: .. kg")
                                            }
                                        }
                                        TextButton(onClick = { /*TODO*/ }) {
                                            Text("Add set")
                                        }
                                    }
                                }
//                                if (There is history){
                                Text("History", Modifier.padding(vertical = 8.dp), fontWeight = FontWeight.Bold)
                                repeat(4){
                                    Card (Modifier.fillMaxWidth()){
                                        Column(Modifier.padding(8.dp)) {
                                            Text(it.toString())
                                            repeat(5) {
                                                Text("Some exercise history")
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                // bottom bar
                Surface (tonalElevation = NavigationBarDefaults.Elevation,
                    modifier = Modifier
                        .background(NavigationBarDefaults.containerColor)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .wrapContentHeight(CenterVertically)
                        .onGloballyPositioned { coord -> bottomBarHeight = coord.size.height }){
                    Column (
                        Modifier
                            .padding(horizontal = 16.dp)
                            .navigationBarsPadding()
                            .imePadding()){
                        if (viewModel.state.value.workoutStarted != null) {
                            Row {
                                val reps =
                                    remember { mutableStateOf(currentExercise?.reps?.get(0) ?: 0) } // fixme
                                TextFieldWithButtons(
                                    "Reps",
                                    initialValue = reps,
                                    increment = 1
                                )
                                Spacer(Modifier.width(8.dp))
                                TextFieldWithButtons(
                                    "Weight",
                                    initialValue = mutableStateOf(0),
                                    2
                                ) // FIXME: equipment2increment[currentExercise?]
                            }
                        }
                        Row (Modifier.fillMaxWidth()){
                            if (viewModel.state.value.workoutStarted != null) {
                                Button(onClick = { /*TODO*/ }, Modifier.fillMaxWidth()) {
                                    Text("Complete")
                                }
                            } else {
                                Button(onClick = {viewModel.onEvent(WorkoutEvent.StartWorkout)},
                                    Modifier.fillMaxWidth()){
                                    Text("Start workout")
                                }
                            }
                        }
                    }
                }
            }
        })
    }
}

@Composable
fun ExerciseSettingsMenu() {
    Box(
        modifier = Modifier.wrapContentSize()
    ) {
        var expanded by remember { mutableStateOf(false) }
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Localized description"
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Change exercise") },
                onClick = { /* Handle edit! */ },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null
                    )
                })
            DropdownMenuItem(
                text = { Text("Send Feedback") },
                onClick = { /* Handle send feedback! */ },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Email,
                        contentDescription = null
                    )
                })
            DropdownMenuItem(
                text = { Text("Cancel workout") },
                onClick = { /* Handle cancel! */ },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = null
                    )
                })
        }
    }
}

class ReversedCornersShape(private val radius: Dp) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            // Draw your custom path here
            path = reverseRoundedCorners(size = size, radius = with(density) { radius.toPx() })
        )
    }

    private fun reverseRoundedCorners(size: Size, radius: Float): Path {
        return Path().apply {
            val rect = size.toRect()
            addRect(rect)
            val outerCornerDiameter = radius * 2
            val cornerSize = Size(outerCornerDiameter, outerCornerDiameter)
            val cornerOffset = Offset(0f, -outerCornerDiameter)
            val cornerYOffset = Offset(-outerCornerDiameter, -outerCornerDiameter)
            addArc(
                Rect(
                    offset = rect.topLeft + cornerOffset,
                    size = cornerSize
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
            )
            lineTo(rect.topLeft.x, rect.topLeft.y)

            addArc(
                Rect(
                    offset = rect.topRight + cornerYOffset,
                    size = cornerSize
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -90f,
            )
            lineTo(rect.topRight.x, rect.topRight.y)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.TextFieldWithButtons(
    prompt: String,
    initialValue: MutableState<Int> = mutableStateOf(0),
    increment: Int
) {
    Row(verticalAlignment = CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f, true)
    ) {
        var text by remember { mutableStateOf("${initialValue.value}") }
        IconButton(onClick = { text = "${text.toInt() - increment}" }, modifier = Modifier.weight(0.3f)) {
            Icon(Icons.Filled.Remove, null)
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            label = { Text(prompt) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .widthIn(1.dp, Dp.Infinity)
                .heightIn(1.dp, Dp.Infinity)
                .weight(0.5f)
        )
        IconButton(onClick = { text = "${text.toInt() + increment}" }, modifier = Modifier.weight(0.3f)) {
            Icon(Icons.Filled.Add, null)
        }
    }
}

//@Preview
//@Composable
//fun WorkoutScreenPreview() {
//
//}