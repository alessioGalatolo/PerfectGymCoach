package agdesigns.elevatefitness.ui.screens

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
import androidx.hilt.navigation.compose.hiltViewModel
import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.Sex
import agdesigns.elevatefitness.data.Theme
import agdesigns.elevatefitness.ui.BottomNavigationGraph
import agdesigns.elevatefitness.ui.FadeTransition
import agdesigns.elevatefitness.ui.components.GroupedCard
import agdesigns.elevatefitness.ui.components.InfoDialog
import agdesigns.elevatefitness.ui.maybeKgToLb
import agdesigns.elevatefitness.ui.maybeLbToKg
import agdesigns.elevatefitness.viewmodels.ProfileEvent
import agdesigns.elevatefitness.viewmodels.ProfileState
import agdesigns.elevatefitness.viewmodels.ProfileViewModel
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt

@Destination<BottomNavigationGraph>(style = FadeTransition::class)
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
fun Profile(
    destinationsNavigator: DestinationsNavigator,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profileState by viewModel.state.collectAsState()
    var editName by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    LaunchedEffect(profileState.name){
        name = profileState.name
    }
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

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Spacer(Modifier.statusBarsPadding())
        }
        // Header Section
        item {
            ProfileHeader(
                name = profileState.name,
                editName = editName,
                nameValue = name,
                onNameChange = { name = it },
                onEditToggle = {
                    editName = !editName
                    if (!editName) {
                        viewModel.onEvent(ProfileEvent.UpdateName(name))
                    }
                },
                onNameSubmit = {
                    keyboardController?.hide()
                    editName = false
                    viewModel.onEvent(ProfileEvent.UpdateName(name))
                }
            )
        }

        // Personal Information Section
        item {
            ProfileSection(title = "Personal Information") {
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
            ProfileSection(title = "Physical Measurements") {
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
            ProfileSection(title = "Preferences") {
                PreferencesContent(
                    profileState = profileState,
                    viewModel = viewModel
                )
            }
        }

        // Equipment Increments Section
        item {
            Text(
                "Equipment Increments",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            GroupedCard(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                items = listOf(
                    {
                        IncrementRow(
                            label = "Barbell increment",
                            value = profileState.incrementBarbell,
                            unit = if (profileState.imperialSystem) "lb" else "kg",
                            onValueChange = { viewModel.onEvent(ProfileEvent.UpdateIncrementBarbell(it)) },
                            keyboardController = keyboardController,
                            focusManager = focusManager
                        )
                    }, {
                        IncrementRow(
                            label = "Bodyweight increment",
                            value = profileState.incrementBodyweight,
                            unit = if (profileState.imperialSystem) "lb" else "kg",
                            onValueChange = { viewModel.onEvent(ProfileEvent.UpdateIncrementBodyweight(it)) },
                            keyboardController = keyboardController,
                            focusManager = focusManager
                        )
                    }, {
                        IncrementRow(
                            label = "Cable increment",
                            value = profileState.incrementCable,
                            unit = if (profileState.imperialSystem) "lb" else "kg",
                            onValueChange = { viewModel.onEvent(ProfileEvent.UpdateIncrementCable(it)) },
                            keyboardController = keyboardController,
                            focusManager = focusManager
                        )
                    }, {
                        IncrementRow(
                            label = "Dumbbell increment",
                            value = profileState.incrementDumbbell,
                            unit = if (profileState.imperialSystem) "lb" else "kg",
                            onValueChange = { viewModel.onEvent(ProfileEvent.UpdateIncrementDumbbell(it)) },
                            keyboardController = keyboardController,
                            focusManager = focusManager
                        )
                    }, {
                        IncrementRow(
                            label = "Machine increment",
                            value = profileState.incrementMachine,
                            unit = if (profileState.imperialSystem) "lb" else "kg",
                            onValueChange = { viewModel.onEvent(ProfileEvent.UpdateIncrementMachine(it)) },
                            keyboardController = keyboardController,
                            focusManager = focusManager
                        )
                    }
                )
            )
        }
        item {
            ProfileSection(title = "Feedback") {
                FeedbackContent()
            }
        }
        item {
            ProfileSection(title = "Acknowledgements") {
                Text("I do not own any of the images used in this app. They are copyright free and were collected mostly through pexels and unsplash. Many thanks to all the artist that made their images freely available: Lukas, Alesia Kozik, Tima Miroshnichenko, Bruno Bueno, Cottonbro Studio, Andrea Piacquadio, Li Sun, Gustavo Fring, Ketut Subiyanto, Ivan Samkov, Mart Production, Jonathan Borba, Max Vakhtbovych, Anete Lusina, Monstera, Andres Ayrton, Pixabay, Daniel Apodaca, Sinitta Leunen, Leon Ardho, Anastasia Shuraeva, Ruslan Khmelevsky, Barbara Olsen, Anna Shvets, Ronald Slaton, Scott Webb.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ProfileHeader(
    name: String,
    editName: Boolean,
    nameValue: String,
    onNameChange: (String) -> Unit,
    onEditToggle: () -> Unit,
    onNameSubmit: () -> Unit
) {
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
                        text = "Hi, $name",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onEditToggle,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Edit, "Edit name")
                    }
                } else {
                    OutlinedTextField(
                        value = nameValue,
                        onValueChange = onNameChange,
                        label = { Text("Name") },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            capitalization = KeyboardCapitalization.Words
                        ),
                        keyboardActions = KeyboardActions(onDone = { onNameSubmit() }),
                        modifier = Modifier.weight(1f),
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
                        onClick = onEditToggle,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Done, "Done")
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
        modifier = Modifier.padding(bottom = 16.dp)
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
                    text = "Age",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$age years",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            IconButton(onClick = onEditYearToggle) {
                if (editYear) {
                    if (validUserYear) {
                        Icon(Icons.Default.Done, "Done")
                    } else {
                        Icon(Icons.Default.Close, "Cancel")
                    }
                } else {
                    Icon(Icons.Default.Edit, "Edit age")
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
                    text = "Born in:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = userYear,
                    onValueChange = onUserYearChange,
                    label = { Text("Year") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    isError = !validUserYear,
                    keyboardActions = KeyboardActions(onDone = { onYearSubmit() }),
                    supportingText = {
                        if (!validUserYear) {
                            Text("Please enter a valid year")
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
                text = "Sex:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            Row(
                Modifier.padding(horizontal = 8.dp).weight(5f),
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
                        Text(sex.displayName)
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
            label = "Weight",
            value = maybeKgToLb(profileState.weight, profileState.imperialSystem),
            unit = if (profileState.imperialSystem) "lb" else "kg",
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
            label = "Height",
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
            bmi < 18.5f -> "underweight"
            bmi > 30f -> "obese"
            bmi > 25f -> "overweight"
            else -> "normal"
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Body Mass Index",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(bmi * 10).roundToInt() / 10.0} ($bmiCategory)",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            IconButton(onClick = onBmiInfoClick) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, "BMI Info")
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
                Icon(Icons.Default.Edit, "Edit $label")
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
                        Text("Please enter a valid number")
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
                    Icon(Icons.Default.Done, "Done")
                } else {
                    Icon(Icons.Default.Close, "Cancel")
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
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Imperial System Switch
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Use imperial system:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = profileState.imperialSystem,
                onCheckedChange = { viewModel.onEvent(ProfileEvent.SwitchImperialSystem(it)) }
            )
        }

        // Dark Theme Dropdown
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Theme:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            Row(
                Modifier.padding(horizontal = 8.dp).weight(5.8f),
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
                            theme.displayName,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = textAlign,
                        )
                    }
                }
            }
        }
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
                Icon(Icons.Default.Edit, "Edit $label")
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
                        Text("Please enter a valid number")
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
                    Icon(Icons.Default.Done, "Done")
                } else {
                    Icon(Icons.Default.Close, "Cancel")
                }
            }
        }
    }
}

@Composable
fun FeedbackContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ExternalLink(
            title = "Bug report",
            description = "Will open externally, once there tap on 'New issue'. Requires a GitHub account.",
            url = "https://github.com/alessioGalatolo/PerfectGymCoach/issues",
            leadingIcon = Icons.Default.BugReport
        )
        ExternalLink(
            title = "Feature requests",
            description = "Will open externally, once there tap on 'New discussion'. Requires a GitHub account.",
            url = "https://github.com/alessioGalatolo/PerfectGymCoach/discussions",
            leadingIcon = Icons.Default.Feedback
        )
        ExternalLink(
            title = "Source code",
            description = "The entirety of this app is open source. Feel free to contribute or just look around.",
            url = "https://github.com/alessioGalatolo/PerfectGymCoach",
            leadingIcon = Icons.Default.Code
        )
    }
}

@Composable
fun ExternalLink(title: String, description: String, url: String, leadingIcon: ImageVector? = null) {
    val uriHandler = LocalUriHandler.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
            .clip(CardDefaults.shape)
            .combinedClickable(
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    uriHandler.openUri(url)
                },
                onLongClick = { }
            )
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
            "Open in browser",
        )
    }
}