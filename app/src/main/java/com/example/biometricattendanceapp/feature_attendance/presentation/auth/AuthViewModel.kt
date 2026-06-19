package com.example.biometricattendanceapp.feature_attendance.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometricattendanceapp.feature_attendance.data.AttendanceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val status: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AttendanceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(
        if (auth.currentUser != null) AuthState.Loading else AuthState.Unauthenticated
    )
    val authState = _authState.asStateFlow()

    private var statusListener: ListenerRegistration? = null

    init {
        auth.currentUser?.let { user ->
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
            observeUserStatus(user.uid, deviceId)
        }
    }

    private suspend fun checkDeviceConflict(userId: String, deviceId: String): Boolean {
        val registryDoc = db.collection("device_registry").document(deviceId).get().await()
        if (registryDoc.exists()) {
            val registeredUserId = registryDoc.getString("userId")
            if (registeredUserId != null && registeredUserId != userId) {
                val oldUserDoc = db.collection("users").document(registeredUserId).get().await()
                val oldStatus = oldUserDoc.getString("status")

                if (oldUserDoc.exists() && oldStatus != "rejected") {
                    return true
                }
            }
        }
        return false
    }
    private fun observeUserStatus(userId: String, deviceId: String) {
        statusListener?.remove()
        _authState.value = AuthState.Loading

        statusListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _authState.value = AuthState.Error("Network error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status") ?: "pending"
                    val role = snapshot.getString("role") ?: "employee"

                    if (status == "approved") {
                        if (role == "admin") {
                            _authState.value = AuthState.Authenticated("approved")
                            return@addSnapshotListener
                        }

                        val approvedDevices = snapshot.get("approvedDevices") as? List<String> ?: emptyList()
                        val pendingDevices = snapshot.get("pendingDevices") as? List<String> ?: emptyList()

                        when {
                            approvedDevices.contains(deviceId) -> {
                                _authState.value = AuthState.Authenticated("approved")
                            }
                            pendingDevices.contains(deviceId) -> {
                                _authState.value = AuthState.Authenticated("device_pending")
                            }
                            else -> {
                                db.collection("users").document(userId)
                                    .update("pendingDevices", FieldValue.arrayUnion(deviceId))
                                _authState.value = AuthState.Authenticated("device_pending")
                            }
                        }
                    } else {
                        _authState.value = AuthState.Authenticated(status)
                    }
                } else {
                    viewModelScope.launch {
                        try { repository.clearAllLocalData() } catch (e: Exception) {}
                        auth.signOut()
                        _authState.value = AuthState.Error("Your account was removed by the Administrator.")
                    }
                }
            }
    }

    fun signUp(email: String, pass: String, name: String, deviceId: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                try { repository.clearAllLocalData() } catch (e: Exception) {}
                if (checkDeviceConflict("NEW_USER", deviceId)) {
                    _authState.value = AuthState.Error("Device Security: This phone is registered to an active or pending employee.")
                    return@launch
                }

                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val userId = result.user?.uid ?: throw Exception("User creation failed")

                val userProfile = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "role" to "employee",
                    "status" to "pending",
                    "approvedDevices" to emptyList<String>(),
                    "pendingDevices" to listOf(deviceId)
                )

                db.collection("users").document(userId).set(userProfile).await()
                observeUserStatus(userId, deviceId)

            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Signup failed")
            }
        }
    }

    fun login(email: String, pass: String, deviceId: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                try { repository.clearAllLocalData() } catch (e: Exception) {}

                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val userId = result.user?.uid ?: throw Exception("Login failed")

                val userDoc = db.collection("users").document(userId).get().await()
                val role = userDoc.getString("role") ?: "employee"
                if (role != "admin" && checkDeviceConflict(userId, deviceId)) {
                    auth.signOut()
                    _authState.value = AuthState.Error("Device Security: This phone is registered to an active or pending employee.")
                    return@launch
                }

                observeUserStatus(userId, deviceId)

            } catch (e: Exception) {
                if (_authState.value is AuthState.Error && (_authState.value as AuthState.Error).message.contains("Device Security")) {
                } else {
                    _authState.value = AuthState.Error("Invalid credentials")
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                repository.clearAllLocalData()
            } catch (e: Exception) {}

            statusListener?.remove()
            auth.signOut()
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun reapply() {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                db.collection("users").document(currentUser.uid)
                    .update("status", "pending")
                    .await()
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to re-submit: ${e.message}")
            }
        }
    }
}