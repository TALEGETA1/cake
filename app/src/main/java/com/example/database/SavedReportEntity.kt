package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_reports")
data class SavedReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val generatedDateMillis: Long,
    val reportType: String, // "Daily", "Weekly", "Monthly"
    val totalRevenue: Double,
    val outstandingBalances: Double,
    val completedCount: Int,
    val cancelledCount: Int,
    val telebirrTotal: Double,
    val ebirrTotal: Double,
    val cbeTotal: Double,
    val cashTotal: Double
)
