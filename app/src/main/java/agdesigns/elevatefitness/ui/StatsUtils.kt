package agdesigns.elevatefitness.ui

import agdesigns.elevatefitness.data.exercise.Exercise
import agdesigns.elevatefitness.data.exercise.ExerciseRecordAndEquipment
import agdesigns.elevatefitness.viewmodels.ExerciseStats
import agdesigns.elevatefitness.viewmodels.PersonalRecord
import android.util.Log
import com.jaikeerthick.composable_graphs.composables.donut.model.DonutData
import com.jaikeerthick.composable_graphs.composables.line.model.LineData
import com.jaikeerthick.composable_graphs.composables.pie.model.PieData
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.exp
import kotlin.math.pow

fun computeVolume(weights: List<Float>, reps: List<Int>, tare: Float): Float {
    if (weights.size != reps.size)
        throw Exception("Weights and reps must be the same size")
    var volume = 0f
    volume += tare * reps.sum()
    volume += weights.zip(reps) { weight, rep ->
        weight * rep
    }.sum()
    return volume
}

fun generateVolumeProgressionData(
    records: List<ExerciseRecordAndEquipment>,
    maxRecords: Int = 20, // max points to display
    maxLabels: Int = 5 // among the points, max n of labels
): List<LineData> {
    val formatter = DateTimeFormatter.ofPattern("MMM dd")

    val dateVolumePairs = records.map {
        Pair(it.date, computeVolume(it.weights, it.reps, it.tare))
    }
    val dateVolumePairsNeeded = mutableListOf<Pair<ZonedDateTime, Float>>()
    // get maxRecords but get avg from missing ones
    var cumulatedVolume = 0f
    var cumulatedN = 0
    var saveRecordEvery = dateVolumePairs.size.floorDiv(maxRecords)
    if (dateVolumePairs.size.mod(maxRecords) != 0) {
        saveRecordEvery += 1
    }
    dateVolumePairs.forEachIndexed { index, pair ->
        cumulatedVolume += pair.second
        cumulatedN++
        if (index % saveRecordEvery == 0) {
            val pair2add = Pair(pair.first, cumulatedVolume / cumulatedN)
            dateVolumePairsNeeded.add(pair2add)
            cumulatedVolume = 0f
            cumulatedN = 0
        }

    }
    Log.d("StatisticsViewModel", "dateVolumePairsNeeded: $dateVolumePairsNeeded")
    var saveLabelEvery = dateVolumePairsNeeded.size.floorDiv(maxLabels)
    if (dateVolumePairsNeeded.size.mod(maxLabels) != 0) {
        saveLabelEvery += 1
    }
    return dateVolumePairsNeeded.sortedBy { it.first }.mapIndexed { index, pair ->
        if (index % saveLabelEvery == 0) {
            LineData(
                pair.first.format(formatter),
                pair.second
            )
        } else {
            LineData(
                "",
                pair.second
            )
        }
    }
}

enum class OneRepMaxFormula(val displayName: String) {
    EPLEY("Epley"),
    BRZYCKI("Brzycki"),
    LOMBARDI("Lombardi"),
    OCONNER("O'Conner"),
    WATHEN("Wathen"),
    LANDER("Lander")
}

fun estimate1RM(record: ExerciseRecordAndEquipment, formula: OneRepMaxFormula = OneRepMaxFormula.EPLEY): Float {
    return record.reps.zip(record.weights).maxOfOrNull { (reps, weight) ->
        val totalWeight = weight + record.tare
        oneRepMax(totalWeight, reps, formula)
    } ?: 0f
}

fun oneRepMax(weight: Float, reps: Int, formula: OneRepMaxFormula): Float {
    return when (formula) {
        OneRepMaxFormula.EPLEY -> weight * (1 + reps / 30f)
        OneRepMaxFormula.BRZYCKI -> if (reps in 1..10) weight * (36f / (37f - reps)) else weight
        OneRepMaxFormula.LOMBARDI -> weight * reps.toDouble().pow(0.10).toFloat()
        OneRepMaxFormula.OCONNER -> weight * (1 + 0.025f * reps)
        OneRepMaxFormula.WATHEN -> weight * (100f / (48.8f + 53.8f * exp(-0.075f * reps)))
        OneRepMaxFormula.LANDER -> weight * (100f / (101.3f - 2.67123f * reps))
    }
}