package com.example.biometricattendanceapp.feature_attendance.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.biometricattendanceapp.feature_attendance.data.AttendanceRepository
import com.example.biometricattendanceapp.feature_attendance.data.remote.worker.PunchSyncWorker
import com.example.biometricattendanceapp.feature_attendance.domain.location.LocationEngine
import com.example.biometricattendanceapp.feature_attendance.presentation.notifications.NotificationScheduler
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.example.biometricattendanceapp.BuildConfig

data class OfficeLocation(
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Float = 100f
)

sealed class DashboardState {
    object Idle : DashboardState()
    object Loading : DashboardState()
    data class LocationVerified(val officeName: String) : DashboardState()
    data class Success(val message: String) : DashboardState()
    data class Error(val message: String) : DashboardState()

    data class OutOfArea(
        val nearestOfficeName: String,
        val distance: Float,
        val officeLat: Double,
        val officeLng: Double
    ) : DashboardState()
}

enum class PunchAction { IN, OUT }

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AttendanceRepository,
    private val locationEngine: LocationEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<DashboardState>(DashboardState.Idle)
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    private val _isPunchedIn = MutableStateFlow(false)
    val isPunchedIn: StateFlow<Boolean> = _isPunchedIn.asStateFlow()

    private var pendingAction: PunchAction? = null
    private var lastPunchTimestamp: Long = 0L
    private var currentOfficeName: String = "Unknown"

    private val authorizedOffices = listOf(
        OfficeLocation(
            name = "Mecon",
            lat = BuildConfig.OFFICE1_LATITUDE.toDouble(),
            lng = BuildConfig.OFFICE1_LONGITUDE.toDouble(),
            radiusMeters = 50f
        ),
        OfficeLocation(
            name = "Home",
            lat = BuildConfig.OFFICE2_LATITUDE.toDouble(),
            lng = BuildConfig.OFFICE2_LONGITUDE.toDouble(),
            radiusMeters = 10f
        )
    )

    init {
        checkCurrentStatus()
    }

    private fun checkCurrentStatus() {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            var lastRecord = repository.getLastRecordForToday(today)
            if (lastRecord == null) {
                lastRecord = repository.fetchTodayLogFromFirebase(today)
            }
            if (lastRecord != null) {
                _isPunchedIn.value = lastRecord.punchOutTime == null
                lastPunchTimestamp = lastRecord.punchOutTime ?: lastRecord.punchInTime
            } else {
                _isPunchedIn.value = false
                lastPunchTimestamp = 0L
            }
        }
    }

    private fun isCooldownActive(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - lastPunchTimestamp
        val cooldownMillis = 120_000

        if (timeDifference < cooldownMillis) {
            val remainingSeconds = (cooldownMillis - timeDifference) / 1000
            _uiState.value = DashboardState.Error("Please wait $remainingSeconds seconds before punching again.")
            return true
        }
        return false
    }

    fun onPunchInClicked() {
        if (isCooldownActive()) return

        pendingAction = PunchAction.IN
        verifyLocationAgainstOffices()
    }

    fun onPunchOutClicked() {
        if (isCooldownActive()) return

        pendingAction = PunchAction.OUT
        verifyLocationAgainstOffices()
    }

    private fun verifyLocationAgainstOffices() {
        _uiState.value = DashboardState.Loading

        viewModelScope.launch {
            var closestOffice: OfficeLocation? = null
            var minDistance = Float.MAX_VALUE
            var verifiedOffice: OfficeLocation? = null
            var hasSpoofError = false
            var spoofErrorMessage = ""

            for (office in authorizedOffices) {
                val result = locationEngine.verifyLocation(office.lat, office.lng, office.radiusMeters)

                if (result.isSpoofed) {
                    hasSpoofError = true
                    spoofErrorMessage = result.errorMessage ?: "Security Alert: Fake GPS detected."
                    break
                }

                if (result.isSuccessful) {
                    verifiedOffice = office
                    break
                }

                if (result.distanceMeters < minDistance) {
                    minDistance = result.distanceMeters
                    closestOffice = office
                }
            }

            when {
                hasSpoofError -> {
                    _uiState.value = DashboardState.Error(spoofErrorMessage)
                    pendingAction = null
                }
                verifiedOffice != null -> {
                    currentOfficeName = verifiedOffice.name
                    _uiState.value = DashboardState.LocationVerified(verifiedOffice.name)
                }
                closestOffice != null -> {
                    _uiState.value = DashboardState.OutOfArea(
                        nearestOfficeName = closestOffice.name,
                        distance = minDistance,
                        officeLat = closestOffice.lat,
                        officeLng = closestOffice.lng
                    )
                    pendingAction = null
                }
                else -> {
                    _uiState.value = DashboardState.Error("Location verification completely failed.")
                    pendingAction = null
                }
            }
        }
    }

    fun onBiometricSuccess() {
        _uiState.value = DashboardState.Loading

        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val timestamp = System.currentTimeMillis()

            when (pendingAction) {
                PunchAction.IN -> {
                    repository.punchIn(today, timestamp, isMock = false, officeName = currentOfficeName)
                    _uiState.value = DashboardState.Success("Punched In Successfully!")
                    triggerCloudSync()
                    scheduleDynamicPunchOutReminder()
                }
                PunchAction.OUT -> {
                    val lastRecord = repository.getLastRecordForToday(today)
                    if (lastRecord != null && lastRecord.punchOutTime == null) {
                        repository.punchOut(today, timestamp)

                        _uiState.value = DashboardState.Success("Punched Out Successfully!")
                        triggerCloudSync()
                        NotificationScheduler(context).cancelReminder()
                    } else {
                        _uiState.value = DashboardState.Error("Could not find an active shift to punch out of.")
                    }
                }
                null -> {
                    _uiState.value = DashboardState.Error("Unknown action.")
                }
            }

            checkCurrentStatus()
            pendingAction = null
        }
    }

    fun resetState() {
        _uiState.value = DashboardState.Idle
    }

    private fun triggerCloudSync() {
        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<PunchSyncWorker>()
            .setConstraints(syncConstraints)
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
    }

    /**
     * Fetches the dynamic closing time from Firebase and schedules the 10-minute warning.
     */
    private fun scheduleDynamicPunchOutReminder() {
        viewModelScope.launch {
            try {
                val settingsDoc = db.collection("settings").document("office").get().await()
                val closingTime = settingsDoc.getString("closingTime") ?: "18:00"

                val scheduler = NotificationScheduler(context)
                scheduler.schedulePunchOutReminder(closingTime)

            } catch (e: Exception) {
                val scheduler = NotificationScheduler(context)
                scheduler.schedulePunchOutReminder("18:00")
            }
        }
    }
}