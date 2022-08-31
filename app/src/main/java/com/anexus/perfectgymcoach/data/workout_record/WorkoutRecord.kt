package com.anexus.perfectgymcoach.data.workout_record

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.anexus.perfectgymcoach.data.workout_program.WorkoutProgram
import kotlinx.parcelize.Parcelize
import java.util.*

@Entity(
    foreignKeys = [ForeignKey(
        entity = WorkoutProgram::class,
        parentColumns = ["programId"],
        childColumns = ["extProgramId"],
//        onDelete = CASCADE
    )]
)
@Parcelize
data class WorkoutRecord(
    @PrimaryKey(autoGenerate = true) val workoutId: Long = 0L,
    val extProgramId: Long,
    val startDate: Calendar,
    val intensity: WorkoutIntensity = WorkoutIntensity.NORMAL_INTENSITY,
    val duration: Long = 0L// seconds
//    val calories: Long // MET value * weight_kg / 60 * n_minutes // MET value 3-6 based on intensity
) : Parcelable {
    enum class WorkoutIntensity(val description: String, val metValue: Float) {
        HIGH_INTENSITY("High intensity (...)", 6f),
        NORMAL_INTENSITY("Normal intensity (...)", 4.5f),
        LOW_INTENSITY("Low intensity (...)", 3f)
    }
}

@Parcelize
data class WorkoutRecordFinish(
    val workoutId: Long,
    val duration: Long,
    val intensity: WorkoutRecord.WorkoutIntensity
): Parcelable

@Parcelize
data class WorkoutRecordAndName(
    @PrimaryKey(autoGenerate = true) val workoutId: Long = 0L,
    val extProgramId: Long,
    val startDate: Calendar,
    val intensity: WorkoutRecord.WorkoutIntensity,
    val name: String,
    val duration: Long = 0L// seconds
//    val calories: Long
) : Parcelable