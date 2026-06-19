package com.example.biometricattendanceapp.feature_attendance.presentation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.biometricattendanceapp.feature_attendance.domain.biometric.BiometricHelper
import com.example.biometricattendanceapp.feature_attendance.presentation.home.DashboardViewModel
import com.example.biometricattendanceapp.feature_attendance.presentation.home.DashboardState
import com.example.biometricattendanceapp.feature_attendance.presentation.home.PunchAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPunchedIn by viewModel.isPunchedIn.collectAsState()

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricHelper = remember { BiometricHelper() }

    var permissionAction by remember { mutableStateOf(PunchAction.IN) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            if (permissionAction == PunchAction.IN) {
                viewModel.onPunchInClicked()
            } else {
                viewModel.onPunchOutClicked()
            }
        } else {
            Toast.makeText(context, "Location permission is required to punch.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is DashboardState.LocationVerified -> {
                if (activity != null) {
                    biometricHelper.showBiometricPrompt(
                        activity = activity,
                        onSuccess = { viewModel.onBiometricSuccess() },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                    )
                }
            }
            is DashboardState.Success -> {
                val message = (uiState as DashboardState.Success).message
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            is DashboardState.Error -> {
                val message = (uiState as DashboardState.Error).message
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> { }
        }
    }

    if (uiState is DashboardState.OutOfArea) {
        val state = uiState as DashboardState.OutOfArea
        OutOfAreaDialog(
            nearestOfficeName = state.nearestOfficeName,
            distance = state.distance,
            officeLat = state.officeLat,
            officeLng = state.officeLng,
            onDismiss = { viewModel.resetState() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mark Attendance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Biometric Icon",
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    permissionAction = PunchAction.IN
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isPunchedIn && uiState !is DashboardState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("PUNCH IN", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    permissionAction = PunchAction.OUT
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isPunchedIn && uiState !is DashboardState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("PUNCH OUT", style = MaterialTheme.typography.titleMedium)
            }

            if (uiState is DashboardState.Loading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Requires GPS and Biometric Verification",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OutOfAreaDialog(
    nearestOfficeName: String,
    distance: Float,
    officeLat: Double,
    officeLng: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Map, contentDescription = "Map Icon", tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Text("Out of Geofence")
        },
        text = {
            Text("You are currently ${distance.toInt()} meters away from $nearestOfficeName. You must be within the office radius to punch in. Would you like directions to the office?")
        },
        confirmButton = {
            Button(
                onClick = {
                    val gmmIntentUri = Uri.parse("google.navigation:q=$officeLat,$officeLng")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")

                    try {
                        context.startActivity(mapIntent)
                    } catch (e: Exception) {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$officeLat,$officeLng")
                        )
                        context.startActivity(browserIntent)
                    }
                    onDismiss()
                }
            ) {
                Text("Get Directions")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}