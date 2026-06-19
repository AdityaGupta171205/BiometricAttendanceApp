package com.example.biometricattendanceapp.feature_attendance.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceEntity(
    @PrimaryKey
    val id: String,
    val userId: String,

    val date: String,
    val punchInTime: Long,
    val punchOutTime: Long? = null,
    val isMock: Boolean = false,
    val timestamp: Long,
    val officeName: String,

    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING
)