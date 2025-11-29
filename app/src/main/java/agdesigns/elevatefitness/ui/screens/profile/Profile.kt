package agdesigns.elevatefitness.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.Sex
import agdesigns.elevatefitness.data.db.entity.Theme
import agdesigns.elevatefitness.navigation.BottomNavigationGraph
import agdesigns.elevatefitness.navigation.FadeTransition
import agdesigns.elevatefitness.ui.common.GroupedCard
import agdesigns.elevatefitness.ui.common.InfoDialog
import agdesigns.elevatefitness.utils.getLangPreferenceDropdownEntries
import agdesigns.elevatefitness.utils.plus
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import com.agdesignes.shared.maybeKgToLb
import com.agdesignes.shared.maybeLbToKg
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.android.awaitFrame
import java.time.ZonedDateTime
import kotlin.math.pow
import kotlin.math.roundToInt

@Destination<BottomNavigationGraph>(style = FadeTransition::class)
@Composable
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
fun Profile(
    destinationsNavigator: DestinationsNavigator,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profileState by viewModel.state.collectAsState()
    var editName by remember { mutableStateOf(false) }
    var userYear by remember { mutableStateOf("0") }
    val validUserYear by remember { derivedStateOf {
        userYear.toIntOrNull() != null && userYear.toInt() in 1900..ZonedDateTime.now().year
    }}
    LaunchedEffect(profileState.userYear){
        userYear = profileState.userYear.toString()
    }
    var editYear by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var bmiDialogueShown by remember { mutableStateOf(false) }

    InfoDialog(dialogueIsOpen = bmiDialogueShown,
        toggleDialogue = { bmiDialogueShown = !bmiDialogueShown }
    ) {
        Text(stringResource(R.string.bmi_info))
    }
    val createDocForDbBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(mimeType = "application/x-sqlite3")
    ) { uri ->
        uri?.let { viewModel.onEvent(ProfileEvent.ExportDatabase(it)) }
    }

    val openDocForDbRestore = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onEvent(ProfileEvent.ImportDatabase(it)) }
    }
    val createDocForPreferencesBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(mimeType = "application/json")
    ) { uri ->
        uri?.let { viewModel.onEvent(ProfileEvent.ExportPreferences(uri)) }
    }
    val openDocForPreferencesRestore = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onEvent(ProfileEvent.ImportPreferences(it)) }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = profileState.backupOutcomeResId?.let { stringResource(it) }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
        viewModel.onEvent(ProfileEvent.ResetOutcomeMessage)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding.plus(PaddingValues(vertical = 16.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp),
        ) {
            // Header Section
            item {
                val nameTextFieldState = rememberTextFieldState()
                ProfileHeader(
                    name = profileState.name,
                    editName = editName,
                    nameTextFieldState = nameTextFieldState,
                    onEditToggle = { newName ->
                        editName = !editName
                        if (!editName) {
                            viewModel.onEvent(ProfileEvent.UpdateName(newName))
                        }
                    },
                    onNameSubmit = { newName ->
                        keyboardController?.hide()
                        editName = false
                        viewModel.onEvent(ProfileEvent.UpdateName(newName))
                    }
                )
            }

            // Personal Information Section
            item {
                ProfileSection(title = stringResource(R.string.personal_information_title)) {
                    PersonalInfoContent(
                        profileState = profileState,
                        editYear = editYear,
                        userYear = userYear,
                        validUserYear = validUserYear,
                        onUserYearChange = { userYear = it },
                        onEditYearToggle = {
                            editYear = !editYear
                            if (!editYear) {
                                if (validUserYear) {
                                    viewModel.onEvent(ProfileEvent.UpdateAgeYear(userYear.toInt()))
                                } else {
                                    userYear = profileState.userYear.toString()
                                }
                            }
                        },
                        onYearSubmit = {
                            if (validUserYear) {
                                keyboardController?.hide()
                                viewModel.onEvent(ProfileEvent.UpdateAgeYear(userYear.toInt()))
                                editYear = false
                                focusManager.clearFocus()
                            }
                        },
                        onEditSex = {
                            viewModel.onEvent(ProfileEvent.UpdateSex(it))
                        }
                    )
                }
            }

            // Physical Measurements Section
            item {
                ProfileSection(title = stringResource(R.string.physical_measurements_title)) {
                    PhysicalMeasurementsContent(
                        profileState = profileState,
                        viewModel = viewModel,
                        keyboardController = keyboardController,
                        focusManager = focusManager,
                        onBmiInfoClick = { bmiDialogueShown = true }
                    )
                }
            }

            // Preferences Section
            item {
                Text(
                    stringResource(R.string.preferences_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.header_to_content_padding))
                )
                PreferencesContent(
                    profileState = profileState,
                    viewModel = viewModel
                )
            }
            item {
                Text(
                    text = stringResource(R.string.backup_and_restore),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.header_to_content_padding))
                )
                GroupedCard(items = listOf(
                    {
                        Text(
                            stringResource(R.string.backup_restore_info),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }, {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.plans_and_workouts),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (profileState.isBackupLoading) {
                                CircularWavyProgressIndicator()
                            } else {
                                Row {
                                    TextButton(onClick = {
                                        createDocForDbBackup.launch("backup.db")
                                    }) {
                                        Text(stringResource(R.string.backup_export))
                                    }
                                    TextButton(onClick = {
                                        openDocForDbRestore.launch(
                                            arrayOf(
                                                "application/x-sqlite3",
                                                "application/octet-stream",
                                                "*/*"
                                            )
                                        )
                                    }) {
                                        Text(stringResource(R.string.backup_import))
                                    }
                                }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.preferences),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (profileState.isPreferencesBackupLoading) {
                                CircularWavyProgressIndicator()
                            } else {
                                Row {
                                    TextButton(onClick = {
                                        createDocForPreferencesBackup.launch("settings.preferences_pb")
                                    }) {
                                        Text(stringResource(R.string.backup_export))
                                    }
                                    TextButton(onClick = {
                                        openDocForPreferencesRestore.launch(
                                            arrayOf(
                                                "application/json",
                                                "application/octet-stream",
                                                "*/*"
                                            )
                                        )
                                    }) {
                                        Text(stringResource(R.string.backup_import))
                                    }
                                }
                            }
                        }
                    }
                ), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow))
            }

            // Equipment Increments Section
            item {
                Text(
                    stringResource(R.string.equipment_increments_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.header_to_content_padding))
                )
                GroupedCard(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    items = listOf(
                        {
                            IncrementRow(
                                label = stringResource(R.string.barbell_increment),
                                value = profileState.incrementBarbell,
                                unit = if (profileState.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg),
                                onValueChange = { viewModel.onEvent(ProfileEvent.UpdateIncrementBarbell(it)) },
                                keyboardController = keyboardController,
                                focusManager = focusManager
                            )
                        }, {
                            IncrementRow(
                                label = stringResource(R.string.bodyweight_increment),
                                value = profileState.incrementBodyweight,
                                unit = if (profileState.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg),
                                onValueChange = { viewModel.onEvent(ProfileEvent.UpdateIncrementBodyweight(it)) },
                                keyboardController = keyboardController,
                                focusManager = focusManager
                            )
                        }, {
                            IncrementRow(
                                label = stringResource(R.string.cable_increment),
                                value = profileState.incrementCable,
                                unit = if (profileState.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg),
                                onValueChange = { viewModel.onEvent(ProfileEvent.UpdateIncrementCable(it)) },
                                keyboardController = keyboardController,
                                focusManager = focusManager
                            )
                        }, {
                            IncrementRow(
                                label = stringResource(R.string.dumbbell_increment),
                                value = profileState.incrementDumbbell,
                                unit = if (profileState.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg),
                                onValueChange = { viewModel.onEvent(ProfileEvent.UpdateIncrementDumbbell(it)) },
                                keyboardController = keyboardController,
                                focusManager = focusManager
                            )
                        }, {
                            IncrementRow(
                                label = stringResource(R.string.machine_increment),
                                value = profileState.incrementMachine,
                                unit = if (profileState.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg),
                                onValueChange = { viewModel.onEvent(ProfileEvent.UpdateIncrementMachine(it)) },
                                keyboardController = keyboardController,
                                focusManager = focusManager
                            )
                        }
                    )
                )
            }
            item {
                val uriHandler = LocalUriHandler.current
                val urls = listOf(
                    "https://github.com/alessioGalatolo/PerfectGymCoach/issues",
                    "https://github.com/alessioGalatolo/PerfectGymCoach/discussions",
                    "https://github.com/alessioGalatolo/PerfectGymCoach"
                )
                Text(
                    stringResource(R.string.feedback_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = dimensionResource(R.dimen.header_to_content_padding))
                )
                GroupedCard(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    items = listOf(
                        { ExternalLink(
                            title = stringResource(R.string.bug_report_title),
                            description = stringResource(R.string.bug_report_info),
                            leadingIcon = Icons.Default.BugReport
                        ) },
                        { ExternalLink(
                            title = stringResource(R.string.feature_requests_title),
                            description = stringResource(R.string.feature_request_info),
                            leadingIcon = Icons.Default.Feedback
                        ) },
                        { ExternalLink(
                            title = stringResource(R.string.source_code_title),
                            description = stringResource(R.string.source_code_info),
                            leadingIcon = Icons.Default.Code
                        ) }
                    ),
                    onClicks = urls.map { { uriHandler.openUri(it) } }
                )
            }
            item {
                ProfileSection(title = stringResource(R.string.acknowledgements_title)) {
                    Text(
                        stringResource(R.string.acknowledgements),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileHeader(
    name: String,
    editName: Boolean,
    nameTextFieldState: TextFieldState,
    onEditToggle: (String) -> Unit,
    onNameSubmit: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(editName) {
        if (editName) {
            nameTextFieldState.setTextAndSelectAll(name)
            awaitFrame()
            awaitFrame()
            awaitFrame()
            awaitFrame()
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!editName) {
                    Text(
                        text = if (name.isNotBlank())
                            stringResource(R.string.salute_user, name)
                        else
                            stringResource(R.string.what_is_your_name),
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onEditToggle(nameTextFieldState.text.toString()) },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Edit, stringResource(R.string.edit_icon_user_name))
                    }
                } else {
                    OutlinedTextField(
                        state = nameTextFieldState,
                        isError = nameTextFieldState.text.isBlank(),
                        label = { Text(stringResource(R.string.name)) },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            capitalization = KeyboardCapitalization.Words
                        ),
                        onKeyboardAction = KeyboardActionHandler {
                            onNameSubmit(nameTextFieldState.text.toString())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            focusedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                            cursorColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                    )
                    IconButton(
                        onClick = { onEditToggle(nameTextFieldState.text.toString()) },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Done, stringResource(R.string.done_icon))
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = dimensionResource(R.dimen.header_to_content_padding))
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PersonalInfoContent(
    profileState: ProfileState,
    editYear: Boolean,
    userYear: String,
    validUserYear: Boolean,
    onUserYearChange: (String) -> Unit,
    onEditYearToggle: () -> Unit,
    onYearSubmit: () -> Unit,
    onEditSex: (Sex) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Age Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val age = ZonedDateTime.now().year - profileState.userYear

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.age),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.i_years, age),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            IconButton(onClick = onEditYearToggle) {
                if (editYear) {
                    if (validUserYear) {
                        Icon(Icons.Default.Done, stringResource(R.string.done_icon))
                    } else {
                        Icon(Icons.Default.Close, stringResource(R.string.cancel_icon))
                    }
                } else {
                    Icon(Icons.Default.Edit, stringResource(R.string.edit_icon_age))
                }
            }
        }

        // Year Input Row (when editing)
        if (editYear) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.born_in),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = userYear,
                    onValueChange = onUserYearChange,
                    label = { Text(stringResource(R.string.year)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    isError = !validUserYear,
                    keyboardActions = KeyboardActions(onDone = { onYearSubmit() }),
                    supportingText = {
                        if (!validUserYear) {
                            Text(stringResource(R.string.please_enter_a_valid_year))
                        }
                    },
                    modifier = Modifier.width(120.dp)
                )
            }
        }

        // Biological Sex Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.sex),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            Row(
                Modifier
                    .padding(horizontal = 8.dp)
                    .weight(5f),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                Sex.entries.forEachIndexed { index, sex ->
                    val modifier = if (sex == profileState.sex)
                        Modifier.weight(1f + ButtonGroupDefaults.ExpandedRatio) // expanded
                    else Modifier.weight(1f)

                    ToggleButton(
                        checked = sex == profileState.sex,
                        onCheckedChange = { onEditSex(sex) },
                        modifier = modifier,
                        shapes =
                            when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                Sex.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                    ) {
                        Text(stringResource(sex.displayRes))
                    }
                }
            }
        }
    }
}

