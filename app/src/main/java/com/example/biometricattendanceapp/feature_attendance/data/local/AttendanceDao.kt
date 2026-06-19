package com.example.biometricattendanceapp.feature_attendance.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AttendanceEntity)

    @Update
    suspend fun update(record: AttendanceEntity)

    @Query("SELECT * FROM attendance_records WHERE id = :logId LIMIT 1")
    suspend fun getLogById(logId: String): AttendanceEntity?

    @Query("SELECT * FROM attendance_records WHERE date LIKE :month || '%'")
    fun getAttendanceForMonth(month: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE sync_status = 'PENDING'")
    suspend fun getUnsyncedRecords(): List<AttendanceEntity>

    @Query("UPDATE attendance_records SET sync_status = 'SYNCED' WHERE id = :recordId")
    suspend fun markAsSynced(recordId: String)

    @Query("UPDATE attendance_records SET sync_status = 'SYNCED' WHERE id IN (:recordIds)")
    suspend fun markRecordsAsSynced(recordIds: List<String>)

    @Query("DELETE FROM attendance_records")
    suspend fun clearAll()
}