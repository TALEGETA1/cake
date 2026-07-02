package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CakeDao {

    // --- CUSTOMERS ---
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Int): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)


    // --- ORDERS ---
    @Query("SELECT * FROM orders ORDER BY deliveryDateMillis ASC, deliveryTime ASC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Transaction
    @Query("SELECT * FROM orders ORDER BY deliveryDateMillis ASC, deliveryTime ASC")
    fun getOrdersWithCustomerFlow(): Flow<List<OrderWithCustomer>>

    @Transaction
    @Query("SELECT * FROM orders ORDER BY deliveryDateMillis ASC, deliveryTime ASC")
    suspend fun getOrdersWithCustomerList(): List<OrderWithCustomer>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: Int): OrderEntity?

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderWithCustomerById(id: Int): OrderWithCustomer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Delete
    suspend fun deleteOrder(order: OrderEntity)


    // --- PAYMENTS ---
    @Query("SELECT * FROM payments WHERE orderId = :orderId ORDER BY paymentDateMillis DESC")
    fun getPaymentsForOrder(orderId: Int): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE orderId = :orderId ORDER BY paymentDateMillis ASC")
    suspend fun getPaymentsForOrderList(orderId: Int): List<PaymentEntity>

    @Query("SELECT * FROM payments ORDER BY paymentDateMillis DESC")
    fun getAllPaymentsFlow(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY paymentDateMillis DESC")
    suspend fun getAllPayments(): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)


    // --- NOTIFICATIONS ---
    @Query("SELECT * FROM notifications ORDER BY triggerTimeMillis ASC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isSent = 0 AND triggerTimeMillis <= :currentTime")
    suspend fun getPendingNotifications(currentTime: Long): List<NotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isSent = 1 WHERE id = :id")
    suspend fun markNotificationAsSent(id: Int)

    @Query("DELETE FROM notifications WHERE orderId = :orderId")
    suspend fun deleteNotificationsForOrder(orderId: Int)


    // --- SAVED REPORTS ---
    @Query("SELECT * FROM saved_reports ORDER BY generatedDateMillis DESC")
    fun getAllSavedReports(): Flow<List<SavedReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedReport(report: SavedReportEntity): Long

    @Delete
    suspend fun deleteSavedReport(report: SavedReportEntity)
}
