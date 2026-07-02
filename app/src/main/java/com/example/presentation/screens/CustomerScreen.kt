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
import com.example.database.CustomerEntity
import com.example.database.OrderWithCustomer
import com.example.presentation.CakeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: CakeViewModel,
    onNavigateToOrderDetails: (Int) -> Unit
) {
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val orders by viewModel.ordersWithCustomer.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var isAddingCustomer by remember { mutableStateOf(false) }
    var selectedCustomerForHistory by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }

    // Form states
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    val filteredCustomers = customers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.phone.contains(searchQuery, ignoreCase = true)
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
                        Text("Customer Directory", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { isAddingCustomer = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer", tint = MaterialTheme.colorScheme.primary)
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
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Name or Phone") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PeopleOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No customers found", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { isAddingCustomer = true }) {
                            Text("Register first customer")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCustomers) { customer ->
                        // Calculate Customer History statistics
                        val customerOrders = orders.filter { it.order.customerId == customer.id }
                        val totalSpent = customerOrders.filter { it.order.orderStatus != "Cancelled" }.sumOf { it.order.totalPrice }
                        val pendingBalance = customerOrders.filter { it.order.orderStatus != "Cancelled" }.sumOf { it.order.remainingBalance }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCustomerForHistory = customer }
                                .testTag("customer_card_${customer.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { customerToDelete = customer }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Customer", tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                                    }
                                }
                                Text("📞 ${customer.phone}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!customer.address.isNullOrBlank()) {
                                    Text("📍 ${customer.address}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Orders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${customerOrders.size}", fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Total Spent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${String.format("%.2f", totalSpent)} $currency", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Column {
                                        Text("Pending Bal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${String.format("%.2f", pendingBalance)} $currency", fontWeight = FontWeight.Bold, color = if (pendingBalance > 0) Color(0xFFE53935) else Color(0xFF4CAF50))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Customer Dialog
        if (isAddingCustomer) {
            AlertDialog(
                onDismissRequest = { isAddingCustomer = false },
                title = { Text("Register Customer", fontWeight = FontWeight.Bold) },
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
                                name = ""
                                phone = ""
                                address = ""
                                isAddingCustomer = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Register")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isAddingCustomer = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // View Customer History Dialog
        if (selectedCustomerForHistory != null) {
            val customer = selectedCustomerForHistory!!
            val customerOrders = orders.filter { it.order.customerId == customer.id }
            
            AlertDialog(
                onDismissRequest = { selectedCustomerForHistory = null },
                title = {
                    Column {
                        Text("${customer.name}'s History", fontWeight = FontWeight.Bold)
                        Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Previous Orders (${customerOrders.size}):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        
                        if (customerOrders.isEmpty()) {
                            Text("No orders recorded yet for this customer.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(customerOrders) { orderWithCustomer ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCustomerForHistory = null
                                                onNavigateToOrderDetails(orderWithCustomer.order.id)
                                            },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(orderWithCustomer.order.cakeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("Status: ${orderWithCustomer.order.orderStatus}", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Text("${orderWithCustomer.order.totalPrice} $currency", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { selectedCustomerForHistory = null }) {
                        Text("Close")
                    }
                }
            )
        }

        if (customerToDelete != null) {
            AlertDialog(
                onDismissRequest = { customerToDelete = null },
                title = { Text("Delete Customer", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete ${customerToDelete?.name}? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            customerToDelete?.let { viewModel.deleteCustomer(it) }
                            customerToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { customerToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
