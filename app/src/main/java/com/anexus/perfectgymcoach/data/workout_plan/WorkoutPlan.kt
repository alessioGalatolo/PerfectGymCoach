package com.anexus.perfectgymcoach.data.workout_plan

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Entity(tableName = "plan")
@Parcelize
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val name: String
    ): Parcelable