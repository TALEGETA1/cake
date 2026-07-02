package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val paymentType: String, // "First Payment (Deposit)", "Remaining Payment", "Full Payment"
    val amount: Double,
    val paymentMethod: String, // "Telebirr", "E-Birr", "CBE Bank", "Cash"
    val paymentDateMillis: Long,
    val paymentTime: String,
    val referenceNumber: String? = null,
    val collectedBy: String? = null,
    val paymentNote: String? = null
)
