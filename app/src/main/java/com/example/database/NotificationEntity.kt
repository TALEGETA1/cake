package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val title: String,
    val message: String,
    val triggerTimeMillis: Long,
    val isSent: Boolean = false,
    val notificationType: String // "1_DAY_BEFORE", "3_HOURS_BEFORE", "30_MINUTES_BEFORE", "TODAY_REMINDER"
)
