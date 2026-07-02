package com.example.repository

import com.example.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class CakeRepository(private val cakeDao: CakeDao) {

    // --- CUSTOMERS ---
    val allCustomers: Flow<List<CustomerEntity>> = cakeDao.getAllCustomers()

    suspend fun getCustomerById(id: Int): CustomerEntity? {
        return cakeDao.getCustomerById(id)
    }

    suspend fun insertCustomer(customer: CustomerEntity): Long {
        return cakeDao.insertCustomer(customer)
    }

    suspend fun deleteCustomer(customer: CustomerEntity) {
        cakeDao.deleteCustomer(customer)
    }


    // --- ORDERS ---
    val allOrders: Flow<List<OrderEntity>> = cakeDao.getAllOrders()
    val allOrdersWithCustomer: Flow<List<OrderWithCustomer>> = cakeDao.getOrdersWithCustomerFlow()

    suspend fun getOrderById(id: Int): OrderEntity? {
        return cakeDao.getOrderById(id)
    }

    suspend fun getOrderWithCustomerById(id: Int): OrderWithCustomer? {
        return cakeDao.getOrderWithCustomerById(id)
    }

    suspend fun insertOrder(order: OrderEntity): Long {
        return cakeDao.insertOrder(order)
    }

    suspend fun updateOrder(order: OrderEntity) {
        cakeDao.updateOrder(order)
    }

    suspend fun deleteOrder(order: OrderEntity) {
        cakeDao.deleteNotificationsForOrder(order.id)
        cakeDao.deleteOrder(order)
    }

    /**
     * Mark an order as completed and record the completion time.
     */
    suspend fun completeOrder(orderId: Int, completionTime: String, requireZeroBalance: Boolean): Result<Unit> {
        val order = cakeDao.getOrderById(orderId) ?: return Result.failure(Exception("Order not found"))
        
        if (requireZeroBalance && order.remainingBalance > 0) {
            return Result.failure(Exception("Cannot complete order. Remaining balance must be zero!"))
        }

        val updatedOrder = order.copy(
            orderStatus = "Completed",
            completionDateMillis = System.currentTimeMillis(),
            completionTime = completionTime
        )
        cakeDao.updateOrder(updatedOrder)
        return Result.success(Unit)
    }

    /**
     * Cancel an order with a reason and registrar.
     */
    suspend fun cancelOrder(orderId: Int, reason: String, cancelledBy: String) {
        val order = cakeDao.getOrderById(orderId) ?: return
        val updatedOrder = order.copy(
            orderStatus = "Cancelled",
            cancelledReason = reason,
            cancelledBy = cancelledBy,
            cancelledDateMillis = System.currentTimeMillis()
        )
        cakeDao.updateOrder(updatedOrder)
    }

    /**
     * Postpone an order to a new date and time.
     */
    suspend fun postponeOrder(orderId: Int, newDateMillis: Long, newTime: String, reason: String) {
        val order = cakeDao.getOrderById(orderId) ?: return
        val updatedOrder = order.copy(
            orderStatus = "Postponed",
            deliveryDateMillis = newDateMillis,
            deliveryTime = newTime,
            postponedReason = reason,
            postponedDateMillis = System.currentTimeMillis(),
            postponedTime = newTime
        )
        cakeDao.updateOrder(updatedOrder)
        
        // Re-schedule notifications for this postponed order
        scheduleNotificationsForOrder(updatedOrder)
    }


    // --- PAYMENTS ---
    val allPaymentsFlow: Flow<List<PaymentEntity>> = cakeDao.getAllPaymentsFlow()

    fun getPaymentsForOrder(orderId: Int): Flow<List<PaymentEntity>> {
        return cakeDao.getPaymentsForOrder(orderId)
    }

    /**
     * Records a payment against an order, updating the order's remaining balance.
     */
    suspend fun addPayment(payment: PaymentEntity): Result<Long> {
        val order = cakeDao.getOrderById(payment.orderId) ?: return Result.failure(Exception("Order not found"))
        
        // Ensure amount is valid
        if (payment.amount <= 0) {
            return Result.failure(Exception("Payment amount must be greater than zero"))
        }

        // Validate remaining balance
        if (payment.amount > order.remainingBalance) {
            return Result.failure(Exception("Cannot save payment greater than remaining balance (ETB ${order.remainingBalance})"))
        }

        val newRemainingBalance = order.remainingBalance - payment.amount
        val paymentId = cakeDao.insertPayment(payment)
        
        // Update the order remaining balance in database
        val updatedOrder = order.copy(remainingBalance = newRemainingBalance)
        cakeDao.updateOrder(updatedOrder)
        
        return Result.success(paymentId)
    }

    /**
     * Deletes a payment and restores the order's remaining balance.
     */
    suspend fun deletePayment(payment: PaymentEntity) {
        val order = cakeDao.getOrderById(payment.orderId) ?: return
        cakeDao.deletePayment(payment)
        
        // Recalculate remaining balance from all surviving payments
        val payments = cakeDao.getPaymentsForOrderList(payment.orderId)
        val totalPaid = payments.sumOf { it.amount }
        val remainingBalance = order.totalPrice - totalPaid
        
        val updatedOrder = order.copy(remainingBalance = remainingBalance)
        cakeDao.updateOrder(updatedOrder)
    }


    // --- NOTIFICATIONS & REMINDERS ---
    val allNotifications: Flow<List<NotificationEntity>> = cakeDao.getAllNotifications()

    suspend fun insertNotification(notification: NotificationEntity): Long {
        return cakeDao.insertNotification(notification)
    }

    suspend fun markNotificationAsSent(id: Int) {
        cakeDao.markNotificationAsSent(id)
    }

    suspend fun getPendingNotifications(currentTime: Long): List<NotificationEntity> {
        return cakeDao.getPendingNotifications(currentTime)
    }

    /**
     * Dynamically generates and schedules standard notification reminders in local Room table.
     */
    suspend fun scheduleNotificationsForOrder(order: OrderEntity) {
        // Clear old pending notifications for this order
        cakeDao.deleteNotificationsForOrder(order.id)
        
        if (order.orderStatus == "Cancelled" || order.orderStatus == "Completed") return

        val deliveryTime = order.deliveryDateMillis
        
        // Notification types:
        // 1 day before (24 hours before)
        val oneDayBefore = deliveryTime - (24 * 60 * 60 * 1000)
        if (oneDayBefore > System.currentTimeMillis()) {
            cakeDao.insertNotification(
                NotificationEntity(
                    orderId = order.id,
                    title = "Upcoming Cake Delivery Tomorrow",
                    message = "Order #${order.orderNumber} for ${order.cakeName} is scheduled for delivery tomorrow at ${order.deliveryTime}.",
                    triggerTimeMillis = oneDayBefore,
                    notificationType = "1_DAY_BEFORE"
                )
            )
        }

        // 3 hours before
        val threeHoursBefore = deliveryTime - (3 * 60 * 60 * 1000)
        if (threeHoursBefore > System.currentTimeMillis()) {
            cakeDao.insertNotification(
                NotificationEntity(
                    orderId = order.id,
                    title = "Cake Delivery in 3 Hours",
                    message = "Order #${order.orderNumber} for ${order.cakeName} is due in 3 hours at ${order.deliveryTime}.",
                    triggerTimeMillis = threeHoursBefore,
                    notificationType = "3_HOURS_BEFORE"
                )
            )
        }

        // 2 hours before (ALARM RINGING!)
        val twoHoursBefore = deliveryTime - (2 * 60 * 60 * 1000)
        if (twoHoursBefore > System.currentTimeMillis()) {
            cakeDao.insertNotification(
                NotificationEntity(
                    orderId = order.id,
                    title = "⏰ Cake Delivery Alarm!",
                    message = "ALERT: Order #${order.orderNumber} for ${order.cakeName} is due in 2 hours! Time to prepare for delivery.",
                    triggerTimeMillis = twoHoursBefore,
                    notificationType = "2_HOURS_BEFORE"
                )
            )
        }

        // 30 minutes before
        val thirtyMinsBefore = deliveryTime - (30 * 60 * 1000)
        if (thirtyMinsBefore > System.currentTimeMillis()) {
            cakeDao.insertNotification(
                NotificationEntity(
                    orderId = order.id,
                    title = "Cake Delivery in 30 Minutes",
                    message = "Order #${order.orderNumber} for ${order.cakeName} is due in 30 minutes at ${order.deliveryTime}.",
                    triggerTimeMillis = thirtyMinsBefore,
                    notificationType = "30_MINUTES_BEFORE"
                )
            )
        }
    }


    // --- SAVED REPORTS ---
    val allSavedReports: Flow<List<SavedReportEntity>> = cakeDao.getAllSavedReports()

    suspend fun insertSavedReport(report: SavedReportEntity): Long {
        return cakeDao.insertSavedReport(report)
    }

    suspend fun deleteSavedReport(report: SavedReportEntity) {
        cakeDao.deleteSavedReport(report)
    }
}
