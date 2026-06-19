package com.example.biometricattendanceapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.biometricattendanceapp.feature_attendance.data.local.AttendanceDao
import com.example.biometricattendanceapp.feature_attendance.data.local.AttendanceEntity

@Database(
    entities = [AttendanceEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao
}