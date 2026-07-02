package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderNumber: String, // E.g., "CAKE-1000" + id
    val customerId: Int,
    val cakeName: String,
    val cakeType: String,
    val cakeSize: String,
    val flavor: String,
    val weight: Double,
    val colorTheme: String,
    val customDesignNotes: String? = null,
    val specialInstructions: String? = null,
    val quantity: Int = 1,
    val orderDateMillis: Long,
    val deliveryDateMillis: Long,
    val deliveryTime: String, // "08:00 AM" etc.
    val deliveryAddress: String? = null,
    val orderStatus: String, // "Scheduled", "In Progress", "Ready", "Completed", "Cancelled", "Postponed"
    val totalPrice: Double,
    val remainingBalance: Double,
    
    // Postponed info
    val postponedReason: String? = null,
    val postponedDateMillis: Long? = null,
    val postponedTime: String? = null,
    
    // Cancelled info
    val cancelledReason: String? = null,
    val cancelledBy: String? = null,
    val cancelledDateMillis: Long? = null,
    
    // Completion info
    val completionDateMillis: Long? = null,
    val completionTime: String? = null
)