@Composable
fun PhysicalMeasurementsContent(
    profileState: ProfileState,
    viewModel: ProfileViewModel,
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager,
    onBmiInfoClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Weight
        MeasurementRow(
            label = stringResource(R.string.weight),
            value = maybeKgToLb(profileState.weight, profileState.imperialSystem),
            unit = if (profileState.imperialSystem) stringResource(R.string.lb) else stringResource(R.string.kg),
            onValueChange = { newWeight ->
                viewModel.onEvent(ProfileEvent.UpdateWeight(
                    maybeLbToKg(newWeight, profileState.imperialSystem)
                ))
            },
            keyboardController = keyboardController,
            focusManager = focusManager
        )

        // Height
        MeasurementRow(
            label = stringResource(R.string.height),
            value = if (profileState.imperialSystem) profileState.height / 2.54f else profileState.height,
            unit = if (profileState.imperialSystem) "in" else "cm",
            onValueChange = { newHeight ->
                viewModel.onEvent(ProfileEvent.UpdateHeight(
                    if (profileState.imperialSystem) newHeight * 2.54f else newHeight
                ))
            },
            keyboardController = keyboardController,
            focusManager = focusManager
        )

        // BMI
        val bmi = if (profileState.height != 0f)
            profileState.weight / (profileState.height/100).pow(2)
        else
            0f
        val bmiCategory = when {
            bmi < 18.5f -> stringResource(R.string.underweight)
            bmi > 30f -> stringResource(R.string.obese)
            bmi > 25f -> stringResource(R.string.overweight)
            else -> stringResource(R.string.normal)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.body_mass_index),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(bmi * 10).roundToInt() / 10.0} ($bmiCategory)",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            IconButton(onClick = onBmiInfoClick) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, stringResource(R.string.info_icon_bmi))
            }
        }
    }
}

