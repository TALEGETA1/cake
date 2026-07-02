package com.example.database

import androidx.room.Embedded
import androidx.room.Relation

data class OrderWithCustomer(
    @Embedded val order: OrderEntity,
    @Relation(
        parentColumn = "customerId",
        entityColumn = "id"
    )
    val customer: CustomerEntity
)
