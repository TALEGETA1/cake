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
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.CakeViewModel
import com.example.utils.EthiopianCalendarHelper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: CakeViewModel
) {
    val savedReports by viewModel.savedReports.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val useEthiopian by viewModel.useEthiopianCalendar.collectAsStateWithLifecycle()

    var activeReportType by remember { mutableStateOf("Monthly") } // "Daily", "Weekly", "Monthly"
    var reportToDelete by remember { mutableStateOf<com.example.database.SavedReportEntity?>(null) }

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
                        Text("Business Reports", fontWeight = FontWeight.Bold)
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
            // Generate New Report Trigger
            item {
                Text("Generate Reports", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select Report Scope:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Daily", "Weekly", "Monthly").forEach { type ->
                                FilterChip(
                                    selected = activeReportType == type,
                                    onClick = { activeReportType = type },
                                    label = { Text(type, modifier = Modifier.padding(horizontal = 4.dp)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.generateReportAndSave(activeReportType) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("generate_report_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate & Export $activeReportType Report", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Saved / Historic Reports
            item {
                Text("Saved History Reports (${savedReports.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            if (savedReports.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No reports generated yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(savedReports) { report ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = report.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                IconButton(onClick = { reportToDelete = report }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                                }
                            }

                            Text(
                                "Generated: " + EthiopianCalendarHelper.formatDate(
                                    Instant.ofEpochMilli(report.generatedDateMillis).atZone(ZoneId.systemDefault()).toLocalDate(),
                                    useEthiopian
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Revenue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${report.totalRevenue} $currency", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Outstanding Bal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${report.outstandingBalances} $currency", fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Orders (C / X)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${report.completedCount} Completed / ${report.cancelledCount} Cancelled", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            val isNewFormat = report.cashTotal == -1.0
                            val ordersReceived = if (isNewFormat) report.telebirrTotal.toInt() else (report.completedCount + report.cancelledCount)
                            val ordersDelivered = report.completedCount
                            val totalAmountReceived = report.totalRevenue
                            val activeOrdersCount = if (isNewFormat) report.ebirrTotal.toInt() else 0

                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Report Summary Details:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ReportMetricCard(
                                        label = "Orders Received",
                                        value = "$ordersReceived",
                                        icon = Icons.Default.ShoppingCart,
                                        iconColor = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ReportMetricCard(
                                        label = "Orders Delivered",
                                        value = "$ordersDelivered",
                                        icon = Icons.Default.Check,
                                        iconColor = Color(0xFF4CAF50),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ReportMetricCard(
                                        label = "Total Received",
                                        value = "${totalAmountReceived.toInt()} $currency",
                                        icon = Icons.Default.Assessment,
                                        iconColor = Color(0xFFE91E63),
                                        modifier = Modifier.weight(1f)
                                    )
                                    ReportMetricCard(
                                        label = "Active Orders",
                                        value = "$activeOrdersCount",
                                        icon = Icons.Default.Schedule,
                                        iconColor = Color(0xFFFF9800),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (reportToDelete != null) {
            AlertDialog(
                onDismissRequest = { reportToDelete = null },
                title = { Text("Delete Report", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete the report \"${reportToDelete?.title}\"? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            reportToDelete?.let { viewModel.deleteSavedReport(it) }
                            reportToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Report")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { reportToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ReportMetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
