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
    val duration: Long = 0L// seconds
//    val calories: Long
) : Parcelable

@Parcelize
data class WorkoutRecordFinish(val workoutId: Long, val duration: Long): Parcelable