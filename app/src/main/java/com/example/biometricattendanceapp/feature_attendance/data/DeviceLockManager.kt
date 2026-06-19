package com.example.biometricattendanceapp.feature_attendance.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeviceLockManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("device_lock_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_REGISTERED_USER = "registered_user_id"
        private const val KEY_DEVICE_UUID = "device_unique_uuid"
    }

    /**
     * Returns the UID of the employee who owns this phone, or null if the phone is brand new/unregistered.
     */
    fun getRegisteredUserId(): String? {
        return prefs.getString(KEY_REGISTERED_USER, null)
    }

    /**
     * Permanently locks this phone to the given UID.
     */
    fun registerDeviceToUser(userId: String) {
        prefs.edit().putString(KEY_REGISTERED_USER, userId).apply()
    }

    /**
     * Optional: An admin function to wipe the device if an employee leaves the company.
     * (You would only trigger this from a hidden admin menu).
     */
    fun clearDeviceLock() {
        prefs.edit().remove(KEY_REGISTERED_USER).apply()
    }

    fun getUniqueDeviceId(): String {
        var id = prefs.getString(KEY_DEVICE_UUID, null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_UUID, id).apply()
        }
        return id
    }
}