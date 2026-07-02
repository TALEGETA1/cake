package com.example.presentation.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.OrderWithCustomer
import com.example.database.PaymentEntity
import com.example.presentation.CakeViewModel
import com.example.utils.EthiopianCalendarHelper
import com.example.utils.rememberCountdownText
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Int,
    viewModel: CakeViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val orders by viewModel.ordersWithCustomer.collectAsStateWithLifecycle()
    val allPayments by viewModel.allPayments.collectAsStateWithLifecycle()
    val useEthiopian by viewModel.useEthiopianCalendar.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val requireZeroBalance by viewModel.requireZeroBalanceToComplete.collectAsStateWithLifecycle()

    val orderWithCustomer = orders.find { it.order.id == orderId }
    val orderPayments = allPayments.filter { it.orderId == orderId }

    // Dialog control states
    var isRecordingPayment by remember { mutableStateOf(false) }
    var isCancellingOrder by remember { mutableStateOf(false) }
    var isPostponingOrder by remember { mutableStateOf(false) }
    var isDeletingOrder by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<PaymentEntity?>(null) }

    // New Payment States
    var paymentAmount by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf("First Payment (Deposit)") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var referenceNumber by remember { mutableStateOf("") }
    var collectedBy by remember { mutableStateOf("") }
    var paymentNote by remember { mutableStateOf("") }

    // Cancellation States
    var cancelReason by remember { mutableStateOf("") }
    var cancelledByPerson by remember { mutableStateOf("") }

    // Postponement States
    var postponedDate by remember { mutableStateOf(LocalDate.now().plusDays(2)) }
    var postponedTime by remember { mutableStateOf("10:00 AM") }
    var postponeReason by remember { mutableStateOf("") }

    if (orderWithCustomer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Order not found or deleted")
        }
        return
    }

    val order = orderWithCustomer.order
    val customer = orderWithCustomer.customer

    val statusColor = when (order.orderStatus) {
        "Completed" -> Color(0xFF4CAF50)
        "Cancelled" -> Color(0xFFE53935)
        "Postponed" -> Color(0xFFFFB300)
        "In Progress" -> Color(0xFF1E88E5)
        else -> MaterialTheme.colorScheme.primary
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_app_icon),
                                contentDescription = "Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text("Order Details", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isDeletingOrder = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Order", tint = Color(0xFFE53935))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card with status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = order.orderNumber,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = statusColor
                            )
                            Badge(containerColor = statusColor) {
                                Text(
                                    text = order.orderStatus.uppercase(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(order.cakeName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        val themeText = if (order.colorTheme.isNotBlank()) " • Theme: ${order.colorTheme}" else ""
                        Text("Flavor: ${order.flavor} • Weight: ${order.weight} kg$themeText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!order.specialInstructions.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Special Instructions: ${order.specialInstructions}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Customer Profile Card
            item {
                Text("Customer & Delivery Profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Customer Name", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(customer.name, fontWeight = FontWeight.Bold)
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Phone Number", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(customer.phone, fontWeight = FontWeight.Bold)
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Delivery / Shipping Address", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(order.deliveryAddress ?: "No address recorded", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Schedule Timeline Card
            item {
                Text("Delivery Schedule", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("Scheduled Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    EthiopianCalendarHelper.formatDate(
                                        Instant.ofEpochMilli(order.deliveryDateMillis).atZone(ZoneId.systemDefault()).toLocalDate(),
                                        useEthiopian
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Estimated Time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(order.deliveryTime, fontWeight = FontWeight.Bold)
                                val countdown = rememberCountdownText(
                                    deliveryDateMillis = order.deliveryDateMillis,
                                    deliveryTimeStr = order.deliveryTime,
                                    status = order.orderStatus
                                )
                                if (countdown.isNotEmpty()) {
                                    Text(
                                        text = countdown,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (countdown.contains("overdue")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Financial Summary & Management
            item {
                Text("Pricing & Balance Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Price")
                            Text("${order.totalPrice} $currency", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Paid")
                            val paid = order.totalPrice - order.remainingBalance
                            Text("${String.format("%.2f", paid)} $currency", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Remaining Balance", fontWeight = FontWeight.Bold)
                            Text("${order.remainingBalance} $currency", fontWeight = FontWeight.ExtraBold, color = if (order.remainingBalance > 0) Color(0xFFE53935) else Color(0xFF4CAF50))
                        }
                    }
                }
            }

            // Action Buttons Group (Skip if cancelled/completed)
            if (order.orderStatus != "Cancelled" && order.orderStatus != "Completed") {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { isRecordingPayment = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Record New Payment")
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Complete button
                            Button(
                                onClick = {
                                    val nowTime = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US))
                                    viewModel.completeOrder(order.id, nowTime) { result ->
                                        if (result.isSuccess) {
                                            Toast.makeText(context, "Order marked as COMPLETED!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, result.exceptionOrNull()?.message ?: "Completion failed", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Complete", color = Color.White)
                            }

                            // Postpone Button
                            Button(
                                onClick = { isPostponingOrder = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Update, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Postpone", color = Color.White)
                            }

                            // Cancel Button
                            Button(
                                onClick = { isCancellingOrder = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel", color = Color.White)
                            }
                        }
                    }
                }
            }

            // Payment History Log Section
            item {
                Text("Payment History Log", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            if (orderPayments.isEmpty()) {
                item {
                    Text("No payments have been recorded for this order.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(orderPayments) { payment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(payment.paymentType, fontWeight = FontWeight.Bold)
                                    Text("via ${payment.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${payment.amount} $currency", fontWeight = FontWeight.ExtraBold, color = Color(0xFF4CAF50))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(onClick = { paymentToDelete = payment }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            if (!payment.referenceNumber.isNullOrBlank()) {
                                Text("Ref: ${payment.referenceNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!payment.collectedBy.isNullOrBlank()) {
                                Text("Collected by: ${payment.collectedBy}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!payment.paymentNote.isNullOrBlank()) {
                                Text("Note: ${payment.paymentNote}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // DIALOGS:
        // 1. Record Payment Dialog
        if (isRecordingPayment) {
            val alreadyPaid = order.totalPrice - order.remainingBalance
            val enteredAmount = paymentAmount.toDoubleOrNull() ?: 0.0
            val remainingAfter = (order.remainingBalance - enteredAmount).coerceAtLeast(0.0)

            AlertDialog(
                onDismissRequest = { isRecordingPayment = false },
                title = { Text("Record Payment", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Pricing & Balance Summary Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Price:", style = MaterialTheme.typography.bodyMedium)
                                    Text("${order.totalPrice} $currency", fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Already Paid:", style = MaterialTheme.typography.bodyMedium)
                                    Text("${String.format("%.2f", alreadyPaid)} $currency", fontWeight = FontWeight.Medium, color = Color(0xFF4CAF50))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Current Remaining:", style = MaterialTheme.typography.bodyMedium)
                                    Text("${order.remainingBalance} $currency", fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                                }
                                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Remaining After Payment:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    val remainingColor = if (remainingAfter <= 0.0) Color(0xFF4CAF50) else Color(0xFFE53935)
                                    Text(
                                        text = if (remainingAfter <= 0.0) "0.00 $currency (Fully Paid!)" else "${String.format("%.2f", remainingAfter)} $currency",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = remainingColor
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = paymentAmount,
                            onValueChange = { paymentAmount = it },
                            label = { Text("Amount Paid *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Payment Type
                        Text("Payment Type:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = paymentType == "First Payment (Deposit)", onClick = { paymentType = "First Payment (Deposit)" })
                            Text("Deposit", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            RadioButton(selected = paymentType == "Remaining Payment", onClick = { paymentType = "Remaining Payment" })
                            Text("Remaining", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            RadioButton(selected = paymentType == "Full Payment", onClick = { paymentType = "Full Payment" })
                            Text("Full", fontSize = 13.sp)
                        }

                        // Auto-assign paymentType when amount changes
                        LaunchedEffect(paymentAmount) {
                            val amt = paymentAmount.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                if (alreadyPaid == 0.0 && amt >= order.totalPrice) {
                                    paymentType = "Full Payment"
                                } else if (alreadyPaid == 0.0 && amt < order.totalPrice) {
                                    paymentType = "First Payment (Deposit)"
                                } else if (alreadyPaid > 0.0 && amt >= order.remainingBalance) {
                                    paymentType = "Full Payment"
                                } else {
                                    paymentType = "Remaining Payment"
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = paymentAmount.toDoubleOrNull()
                            if (amount == null || amount <= 0) {
                                Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            coroutineScope.launch {
                                val result = viewModel.addPaymentSuspend(
                                    orderId = order.id,
                                    paymentType = paymentType,
                                    amount = amount,
                                    paymentMethod = paymentMethod,
                                    referenceNumber = null,
                                    collectedBy = null,
                                    paymentNote = null
                                )

                                if (result.isSuccess) {
                                    Toast.makeText(context, "Payment recorded successfully!", Toast.LENGTH_SHORT).show()
                                    paymentAmount = ""
                                    referenceNumber = ""
                                    collectedBy = ""
                                    paymentNote = ""
                                    isRecordingPayment = false
                                } else {
                                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "Payment failed", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Text("Save Payment")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isRecordingPayment = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 2. Cancel Order Dialog
        if (isCancellingOrder) {
            AlertDialog(
                onDismissRequest = { isCancellingOrder = false },
                title = { Text("Cancel Order", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Are you sure you want to cancel this order? This action is irreversible.", color = Color(0xFFE53935))
                        OutlinedTextField(
                            value = cancelReason,
                            onValueChange = { cancelReason = it },
                            label = { Text("Cancellation Reason *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = cancelledByPerson,
                            onValueChange = { cancelledByPerson = it },
                            label = { Text("Cancelled By *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (cancelReason.isNotBlank() && cancelledByPerson.isNotBlank()) {
                                viewModel.cancelOrder(order.id, cancelReason, cancelledByPerson)
                                isCancellingOrder = false
                            } else {
                                Toast.makeText(context, "Please enter cancel reason and name", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("Confirm Cancellation")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isCancellingOrder = false }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        // 3. Postpone Order Dialog
        if (isPostponingOrder) {
            AlertDialog(
                onDismissRequest = { isPostponingOrder = false },
                title = { Text("Postpone Order Delivery", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Date picker row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val calendar = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            postponedDate = LocalDate.of(y, m + 1, d)
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("New Date:")
                            Text(EthiopianCalendarHelper.formatDate(postponedDate, useEthiopian), fontWeight = FontWeight.Bold)
                        }

                        // Time picker row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val calendar = Calendar.getInstance()
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            val ampm = if (h >= 12) "PM" else "AM"
                                            val hours = if (h % 12 == 0) 12 else h % 12
                                            postponedTime = String.format("%02d:%02d %s", hours, m, ampm)
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                }
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("New Time:")
                            Text(postponedTime, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = postponeReason,
                            onValueChange = { postponeReason = it },
                            label = { Text("Postpone Reason *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (postponeReason.isNotBlank()) {
                                val epoch = postponedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                viewModel.postponeOrder(order.id, epoch, postponedTime, postponeReason)
                                isPostponingOrder = false
                            } else {
                                Toast.makeText(context, "Postpone reason is required", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                    ) {
                        Text("Postpone Schedule")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isPostponingOrder = false }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        // 4. Delete Order Confirmation Dialog
        if (isDeletingOrder) {
            AlertDialog(
                onDismissRequest = { isDeletingOrder = false },
                title = { Text("Delete Order", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete this order? All associated payments and data will be permanently removed. This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteOrder(order)
                            isDeletingOrder = false
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Order")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isDeletingOrder = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 5. Delete Payment Confirmation Dialog
        if (paymentToDelete != null) {
            AlertDialog(
                onDismissRequest = { paymentToDelete = null },
                title = { Text("Delete Payment", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete this payment record of ${paymentToDelete?.amount} $currency? The order remaining balance will be adjusted accordingly.") },
                confirmButton = {
                    Button(
                        onClick = {
                            paymentToDelete?.let { viewModel.deletePayment(it) }
                            paymentToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Payment")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { paymentToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
