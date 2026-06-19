package com.example.biometricattendanceapp.feature_attendance.presentation.attendance

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel = viewModel(),
    onBiometricPrompt: () -> Unit
) {
    val canPunchIn by viewModel.canPunchIn.collectAsState()
    val currentOffice by viewModel.currentOffice.collectAsState()
    val nearestOffice by viewModel.nearestOffice.collectAsState()
    val distanceToNearest by viewModel.distanceToNearest.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (canPunchIn && currentOffice != null) {
            Text(
                text = "Welcome to ${currentOffice!!.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBiometricPrompt,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Authenticate & Punch In")
            }
        } else if (nearestOffice != null) {
            Text(
                text = "You are not at a recognized office.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nearest Office: ${nearestOffice!!.name}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = String.format("Distance: %.2f km", distanceToNearest / 1000),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val uri = Uri.parse("google.navigation:q=${nearestOffice!!.lat},${nearestOffice!!.lng}")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.setPackage("com.google.android.apps.maps")
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Route to Nearest Office")
            }
        } else {
            CircularProgressIndicator()
        }
    }
}