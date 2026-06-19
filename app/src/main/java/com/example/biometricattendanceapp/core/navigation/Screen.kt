package com.example.biometricattendanceapp.core.navigation

sealed class Screen(val route: String) {
    object EmployeeDashboard : Screen("employee_dashboard")
    object AdminLogin : Screen("admin_login")
    object AdminDashboard : Screen("admin_dashboard")
    object Home : Screen("home")
    object Calendar : Screen("calendar")
    object UserLedger : Screen("user_ledger")
}