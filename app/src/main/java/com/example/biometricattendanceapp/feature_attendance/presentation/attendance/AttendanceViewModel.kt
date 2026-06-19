package com.example.biometricattendanceapp.feature_attendance.presentation.attendance

import android.location.Location
import androidx.lifecycle.ViewModel
import com.example.biometricattendanceapp.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Office(
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Float = 100f
)

class AttendanceViewModel : ViewModel() {
    private val offices = listOf(
        Office(
            name = "Mecon",
            lat = BuildConfig.OFFICE1_LATITUDE.toDouble(),
            lng = BuildConfig.OFFICE1_LONGITUDE.toDouble()
        ),
        Office(
            name = "Home",
            lat = BuildConfig.OFFICE2_LATITUDE.toDouble(),
            lng = BuildConfig.OFFICE2_LONGITUDE.toDouble()
        )
    )

    private val _canPunchIn = MutableStateFlow(false)
    val canPunchIn = _canPunchIn.asStateFlow()

    private val _currentOffice = MutableStateFlow<Office?>(null)
    val currentOffice = _currentOffice.asStateFlow()

    private val _nearestOffice = MutableStateFlow<Office?>(null)
    val nearestOffice = _nearestOffice.asStateFlow()

    private val _distanceToNearest = MutableStateFlow(0f)
    val distanceToNearest = _distanceToNearest.asStateFlow()

    fun updateLocation(userLat: Double, userLng: Double) {
        var closest: Office? = null
        var minDistance = Float.MAX_VALUE
        var validOffice: Office? = null

        for (office in offices) {
            val results = FloatArray(1)
            Location.distanceBetween(userLat, userLng, office.lat, office.lng, results)
            val distance = results[0]

            if (distance < office.radiusMeters) {
                validOffice = office
            }

            if (distance < minDistance) {
                minDistance = distance
                closest = office
            }
        }

        if (validOffice != null) {
            _canPunchIn.value = true
            _currentOffice.value = validOffice
            _nearestOffice.value = null
            _distanceToNearest.value = 0f
        } else {
            _canPunchIn.value = false
            _currentOffice.value = null
            _nearestOffice.value = closest
            _distanceToNearest.value = minDistance
        }
    }
}