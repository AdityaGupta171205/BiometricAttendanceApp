package com.example.biometricattendanceapp.feature_attendance.data

import com.example.biometricattendanceapp.feature_attendance.data.local.AttendanceDao
import com.example.biometricattendanceapp.feature_attendance.data.local.AttendanceEntity
import com.example.biometricattendanceapp.feature_attendance.data.local.SyncStatus
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FirebaseFirestore

class AttendanceRepository @Inject constructor(
    private val dao: AttendanceDao
) {
    private val auth = FirebaseAuth.getInstance()

    suspend fun punchIn(date: String, timestamp: Long, isMock: Boolean, officeName: String) {
        val userId = auth.currentUser?.uid ?: return
        val shiftId = "${date}_${userId}"

        val localEntity = dao.getLogById(shiftId)

        if (localEntity == null) {
            val newEntity = AttendanceEntity(
                id = shiftId,
                userId = userId,
                date = date,
                punchInTime = timestamp,
                punchOutTime = null,
                isMock = isMock,
                timestamp = timestamp,
                officeName = officeName,
                syncStatus = SyncStatus.PENDING
            )
            dao.insert(newEntity)
        } else {
            dao.update(
                localEntity.copy(
                    punchOutTime = null,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
    }

    suspend fun punchOut(date: String, timestamp: Long) {
        val userId = auth.currentUser?.uid ?: return
        val shiftId = "${date}_${userId}"

        val localEntity = dao.getLogById(shiftId)
        if (localEntity != null) {
            dao.update(
                localEntity.copy(
                    punchOutTime = timestamp,
                    syncStatus = SyncStatus.PENDING
                )
            )
        }
    }

    suspend fun getLastRecordForToday(date: String): AttendanceEntity? {
        val userId = auth.currentUser?.uid ?: return null
        val shiftId = "${date}_${userId}"
        return dao.getLogById(shiftId)
    }

    suspend fun clearAllLocalData() {
        dao.clearAll()
    }

    suspend fun fetchTodayLogFromFirebase(date: String): AttendanceEntity? {
        val userId = auth.currentUser?.uid ?: return null
        val shiftId = "${date}_${userId}"

        return try {
            val db = FirebaseFirestore.getInstance()
            val document = db.collection("attendance_logs").document(shiftId).get().await()

            if (document.exists()) {
                val entity = AttendanceEntity(
                    id = shiftId,
                    userId = userId,
                    date = date,
                    punchInTime = document.getLong("punchInTime") ?: 0L,
                    punchOutTime = document.getLong("punchOutTime"),
                    isMock = document.getBoolean("isMock") ?: false,
                    timestamp = document.getLong("timestamp") ?: 0L,
                    officeName = document.getString("officeName") ?: "Unknown",
                    syncStatus = SyncStatus.SYNCED // Already in Firebase!
                )
                dao.insert(entity)

                entity
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}