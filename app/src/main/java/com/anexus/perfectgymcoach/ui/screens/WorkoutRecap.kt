package com.anexus.perfectgymcoach.ui.screens

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anexus.perfectgymcoach.R
import com.anexus.perfectgymcoach.ui.NavigationScreen
import com.anexus.perfectgymcoach.ui.components.InfoDialog
import com.anexus.perfectgymcoach.viewmodels.RecapEvent
import com.anexus.perfectgymcoach.viewmodels.RecapViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.jaikeerthick.composable_graphs.color.*
import com.jaikeerthick.composable_graphs.composables.LineGraph
import com.jaikeerthick.composable_graphs.data.GraphData
import com.jaikeerthick.composable_graphs.style.LineGraphStyle
import com.jaikeerthick.composable_graphs.style.LinearGraphVisibility
import java.text.SimpleDateFormat
import kotlin.math.ceil


@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun WorkoutRecap(
    navController: NavHostController,
    workoutId: Long,
    viewModel: RecapViewModel = hiltViewModel()
) {
    viewModel.onEvent(RecapEvent.SetWorkoutId(workoutId)) // fixme: is not updated after direct completion (may have been fixed)
    val volumeDialogIsOpen = rememberSaveable { mutableStateOf(false) }
    val calorieDialogIsOpen = rememberSaveable { mutableStateOf(false) }
    InfoDialog(dialogueIsOpen = volumeDialogIsOpen.value,
        toggleDialogue = { volumeDialogIsOpen.value = !volumeDialogIsOpen.value })
    {
        val annotatedText = buildAnnotatedString {
            append(stringResource(R.string.volume_info))

            // We attach this *URL* annotation to the following content
            // until `pop()` is called
            pushStringAnnotation(tag = "URL",
                annotation = "https://developer.android.com") // FIXME
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)
            ) {
                append("Learn more.")
            }
            pop()
        }
        val context = LocalContext.current
        ClickableText(
            text = annotatedText,
            onClick = { offset ->
                // We check if there is an *URL* annotation attached to the text
                // at the clicked position
                annotatedText.getStringAnnotations(tag = "URL", start = offset,
                    end = offset)
                    .firstOrNull()?.let { annotation ->
                        startActivity(
                            context,
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(annotation.item)
                            ),
                            null
                        )
                    }
            }
        )
    }
    InfoDialog(dialogueIsOpen = calorieDialogIsOpen.value,
        toggleDialogue = { calorieDialogIsOpen.value = !calorieDialogIsOpen.value })
    {
        Text(stringResource(R.string.calories_info))
    }
    if (
        viewModel.state.value.workoutId != 0L &&
        viewModel.state.value.exerciseRecords.isNotEmpty() &&
        viewModel.state.value.workoutRecord != null
    ){
        Scaffold(topBar = {
            TopAppBar (title = {
                Text("Workout recap")
            }, navigationIcon = {
                IconButton(onClick = {
                    navController.popBackStack()
                    navController.popBackStack()
                    navController.navigate(NavigationScreen.History.route)
                }) {
                    Icon(Icons.Default.Close, null)
                }
            })
        }) { innerPadding ->
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ){
                item {
                    Text("Great job!",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
                val records = viewModel.state.value.olderRecords
                val graphsYaxis = listOf (
                    Pair(Pair("Volume", "kg"), records.map { it.volume }),
                    Pair(Pair("Calories", "kcal"), records.map { it.calories }),
                    Pair(Pair("Workout time", "s"), records.map { it.duration }),
                    Pair(Pair("Workout active time", "s"), records.map { it.activeTime })
                )
                item{
                    val pagerState = rememberPagerState()
                    ElevatedCard (Modifier.padding(horizontal = 16.dp)) {
                        if (records.size > 1){
                            HorizontalPager(count = graphsYaxis.size, state = pagerState) { page ->
                                Column(Modifier.padding(8.dp)) {
                                    val clickedValue: MutableState<Pair<Any, Any>> = remember {
                                        mutableStateOf(
                                            Pair(
                                                SimpleDateFormat("d MMM").format(
                                                    viewModel.state.value.workoutRecord!!.startDate.time
                                                ),
                                                graphsYaxis[page].second.last()
    //                                            viewModel.state.value.workoutRecord!!.volume
                                            )
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .padding(all = 25.dp)
                                    ) {
                                        Text(
                                            text = "${graphsYaxis[page].first.first}: ",
                                            fontStyle = Italic
                                        )
                                        Text(
                                            text = "${
                                                if (clickedValue.value.first.toString()
                                                        .isBlank()
                                                ) "-"
                                                else clickedValue.value.first
                                            }" +
                                                    ", ${clickedValue.value.second} " +
                                                    graphsYaxis[page].first.second,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    val maxDatesInXAxis = 6
                                    LineGraph(
                                        xAxisData = List(records.size) { index ->
                                            GraphData.String(
                                                if (index % ceil(records.size.toDouble() / maxDatesInXAxis).toInt() == 0)
                                                    SimpleDateFormat("d MMM").format(records[index].startDate.time)
                                                else
                                                    ""
                                            )
                                        },
                                        yAxisData = graphsYaxis[page].second,
                                        onPointClicked = { x ->
                                            clickedValue.value = x
                                        },
                                        style = LineGraphStyle(
                                            visibility = LinearGraphVisibility(
                                                isHeaderVisible = true,
                                                isYAxisLabelVisible = true,
                                                isXAxisLabelVisible = true,
                                                isCrossHairVisible = true,
                                                isGridVisible = true
                                            ),
                                            colors = LinearGraphColors(
                                                lineColor = MaterialTheme.colorScheme.primary,
                                                pointColor = MaterialTheme.colorScheme.primary,
                                                clickHighlightColor = PointHighlight2,
                                                fillGradient = Brush.verticalGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.secondary,
                                                        Gradient2
                                                    )
                                                )
                                            )
                                        )
                                    )
                                }
                            }
                            HorizontalPagerIndicator(
                                pagerState = pagerState,
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp)
                            )
                        } else {
                            Text(
                                stringResource(R.string.no_analytics),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 128.dp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                item {
                    if (viewModel.state.value.workoutRecord != null) {
                        ElevatedCard(Modifier.padding(horizontal = 16.dp)) {
                            Column(Modifier.padding(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Outlined.LocalFireDepartment, null,
                                            Modifier.size(50.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Calorie consumption: " + // split for easier translation
                                                    "${viewModel.state.value.workoutRecord!!.calories.toInt()} kcal"
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    IconButton(
                                        onClick = { calorieDialogIsOpen.value = true },
                                        modifier = Modifier.weight(0.1f)
                                    ) {
                                        Icon(Icons.Default.HelpOutline, null)
                                    }
                                }
                                Divider()
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.weight_icon), null,
                                            Modifier.size(50.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Total volume: " +
                                                    "%.2f kg".format(viewModel.state.value.workoutRecord!!.volume)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    IconButton(
                                        onClick = { volumeDialogIsOpen.value = true },
                                        modifier = Modifier.weight(0.1f)
                                    ) {
                                        Icon(Icons.Default.HelpOutline, null)
                                    }
                                }
                                Divider()
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Icon(
                                        Icons.Outlined.Schedule, null,
                                        Modifier.size(50.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Total time: " +
                                                DateUtils.formatElapsedTime(
                                                    viewModel.state.value.workoutRecord!!.duration
                                                )
                                    )
                                }
                                Divider()
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Icon(
                                        Icons.Outlined.PendingActions, null,
                                        Modifier.size(50.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Active time: " +
                                                DateUtils.formatElapsedTime(
                                                    viewModel.state.value.workoutRecord!!.activeTime
                                                )
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Text("Workout history",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp).padding(horizontal = 16.dp))
                }
                items (items = viewModel.state.value.exerciseRecords, key = { it.recordId }) { exercise ->
                    Card (Modifier.fillMaxWidth().padding(horizontal = 16.dp)){
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
                        Column(Modifier.padding(8.dp)) {
                            Text(text = exercise.name + exercise.variation, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tare: ${exercise.tare}") // FIXME
                            Spacer(modifier = Modifier.height(4.dp))
                            exercise.reps.forEachIndexed { index, rep ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    FilledIconToggleButton(checked = false, // FIXME: can use different component?
                                        onCheckedChange = { }) {
                                        Text((index + 1).toString())
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Reps: $rep Weight: ${exercise.weights[index]} kg"
                                    )
                                }
                            }

                        }
                    }
                }
            }
        }
    }
}
