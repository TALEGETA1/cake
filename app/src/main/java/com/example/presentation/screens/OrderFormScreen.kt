package com.example.presentation.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.CakeViewModel
import com.example.utils.EthiopianCalendarHelper
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import androidx.compose.ui.unit.sp

val cakeNameSuggestions = listOf(
    "Custom Birthday Cake",
    "Classic Vanilla Celebration",
    "Chocolate Ganache Dream",
    "Elegant Rose Gold Wedding",
    "Red Velvet Luxury",
    "Strawberry Cream Delight",
    "Cute Baby Shower Theme",
    "Graduation Cap Special",
    "Unicorn Magic",
    "Fresh Fruit Gateau"
)

val cakeTypeSuggestions = listOf(
    "Birthday",
    "Wedding",
    "Anniversary",
    "Baby Shower",
    "Graduation",
    "Bridal Shower",
    "Gender Reveal",
    "Corporate Event",
    "Holiday Celebration",
    "Custom Celebration"
)

val colorThemeSuggestions = listOf(
    "Pink & White Pearl",
    "Chocolate Brown & Gold Accent",
    "Classic Cream & Off-White",
    "Royal Blue & Metallic Silver",
    "Emerald Green & Gold Foil",
    "Pastel Rainbow Multi-color",
    "Midnight Black & Gold Drip",
    "Lavender & Soft Lilac",
    "Mint Green & Peach Blush",
    "Red & Warm Gold Romance"
)

val flavorSuggestions = listOf(
    "Classic Chocolate Fudge",
    "Red Velvet Premium",
    "Classic Vanilla Bean",
    "Strawberry Whipped Cream",
    "Mocha Dark Chocolate Espresso",
    "Rich Caramel Butterscotch",
    "Lemon Zest & Blueberry",
    "Traditional Black Forest",
    "Coconut Cream Mango Twist"
)

val weightSuggestions = listOf(
    "1.0",
    "1.5",
    "2.0",
    "2.5",
    "3.0",
    "4.0",
    "5.0",
    "6.0",
    "8.0",
    "10.0"
)

val designSuggestions = listOf(
    "Smooth Buttercream Frosting",
    "Elegant Fondant Sculpting",
    "Rustic Semi-Naked Frosting",
    "Beautiful Drip Cake Finish",
    "Glistening Geode Sugar Art",
    "Fresh Flower Arrangements",
    "Hand-sculpted Sugar Flowers",
    "High-definition Photo Edible Print",
    "Minimalist Palette Knife Palette Art"
)

