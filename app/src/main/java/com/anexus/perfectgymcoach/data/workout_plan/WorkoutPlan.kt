package com.anexus.perfectgymcoach.data.workout_plan

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Entity(tableName = "plan")
@Parcelize
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true) val planId: Long = 0L,
    val name: String,
    val currentProgram: Int = 0 // The index of the upcoming program after ordering
): Parcelable

@Parcelize
data class WorkoutPlanUpdateProgram(
    val planId: Long,
    val currentProgram: Int
): Parcelable
