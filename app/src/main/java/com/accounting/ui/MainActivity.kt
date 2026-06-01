package com.accounting.ui

import android.os.Bundle
import android.content.ComponentName
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.accounting.ui.navigation.Screen
import com.accounting.ui.screens.MainScreen
import com.accounting.ui.screens.SettingsScreen
import com.accounting.ui.screens.StatisticsScreen
import com.accounting.ui.theme.AutoAccountingTheme
import com.accounting.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AutoAccountingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: MainViewModel = viewModel()

                    val uiState by viewModel.uiState.collectAsState()

                    var notificationEnabled by remember { mutableStateOf(false) }
                    var accessibilityEnabled by remember { mutableStateOf(false) }
                    val lifecycleOwner = LocalLifecycleOwner.current

                    fun refreshPermissionState() {
                        notificationEnabled = isNotificationListenerEnabled()
                        accessibilityEnabled = isAccessibilityServiceEnabled()
                    }

                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                refreshPermissionState()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        refreshPermissionState()
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Main.route
                    ) {
                        composable(Screen.Main.route) {
                            MainScreen(
                                transactions = uiState.filteredTransactions,
                                timeFilter = uiState.timeFilter,
                                onTimeFilterChange = { viewModel.setTimeFilter(it) },
                                onNavigateToStatistics = {
                                    navController.navigate(Screen.Statistics.route)
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route)
                                }
                            )
                        }
                        composable(Screen.Statistics.route) {
                            StatisticsScreen(
                                monthlyExpense = uiState.monthlyExpense,
                                monthlyIncome = uiState.monthlyIncome,
                                categoryTotals = uiState.categoryTotals,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                notificationEnabled = notificationEnabled,
                                accessibilityEnabled = accessibilityEnabled,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val service = ComponentName(
            this,
            com.accounting.service.TransactionNotificationListener::class.java
        ).flattenToString()
        return enabledListeners.split(":").any { it.equals(service, ignoreCase = true) }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val service = ComponentName(
            this,
            com.accounting.service.TransactionAccessibilityService::class.java
        ).flattenToString()
        return enabledServices.split(":").any { TextUtils.equals(it, service) }
    }
}