@Composable
fun EditableDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { textFieldWidth = it.width }
                .testTag(testTag),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown for $label"
                    )
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(density) { textFieldWidth.toDp() })
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderFormScreen(
    viewModel: CakeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCustomers: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val useEthiopian by viewModel.useEthiopianCalendar.collectAsStateWithLifecycle()
    val allOrders by viewModel.ordersWithCustomer.collectAsStateWithLifecycle()
    var showMaxOrdersDialog by remember { mutableStateOf(false) }
    var maxOrdersDateString by remember { mutableStateOf("") }

    // Retrieve the current draft from ViewModel
    val draft = viewModel.orderDraft.value

    // Form states initialized from draft
    var currentStep by remember { mutableStateOf(draft.currentStep) }
    var selectedCustomerId by remember { mutableStateOf<Int?>(draft.selectedCustomerId) }
    var cakeName by remember { mutableStateOf(draft.cakeName) }
    var cakeType by remember { mutableStateOf(draft.cakeType) }
    var cakeSize by remember { mutableStateOf(draft.cakeSize) }
    var flavor by remember { mutableStateOf(draft.flavor) }
    var weightString by remember { mutableStateOf(draft.weightString) }
    var colorTheme by remember { mutableStateOf(draft.colorTheme) }
    var customDesignNotes by remember { mutableStateOf(draft.customDesignNotes) }
    var specialInstructions by remember { mutableStateOf(draft.specialInstructions) }
    var deliveryAddress by remember { mutableStateOf(draft.deliveryAddress) }
    var quantityString by remember { mutableStateOf(draft.quantityString) }
    var totalPriceString by remember { mutableStateOf(draft.totalPriceString) }

    // Date & Time Picker states initialized from draft
    var deliveryDate by remember { mutableStateOf(draft.deliveryDate) }
    var deliveryTime by remember { mutableStateOf(draft.deliveryTime) }

    // Automatically synchronize state to ViewModel draft on any change
    LaunchedEffect(
        currentStep, selectedCustomerId, cakeName, cakeType, cakeSize, flavor,
        weightString, colorTheme, customDesignNotes, specialInstructions,
        deliveryAddress, quantityString, totalPriceString, deliveryDate, deliveryTime
    ) {
        viewModel.updateOrderDraft(
            com.example.presentation.OrderDraft(
                selectedCustomerId = selectedCustomerId,
                cakeName = cakeName,
                cakeType = cakeType,
                cakeSize = cakeSize,
                flavor = flavor,
                weightString = weightString,
                colorTheme = colorTheme,
                customDesignNotes = customDesignNotes,
                specialInstructions = specialInstructions,
                deliveryAddress = deliveryAddress,
                quantityString = quantityString,
                totalPriceString = totalPriceString,
                deliveryDate = deliveryDate,
                deliveryTime = deliveryTime,
                currentStep = currentStep
            )
        )
    }

    // Customer Dropdown state
    var isCustomerDropdownExpanded by remember { mutableStateOf(false) }

    val selectedCustomerName = customers.find { it.id == selectedCustomerId }?.name ?: "Select Customer *"

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
                        Text("Create Cake Order", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentStep == 2) {
                                currentStep = 1
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Step Progress Indicator Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Step 1 Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { if (currentStep == 2) currentStep = 1 },
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentStep == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = if (currentStep == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "1",
                                    color = if (currentStep == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Step 1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Cake Info", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    // Step 2 Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (currentStep == 1) {
                                    if (selectedCustomerId == null) {
                                        Toast.makeText(context, "Please select or register a customer first!", Toast.LENGTH_SHORT).show()
                                    } else if (cakeName.isBlank()) {
                                        Toast.makeText(context, "Cake name cannot be empty!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        currentStep = 2
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentStep == 2) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = if (currentStep == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "2",
                                    color = if (currentStep == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Step 2", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Delivery", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (currentStep == 1) {
                // Section: Customer Selection
                item {
                    Text("Customer", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCustomerName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Customer *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCustomerDropdownExpanded = true }
                                .testTag("customer_selector"),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                        )

                        DropdownMenu(
                            expanded = isCustomerDropdownExpanded,
                            onDismissRequest = { isCustomerDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            if (customers.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("+ Register New Customer First") },
                                    onClick = {
                                        isCustomerDropdownExpanded = false
                                        onNavigateToCustomers()
                                    }
                                )
                            } else {
                                customers.forEach { customer ->
                                    DropdownMenuItem(
                                        text = { Text("${customer.name} (${customer.phone})") },
                                        onClick = {
                                            selectedCustomerId = customer.id
                                            isCustomerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section: Cake Configurations
                item {
                    Text("Cake Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                item {
                    EditableDropdownField(
                        value = cakeName,
                        onValueChange = { cakeName = it },
                        label = "Cake Name / Name of Design *",
                        options = cakeNameSuggestions,
                        testTag = "cake_name_input"
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Flavor (Dropdown)
                        EditableDropdownField(
                            value = flavor,
                            onValueChange = { flavor = it },
                            label = "Flavor *",
                            options = flavorSuggestions,
                            modifier = Modifier.weight(1.2f),
                            testTag = "flavor_dropdown"
                        )
                        // Weight (Dropdown)
                        EditableDropdownField(
                            value = weightString,
                            onValueChange = { weightString = it },
                            label = "Weight (kg) *",
                            options = weightSuggestions,
                            modifier = Modifier.weight(0.8f),
                            testTag = "weight_dropdown"
                        )
                    }
                }

                item {
                    EditableDropdownField(
                        value = colorTheme,
                        onValueChange = { colorTheme = it },
                        label = "Color Theme / Frosting Colors",
                        options = colorThemeSuggestions,
                        testTag = "color_theme_dropdown"
                    )
                }

                // Design Type dropdown removed per request

                item {
                    OutlinedTextField(
                        value = specialInstructions,
                        onValueChange = { specialInstructions = it },
                        label = { Text("Special Instructions (Delivery details, writing)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Save and Cancel Buttons
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateBack() },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("cancel_step1_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (selectedCustomerId == null) {
                                    Toast.makeText(context, "Please select or register a customer first!", Toast.LENGTH_SHORT).show()
                                } else if (cakeName.isBlank()) {
                                    Toast.makeText(context, "Cake name cannot be empty!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "First page saved. Going to next page...", Toast.LENGTH_SHORT).show()
                                    currentStep = 2
                                }
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(54.dp)
                                .testTag("save_step1_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Section: Scheduling
                item {
                    Text("3. Delivery Schedule", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                item {
                    // Delivery Date Picker Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        deliveryDate = LocalDate.of(y, m + 1, d)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Delivery Date *", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    EthiopianCalendarHelper.formatDate(deliveryDate, useEthiopian),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                item {
                    // Delivery Time Picker Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val calendar = Calendar.getInstance()
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        val ampm = if (hour >= 12) "PM" else "AM"
                                        val h = if (hour % 12 == 0) 12 else hour % 12
                                        deliveryTime = String.format("%02d:%02d %s", h, minute, ampm)
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Delivery Time *", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    deliveryTime,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Icon(Icons.Default.AccessTime, contentDescription = "Select Time", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = deliveryAddress,
                        onValueChange = { deliveryAddress = it },
                        label = { Text("Delivery Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Section: Pricing
                item {
                    Text("4. Order Pricing & Count", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Quantity
                        OutlinedTextField(
                            value = quantityString,
                            onValueChange = { quantityString = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        // Total Price
                        OutlinedTextField(
                            value = totalPriceString,
                            onValueChange = { totalPriceString = it },
                            label = { Text("Total Price ($currency) *") },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Save & Back Buttons
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { currentStep = 1 },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("back_to_step1_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null)
                                Text("Back", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                val custId = selectedCustomerId
                                val weight = weightString.toDoubleOrNull() ?: 1.0
                                val qty = quantityString.toIntOrNull() ?: 1
                                val price = totalPriceString.toDoubleOrNull() ?: 0.0

                                if (custId == null) {
                                    Toast.makeText(context, "Please select or register a customer first!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (cakeName.isBlank()) {
                                    Toast.makeText(context, "Cake name cannot be empty!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (deliveryTime.isBlank()) {
                                    Toast.makeText(context, "Delivery time is required!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (price <= 0) {
                                    Toast.makeText(context, "Please enter a valid total price!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                // Save order in suspend coroutine block
                                coroutineScope.launch {
                                    val deliveryEpoch = deliveryDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    
                                    // Check if limit of 30 is reached
                                    val countForDate = allOrders.count {
                                        it.order.deliveryDateMillis == deliveryEpoch &&
                                        it.order.orderStatus != "Cancelled"
                                    }
                                    if (countForDate >= 30) {
                                        maxOrdersDateString = EthiopianCalendarHelper.formatDate(deliveryDate, useEthiopian)
                                        showMaxOrdersDialog = true
                                    } else {
                                        val orderResult = viewModel.createOrderSuspend(
                                            customerId = custId,
                                            cakeName = cakeName,
                                            cakeType = cakeType,
                                            cakeSize = cakeSize,
                                            flavor = flavor,
                                            weight = weight,
                                            colorTheme = colorTheme,
                                            customDesignNotes = customDesignNotes.ifBlank { null },
                                            specialInstructions = specialInstructions.ifBlank { null },
                                            quantity = qty,
                                            orderDateMillis = System.currentTimeMillis(),
                                            deliveryDateMillis = deliveryEpoch,
                                            deliveryTime = deliveryTime,
                                            deliveryAddress = deliveryAddress.ifBlank { null },
                                            totalPrice = price
                                        )

                                        if (orderResult.isSuccess) {
                                            Toast.makeText(context, "Order created successfully!", Toast.LENGTH_SHORT).show()
                                            viewModel.clearOrderDraft()
                                            onNavigateBack()
                                        } else {
                                            Toast.makeText(context, "Failed to create order: ${orderResult.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(54.dp)
                                .testTag("save_order_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save Order", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showMaxOrdersDialog) {
            AlertDialog(
                onDismissRequest = { showMaxOrdersDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Limit Reached",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Daily Limit Reached", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        text = "Delivery Date '$maxOrdersDateString' has reached the maximum capacity of 30 orders. Please choose another date and time.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showMaxOrdersDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Choose Another Date")
                    }
                }
            )
        }
    }
}
