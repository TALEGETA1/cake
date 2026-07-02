package com.example.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.CakeViewModel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: CakeViewModel
) {
    val context = LocalContext.current
    val bName by viewModel.businessName.collectAsStateWithLifecycle()
    val bPhone by viewModel.businessPhone.collectAsStateWithLifecycle()
    val currentCurrency by viewModel.currency.collectAsStateWithLifecycle()
    val requireZeroBalance by viewModel.requireZeroBalanceToComplete.collectAsStateWithLifecycle()

    var editableName by remember { mutableStateOf(bName) }
    var editablePhone by remember { mutableStateOf(bPhone) }
    var selectedCurrency by remember { mutableStateOf(currentCurrency) }

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
                        Text("App Settings", fontWeight = FontWeight.Bold)
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
            // Profile Setup
            item {
                Text("Business Profile Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = editableName,
                            onValueChange = {
                                editableName = it
                                viewModel.businessName.value = it
                            },
                            label = { Text("Business Name") },
                            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = editablePhone,
                            onValueChange = {
                                editablePhone = it
                                viewModel.businessPhone.value = it
                            },
                            label = { Text("Business Phone") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Currency & Validation Constraints
            item {
                Text("Application Rules", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Currency Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Business Currency", fontWeight = FontWeight.Bold)
                                Text("Display currency symbol throughout", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("ETB").forEach { cur ->
                                    FilterChip(
                                        selected = selectedCurrency == cur,
                                        onClick = {
                                            selectedCurrency = cur
                                            viewModel.currency.value = cur
                                        },
                                        label = { Text(cur, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        // Completion Rule Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Require Balance Payment", fontWeight = FontWeight.Bold)
                                Text("Cannot mark cake order Completed unless remaining balance is zero.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = requireZeroBalance,
                                onCheckedChange = { viewModel.requireZeroBalanceToComplete.value = it },
                                modifier = Modifier.testTag("require_zero_balance_switch")
                            )
                        }
                    }
                }
            }

            // Database Actions (Backup & Restore)
            item {
                Text("Database Backup & Restore", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Securely save and preserve your offline database locally to prevent loss.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Backup
                            Button(
                                onClick = {
                                    try {
                                        val dbFile = context.getDatabasePath("cake_scheduler_db")
                                        val backupFile = File(context.getExternalFilesDir(null), "cake_scheduler_db_backup")
                                        
                                        if (dbFile.exists()) {
                                            FileInputStream(dbFile).use { input ->
                                                FileOutputStream(backupFile).use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                            Toast.makeText(context, "Database backed up successfully to local storage!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "No database file exists to backup yet.", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup DB")
                            }

                            // Restore
                            Button(
                                onClick = {
                                    try {
                                        val dbFile = context.getDatabasePath("cake_scheduler_db")
                                        val backupFile = File(context.getExternalFilesDir(null), "cake_scheduler_db_backup")
                                        
                                        if (backupFile.exists()) {
                                            FileInputStream(backupFile).use { input ->
                                                FileOutputStream(dbFile).use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                            Toast.makeText(context, "Database restored successfully! Please restart the app.", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "No backup file found to restore.", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore DB")
                            }
                        }
                    }
                }
            }
        }
    }
}
