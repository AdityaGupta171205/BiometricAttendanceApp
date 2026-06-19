package com.example.biometricattendanceapp.feature_attendance.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToAttendance: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToLedger: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onSignOut: () -> Unit
) {
    val userName by viewModel.userName.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(userName, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeActionCard(
                title = "Mark Attendance",
                subtitle = "Punch In / Punch Out via GPS",
                icon = Icons.Default.Fingerprint,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = onNavigateToAttendance
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeActionCard(
                title = "My Calendar",
                subtitle = "View attendance history & logs",
                icon = Icons.Default.CalendarMonth,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                onClick = onNavigateToCalendar
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeActionCard(
                title = "My Ledgers",
                subtitle = "View your personal punch times",
                icon = Icons.Default.FormatListBulleted,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                onClick = onNavigateToLedger
            )

            if (isAdmin) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                HomeActionCard(
                    title = "HR Admin Panel",
                    subtitle = "Manage approvals & view ledger",
                    icon = Icons.Default.AdminPanelSettings,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    onClick = onNavigateToAdmin
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}