package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String, // e.g., Makanan, Transportasi, Hiburan, Belanja, Tagihan, Lainnya
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false // Track sync state to Cloud (Firebase/Supabase)
)
