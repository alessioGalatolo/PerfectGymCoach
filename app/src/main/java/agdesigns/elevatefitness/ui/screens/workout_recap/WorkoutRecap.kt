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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.navigation.FullscreenDialogTransition
import agdesigns.elevatefitness.ui.common.InfoDialog
import agdesigns.elevatefitness.shared.maybeKgToLb
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import agdesigns.elevatefitness.navigation.WorkoutOnlyGraph
import agdesigns.elevatefitness.ui.common.CurrentColumnKey
import agdesigns.elevatefitness.ui.common.ExerciseRecordsList
import agdesigns.elevatefitness.ui.common.HorizontalPagerIndicator
import agdesigns.elevatefitness.ui.common.columnProviderWithHighlight
import agdesigns.elevatefitness.ui.common.highlightSeriesKey
import agdesigns.elevatefitness.ui.common.lazyGroupedCard
import agdesigns.elevatefitness.ui.common.lineProviderWithHighlight
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withLink
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
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
import agdesigns.elevatefitness.data.HealthConnectRepository
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.health.connect.client.PermissionController
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis.ItemPlacer
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalBox
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.destinations.HistoryDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.ExerciseStatsDestination
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter


@Destination<WorkoutOnlyGraph>(style = FullscreenDialogTransition::class)
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
    val highlightsCardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
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
        val titles = listOf(
            "${stringResource(R.string.volume)} (${if (state.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg)})",
            "${stringResource(R.string.calories)} (kcal)",
            "${stringResource(R.string.workout_time)} (s)",

        )
        val legend = listOf (
            listOf(
                "", // This was transferred to title
                stringResource(R.string.this_workout)
            ),
            listOf(
                "", // This was transferred to title
                stringResource(R.string.this_workout)
            ),
            listOf(
                "${stringResource(R.string.workout_time)} (s)",
                "${stringResource(R.string.workout_active_time)} (s)",
                stringResource(R.string.this_workout),
            )
        )
        Scaffold(topBar = {
            TopAppBar (
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = {
                    Text(stringResource(R.string.workout_recap))
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navigator.navigateUp()
                            navigator.navigateUp()
                            navigator.navigate(
                                HistoryDestination()
                            )
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
            LazyColumn(
                state = listState,
                contentPadding = innerPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ){
                item {
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
                if (state.hasHRData) {
                    item {
                        ElevatedCard(Modifier.padding(dimensionResource(R.dimen.card_outside_padding))) {
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

                            Text(stringResource(R.string.heart_rate_bpm),
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
                                    decorations = zoneDecorations,
                                    startAxis = VerticalAxis.rememberStart(
                                        itemPlacer = remember {
                                            VerticalAxis.ItemPlacer.step(step = { 20.0 })
                                        }
                                    ),
                                    bottomAxis = HorizontalAxis.rememberBottom(
                                        valueFormatter = CartesianValueFormatter { _, value, _ ->
                                            secondsToMmSs(value.toInt())
                                        },
                                        itemPlacer = remember {
                                            ItemPlacer.aligned(
                                                spacing = { 60 },
                                                offset = { 300 }
                                            )
                                        }

                                    ),
                                ),
                                modelProducer = state.hrChartProducer,
                                modifier = Modifier.padding(8.dp),
                                scrollState = rememberVicoScrollState(
                                    scrollEnabled = false,
                                    initialScroll = Scroll.Absolute.End
                                ),
                            )
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.progression_over_time),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                    )
                }
                item{
                    if (records.size > 1){
                        val pagerState = rememberPagerState(pageCount = { 3 })
                        HorizontalPager(
                            state = pagerState,
                        ) { page ->
                            ElevatedCard(Modifier.padding(horizontal = dimensionResource(R.dimen.card_outside_padding))) {
                                val formatter = DateTimeFormatter.ofPattern("d MMM")
                                val legendItemLabelComponent = rememberTextComponent(
                                    TextStyle(vicoTheme.textColor)
                                )
                                val colors = vicoTheme.lineCartesianLayerColors.drop(1)
                                val primaryColor = vicoTheme.lineCartesianLayerColors[0]
                                val cartesianLayer = when (page) {
                                    // calories chart is column, others are lines
                                    1 -> rememberColumnCartesianLayer(
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
                                Text(titles.getOrNull(page) ?: "",
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
                                        startAxis = VerticalAxis.rememberStart(),
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
                                                    val currentLegend = legend.getOrNull(page) ?: emptyList()

                                                    currentLegend.forEachIndexed { index, label ->
                                                        if (label.isBlank()) return@forEachIndexed
                                                        val highlights = extraStore.getOrNull(
                                                            highlightSeriesKey
                                                        ) ?: emptyList()
                                                        val color = if (highlights.contains(index)) {
                                                            primaryColor
                                                        } else {
                                                            colors[index % colors.size]
                                                        }
                                                        val shape = if (
                                                        // calories chart has opposite shapes
                                                            (page == 1 && !highlights.contains(index))
                                                            || (page != 1 && highlights.contains(index))
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
                                    ),
                                    modelProducer = when (page) {
                                        0 -> state.volumeChartProducer
                                        1 -> state.caloriesChartProducer
                                        2 -> state.timeChartProducer
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
                if (state.workoutRecord != null) {
                    item {
                        Text(
                            stringResource(R.string.s_header1_highlights),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(vertical = 8.dp)
                        )
                    }
                    lazyGroupedCard(
                        colors = highlightsCardColors,
                    ) {
                        subCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Card(
                                        shape = MaterialTheme.shapes.extraExtraLarge,
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                        ),
                                    ) {
                                        Icon(
                                            Icons.Outlined.LocalFireDepartment,
                                            stringResource(R.string.calories_burned),
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .padding(8.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(
                                            R.string.calorie_consumption_i_kcal,
                                            state.workoutRecord!!.calories.toInt()
                                        )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                IconButton(
                                    onClick = { calorieDialogIsOpen.value = true },
                                    modifier = Modifier.weight(0.1f)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.HelpOutline,
                                        stringResource(R.string.help_icon_info)
                                    )
                                }
                            }
                        }
                        subCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Card(
                                        shape = MaterialTheme.shapes.extraExtraLarge,
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                        ),
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.weight_icon),
                                            stringResource(R.string.volume_lifted),
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .padding(8.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.total_volume) +
                                                ": %.2f ".format(
                                                    maybeKgToLb(
                                                        state.workoutRecord!!.volume.toFloat(),
                                                        state.imperialSystem
                                                    )
                                                ) + if (state.imperialSystem) stringResource(
                                            R.string.lb
                                        ) else stringResource(R.string.kg)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                IconButton(
                                    onClick = { volumeDialogIsOpen.value = true },
                                    modifier = Modifier.weight(0.1f)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.HelpOutline,
                                        stringResource(R.string.help_icon_info)
                                    )
                                }
                            }
                        }
                        subCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    shape = MaterialTheme.shapes.extraExtraLarge,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    ),
                                ) {
                                    Icon(
                                        Icons.Outlined.Schedule,
                                        stringResource(R.string.workout_time),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .padding(8.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.total_time) +
                                            DateUtils.formatElapsedTime(
                                                state.workoutRecord!!.durationSeconds
                                            )
                                )
                            }
                        }
                        subCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    shape = MaterialTheme.shapes.extraExtraLarge,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    ),
                                ) {
                                    Icon(
                                        Icons.Outlined.PendingActions,
                                        stringResource(R.string.workout_active_time),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .padding(8.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.workout_active_time) + ": " +
                                            DateUtils.formatElapsedTime(
                                                state.workoutRecord!!.activeTimeSeconds
                                            )
                                )
                            }
                        }
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
                            fontWeight = FontWeight.Bold,
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

/** BPM boundaries and colors for each HR zone */
data class HrZone(
    val labelResId: Int,
    val minBpm: Double,
    val maxBpm: Double,
    val color: Color
)

private val HrZones = listOf(
    // TODO: add zone res ids
    HrZone(0, 0.0,   100.0, Color(0xFF42A5F5)), // light blue
    HrZone(0, 100.0, 120.0, Color(0xFF66BB6A)), // green
    HrZone(0, 120.0, 140.0, Color(0xFFFFEE58)), // yellow
    HrZone(0, 140.0, 160.0, Color(0xFFFFA726)), // orange
    HrZone(0, 160.0, 220.0, Color(0xFFEF5350)), // red
)

/**
 * Formats elapsed seconds as "m:ss" for the X-axis labels.
 * e.g. 90 → "1:30"
 */
private fun secondsToMmSs(seconds: Int): CharSequence {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(minutes, secs)
}