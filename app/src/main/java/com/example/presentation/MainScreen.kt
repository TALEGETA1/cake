package com.example.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.presentation.screens.*

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Schedule : BottomNavItem("schedule", "Schedule", Icons.Default.CalendarMonth)
    object Customers : BottomNavItem("customers", "Customers", Icons.Default.People)
    object Reports : BottomNavItem("reports", "Reports", Icons.Default.BarChart)
    object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun MainScreen(viewModel: CakeViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Dashboard,
        BottomNavItem.Schedule,
        BottomNavItem.Reports,
        BottomNavItem.Settings
    )

    // Show BottomBar only on primary tab destinations
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet = maxWidth >= 600.dp

            // Subtle, elegant watermark of the business logo centered in the background
            Image(
                painter = painterResource(id = R.drawable.img_app_icon),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(0.85f)
                    .alpha(0.08f),
                contentScale = ContentScale.Fit
            )

            Row(modifier = Modifier.fillMaxSize()) {
                if (isTablet && showBottomBar) {
                    NavigationRail(
                        containerColor = Color.Transparent,
                        header = {
                            Card(
                                shape = CircleShape,
                                modifier = Modifier
                                    .padding(vertical = 16.dp)
                                    .size(48.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_app_icon),
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    ) {
                        bottomNavItems.forEach { item ->
                            val isSelected = currentRoute == item.route
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.title) },
                                label = { Text(item.title) },
                                alwaysShowLabel = true
                            )
                        }
                    }
                }

                Scaffold(
                    containerColor = Color.Transparent,
                    modifier = Modifier.weight(1f),
                    bottomBar = {
                        if (!isTablet && showBottomBar) {
                            NavigationBar(
                                containerColor = Color.Transparent,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                bottomNavItems.forEach { item ->
                                    val isSelected = currentRoute == item.route
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = { Icon(item.icon, contentDescription = item.title) },
                                        label = { Text(item.title) },
                                        alwaysShowLabel = true
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(max = 900.dp)
                                .fillMaxWidth()
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = BottomNavItem.Dashboard.route,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(BottomNavItem.Dashboard.route) {
                                    DashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToOrderDetails = { orderId ->
                                            navController.navigate("order_details/$orderId")
                                        },
                                        onNavigateToCreateOrder = {
                                            navController.navigate("order_form")
                                        },
                                        onNavigateToSchedule = {
                                            navController.navigate("schedule")
                                        }
                                    )
                                }

                                composable(BottomNavItem.Schedule.route) {
                                    ScheduleScreen(
                                        viewModel = viewModel,
                                        onNavigateToOrderDetails = { orderId ->
                                            navController.navigate("order_details/$orderId")
                                        }
                                    )
                                }

                                composable(BottomNavItem.Customers.route) {
                                    CustomerScreen(
                                        viewModel = viewModel,
                                        onNavigateToOrderDetails = { orderId ->
                                            navController.navigate("order_details/$orderId")
                                        }
                                    )
                                }

                                composable(BottomNavItem.Reports.route) {
                                    ReportScreen(viewModel = viewModel)
                                }

                                composable(BottomNavItem.Settings.route) {
                                    SettingsScreen(viewModel = viewModel)
                                }

                                composable("order_form") {
                                    OrderFormScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { navController.popBackStack() },
                                        onNavigateToCustomers = {
                                            navController.navigate(BottomNavItem.Customers.route) {
                                                popUpTo(BottomNavItem.Dashboard.route)
                                            }
                                        }
                                    )
                                }

                                composable(
                                    route = "order_details/{orderId}",
                                    arguments = listOf(navArgument("orderId") { type = NavType.IntType })
                                ) { backStackEntry ->
                                    val orderId = backStackEntry.arguments?.getInt("orderId") ?: 0
                                    OrderDetailScreen(
                                        orderId = orderId,
                                        viewModel = viewModel,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
