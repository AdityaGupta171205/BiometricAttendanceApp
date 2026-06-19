package com.example.biometricattendanceapp.feature_attendance.domain.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    data class LocationCheckResult(
        val isSuccessful: Boolean,
        val isSpoofed: Boolean,
        val distanceMeters: Float = -1f,
        val userLocation: Location? = null,
        val errorMessage: String? = null
    )

    @SuppressLint("MissingPermission")
    suspend fun verifyLocation(
        officeLat: Double,
        officeLng: Double,
        allowedRadiusMeters: Float
    ): LocationCheckResult {
        return try {
            val cancellationTokenSource = CancellationTokenSource()
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()

            if (location == null) {
                return LocationCheckResult(
                    isSuccessful = false,
                    isSpoofed = false,
                    errorMessage = "Could not fetch location. Ensure GPS is turned on."
                )
            }

            val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                location.isMock
            } else {
                @Suppress("DEPRECATION")
                location.isFromMockProvider
            }

            if (isMock) {
                return LocationCheckResult(
                    isSuccessful = false,
                    isSpoofed = true,
                    userLocation = location,
                    errorMessage = "Fake GPS detected. Please disable location spoofing."
                )
            }

            val officeLocation = Location("Office").apply {
                latitude = officeLat
                longitude = officeLng
            }

            val distanceToOffice = location.distanceTo(officeLocation)

            if (distanceToOffice <= allowedRadiusMeters) {
                LocationCheckResult(
                    isSuccessful = true,
                    isSpoofed = false,
                    distanceMeters = distanceToOffice,
                    userLocation = location
                )
            } else {
                LocationCheckResult(
                    isSuccessful = false,
                    isSpoofed = false,
                    distanceMeters = distanceToOffice,
                    userLocation = location,
                    errorMessage = "You are ${distanceToOffice.toInt()}m away. Must be within ${allowedRadiusMeters.toInt()}m."
                )
            }
        } catch (e: Exception) {
            LocationCheckResult(
                isSuccessful = false,
                isSpoofed = false,
                errorMessage = e.localizedMessage ?: "Unknown GPS Error occurred."
            )
        }
    }
}