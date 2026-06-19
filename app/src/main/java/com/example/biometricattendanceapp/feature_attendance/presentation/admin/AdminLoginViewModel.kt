package com.example.biometricattendanceapp.feature_attendance.presentation.admin

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class AdminLoginState {
    REQUIRE_BIOMETRIC, SUCCESS, ERROR
}

@HiltViewModel
class AdminLoginViewModel @Inject constructor() : ViewModel() {

    private val _loginState = MutableStateFlow(AdminLoginState.REQUIRE_BIOMETRIC)
    val loginState = _loginState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun onBiometricSuccess() {
        _loginState.value = AdminLoginState.SUCCESS
    }

    fun onBiometricFailed(error: String) {
        _errorMessage.value = error
        _loginState.value = AdminLoginState.ERROR
    }

    fun triggerBiometricAgain() {
        _errorMessage.value = null
        _loginState.value = AdminLoginState.REQUIRE_BIOMETRIC
    }
}