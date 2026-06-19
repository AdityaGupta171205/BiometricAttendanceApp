package com.example.biometricattendanceapp.feature_attendance.data.remote.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.biometricattendanceapp.feature_attendance.data.local.AttendanceDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class PunchSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dao: AttendanceDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) return Result.failure()

        return try {
            val unsyncedRecords = dao.getUnsyncedRecords()

            if (unsyncedRecords.isEmpty()) {
                return Result.success()
            }
            val syncedIds = mutableListOf<String>()
            for (record in unsyncedRecords) {
                val punchMap = hashMapOf(
                    "id" to record.id,
                    "userId" to currentUserId,
                    "date" to record.date,
                    "punchInTime" to record.punchInTime,
                    "punchOutTime" to record.punchOutTime,
                    "isMock" to record.isMock,
                    "timestamp" to record.timestamp,
                    "officeName" to record.officeName
                )
                db.collection("attendance_logs")
                    .document(record.id)
                    .set(punchMap)
                    .await()

                syncedIds.add(record.id)
            }

            if (syncedIds.isNotEmpty()) {
                dao.markRecordsAsSynced(syncedIds)
            }

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}