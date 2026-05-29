package com.accounting.ui.navigation

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Statistics : Screen("statistics")
    object Settings : Screen("settings")
}