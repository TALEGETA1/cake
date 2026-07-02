package com.example.presentation

import android.app.Application
import android.os.Environment
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.database.*
import com.example.repository.CakeRepository
import com.example.utils.EthiopianCalendarHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class OrderDraft(
    val selectedCustomerId: Int? = null,
    val cakeName: String = "",
    val cakeType: String = "Birthday",
    val cakeSize: String = "8 inch (Medium)",
    val flavor: String = "Chocolate",
    val weightString: String = "1.5",
    val colorTheme: String = "",
    val customDesignNotes: String = "",
    val specialInstructions: String = "",
    val deliveryAddress: String = "",
    val quantityString: String = "1",
    val totalPriceString: String = "2500",
    val deliveryDate: LocalDate = LocalDate.now().plusDays(1),
    val deliveryTime: String = "09:00 AM",
    val currentStep: Int = 1
)

class CakeViewModel(
    private val application: Application,
    private val repository: CakeRepository
) : AndroidViewModel(application) {

    // --- ORDER DRAFT ---
    val orderDraft = MutableStateFlow(OrderDraft())

    fun updateOrderDraft(draft: OrderDraft) {
        orderDraft.value = draft
    }

    fun clearOrderDraft() {
        orderDraft.value = OrderDraft()
    }

    // --- SEARCH & FILTER ---
    val searchQuery = MutableStateFlow("")
    val scheduleFilter = MutableStateFlow("All") // "Today", "Tomorrow", "This Week", "All"
    val useEthiopianCalendar = MutableStateFlow(true)

    // --- BUSINESS SETTINGS (stored in memory, editable in settings) ---
    val businessName = MutableStateFlow("Exodus Art Cake")
    val businessPhone = MutableStateFlow("+251 939 454 780")
    val currency = MutableStateFlow("ETB")
    val requireZeroBalanceToComplete = MutableStateFlow(true)

    // --- DATA STREAMS ---
    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ordersWithCustomer: StateFlow<List<OrderWithCustomer>> = repository.allOrdersWithCustomer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedReports: StateFlow<List<SavedReportEntity>> = repository.allSavedReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<PaymentEntity>> = repository.allPaymentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- SEARCHED & FILTERED ORDERS ---
    val filteredOrders: StateFlow<List<OrderWithCustomer>> = combine(
        ordersWithCustomer,
        searchQuery,
        scheduleFilter
    ) { orders, query, filter ->
        var list = orders

        // 1. Search filter
        if (query.isNotBlank()) {
            list = list.filter {
                it.order.orderNumber.contains(query, ignoreCase = true) ||
                it.customer.name.contains(query, ignoreCase = true) ||
                it.customer.phone.contains(query, ignoreCase = true) ||
                it.order.cakeName.contains(query, ignoreCase = true)
            }
        }

        // 2. Schedule filter (Date range filter)
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val endOfWeek = today.plusDays(7)

        list = when (filter) {
            "Today" -> list.filter {
                val deliveryDate = Instant.ofEpochMilli(it.order.deliveryDateMillis)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                deliveryDate == today
            }
            "Tomorrow" -> list.filter {
                val deliveryDate = Instant.ofEpochMilli(it.order.deliveryDateMillis)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                deliveryDate == tomorrow
            }
            "This Week" -> list.filter {
                val deliveryDate = Instant.ofEpochMilli(it.order.deliveryDateMillis)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                !deliveryDate.isBefore(today) && !deliveryDate.isAfter(endOfWeek)
            }
            else -> list // "All"
        }

        // Sort: Delivery Date, then Delivery Time
        list.sortedWith(
            compareBy<OrderWithCustomer> { it.order.deliveryDateMillis }
                .thenBy { it.order.deliveryTime }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- OPERATIONS ---

    // CUSTOMERS
    fun addCustomer(name: String, phone: String, address: String?, notes: String?) {
        viewModelScope.launch {
            val customer = CustomerEntity(
                name = name,
                phone = phone,
                address = address,
                notes = notes
            )
            repository.insertCustomer(customer)
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    // ORDERS
    fun createOrder(
        customerId: Int,
        cakeName: String,
        cakeType: String,
        cakeSize: String,
        flavor: String,
        weight: Double,
        colorTheme: String,
        customDesignNotes: String?,
        specialInstructions: String?,
        quantity: Int,
        orderDateMillis: Long,
        deliveryDateMillis: Long,
        deliveryTime: String,
        deliveryAddress: String?,
        totalPrice: Double
    ): Result<Long> {
        if (deliveryTime.isBlank()) {
            return Result.failure(Exception("Delivery time cannot be empty!"))
        }
        if (customerId <= 0) {
            return Result.failure(Exception("An order must be associated with a valid customer!"))
        }

        val order = OrderEntity(
            orderNumber = "CAKE-${1000 + (0..9999).random()}", // formatted random unique id
            customerId = customerId,
            cakeName = cakeName,
            cakeType = cakeType,
            cakeSize = cakeSize,
            flavor = flavor,
            weight = weight,
            colorTheme = colorTheme,
            customDesignNotes = customDesignNotes,
            specialInstructions = specialInstructions,
            quantity = quantity,
            orderDateMillis = orderDateMillis,
            deliveryDateMillis = deliveryDateMillis,
            deliveryTime = deliveryTime,
            deliveryAddress = deliveryAddress,
            orderStatus = "Scheduled",
            totalPrice = totalPrice,
            remainingBalance = totalPrice
        )

        val resultFlow = MutableStateFlow<Result<Long>?>(null)
        viewModelScope.launch {
            try {
                val id = repository.insertOrder(order)
                val insertedOrder = repository.getOrderById(id.toInt())
                if (insertedOrder != null) {
                    repository.scheduleNotificationsForOrder(insertedOrder)
                }
                resultFlow.value = Result.success(id)
            } catch (e: Exception) {
                resultFlow.value = Result.failure(e)
            }
        }
        return Result.success(1L) // fallback placeholder, operations are done in suspend
    }

    // We make a direct suspend creation for strict verification
    suspend fun createOrderSuspend(
        customerId: Int,
        cakeName: String,
        cakeType: String,
        cakeSize: String,
        flavor: String,
        weight: Double,
        colorTheme: String,
        customDesignNotes: String?,
        specialInstructions: String?,
        quantity: Int,
        orderDateMillis: Long,
        deliveryDateMillis: Long,
        deliveryTime: String,
        deliveryAddress: String?,
        totalPrice: Double
    ): Result<Long> {
        if (deliveryTime.isBlank()) {
            return Result.failure(Exception("Delivery time cannot be empty!"))
        }
        if (customerId <= 0) {
            return Result.failure(Exception("An order must be associated with a valid customer!"))
        }

        val order = OrderEntity(
            orderNumber = "CAKE-${1000 + (100..999).random()}",
            customerId = customerId,
            cakeName = cakeName,
            cakeType = cakeType,
            cakeSize = cakeSize,
            flavor = flavor,
            weight = weight,
            colorTheme = colorTheme,
            customDesignNotes = customDesignNotes,
            specialInstructions = specialInstructions,
            quantity = quantity,
            orderDateMillis = orderDateMillis,
            deliveryDateMillis = deliveryDateMillis,
            deliveryTime = deliveryTime,
            deliveryAddress = deliveryAddress,
            orderStatus = "Scheduled",
            totalPrice = totalPrice,
            remainingBalance = totalPrice
        )

        return try {
            val id = repository.insertOrder(order)
            val insertedOrder = repository.getOrderById(id.toInt())
            if (insertedOrder != null) {
                repository.scheduleNotificationsForOrder(insertedOrder)
            }
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun completeOrder(orderId: Int, completionTime: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.completeOrder(orderId, completionTime, requireZeroBalanceToComplete.value)
            onResult(result)
        }
    }

    fun cancelOrder(orderId: Int, reason: String, cancelledBy: String) {
        viewModelScope.launch {
            repository.cancelOrder(orderId, reason, cancelledBy)
        }
    }

    fun postponeOrder(orderId: Int, newDateMillis: Long, newTime: String, reason: String) {
        viewModelScope.launch {
            repository.postponeOrder(orderId, newDateMillis, newTime, reason)
        }
    }

    fun deleteOrder(order: OrderEntity) {
        viewModelScope.launch {
            repository.deleteOrder(order)
        }
    }

    // PAYMENTS
    suspend fun addPaymentSuspend(
        orderId: Int,
        paymentType: String,
        amount: Double,
        paymentMethod: String,
        referenceNumber: String?,
        collectedBy: String?,
        paymentNote: String?
    ): Result<Long> {
        val payment = PaymentEntity(
            orderId = orderId,
            paymentType = paymentType,
            amount = amount,
            paymentMethod = paymentMethod,
            paymentDateMillis = System.currentTimeMillis(),
            paymentTime = LocalTimeNowString(),
            referenceNumber = referenceNumber,
            collectedBy = collectedBy,
            paymentNote = paymentNote
        )
        return repository.addPayment(payment)
    }

    fun deletePayment(payment: PaymentEntity) {
        viewModelScope.launch {
            repository.deletePayment(payment)
        }
    }

    fun getPaymentsForOrderFlow(orderId: Int): Flow<List<PaymentEntity>> {
        return repository.getPaymentsForOrder(orderId)
    }

    private fun LocalTimeNowString(): String {
        return java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US))
    }


    // --- REPORTS GENERATION & EXPORT ---
    fun generateReportAndSave(reportType: String) {
        viewModelScope.launch {
            val orders = ordersWithCustomer.value
            val payments = allPayments.value

            val now = System.currentTimeMillis()
            val startMillis = when (reportType) {
                "Daily" -> now - (24 * 60 * 60 * 1000L)
                "Weekly" -> now - (7 * 24 * 60 * 60 * 1000L)
                "Monthly" -> now - (30 * 24 * 60 * 60 * 1000L)
                else -> 0L
            }

            val filteredOrders = if (startMillis > 0) {
                orders.filter { it.order.orderDateMillis >= startMillis }
            } else {
                orders
            }

            val filteredPayments = if (startMillis > 0) {
                payments.filter { it.paymentDateMillis >= startMillis }
            } else {
                payments
            }

            // 1. Calculations
            val completedCount = filteredOrders.count { it.order.orderStatus == "Completed" }
            val cancelledCount = filteredOrders.count { it.order.orderStatus == "Cancelled" }
            val outstandingBalances = filteredOrders.sumOf { it.order.remainingBalance }

            val totalRevenue = filteredPayments.sumOf { it.amount }
            
            val ordersReceived = filteredOrders.size.toDouble()
            val activeOrders = filteredOrders.count { it.order.orderStatus != "Completed" && it.order.orderStatus != "Cancelled" }.toDouble()

            val title = "$reportType Report - ${LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy", java.util.Locale.US))}"

            val report = SavedReportEntity(
                title = title,
                generatedDateMillis = System.currentTimeMillis(),
                reportType = reportType,
                totalRevenue = totalRevenue,
                outstandingBalances = outstandingBalances,
                completedCount = completedCount,
                cancelledCount = cancelledCount,
                telebirrTotal = ordersReceived,
                ebirrTotal = activeOrders,
                cbeTotal = 0.0,
                cashTotal = -1.0 // Marker for new report format
            )

            // Save to DB
            repository.insertSavedReport(report)

            // Export to PDF/Excel (creates formatted files in external files directory)
            exportReportFiles(report)
        }
    }

    private fun exportReportFiles(report: SavedReportEntity) {
        try {
            val dir = application.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (dir != null && !dir.exists()) {
                dir.mkdirs()
            }

            val isNewFormat = report.cashTotal == -1.0
            val rec = if (isNewFormat) report.telebirrTotal.toInt() else (report.completedCount + report.cancelledCount)
            val act = if (isNewFormat) report.ebirrTotal.toInt() else 0

            // 1. Export as Excel CSV
            val csvFile = File(dir, "${report.reportType}_Report_${System.currentTimeMillis()}.csv")
            FileWriter(csvFile).use { writer ->
                writer.append("Exodus Art Cake - ${report.reportType} Report\n")
                writer.append("Title,${report.title}\n")
                writer.append("Generated Date,${Instant.ofEpochMilli(report.generatedDateMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}\n")
                writer.append("\n")
                writer.append("Metric,Value\n")
                writer.append("Orders Received,${rec}\n")
                writer.append("Orders Delivered,${report.completedCount}\n")
                writer.append("Total Amount Received,${report.totalRevenue} ETB\n")
                writer.append("Active Orders,${act}\n")
                writer.append("Outstanding Balance,${report.outstandingBalances} ETB\n")
                writer.append("Cancelled Orders,${report.cancelledCount}\n")
            }

            // 2. Export as PDF Text Report
            val pdfFile = File(dir, "${report.reportType}_Report_${System.currentTimeMillis()}.pdf")
            FileWriter(pdfFile).use { writer ->
                writer.append("==================================================\n")
                writer.append("              CAKE ORDER SCHEDULER                \n")
                writer.append("               ${report.reportType.uppercase()} REPORT                  \n")
                writer.append("==================================================\n")
                writer.append("Title: ${report.title}\n")
                writer.append("Date Generated: ${Instant.ofEpochMilli(report.generatedDateMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", java.util.Locale.US))}\n")
                writer.append("--------------------------------------------------\n\n")
                writer.append("SUMMARY STATISTICS:\n")
                writer.append(" - Orders Received:     $rec\n")
                writer.append(" - Orders Delivered:    ${report.completedCount}\n")
                writer.append(" - Total Amount Recv:   ${report.totalRevenue} ETB\n")
                writer.append(" - Active Orders:       $act\n")
                writer.append(" - Outstanding Balance: ${report.outstandingBalances} ETB\n")
                writer.append(" - Cancelled Orders:    ${report.cancelledCount}\n")
                writer.append("==================================================\n")
            }

            Toast.makeText(
                application,
                "Report exported! CSV: ${csvFile.name}, PDF: ${pdfFile.name}",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(application, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteSavedReport(report: SavedReportEntity) {
        viewModelScope.launch {
            repository.deleteSavedReport(report)
        }
    }
}

class CakeViewModelFactory(
    private val application: Application,
    private val repository: CakeRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CakeViewModel::class.java)) {
            return CakeViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
