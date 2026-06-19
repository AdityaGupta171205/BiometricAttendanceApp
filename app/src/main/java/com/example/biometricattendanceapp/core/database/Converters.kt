package com.example.biometricattendanceapp.core.database

import androidx.room.TypeConverter
import com.example.biometricattendanceapp.feature_attendance.data.local.SyncStatus

class Converters {
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = enumValueOf(value)
}