@Composable
fun MeasurementRow(
    label: String,
    value: Float,
    unit: String,
    onValueChange: (Float) -> Unit,
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager
) {
    var isEditing by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(value.toString()) }
    val isValid by remember { derivedStateOf {
        textValue.toFloatOrNull() != null && textValue.toFloat() > 0
    }}

    LaunchedEffect(value) {
        if (!isEditing) {
            textValue = value.toString()
        }
    }

    val submitValue = {
        if (isValid) {
            onValueChange(textValue.toFloat())
            isEditing = false
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!isEditing) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${value.toInt()} $unit",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            IconButton(onClick = { isEditing = true }) {
                Icon(Icons.Default.Edit, stringResource(R.string.edit_icon_measurement_i, label))
            }
        } else {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text(label) },
                suffix = { Text(unit) },
                isError = !isValid,
                supportingText = {
                    if (!isValid) {
                        Text(stringResource(R.string.please_enter_a_valid_number))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submitValue() }),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    if (isValid) {
                        submitValue()
                    } else {
                        textValue = value.toString()
                        isEditing = false
                    }
                }
            ) {
                if (isValid) {
                    Icon(Icons.Default.Done, stringResource(R.string.done_icon))
                } else {
                    Icon(Icons.Default.Close, stringResource(R.string.cancel_icon))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreferencesContent(
    profileState: ProfileState,
    viewModel: ProfileViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // get locales from .xml config and add system default
    val locales = context.getLangPreferenceDropdownEntries().plus(
        Pair(
            "und",
            stringResource(R.string.system_default)
    ))
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GroupedCard(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            items = listOf({
                // Imperial System Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.use_imperial_system),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = profileState.imperialSystem,
                        onCheckedChange = { viewModel.onEvent(ProfileEvent.SwitchImperialSystem(it)) }
                    )
                }
            }, {
                // Dark Theme Dropdown
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.theme),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        Modifier
                            .padding(horizontal = 8.dp)
                            .weight(5.8f),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    ) {
                        // FIXME: "Same as system" overflows
                        Theme.entries.forEachIndexed { index, theme ->
                            val modifier = if (theme == profileState.theme)
                                Modifier.weight(1f + ButtonGroupDefaults.ExpandedRatio) // expanded
                            else Modifier.weight(1f)

                            var textAlign = when (index) {
                                0 -> TextAlign.End
                                Theme.entries.lastIndex -> TextAlign.Start
                                else -> TextAlign.Center
                            }
                            if (theme == profileState.theme) {
                                textAlign = TextAlign.Center
                            }
                            ToggleButton(
                                checked = theme == profileState.theme,
                                onCheckedChange = {
                                    viewModel.onEvent(ProfileEvent.UpdateTheme(theme))
                                },
                                modifier = modifier,
                                shapes =
                                    when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        Theme.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                            ) {
                                Text(
                                    stringResource(theme.displayRes),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = textAlign,
                                )
                            }
                        }
                    }
                }
            }, {
                // select language
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // LocaleManager is only available for tiramisu+
                        // compat version does not work very well with compose
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = locales[profileState.language] ?: stringResource(R.string.system_default),
                                onValueChange = { },
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = expanded
                                    )
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier.menuAnchor(
                                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    true
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }) {
                                    locales.forEach { locale ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    locale.value,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            },
                                            onClick = {
                                                expanded = false
                                                if (locale.key != "und") {
                                                    viewModel.onEvent(
                                                        ProfileEvent.ChangeLanguage(
                                                            locale.key
                                                        )
                                                    )
                                                } else {
                                                    viewModel.onEvent(ProfileEvent.ChangeLanguage(null))
                                                }
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                stringResource(R.string.inapp_language_selector_not_supported),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
            }, {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.lock_workout_scroll),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = profileState.lockHorizontalScroll,
                        onCheckedChange = { viewModel.onEvent(ProfileEvent.ToggleLockHorizontalScroll(it)) }
                    )
                }
            }, {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.auto_open_wearos),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = profileState.autoOpenWear,
                        onCheckedChange = { viewModel.onEvent(ProfileEvent.ToggleAutoOpenWear(it)) }
                    )
                }
            })
        )
    }
}

