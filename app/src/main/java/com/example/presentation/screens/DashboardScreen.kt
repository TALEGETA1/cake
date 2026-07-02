package com.example.presentation.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.OrderWithCustomer
import com.example.presentation.CakeViewModel
import com.example.utils.EthiopianCalendarHelper
import com.example.utils.rememberCountdownText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: CakeViewModel,
    onNavigateToOrderDetails: (Int) -> Unit,
    onNavigateToCreateOrder: () -> Unit,
    onNavigateToSchedule: () -> Unit
) {
    val orders by viewModel.ordersWithCustomer.collectAsStateWithLifecycle()
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()
    val useEthiopian by viewModel.useEthiopianCalendar.collectAsStateWithLifecycle()
    val bName by viewModel.businessName.collectAsStateWithLifecycle()
    
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    // Calculations
    val today = LocalDate.now()
    val todayOrders = orders.filter {
        val dDate = Instant.ofEpochMilli(it.order.deliveryDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        dDate == today
    }
    val upcomingOrders = orders.filter {
        val dDate = Instant.ofEpochMilli(it.order.deliveryDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        dDate.isAfter(today) && it.order.orderStatus != "Completed" && it.order.orderStatus != "Cancelled"
    }
    val priorityOrders = orders.filter {
        it.order.orderStatus != "Completed" && it.order.orderStatus != "Cancelled"
    }.sortedWith(
        compareBy<OrderWithCustomer> { it.order.deliveryDateMillis }
            .thenBy {
                when (it.order.orderStatus) {
                    "Ready" -> 1
                    "In Progress" -> 2
                    "Scheduled" -> 3
                    "Postponed" -> 4
                    else -> 5
                }
            }
            .thenBy { it.order.deliveryTime }
    )
    val completedCount = orders.count { it.order.orderStatus == "Completed" }
    val cancelledCount = orders.count { it.order.orderStatus == "Cancelled" }
    val postponedCount = orders.count { it.order.orderStatus == "Postponed" }

    val totalRevenue = payments.sumOf { it.amount }
    val remainingBalanceTotal = orders.filter { it.order.orderStatus != "Cancelled" }.sumOf { it.order.remainingBalance }
    val pendingPaymentsCount = orders.count { it.order.remainingBalance > 0 && it.order.orderStatus != "Cancelled" }

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
                        Column {
                            Text(bName, fontWeight = FontWeight.Bold)
                            Text(
                                "Dashboard • " + EthiopianCalendarHelper.formatDate(today, useEthiopian),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.useEthiopianCalendar.value = !useEthiopian }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Switch Calendar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showAddCustomerDialog = true },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = "New Customer") },
                    text = { Text("New Customer") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.testTag("add_customer_fab")
                )
                ExtendedFloatingActionButton(
                    onClick = onNavigateToCreateOrder,
                    icon = { Icon(Icons.Default.Add, contentDescription = "New Order") },
                    text = { Text("New Order") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_order_fab")
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Financial Banner
            item {
                FinancialSummaryCard(
                    revenue = totalRevenue,
                    outstanding = remainingBalanceTotal,
                    pendingCount = pendingPaymentsCount,
                    currency = viewModel.currency.value
                )
            }

            // Next Delivery Widget
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Next Delivery Priority",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = onNavigateToSchedule) {
                        Text("View All Schedule")
                    }
                }
            }

            if (priorityOrders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No pending deliveries.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(priorityOrders) { orderWithCustomer ->
                    OrderCompactRow(
                        orderWithCustomer = orderWithCustomer,
                        useEthiopian = useEthiopian,
                        currency = viewModel.currency.value,
                        onClick = { onNavigateToOrderDetails(orderWithCustomer.order.id) }
                    )
                }
            }
        }

        if (showAddCustomerDialog) {
            var name by remember { mutableStateOf("") }
            var phone by remember { mutableStateOf("") }
            var address by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddCustomerDialog = false },
                title = { Text("Register New Customer", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Customer Name *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Optional Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                viewModel.addCustomer(name, phone, address.ifBlank { null }, null)
                                showAddCustomerDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Register")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCustomerDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun FinancialSummaryCard(
    revenue: Double,
    outstanding: Double,
    pendingCount: Int,
    currency: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                "Total Revenue Collected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
                "${String.format("%.2f", revenue)} $currency",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Outstanding Balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        "${String.format("%.2f", outstanding)} $currency",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Pending Payments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        "$pendingCount Orders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun StatusGridItem(
    title: String,
    count: Int,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(54.dp)
                    .align(Alignment.BottomEnd)
            )
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
fun NextOrderWidget(
    orderWithCustomer: OrderWithCustomer,
    useEthiopian: Boolean,
    currency: String,
    onClick: () -> Unit
) {
    val countdown = rememberCountdownText(
        deliveryDateMillis = orderWithCustomer.order.deliveryDateMillis,
        deliveryTimeStr = orderWithCustomer.order.deliveryTime,
        status = orderWithCustomer.order.orderStatus
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("next_order_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text(
                        orderWithCustomer.order.orderStatus.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = orderWithCustomer.order.deliveryTime,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
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

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = orderWithCustomer.order.cakeName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Customer: ${orderWithCustomer.customer.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date: " + EthiopianCalendarHelper.formatDate(
                        Instant.ofEpochMilli(orderWithCustomer.order.deliveryDateMillis)
                            .atZone(ZoneId.systemDefault()).toLocalDate(),
                        useEthiopian
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Bal: ${orderWithCustomer.order.remainingBalance} $currency",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (orderWithCustomer.order.remainingBalance > 0) Color(0xFFE53935) else Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
fun OrderCompactRow(
    orderWithCustomer: OrderWithCustomer,
    useEthiopian: Boolean,
    currency: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Priority / Status Colored Indicator Dot or Icon
                val (statusColor, statusIcon) = when (orderWithCustomer.order.orderStatus) {
                    "Ready" -> Color(0xFF4CAF50) to Icons.Default.CheckCircle
                    "In Progress" -> Color(0xFFFF9800) to Icons.Default.Star
                    "Scheduled" -> Color(0xFF2196F3) to Icons.Default.CalendarToday
                    "Postponed" -> Color(0xFF9E9E9E) to Icons.Default.Info
                    else -> MaterialTheme.colorScheme.primary to Icons.Default.Info
                }
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = orderWithCustomer.order.orderStatus,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = orderWithCustomer.order.cakeName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Mini status badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor.copy(alpha = 0.12f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = orderWithCustomer.order.orderStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                    val dateText = EthiopianCalendarHelper.formatDate(
                        Instant.ofEpochMilli(orderWithCustomer.order.deliveryDateMillis)
                            .atZone(ZoneId.systemDefault()).toLocalDate(),
                        useEthiopian
                    )
                    val countdown = rememberCountdownText(
                        deliveryDateMillis = orderWithCustomer.order.deliveryDateMillis,
                        deliveryTimeStr = orderWithCustomer.order.deliveryTime,
                        status = orderWithCustomer.order.orderStatus
                    )
                    val subtitle = buildString {
                        append("${orderWithCustomer.customer.name} • $dateText • ${orderWithCustomer.order.deliveryTime}")
                        if (countdown.isNotEmpty()) {
                            append(" $countdown")
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${orderWithCustomer.order.totalPrice} $currency",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = if (orderWithCustomer.order.remainingBalance > 0) {
                        "Bal: ${orderWithCustomer.order.remainingBalance}"
                    } else "Paid",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (orderWithCustomer.order.remainingBalance > 0) Color(0xFFE53935) else Color(0xFF4CAF50)
                )
            }
        }
    }
}
