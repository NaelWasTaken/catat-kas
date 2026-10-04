package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quick_templates")
data class QuickTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val defaultAmount: Double,
    val iconKey: String = "coffee", // fuel, food, coffee, parking, groceries, electric, entertainment
    val orderIndex: Int = 0
)
