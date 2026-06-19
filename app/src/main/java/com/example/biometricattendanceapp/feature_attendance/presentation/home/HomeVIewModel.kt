package com.example.biometricattendanceapp.feature_attendance.presentation.home

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class UserLedgerLog(
    val id: String,
    val date: String,
    val punchInTime: Long,
    val punchOutTime: Long?,
    val officeName: String,
    val timestamp: Long
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _userName = MutableStateFlow("Employee Portal")
    val userName = _userName.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin = _isAdmin.asStateFlow()

    private val _myLogs = MutableStateFlow<List<UserLedgerLog>>(emptyList())
    val myLogs = _myLogs.asStateFlow()

    init {
        fetchUserData()
        fetchMyLedger()
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fetchedName = document.getString("name") ?: ""
                    if (fetchedName.isNotEmpty()) {
                        _userName.value = "Hi, $fetchedName"
                    }

                    val role = document.getString("role") ?: "employee"
                    _isAdmin.value = (role == "admin")
                }
            }
            .addOnFailureListener {
                _isAdmin.value = false
            }
    }

    private fun fetchMyLedger() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("attendance_logs")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val logs = snapshot?.documents?.mapNotNull { doc ->
                    UserLedgerLog(
                        id = doc.id,
                        date = doc.getString("date") ?: "",
                        punchInTime = doc.getLong("punchInTime") ?: 0L,
                        punchOutTime = doc.getLong("punchOutTime"),
                        officeName = doc.getString("officeName") ?: "Unknown",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }?.sortedByDescending { it.timestamp } ?: emptyList()

                _myLogs.value = logs
            }
    }
}