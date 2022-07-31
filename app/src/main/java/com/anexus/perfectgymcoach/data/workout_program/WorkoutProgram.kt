package com.anexus.perfectgymcoach.data.workout_program

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import com.anexus.perfectgymcoach.data.workout_plan.WorkoutPlan
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "program",
    foreignKeys = [ForeignKey(
        entity = WorkoutPlan::class,
        parentColumns = ["planId"],
        childColumns = ["extPlanId"],
        onDelete = CASCADE
    )]
)
@Parcelize
data class WorkoutProgram(
    @PrimaryKey(autoGenerate = true) val programId: Int = 0,
    val extPlanId: Int,
    val name: String
) : Parcelable