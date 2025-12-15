package agdesigns.elevatefitness.utils

import agdesigns.elevatefitness.data.db.entity.ExerciseRecordAndEquipment
import agdesigns.elevatefitness.ui.screens.statistics.TimeFrame
import android.util.Log
import agdesigns.elevatefitness.shared.Equipment
import agdesigns.elevatefitness.shared.maybeKgToLb
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.exp
import kotlin.math.pow

fun computeVolume(weights: List<Float>, reps: List<Int>, tare: Float, equipment: Equipment): Float {
    if (weights.size != reps.size)
        throw Exception("Weights and reps must be the same size")
    var volume = 0f
    volume += tare * reps.sum()
    volume += weights.zip(reps) { weight, rep ->
        when (equipment) {
            Equipment.BARBELL, Equipment.DUMBBELL -> weight * rep * 2
            else -> weight * rep  // FIXME: some cables exercise should be multiplied by 2 e.g., cable fly
        }
    }.sum()
    return volume
}

fun generateVolumeProgressionData(
    records: List<ExerciseRecordAndEquipment>,
    timeFrame: TimeFrame,
    useImperialSystem: Boolean
): List<Pair<String, Float>> {
    val zone: ZoneId = records.firstOrNull()?.date?.zone ?: ZoneId.systemDefault()
    val today: ZonedDateTime = ZonedDateTime.now(zone).truncatedTo(ChronoUnit.DAYS)
    val locale: Locale = Locale.getDefault()

    val weekLabelFmt = DateTimeFormatter.ofPattern("EEE", locale)  // e.g., Sun
    val dayLabelFmt = DateTimeFormatter.ofPattern("d MMM", locale)  // e.g., 30 Aug
    val monthLabelFmt = DateTimeFormatter.ofPattern("MMM", locale)  // e.g., Aug
    val yearLabelFmt = DateTimeFormatter.ofPattern("MMM yyyy", locale)  // e.g., Aug 2025

    // Pre-compute volumes per record in the working zone
    val dateVolumePairs: List<Pair<ZonedDateTime, Float>> = records.map {
        it.date.withZoneSameInstant(zone) to maybeKgToLb(
            computeVolume(it.weights, it.reps, it.tare, it.equipment),
            useImperialSystem
        )
    }

    fun sumByDay(): Map<LocalDate, Float> =
        dateVolumePairs
            .groupBy { it.first.toLocalDate() }
            .mapValues { (_, list) -> list.fold(0f) { acc, pair -> acc + pair.second } }

    fun sumByMonth(): Map<YearMonth, Float> =
        dateVolumePairs
            .groupBy { YearMonth.from(it.first) }
            .mapValues { (_, list) -> list.fold(0f) { acc, pair -> acc + pair.second } }

    return when (timeFrame) {
        TimeFrame.WEEK -> {
            val sums = sumByDay()
            val start = today.minusDays(6)
            (0..6).map { i ->
                val day = start.plusDays(i.toLong())
                weekLabelFmt.format(day) to (sums[day.toLocalDate()] ?: 0f)
            }
        }

        TimeFrame.MONTH -> {
            if (dateVolumePairs.isEmpty()) return emptyList()
            val sums = sumByDay()
            val startDate = dateVolumePairs.minOf { it.first.toLocalDate() } // only back to oldest entry
            val endDate = today.toLocalDate()
            val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toInt()
            (0..daysBetween).map { i ->
                val d = startDate.plusDays(i.toLong())
                val anchor = d.atStartOfDay(zone)
                dayLabelFmt.format(anchor) to (sums[d] ?: 0f)
            }
        }

        TimeFrame.YEAR -> {
            if (dateVolumePairs.isEmpty()) return emptyList()
            val sums = sumByMonth()
            val startYm = YearMonth.from(dateVolumePairs.minOf { it.first }) // only back to oldest entry
            val endYm = YearMonth.from(today)
            val monthsBetween = ChronoUnit.MONTHS.between(startYm.atDay(1), endYm.atDay(1)).toInt()
            (0..monthsBetween).map { i ->
                val ym = startYm.plusMonths(i.toLong())
                val anchor = ym.atDay(1).atStartOfDay(zone)
                monthLabelFmt.format(anchor) to (sums[ym] ?: 0f)
            }
        }

        TimeFrame.ALL_TIME -> {
            if (dateVolumePairs.isEmpty()) return emptyList()
            val sums = sumByMonth()
            val startYm = sums.keys.minOrNull()!!
            val endYm = YearMonth.from(today)
            val monthsBetween = ChronoUnit.MONTHS.between(startYm.atDay(1), endYm.atDay(1)).toInt()
            (0..monthsBetween).map { i ->
                val ym = startYm.plusMonths(i.toLong())
                val anchor = ym.atDay(1).atStartOfDay(zone)
                yearLabelFmt.format(anchor) to (sums[ym] ?: 0f)
            }
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