@Composable
fun IncrementRow(
    label: String,
    value: Float,
    unit: String,
    onValueChange: (Float) -> Unit,
    keyboardController: SoftwareKeyboardController?,
    focusManager: FocusManager
) {
    var isEditing by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(value.toString()) }
    val isValid by remember { derivedStateOf {
        textValue.toFloatOrNull() != null && textValue.toFloat() > 0
    }}

    LaunchedEffect(value) {
        if (!isEditing) {
            textValue = value.toString()
        }
    }

    val submitValue = {
        if (isValid) {
            onValueChange(textValue.toFloat())
            isEditing = false
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!isEditing) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$value $unit",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            IconButton(onClick = { isEditing = true }) {
                Icon(Icons.Default.Edit, stringResource(R.string.edit_icon_increment_i, label))
            }
        } else {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text(label) },
                suffix = { Text(unit) },
                isError = !isValid,
                supportingText = {
                    if (!isValid) {
                        Text(stringResource(R.string.please_enter_a_valid_number))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submitValue() }),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    if (isValid) {
                        submitValue()
                    } else {
                        textValue = value.toString()
                        isEditing = false
                    }
                }
            ) {
                if (isValid) {
                    Icon(Icons.Default.Done, stringResource(R.string.done_icon))
                } else {
                    Icon(Icons.Default.Close, stringResource(R.string.cancel_icon))
                }
            }
        }
    }
}

@Composable
fun ExternalLink(title: String, description: String, leadingIcon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                title,
            )
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.width(16.dp))
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            stringResource(R.string.open_in_icon_browser),
        )
    }
}