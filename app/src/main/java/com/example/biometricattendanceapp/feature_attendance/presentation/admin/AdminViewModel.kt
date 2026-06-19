package com.example.biometricattendanceapp.feature_attendance.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Employee(val id: String, val name: String, val email: String, val status: String)

data class AttendanceLog(
    val id: String,
    val userId: String,
    val userName: String,
    val date: String,
    val punchInTime: Long,
    val punchOutTime: Long?,
    val officeName: String
)

data class AdminAbsenceNote(
    val id: String,
    val userId: String,
    val userName: String,
    val date: String,
    val note: String
)

data class GlobalHoliday(
    val date: String,
    val name: String
)

data class DeviceRequest(
    val userId: String,
    val userName: String,
    val email: String,
    val deviceId: String
)

class AdminViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _pendingEmployees = MutableStateFlow<List<Employee>>(emptyList())
    val pendingEmployees = _pendingEmployees.asStateFlow()

    private val _pendingDeviceRequests = MutableStateFlow<List<DeviceRequest>>(emptyList())
    val pendingDeviceRequests = _pendingDeviceRequests.asStateFlow()

    private val _recentLogs = MutableStateFlow<List<AttendanceLog>>(emptyList())
    val recentLogs = _recentLogs.asStateFlow()

    private val _employeesList = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val employeesList = _employeesList.asStateFlow()

    private val _pendingNotes = MutableStateFlow<List<AdminAbsenceNote>>(emptyList())
    val pendingNotes = _pendingNotes.asStateFlow()

    private val _globalHolidays = MutableStateFlow<List<GlobalHoliday>>(emptyList())
    val globalHolidays = _globalHolidays.asStateFlow()

    private val _officeClosingTime = MutableStateFlow("18:00")
    val officeClosingTime = _officeClosingTime.asStateFlow()

    private val userNamesCache = mutableMapOf<String, String>()
    private var rawLogs = listOf<AttendanceLog>()
    private var rawNotes = listOf<AdminAbsenceNote>()

    init {
        fetchPendingEmployees()
        fetchPendingDeviceRequests()
        fetchAllUserNames()
        fetchRecentLogs()
        fetchPendingNotes()
        fetchGlobalHolidays()
        fetchSettings()
    }

    private fun fetchPendingDeviceRequests() {
        db.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener

            val requests = mutableListOf<DeviceRequest>()
            snapshot?.documents?.forEach { doc ->
                val pendingList = doc.get("pendingDevices") as? List<String> ?: emptyList()
                val name = doc.getString("name") ?: "Unknown"
                val email = doc.getString("email") ?: "Unknown"

                pendingList.forEach { deviceId ->
                    requests.add(DeviceRequest(doc.id, name, email, deviceId))
                }
            }
            _pendingDeviceRequests.value = requests
        }
    }

    fun approveDeviceForUser(userId: String, deviceId: String) {
        viewModelScope.launch {
            try {
                db.collection("users").document(userId).update(
                    mapOf(
                        "pendingDevices" to FieldValue.arrayRemove(deviceId),
                        "approvedDevices" to FieldValue.arrayUnion(deviceId)
                    )
                ).await()

                val registryData = hashMapOf(
                    "userId" to userId,
                    "approvedAt" to System.currentTimeMillis()
                )
                db.collection("device_registry").document(deviceId).set(registryData).await()
            } catch (e: Exception) {
                println("Failed to approve device: ${e.message}")
            }
        }
    }
    fun rejectDeviceForUser(userId: String, deviceId: String) {
        viewModelScope.launch {
            try {
                db.collection("users").document(userId).update(
                    "pendingDevices", FieldValue.arrayRemove(deviceId)
                ).await()
            } catch (e: Exception) {
                println("Failed to reject device: ${e.message}")
            }
        }
    }

    private fun fetchGlobalHolidays() {
        db.collection("holidays").orderBy("date").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener

            _globalHolidays.value = snapshot?.documents?.mapNotNull { doc ->
                GlobalHoliday(
                    date = doc.id,
                    name = doc.getString("name") ?: "Holiday"
                )
            } ?: emptyList()
        }
    }

    fun addGlobalHoliday(date: String, name: String) {
        val holidayData = hashMapOf("date" to date, "name" to name)
        viewModelScope.launch {
            db.collection("holidays").document(date).set(holidayData)
            db.collection("holiday_removal_logs")
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot) {
                        document.reference.delete()
                    }
                }
        }
    }

    fun deleteGlobalHoliday(date: String) {
        viewModelScope.launch {
            db.collection("holidays").document(date).delete()
        }
    }

    private fun fetchAllUserNames() {
        db.collection("users").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener

            userNamesCache.clear()

            snapshot?.documents?.forEach { doc ->
                userNamesCache[doc.id] = doc.getString("name") ?: "Unknown"
            }
            refreshLogsWithNames()
            refreshNotesWithNames()
        }
    }

    private fun fetchPendingNotes() {
        db.collection("attendance_notes").whereEqualTo("status", "pending_review").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            rawNotes = snapshot?.documents?.mapNotNull { doc ->
                AdminAbsenceNote(
                    id = doc.id, userId = doc.getString("userId") ?: "", userName = "Loading...",
                    date = doc.getString("date") ?: "", note = doc.getString("note") ?: ""
                )
            } ?: emptyList()
            refreshNotesWithNames()
        }
    }

    private fun refreshNotesWithNames() {
        _pendingNotes.value = rawNotes.mapNotNull { note ->
            val name = userNamesCache[note.userId]
            if (name == null) null else note.copy(userName = name)
        }
    }

    fun updateNoteStatus(noteId: String, newStatus: String) {
        viewModelScope.launch { db.collection("attendance_notes").document(noteId).update("status", newStatus) }
    }

    private fun fetchRecentLogs() {
        db.collection("attendance_logs").orderBy("timestamp", Query.Direction.DESCENDING).limit(50).addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            rawLogs = snapshot?.documents?.mapNotNull { doc ->
                AttendanceLog(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    userName = "Loading...",
                    date = doc.getString("date") ?: "",
                    punchInTime = doc.getLong("punchInTime") ?: 0L,
                    punchOutTime = doc.getLong("punchOutTime"),
                    officeName = doc.getString("officeName") ?: "Unknown"
                )
            } ?: emptyList()
            refreshLogsWithNames()
        }
    }

    private fun refreshLogsWithNames() {
        _recentLogs.value = rawLogs.mapNotNull { log ->
            val name = userNamesCache[log.userId]
            if (name == null) null else log.copy(userName = name)
        }

        _employeesList.value = rawLogs.mapNotNull { log ->
            val name = userNamesCache[log.userId]
            if (name == null) null else log.userId to name
        }.distinctBy { it.first }
    }

    private fun fetchPendingEmployees() {
        db.collection("users").whereEqualTo("status", "pending").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val employees = snapshot?.documents?.mapNotNull { doc ->
                Employee(id = doc.id, name = doc.getString("name") ?: "Unknown", email = doc.getString("email") ?: "Unknown", status = doc.getString("status") ?: "pending")
            } ?: emptyList()
            _pendingEmployees.value = employees
        }
    }

    fun approveEmployee(employeeId: String) {
        viewModelScope.launch {
            db.collection("users").document(employeeId).update("status", "approved")
        }
    }

    fun rejectEmployee(employeeId: String) {
        viewModelScope.launch {
            db.collection("users").document(employeeId).update("status", "rejected")
        }
    }

    fun removeHolidayWithNote(date: String, reason: String) {
        viewModelScope.launch {
            db.collection("holidays").document(date).delete()

            val removalData = hashMapOf(
                "date" to date,
                "reason" to reason,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("holiday_removal_logs").add(removalData)
        }
    }

    private fun fetchSettings() {
        db.collection("settings").document("office")
            .addSnapshotListener { snapshot, _ ->
                _officeClosingTime.value = snapshot?.getString("closingTime") ?: "18:00"
            }
    }

    fun updateClosingTime(time: String, onSuccess: () -> Unit) {
        db.collection("settings").document("office").set(mapOf("closingTime" to time))
            .addOnSuccessListener {
                onSuccess()
            }
    }

    fun deleteEmployeeCompletely(userId: String) {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()

            try {
                val logsSnapshot = db.collection("attendance_logs")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                for (doc in logsSnapshot.documents) {
                    doc.reference.delete().await()
                }
                val notesSnapshot = db.collection("attendance_notes")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                for (doc in notesSnapshot.documents) {
                    doc.reference.delete().await()
                }
                db.collection("users").document(userId).delete().await()
            } catch (e: Exception) {
                println("Failed to deep delete user: ${e.message}")
            }
        }
    }
}