# 🏢 Biometric Attendance App

> An offline-first, highly secure biometric attendance system designed for enterprise use. Built natively using Kotlin, this application ensures that employees can only mark their attendance when physically present within a predefined office geofence.

## ✨ Key Features

* **Strict Geofencing & Location Engine:** Calculates real-time distance between the device and office coordinates. Attendance marking is strictly restricted to the established workplace radius.
* **Anti-Spoofing Security:** Intercepts location payloads and utilizes the Android SDK's `isMock` flag to detect and block "Fake GPS" applications instantly.
* **Offline-First & Auto-Sync:** Functions seamlessly without an active internet connection. User schedules and logs are cached locally using Room DB. A WorkManager network listener automatically pushes pending logs to Firebase when a stable connection is restored.
* **Device Authorization:** Prevents multi-device fraud by binding user accounts to a unique device ID that requires explicit Admin approval before use.
* **Time-Tamper Prevention:** Validates timestamps using `SystemClock.elapsedRealtime()` against the last known secure server sync to prevent offline device-clock manipulation.
* **Smart Shift Reminders:** Automatically schedules a local push notification 5 minutes before shift-end to remind employees to punch out.
* **Out-of-Bounds Navigation:** If a user is outside the geofence, the app launches a dynamic map with a polyline route guiding them to the office.

## 🛠️ Tech Stack

| Category | Technology / Tool |
| :--- | :--- |
| **Language** | Kotlin |
| **Development Environment**| Android Studio |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Dependency Injection**| Dagger Hilt |
| **Backend & Auth** | Firebase Firestore & Firebase Auth |
| **Local Database** | Room (SQLite) |
| **Background Processing** | WorkManager |
| **Mapping & Routing** | Google Maps SDK |
| **Security APIs** | Android Biometric API, LocationManager (`isMock`) |

## 🏗️ Core Workflow

1. **Onboarding:** Generates a unique Device ID upon first launch. Enters a locked "Pending" state until an Administrator approves the device via the backend.
2. **Daily Validation:** Verifies GPS state, permissions, and checks for mock locations before allowing any interaction.
3. **Authentication:** Triggers Android's native BiometricPrompt (Face ID / Fingerprint) for identity verification.
4. **Exception Handling:** Features a dynamic, color-coded calendar where users can tap on incomplete days to submit explanatory notes directly to HR.

## 📱 User Interface Highlights

* **Primary Dashboard:** Clean, card-based interface providing instant visual hierarchy for marking attendance, viewing ledgers, and accessing the HR panel.
* **Employee Calendar:** A color-coded monthly grid (Green: Present, Blue: Active, Orange: Incomplete, Red: Absent, Yellow: Holiday).
* **Ledger View:** Chronological list of punch records with precise times, location tagging, and highlighted active shifts.

---
*Developed by Aditya Gupta*
