package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_config")
data class BudgetConfig(
    @PrimaryKey
    val id: Int = 1,
    val monthlyBudget: Double = 3500000.0,
    val alertThresholdPercent: Int = 80,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val reminderEnabled: Boolean = true
)
