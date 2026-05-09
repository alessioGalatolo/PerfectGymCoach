package agdesigns.elevatefitness.ui.screens.workout_recap

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.HealthConnectRepository
import agdesigns.elevatefitness.navigation.DestinationsNavigator
import agdesigns.elevatefitness.navigation.ExerciseStatsDestination
import agdesigns.elevatefitness.ui.common.InfoDialog
import agdesigns.elevatefitness.shared.maybeKgToLb
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import agdesigns.elevatefitness.ui.common.CurrentColumnKey
import agdesigns.elevatefitness.ui.common.ExerciseRecordsList
import agdesigns.elevatefitness.ui.common.HorizontalPagerIndicator
import agdesigns.elevatefitness.ui.common.columnProviderWithHighlight
import agdesigns.elevatefitness.ui.common.highlightSeriesKey
import agdesigns.elevatefitness.ui.common.lineProviderWithHighlight
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.health.connect.client.PermissionController
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.rememberHorizontalLegend
import com.patrykandpatrick.vico.compose.common.vicoTheme
import com.patrykandpatrick.vico.compose.cartesian.Scroll
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.common.LegendItem
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalBox
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.Position
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.TextComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerDimensions
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun WorkoutRecap(
    navigator: DestinationsNavigator,
    workoutId: Long,
    viewModel: RecapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(workoutId) {
        viewModel.onEvent(RecapEvent.SetWorkoutId(workoutId))
    }
    val volumeDialogIsOpen = rememberSaveable { mutableStateOf(false) }
    val calorieDialogIsOpen = rememberSaveable { mutableStateOf(false) }
    InfoDialog(dialogueIsOpen = volumeDialogIsOpen.value,
        toggleDialogue = { volumeDialogIsOpen.value = !volumeDialogIsOpen.value })
    {
        val annotatedText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = LocalContentColor.current)) {
                append(stringResource(R.string.volume_info))
            }
            withLink(
                LinkAnnotation.Url(
                    url = "https://doi.org/10.1007/s40279-017-0793-0",
                    styles = TextLinkStyles(
                        style = SpanStyle(color = MaterialTheme.colorScheme.primary)
                    )
                )
            ) {
                append(stringResource(R.string.learn_more))
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
        state.workoutId != 0L &&
        state.workoutRecord != null
    ){
        val listState = rememberLazyListState()
        val records = state.olderRecords
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical),
            topBar = {
            TopAppBar (
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                windowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
                title = {
                    Text(stringResource(R.string.workout_recap))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navigator.navigateUp()
                        },
                        shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(Icons.Default.Close, stringResource(R.string.close_icon))
                }},
                modifier = Modifier.clickable {
                    // scroll to top when click on top app bar
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            )
        }) { innerPadding ->
            // we isolate and manually add top padding, this is to avoid showing "surface" color
            // instead of "surfaceContainer" color when over-scrolling to the top
            val topPadding = innerPadding.calculateTopPadding()
            val otherPadding = innerPadding.minus(
                PaddingValues(top = topPadding)
            )
            LazyColumn(
                state = listState,
                contentPadding = otherPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ){
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(Modifier.height(topPadding))
                    }
                }
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.workout_recap_praise),
                            style = MaterialTheme.typography.headlineLargeEmphasized,
                            color = MaterialTheme.colorScheme.secondary,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
                // Stats grid — 2 cards per row
                if (state.workoutRecord != null) {
                    item {
                        val record = state.workoutRecord!!
                        val prev = state.previousRecord
                        val cardPadding = dimensionResource(R.dimen.card_outside_padding)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = cardPadding)
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Row 1: Calories + Volume
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatCard(
                                        icon = {
                                            Icon(
                                                Icons.Outlined.LocalFireDepartment,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        },
                                        value = stringResource(
                                            R.string.kcal_value,
                                            record.calories.toInt()
                                        ),
                                        label = stringResource(R.string.calories_burned),
                                        trendDelta = prev?.let { record.calories - it.calories },
                                        onHelpClick = { calorieDialogIsOpen.value = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        icon = {
                                            Icon(
                                                painterResource(R.drawable.weight_icon),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        },
                                        value = maybeKgToLb(
                                            record.volume.toFloat(),
                                            state.imperialSystem
                                        ).toInt()
                                            .toString() + if (state.imperialSystem) " " + stringResource(
                                            R.string.lb
                                        ) else " " + stringResource(R.string.kg),
                                        label = stringResource(R.string.volume_lifted),
                                        trendDelta = prev?.let { (record.volume - it.volume).toFloat() },
                                        onHelpClick = { volumeDialogIsOpen.value = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Row 2: Total time + Total sets
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatCard(
                                        icon = {
                                            Icon(
                                                Icons.Outlined.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        },
                                        value = DateUtils.formatElapsedTime(record.durationSeconds)
                                            .toString(),
                                        label = stringResource(R.string.workout_time),
                                        trendDelta = prev?.let { (record.durationSeconds - it.durationSeconds).toFloat() },
                                        modifier = Modifier.weight(1f)
                                    )
                                    val totalSets = state.exerciseRecords.sumOf { it.reps.size }
                                    StatCard(
                                        icon = {
                                            Icon(
                                                Icons.Outlined.FitnessCenter,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        },
                                        value = totalSets.toString(),
                                        label = stringResource(R.string.total_sets),
                                        trendDelta = state.previousWorkoutTotalSets?.let {
                                            (totalSets - it).toFloat()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Row 3: HR zone breakdown (only if HR data available)
                                if (state.hasHRData && record.heartRates != null) {
                                    HrZoneCard(
                                        heartRates = record.heartRates,
                                        durationSeconds = record.durationSeconds,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Card(
                            shape = MaterialTheme.shapes.extraExtraLarge.copy(
                                bottomStart = CornerSize(0.dp),
                                bottomEnd = CornerSize(0.dp)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        ) {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
                if (state.hasHRData) {
                    item {
                        Card(Modifier
                            .padding(horizontal = dimensionResource(R.dimen.card_outside_padding))
                            .padding(bottom = dimensionResource(R.dimen.card_outside_padding))
                        ) {
                            val cartesianLayer = rememberLineCartesianLayer(
                                lineProvider = LineCartesianLayer.LineProvider.series(
                                    LineCartesianLayer.rememberLine(
                                        // Solid red line
                                        fill = LineCartesianLayer.LineFill.single(
                                            Fill(
                                                vicoTheme.lineCartesianLayerColors[0]//HrLineColor
                                            )
                                        ),
                                        stroke = LineCartesianLayer.LineStroke.Continuous(
                                            thickness = 2.dp,
                                        ),
                                    )
                                ),
                                rangeProvider = object : CartesianLayerRangeProvider {
                                    override fun getMinY(
                                        minY: Double,
                                        maxY: Double,
                                        extraStore: ExtraStore
                                    ) = state.minHR

                                    override fun getMaxY(
                                        minY: Double,
                                        maxY: Double,
                                        extraStore: ExtraStore
                                    ) = state.maxHR
                                }
                            )

                            val zoneDecorations = remember {
                                HrZones.filter {
                                    it.maxBpm > state.minHR &&
                                            it.minBpm < state.maxHR
                                }.map { zone ->
                                    HorizontalBox(
                                        y = {
                                            maxOf(zone.minBpm, state.minHR)..minOf(
                                                zone.maxBpm,
                                                state.maxHR
                                            )
                                        },
                                        box = ShapeComponent(
                                            fill = Fill(zone.color.copy(alpha = 0.15f)),
                                            shape = RectangleShape,
                                        ),
                                    )
                                }
                            }

                            Text(
                                stringResource(R.string.heart_rate_bpm),
                                style = MaterialTheme.typography.titleMediumEmphasized,
                                color = MaterialTheme.colorScheme.tertiary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                            val initialZoom = 0f
                            val targetZoom = 0.05f
                            val zoomState = rememberVicoZoomState(initialZoom = Zoom.fixed(initialZoom))
                            val scrollState = rememberVicoScrollState(
                                scrollEnabled = true,
                                initialScroll = Scroll.Absolute.Start
                            )
                            // Show to the user that this chart can be zoom-ed in
                            LaunchedEffect(Unit) {
                                val delayMs = 250
                                val easing = FastOutSlowInEasing
                                val durationMs = 800

                                delay(delayMs.toLong())

                                val startTime = System.currentTimeMillis()
                                while (true) {
                                    val elapsed = System.currentTimeMillis() - startTime
                                    val fraction = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                                    val easedFraction = easing.transform(fraction)

                                    // Interpolate from initialZoom down to 1f (Zoom.Content equivalent)
                                    val currentZoom = initialZoom + (targetZoom - initialZoom) * easedFraction
                                    zoomState.zoom(Zoom.fixed(currentZoom))
                                    scrollState.scroll(Scroll.Absolute.Start)

                                    if (fraction >= 1f) break
                                    delay(16L) // ~60fps
                                }
                            }
                            CartesianChartHost(
                                chart = rememberCartesianChart(
                                    cartesianLayer,
                                    decorations = zoneDecorations,
                                    startAxis = VerticalAxis.rememberStart(
                                        itemPlacer = remember {
                                            VerticalAxis.ItemPlacer.step(step = { 20.0 })
                                        }
                                    ),
                                    bottomAxis = HorizontalAxis.rememberBottom(
                                        valueFormatter = CartesianValueFormatter { _, value, _ ->
                                            // put exercise names
                                            state.secondsToExercise[value.toInt()]?.name ?: "."
                                        },
                                        label = rememberAxisLabelComponent(
                                            TextStyle(vicoTheme.textColor, 8.sp, textAlign = TextAlign.Center),
                                            lineCount = 2
                                        ),
                                        itemPlacer = remember {
                                            FilteredHorizontalAxisItemPlacer(
                                                allowedXValues =
                                                    state.secondsToExercise.toList().groupBy {
                                                        it.second
                                                    }.map {
                                                        it.value.minOf { it.first }.toDouble()
                                                    }
                                            )
                                        }
                                    )
                                ),
                                modelProducer = state.hrChartProducer,
                                modifier = Modifier.padding(8.dp),
                                scrollState = scrollState,
                                zoomState = zoomState,
                                animateIn = false
                            )
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.progression_over_time),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                    )
                }
                item{
                    if (records.size > 1) {
                        val pagerState = rememberPagerState(pageCount = { 4 })

                        val titles = listOf(
                            "${stringResource(R.string.volume)} (${
                                if (state.imperialSystem) stringResource(
                                    R.string.lb
                                ) else stringResource(R.string.kg)
                            })",
                            "${stringResource(R.string.fitness_center_icon_intensity)} (%)",
                            "${stringResource(R.string.calories)} (kcal)",
                            "${stringResource(R.string.workout_time)} (m)",
                        )
                        val legend = listOf(
                            listOf(
                                "", // This was transferred to title
                                stringResource(R.string.this_workout)
                            ),
                            listOf(
                                "", // This was transferred to title
                                stringResource(R.string.this_workout)
                            ),
                            listOf(
                                "", // This was transferred to title
                                stringResource(R.string.this_workout)
                            ),
                            listOf(
                                "${stringResource(R.string.workout_time)} (m)",
                                "${stringResource(R.string.workout_active_time)} (m)",
                                stringResource(R.string.this_workout),
                            )
                        )
                        HorizontalPager(
                            state = pagerState,
                        ) { page ->
                            Card(Modifier.padding(horizontal = dimensionResource(R.dimen.card_outside_padding))) {
                                val formatter = DateTimeFormatter.ofPattern("d MMM")
                                val legendItemLabelComponent = rememberTextComponent(
                                    TextStyle(vicoTheme.textColor)
                                )
                                val colors = vicoTheme.lineCartesianLayerColors.drop(1)
                                val primaryColor = vicoTheme.lineCartesianLayerColors[0]
                                val cartesianLayer = when (page) {
                                    // calories chart is column, others are lines
                                    2 -> rememberColumnCartesianLayer(
                                        columnProvider = columnProviderWithHighlight(
                                            baseShape = MaterialTheme.shapes.extraSmall,
                                            baseColor = MaterialTheme.colorScheme.secondary,
                                            highlightKey = CurrentColumnKey
                                        )
                                    )

                                    else -> rememberLineCartesianLayer(
                                        lineProviderWithHighlight()
                                    )
                                }

                                val shapePill = MaterialTheme.shapes.extraExtraLarge
                                Text(
                                    titles.getOrNull(page) ?: "",
                                    style = MaterialTheme.typography.titleMediumEmphasized,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                )
                                CartesianChartHost(
                                    chart = rememberCartesianChart(
                                        cartesianLayer,
                                        startAxis = VerticalAxis.rememberStart(
                                            valueFormatter = CartesianValueFormatter { context, value, _ ->
                                                val maxY = context.ranges.getYRange(null).maxY
                                                when (page) {
                                                    // volume, should be in tens of thousands
                                                    0 -> if (maxY > 1000.0) "${value / 1000.0}K" else value.toString()
                                                    1 -> value.toInt().toString()
                                                    2 -> value.toInt().toString()
                                                    3 -> "${(value).toInt()}"
                                                    else -> value.toString()
                                                }
                                            },
                                            itemPlacer = remember {
                                                VerticalAxis.ItemPlacer.step({
                                                    5.0
                                                })
                                            }
                                        ),
                                        bottomAxis = HorizontalAxis.rememberBottom(
                                            valueFormatter = CartesianValueFormatter { _, value, _ ->
                                                state.index2date[value.toInt()]?.format(
                                                    formatter
                                                )
                                                    ?: value.toString() // fall back to value, empty string crashes stuff
                                            },
                                        ),
                                        legend =
                                            rememberHorizontalLegend(
                                                items = { extraStore ->
                                                    val currentLegend =
                                                        legend.getOrNull(page) ?: emptyList()

                                                    currentLegend.forEachIndexed { index, label ->
                                                        if (label.isBlank()) return@forEachIndexed
                                                        val highlights = extraStore.getOrNull(
                                                            highlightSeriesKey
                                                        ) ?: emptyList()
                                                        val color =
                                                            if (highlights.contains(index)) {
                                                                primaryColor
                                                            } else {
                                                                colors[index % colors.size]
                                                            }
                                                        val shape = if (
                                                        // calories chart has opposite shapes
                                                            (page == 1 && !highlights.contains(
                                                                index
                                                            ))
                                                            || (page != 1 && highlights.contains(
                                                                index
                                                            ))
                                                        ) {
                                                            RoundedCornerShape(2.0.dp)
                                                        } else
                                                            shapePill
                                                        add(
                                                            LegendItem(
                                                                ShapeComponent(
                                                                    fill = Fill(color),
                                                                    shape = shape
                                                                ),
                                                                legendItemLabelComponent,
                                                                label,
                                                            )
                                                        )
                                                    }
                                                },
                                                padding = Insets(start = 8.dp, top = 16.dp),
                                            ),
                                        marker = rememberDefaultCartesianMarker(
                                            label = rememberTextComponent(
                                                style = TextStyle(
                                                    MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center
                                                ),
                                                padding = Insets(8.dp, 4.dp),
                                                background = rememberShapeComponent(
                                                    fill = Fill(MaterialTheme.colorScheme.background),
                                                    shape = MaterialTheme.shapes.medium,
                                                ),
                                            ),
                                            // the reason we need a custom value formatter (instead of
                                            // the default) is because current workout has two entries
                                            // in the graph and the default formatter would show both
                                            valueFormatter = remember {
                                                DefaultCartesianMarker.ValueFormatter { _, targets ->
                                                    targets.firstOrNull()?.let { target ->
                                                        when (target) {
                                                            is ColumnCartesianLayerMarkerTarget -> {
                                                                target.columns.map {
                                                                    it.entry.y.toInt()
                                                                        .toString()
                                                                }.distinct().joinToString(", ")
                                                            }

                                                            is LineCartesianLayerMarkerTarget -> {
                                                                target.points.map {
                                                                    it.entry.y.toInt()
                                                                        .toString()
                                                                }.distinct().joinToString(", ")
                                                            }

                                                            else -> {
                                                                Log.w(
                                                                    "WorkoutRecap",
                                                                    "Probably tried to format a target of CandlestickCartesianLayerMarkerTarget which is not implemented"
                                                                )
                                                                ""
                                                            }
                                                        }
                                                    } ?: ""
                                                }
                                            }
                                        ),
                                    ),
                                    modelProducer = when (page) {
                                        0 -> state.volumeChartProducer
                                        1 -> state.intensityChartProducer
                                        2 -> state.caloriesChartProducer
                                        3 -> state.timeChartProducer
                                        else -> state.volumeChartProducer // should not happen
                                    },
                                    modifier = Modifier.padding(8.dp),
                                    scrollState = rememberVicoScrollState(
                                        scrollEnabled = false, // TODO: should not enable scroll inside a horizontal pager
                                        initialScroll = Scroll.Absolute.End // FIXME: scroll to current workout instead
                                    ),
                                )
                            }
                        }
                        Column(Modifier.fillMaxWidth()) {
                            HorizontalPagerIndicator(
                                pagerState = pagerState,
                                pageCount = pagerState.pageCount,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.no_analytics),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 128.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                // Health Connect export card
                if (state.isHealthConnectAvailable && state.workoutRecord != null) {
                    item {
                        val healthConnectPermissionsLauncher = rememberLauncherForActivityResult(
                            PermissionController.createRequestPermissionResultContract()
                        ) {
                            viewModel.onEvent(RecapEvent.RefreshHealthConnectPermissions)
                        }
                        Card(
                            modifier = Modifier
                                .padding(horizontal = dimensionResource(R.dimen.card_outside_padding))
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_health_connect_logo),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .padding(end = 8.dp),
                                    tint = Color.Unspecified
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.health_connect_title),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        when (state.healthConnectExportStatus) {
                                            HealthConnectExportStatus.EXPORTED ->
                                                stringResource(R.string.health_connect_exported)

                                            HealthConnectExportStatus.ERROR ->
                                                stringResource(R.string.health_connect_export_error)

                                            else -> stringResource(R.string.health_connect_export_prompt)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                when (state.healthConnectExportStatus) {
                                    HealthConnectExportStatus.EXPORTING -> {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }

                                    HealthConnectExportStatus.EXPORTED -> {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    HealthConnectExportStatus.ERROR -> {
                                        TextButton(onClick = {
                                            viewModel.onEvent(RecapEvent.ExportToHealthConnect)
                                        }) {
                                            Text(stringResource(R.string.health_connect_retry))
                                        }
                                    }

                                    HealthConnectExportStatus.NOT_EXPORTED -> {
                                        if (state.hasHealthConnectPermissions) {
                                            TextButton(onClick = {
                                                viewModel.onEvent(RecapEvent.ExportToHealthConnect)
                                            }) {
                                                Text(stringResource(R.string.health_connect_export))
                                            }
                                        } else {
                                            TextButton(onClick = {
                                                healthConnectPermissionsLauncher.launch(
                                                    HealthConnectRepository.REQUIRED_PERMISSIONS
                                                )
                                            }) {
                                                Text(stringResource(R.string.health_connect_connect_and_export))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.exerciseRecords.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.workout_history),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    ExerciseRecordsList(
                        state.imperialSystem,
                        exerciseRecordsWithImage = state.exerciseRecords,
                        onRecordClick = { recordId ->
                            val exerciseId =
                                state.exerciseRecords.find { it.recordId == recordId }?.extExerciseId
                            if (exerciseId != null) {
                                navigator.navigate(
                                    ExerciseStatsDestination(exerciseId = exerciseId)
                                )
                            }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatCard(
    icon: @Composable () -> Unit,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    trendDelta: Float? = null,
    trendPositiveIsGood: Boolean = true,
    onHelpClick: (() -> Unit)? = null,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary,
//                color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f)
                )

                if (trendDelta != null && trendDelta != 0f) {
                    val isPositive = trendDelta > 0f
                    val isGood = if (trendPositiveIsGood) isPositive else !isPositive
                    val trendColor = if (isGood) Color(0xFF2E7D32) else Color(0xFFC62828)
                    Icon(
                        imageVector = if (isPositive) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        icon()
                    }
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 1,
                    )
                }
                if (onHelpClick != null) {
                    IconButton(onClick = onHelpClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/** BPM boundaries and colors for each HR zone */
data class HrZone(
    val nameResId: Int,
    val minBpm: Double,
    val maxBpm: Double,
    val color: Color
)

private val HrZones = listOf(
    HrZone(R.string.hr_zone_warm_up, 0.0,   100.0, Color(0xFF42A5F5)), // light blue
    HrZone(R.string.hr_zone_light, 100.0, 120.0, Color(0xFF66BB6A)), // green
    HrZone(R.string.hr_zone_aerobic, 120.0, 140.0, Color(0xFFFFEE58)), // yellow
    HrZone(R.string.hr_zone_anaerobic, 140.0, 160.0, Color(0xFFFFA726)), // orange
    HrZone(R.string.hr_zone_peak, 160.0, 220.0, Color(0xFFEF5350)), // red
)

@Composable
private fun HrZoneCard(
    heartRates: List<Int>,
    durationSeconds: Long,
    modifier: Modifier = Modifier,
) {
    // Compute seconds per sample, then tally seconds in each zone
    val secondsPerSample = if (heartRates.isNotEmpty())
        durationSeconds.toDouble() / heartRates.size else 0.0
    val zoneSeconds = HrZones.map { zone ->
        heartRates.count { it >= zone.minBpm && it < zone.maxBpm } * secondsPerSample
    }
    val totalSeconds = zoneSeconds.sum().coerceAtLeast(1.0)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    stringResource(R.string.heart_rate_zones),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            // Segmented proportional bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
            ) {
                HrZones.forEachIndexed { index, zone ->
                    val fraction = (zoneSeconds[index] / totalSeconds).toFloat()
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(fraction)
                                .fillMaxHeight()
                                .background(zone.color)
                        )
                    }
                }
            }
            // Zone labels with durations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HrZones.forEachIndexed { index, zone ->
                    val minutes = (zoneSeconds[index] / 60.0).toInt()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(zone.color)
                        )
                        Text(
                            stringResource(R.string.n_min, minutes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            stringResource(zone.nameResId),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A [HorizontalAxis.ItemPlacer] that only renders labels, ticks, and guidelines
 * at X values present in [allowedXValues].
 *
 * @param allowedXValues The X values at which axis items should appear.
 */
class FilteredHorizontalAxisItemPlacer(
    allowedXValues: List<Double>,
) : HorizontalAxis.ItemPlacer {

    private val sortedXValues: List<Double> = allowedXValues
        .distinct()
        .sorted()

    override fun getShiftExtremeLines(context: CartesianDrawingContext): Boolean = true

    override fun getFirstLabelValue(
        context: CartesianMeasuringContext,
        maxLabelWidth: Float,
    ): Double? = sortedXValues.firstOrNull()

    override fun getLastLabelValue(
        context: CartesianMeasuringContext,
        maxLabelWidth: Float,
    ): Double? = sortedXValues.lastOrNull()

    override fun getLabelValues(
        context: CartesianDrawingContext,
        visibleXRange: ClosedFloatingPointRange<Double>,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double> = sortedXValues.filter { it in visibleXRange }

    override fun getWidthMeasurementLabelValues(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
    ): List<Double> = sortedXValues

    override fun getHeightMeasurementLabelValues(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double> = listOfNotNull(sortedXValues.firstOrNull())

    override fun getLineValues(
        context: CartesianDrawingContext,
        visibleXRange: ClosedFloatingPointRange<Double>,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double>? = null

    override fun getStartLayerMargin(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float,
    ): Float = 0f

    override fun getEndLayerMargin(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float,
    ): Float = 0f
}