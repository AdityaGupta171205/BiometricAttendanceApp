package com.example.biometricattendanceapp.core.theme

import androidx.compose.ui.graphics.Color

val PrimaryLight = Color(0xFF1A1A1A) // Near black for sharp, dominant elements
val SecondaryLight = Color(0xFF4A4A4A) // Soft graphite for secondary actions
val BackgroundLight = Color(0xFFF8F9FA) // Crisp, slightly off-white background to reduce eye strain
val SurfaceLight = Color(0xFFFFFFFF) // Pure white for cards to pop against the background
val ErrorLight = Color(0xFFD32F2F) // Clean, readable red for rejections/errors

val PrimaryDark = Color(0xFFE0E0E0) // Crisp off-white for primary text/icons in dark mode
val SecondaryDark = Color(0xFFA0A0A0) // Muted grey for secondary text
val BackgroundDark = Color(0xFF121212) // Deep OLED-friendly black
val SurfaceDark = Color(0xFF1E1E1E) // Slightly elevated grey for cards
val ErrorDark = Color(0xFFCF6679) // Softer red that doesn't bleed on dark screens

// --- ACCENT COLORS (For specific buttons like Punch In/Out) ---
val SuccessGreen = Color(0xFF2E7D32) // Professional, earthy green
val AlertOrange = Color(0xFFF57C00) // For warnings or "Out of Area" maps