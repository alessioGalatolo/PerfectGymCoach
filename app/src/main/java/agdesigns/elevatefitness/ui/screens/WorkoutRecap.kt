package agdesigns.elevatefitness.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.exercise.Exercise
import agdesigns.elevatefitness.ui.FullscreenDialogTransition
import agdesigns.elevatefitness.ui.barbellFromWeight
import agdesigns.elevatefitness.ui.components.InfoDialog
import agdesigns.elevatefitness.ui.maybeKgToLb
import agdesigns.elevatefitness.viewmodels.RecapEvent
import agdesigns.elevatefitness.viewmodels.RecapViewModel
import androidx.compose.foundation.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import agdesigns.elevatefitness.ui.WorkoutOnlyGraph
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.withLink
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaikeerthick.composable_graphs.composables.line.LineGraph
import com.jaikeerthick.composable_graphs.composables.line.model.LineData
import com.jaikeerthick.composable_graphs.composables.line.style.LineGraphColors
import com.jaikeerthick.composable_graphs.composables.line.style.LineGraphFillType
import com.jaikeerthick.composable_graphs.composables.line.style.LineGraphStyle
import com.jaikeerthick.composable_graphs.composables.line.style.LineGraphVisibility
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.HistoryDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlin.math.ceil
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import java.time.format.DateTimeFormatter


