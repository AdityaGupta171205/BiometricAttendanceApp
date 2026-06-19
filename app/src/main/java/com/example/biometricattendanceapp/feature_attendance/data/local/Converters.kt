package com.example.biometricattendanceapp.feature_attendance.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String {
        return value.name
    }

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return enumValueOf<SyncStatus>(value)
    }
}