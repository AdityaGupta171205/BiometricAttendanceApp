package com.example.biometricattendanceapp.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.biometricattendanceapp.feature_attendance.presentation.DashboardScreen
import com.example.biometricattendanceapp.feature_attendance.presentation.home.HomeScreen
import com.example.biometricattendanceapp.feature_attendance.presentation.home.UserLedgerScreen
import com.example.biometricattendanceapp.feature_attendance.presentation.admin.AdminLoginScreen
import com.example.biometricattendanceapp.feature_attendance.presentation.admin.AdminScreen
import com.example.biometricattendanceapp.feature_attendance.presentation.auth.AuthState
import com.example.biometricattendanceapp.feature_attendance.presentation.auth.AuthViewModel
import com.example.biometricattendanceapp.feature_attendance.presentation.auth.LoginSignupScreen
import com.example.biometricattendanceapp.feature_attendance.presentation.auth.RejectedScreen
import com.example.biometricattendanceapp.feature_attendance.presentation.auth.WaitingForApprovalScreen
import com.example.biometricattendanceapp.feature_attendance.presentation.calendar.CalendarScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    val authState by authViewModel.authState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "gatekeeper"
    ) {
        composable(route = "gatekeeper") {
            when (val state = authState) {
                is AuthState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AuthState.Unauthenticated, is AuthState.Error -> {
                    LoginSignupScreen(
                        viewModel = authViewModel,
                        onStatusDetermined = { }
                    )
                }
                is AuthState.Authenticated -> {
                    when (state.status) {
                        "pending" -> {
                            WaitingForApprovalScreen(
                                onSignOut = {
                                    authViewModel.logout()
                                }
                            )
                        }
                        "rejected" -> {
                            RejectedScreen(
                                onSignOut = { authViewModel.logout() },
                                onReapply = { authViewModel.reapply() }
                            )
                        }
                        "approved" -> {
                            LaunchedEffect(Unit) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo("gatekeeper") { inclusive = true }
                                }
                            }
                        }
                        "device_pending" -> {
                            WaitingForApprovalScreen(
                                onSignOut = {
                                    authViewModel.logout()
                                }
                            )
                        }
                        "device_conflict" -> {
                            RejectedScreen(
                                onSignOut = { authViewModel.logout() },
                                onReapply = { }
                            )
                        }
                    }
                }
            }
        }

        // THE HOME HUB
        composable(route = Screen.Home.route) {

            LaunchedEffect(authState) {
                if (authState is AuthState.Unauthenticated) {
                    navController.navigate("gatekeeper") {
                        popUpTo(0)
                    }
                }
            }

            HomeScreen(
                onNavigateToAttendance = {
                    navController.navigate(Screen.EmployeeDashboard.route)
                },
                onNavigateToCalendar = {
                    navController.navigate(Screen.Calendar.route)
                },
                onNavigateToLedger = {
                    navController.navigate(Screen.UserLedger.route)
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.AdminLogin.route)
                },
                onSignOut = {
                    authViewModel.logout()
                }
            )
        }

        // THE USER LEDGER SCREEN ROUTE
        composable(route = Screen.UserLedger.route) {
            UserLedgerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // THE PUNCH IN/OUT SCREEN
        composable(route = Screen.EmployeeDashboard.route) {
            DashboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "calendar?userId={userId}",
            arguments = listOf(androidx.navigation.navArgument("userId") { defaultValue = "" })
        ) { backStackEntry ->
            val userIdArg = backStackEntry.arguments?.getString("userId")
            val finalUserId = if (userIdArg.isNullOrEmpty()) null else userIdArg

            CalendarScreen(
                targetUserId = finalUserId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.AdminLogin.route) {
            AdminLoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.AdminDashboard.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.AdminDashboard.route,
            deepLinks = listOf(navDeepLink { uriPattern = "biometricapp://admin" })
        ) {
            AdminScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onViewEmployeeCalendar = { userId ->
                    navController.navigate("calendar?userId=$userId")
                }
            )
        }
    }
}