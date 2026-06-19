package com.example.biometricattendanceapp.core.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.biometricattendanceapp.core.theme.BackgroundDark
import com.example.biometricattendanceapp.core.theme.BackgroundLight
import com.example.biometricattendanceapp.core.theme.ErrorDark
import com.example.biometricattendanceapp.core.theme.ErrorLight
import com.example.biometricattendanceapp.core.theme.PrimaryDark
import com.example.biometricattendanceapp.core.theme.PrimaryLight
import com.example.biometricattendanceapp.core.theme.SecondaryDark
import com.example.biometricattendanceapp.core.theme.SecondaryLight
import com.example.biometricattendanceapp.core.theme.SurfaceDark
import com.example.biometricattendanceapp.core.theme.SurfaceLight

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = Color(0xFF2C2C2C), // Slightly lighter for inner card elements
    onPrimary = BackgroundDark, // Text on primary buttons should be dark
    onBackground = PrimaryDark, // Text on background should be light
    onSurface = PrimaryDark,
    onSurfaceVariant = SecondaryDark,
    error = ErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    secondary = SecondaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = Color(0xFFEEEEEE), // Soft grey for inner card elements
    onPrimary = SurfaceLight, // Text on primary buttons should be white
    onBackground = PrimaryLight, // Text on background should be dark
    onSurface = PrimaryLight,
    onSurfaceVariant = SecondaryLight,
    error = ErrorLight
)

@Composable
fun BiometricAttendanceAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}