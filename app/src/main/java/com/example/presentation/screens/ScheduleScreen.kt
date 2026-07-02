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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
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
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: CakeViewModel,
    onNavigateToOrderDetails: (Int) -> Unit
) {
    val orders by viewModel.filteredOrders.collectAsStateWithLifecycle()
    val filter by viewModel.scheduleFilter.collectAsStateWithLifecycle()
    val useEthiopian by viewModel.useEthiopianCalendar.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    val filterOptions = listOf("Today", "Tomorrow", "This Week", "All")

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
                        Text("Delivery Schedule", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    // Switch calendar mode
                    Row(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { viewModel.useEthiopianCalendar.value = !useEthiopian }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (useEthiopian) "Ethiopian Calendar" else "Gregorian Calendar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Horizontal Filter bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { option ->
                    val isSelected = filter == option
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.scheduleFilter.value = option },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = "No orders scheduled",
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No Deliveries Scheduled",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "There are no orders matching '$filter' filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders) { orderWithCustomer ->
                        ScheduleItemCard(
                            orderWithCustomer = orderWithCustomer,
                            useEthiopian = useEthiopian,
                            currency = currency,
                            onClick = { onNavigateToOrderDetails(orderWithCustomer.order.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleItemCard(
    orderWithCustomer: OrderWithCustomer,
    useEthiopian: Boolean,
    currency: String,
    onClick: () -> Unit
) {
    val date = Instant.ofEpochMilli(orderWithCustomer.order.deliveryDateMillis)
        .atZone(ZoneId.systemDefault()).toLocalDate()
    val formattedDate = EthiopianCalendarHelper.formatDate(date, useEthiopian)
    val countdown = rememberCountdownText(
        deliveryDateMillis = orderWithCustomer.order.deliveryDateMillis,
        deliveryTimeStr = orderWithCustomer.order.deliveryTime,
        status = orderWithCustomer.order.orderStatus
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("schedule_item_${orderWithCustomer.order.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Date/Time Badge
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 10.dp, horizontal = 14.dp)
                    .width(68.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = orderWithCustomer.order.deliveryTime,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Order descriptions
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = orderWithCustomer.order.cakeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Customer: " + orderWithCustomer.customer.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Date: $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

            Spacer(modifier = Modifier.width(8.dp))

            // Status and remaining balance
            Column(horizontalAlignment = Alignment.End) {
                // Status Badge
                Badge(
                    containerColor = when (orderWithCustomer.order.orderStatus) {
                        "Completed" -> Color(0xFFE8F5E9)
                        "Cancelled" -> Color(0xFFFFEBEE)
                        "Postponed" -> Color(0xFFFFF8E1)
                        "In Progress" -> Color(0xFFE3F2FD)
                        else -> Color(0xFFF3E5F5)
                    }
                ) {
                    Text(
                        text = orderWithCustomer.order.orderStatus.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = when (orderWithCustomer.order.orderStatus) {
                            "Completed" -> Color(0xFF2E7D32)
                            "Cancelled" -> Color(0xFFC62828)
                            "Postponed" -> Color(0xFFF57F17)
                            "In Progress" -> Color(0xFF1565C0)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Remaining balance indicator
                Text(
                    text = "Bal: ${orderWithCustomer.order.remainingBalance} $currency",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (orderWithCustomer.order.remainingBalance > 0) Color(0xFFE53935) else Color(0xFF4CAF50)
                )
            }
        }
    }
}