@Destination<WorkoutOnlyGraph>(style = FullscreenDialogTransition::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkoutRecap(
    navigator: DestinationsNavigator,
    workoutId: Long,
    viewModel: RecapViewModel = hiltViewModel()
) {
    val recapState by viewModel.state.collectAsState()
    viewModel.onEvent(RecapEvent.SetWorkoutId(workoutId))
    val volumeDialogIsOpen = rememberSaveable { mutableStateOf(false) }
    val calorieDialogIsOpen = rememberSaveable { mutableStateOf(false) }
    InfoDialog(dialogueIsOpen = volumeDialogIsOpen.value,
        toggleDialogue = { volumeDialogIsOpen.value = !volumeDialogIsOpen.value })
    {
        val context = LocalContext.current

        val annotatedText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = LocalContentColor.current)) {
                append("Volume info: ")
            }
            withLink(
                LinkAnnotation.Url(
                    url = "https://doi.org/10.1007/s40279-017-0793-0",
                    styles = TextLinkStyles(
                        style = SpanStyle(color = MaterialTheme.colorScheme.primary)
                    )
                )
            ) {
                append("Learn more.")
            }
        }

        Text(
            text = annotatedText
        )
    }
    InfoDialog(dialogueIsOpen = calorieDialogIsOpen.value,
        toggleDialogue = { calorieDialogIsOpen.value = !calorieDialogIsOpen.value })
    {
        Text(stringResource(R.string.calories_info))
    }
    if (
        recapState.workoutId != 0L &&
        recapState.workoutRecord != null
    ){
        Scaffold(topBar = {
            TopAppBar (title = {
                Text("Workout recap")
            }, navigationIcon = {
                IconButton(onClick = {
                    navigator.navigateUp()
                    navigator.navigateUp()
                    navigator.navigate(
                        HistoryDestination()
                    )
                }) {
                    Icon(Icons.Default.Close, "Close")
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
                val records = recapState.olderRecords
                val graphsYaxis = listOf (
                    Pair(Pair("Volume", if (recapState.imperialSystem) "lb" else "kg"),
                        records.map {
                            maybeKgToLb(it.volume.toFloat(), recapState.imperialSystem)
                        }),
                    Pair(Pair("Calories", "kcal"), records.map { it.calories }),
                    Pair(Pair("Workout time", "s"), records.map { it.durationSeconds }),
                    Pair(Pair("Workout active time", "s"), records.map { it.activeTimeSeconds })
                )
                item{
                    val pagerState = rememberPagerState(pageCount = { graphsYaxis.size })
                    ElevatedCard (Modifier.padding(horizontal = dimensionResource(R.dimen.card_outside_padding))) {
                        if (records.size > 1){
                            HorizontalPager(state = pagerState) { page ->
                                val formatter = DateTimeFormatter.ofPattern("d MMM")
                                Column(Modifier.padding(dimensionResource(R.dimen.card_inner_padding))) {
                                    val clickedValue: MutableState<LineData> = remember {
                                        mutableStateOf(
                                            LineData(
                                                recapState.workoutRecord!!.startDate!!.format(formatter),
                                                graphsYaxis[page].second.last()
    //                                            recapState.workoutRecord!!.volume
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
                                            // FIXME: why are we outputting the date here?
                                            text = (clickedValue.value.x.ifBlank { "-" }) +
                                                    ", ${clickedValue.value.y} " +
                                                    graphsYaxis[page].first.second,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    val maxDatesInXAxis = 6
                                    val interval = ceil(records.size.toDouble() / maxDatesInXAxis).toInt()

                                    val data = records.mapIndexed { index, record ->
                                        val xLabel = if (index % interval == 0)
                                            record.startDate!!.format(formatter)
                                        else
                                            ""
                                        LineData(x = xLabel, y = graphsYaxis[page].second[index])
                                    }
                                    LineGraph(
                                        data = data,
                                        onPointClick = { point ->
                                            clickedValue.value = point
                                        },
                                        style = LineGraphStyle(
                                            visibility = LineGraphVisibility(
                                                isYAxisLabelVisible = true,
                                                isXAxisLabelVisible = true,
                                                isCrossHairVisible = true,
                                                isGridVisible = true
                                            ),
                                            // FIXME: replace colors with theme accents
                                            colors = LineGraphColors(
                                                lineColor = MaterialTheme.colorScheme.primary,
                                                pointColor = MaterialTheme.colorScheme.primary,
                                                clickHighlightColor = Color(0x75388E3C), // was PointHighlight2 but now it's internal
                                                fillType = LineGraphFillType.Gradient(
                                                    brush = Brush.verticalGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.secondary,
                                                            Color(0x00FFFFFF)  // was Gradient2
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                }
                            }
                            HorizontalPagerIndicator(
                                pagerState = pagerState,
                                pageCount = graphsYaxis.size,
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
                    if (recapState.workoutRecord != null) {
                        ElevatedCard(Modifier.padding(horizontal = dimensionResource(R.dimen.card_outside_padding))) {
                            Column(Modifier.padding(dimensionResource(R.dimen.card_inner_padding))) {
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
                                            Icons.Outlined.LocalFireDepartment, "Calories burned",
                                            Modifier.size(50.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Calorie consumption: " + // split for easier translation
                                                    "${recapState.workoutRecord!!.calories.toInt()} kcal"
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    IconButton(
                                        onClick = { calorieDialogIsOpen.value = true },
                                        modifier = Modifier.weight(0.1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "Help/Info")
                                    }
                                }
                                HorizontalDivider()
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
                                            painterResource(R.drawable.weight_icon), "Volume lifted",
                                            Modifier.size(50.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Total volume: " +
                                                    "%.2f ".format(maybeKgToLb(
                                                        recapState.workoutRecord!!.volume.toFloat(),
                                                        recapState.imperialSystem
                                                    )) + if (recapState.imperialSystem) "lb" else "kg"
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    IconButton(
                                        onClick = { volumeDialogIsOpen.value = true },
                                        modifier = Modifier.weight(0.1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "Help/Info")
                                    }
                                }
                                HorizontalDivider()
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Icon(
                                        Icons.Outlined.Schedule, "Workout time",
                                        Modifier.size(50.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Total time: " +
                                                DateUtils.formatElapsedTime(
                                                    recapState.workoutRecord!!.durationSeconds
                                                )
                                    )
                                }
                                HorizontalDivider()
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Icon(
                                        Icons.Outlined.PendingActions, "Active time",
                                        Modifier.size(50.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Active time: " +
                                                DateUtils.formatElapsedTime(
                                                    recapState.workoutRecord!!.activeTimeSeconds
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
                items (items = recapState.exerciseRecords, key = { it.recordId }) { exercise ->
                    Card (Modifier.fillMaxWidth().padding(horizontal = 16.dp)){
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(exercise.image)
                                .crossfade(true)
                                .build(),
                            contentScale = ContentScale.Crop,
                            contentDescription = "Exercise image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(with (LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } / 4 )
                                .align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column(Modifier.padding(dimensionResource(R.dimen.card_inner_padding))) {
                            Text(text = exercise.name + exercise.variation, style = MaterialTheme.typography.titleLarge)
                            if (exercise.equipment == Exercise.Equipment.BARBELL) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Barbell used: " +
                                        barbellFromWeight(exercise.tare, recapState.imperialSystem, true)
                                )
                            } else if (exercise.equipment == Exercise.Equipment.BODY_WEIGHT) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Bodyweight at the time: ${maybeKgToLb(exercise.tare, recapState.imperialSystem)} " + if (recapState.imperialSystem) "lb" else "kg")
                            }
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
                                        "Reps: $rep Weight: ${maybeKgToLb(exercise.weights[index], recapState.imperialSystem)} " + if (recapState.imperialSystem) "lb" else "kg"
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
