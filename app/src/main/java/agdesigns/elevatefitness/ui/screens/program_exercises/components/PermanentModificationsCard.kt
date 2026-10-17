package agdesigns.elevatefitness.ui.screens.program_exercises.components

import agdesigns.elevatefitness.R
import agdesigns.elevatefitness.data.db.entity.WorkoutRecord
import agdesigns.elevatefitness.ui.screens.program_exercises.PermanentModificationProposal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PermanentModificationsCard(
    proposals: List<PermanentModificationProposal>,
    onApply: (List<PermanentModificationProposal>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // all proposals are accepted by default
    val accepted = remember(proposals) {
        mutableStateMapOf<Int, Boolean>().apply {
            proposals.indices.forEach { put(it, true) }
        }
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.permanent_modifications_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.permanent_modifications_desc),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            proposals.forEachIndexed { index, proposal ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = accepted[index] == true,
                            onValueChange = { accepted[index] = it },
                            role = Role.Checkbox
                        )
                ) {
                    Checkbox(
                        checked = accepted[index] == true,
                        onCheckedChange = null,
                        modifier = Modifier.padding(12.dp)
                    )
                    Text(
                        proposal.description(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { onApply(emptyList()) },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.permanent_modifications_dismiss))
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        onApply(proposals.filterIndexed { index, _ -> accepted[index] == true })
                    },
                    enabled = accepted.values.any { it },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.permanent_modifications_apply))
                }
            }
        }
    }
}

@Composable
private fun PermanentModificationProposal.description(): String {
    val targetName = targetWorkoutExercise?.name ?: ""
    return when (modification.modificationType) {
        WorkoutRecord.ModificationType.EXERCISE_ADDED ->
            stringResource(R.string.permanent_modification_add, targetName, sourceName)
        WorkoutRecord.ModificationType.EXERCISE_SKIPPED ->
            stringResource(R.string.permanent_modification_remove, sourceName)
        WorkoutRecord.ModificationType.EXERCISE_REPLACED ->
            stringResource(R.string.permanent_modification_replace, sourceName, targetName)
    }
}
