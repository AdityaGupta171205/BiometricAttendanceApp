// app/build.gradle.kts
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("com.google.gms.google-services")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace   = "com.example.biometricattendanceapp"
    compileSdk  = 34

    defaultConfig {
        applicationId   = "com.example.biometricattendanceapp"
        minSdk          = 26
        targetSdk       = 34
        versionCode     = 1
        versionName     = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        resourceConfigurations += listOf("en", "xxhdpi")
        buildConfigField("String", "OFFICE1_LATITUDE", localProperties.getProperty("OFFICE1_LATITUDE") ?: "\"0.0\"")
        buildConfigField("String", "OFFICE1_LONGITUDE", localProperties.getProperty("OFFICE1_LONGITUDE") ?: "\"0.0\"")
        buildConfigField("String", "OFFICE2_LATITUDE", localProperties.getProperty("OFFICE2_LATITUDE") ?: "\"0.0\"")
        buildConfigField("String", "OFFICE2_LONGITUDE", localProperties.getProperty("OFFICE2_LONGITUDE") ?: "\"0.0\"")
    }

    buildTypes {
        debug {
            isDebuggable = true
            ndk {
                abiFilters += listOf("arm64-v8a")
            }
        }
        release {
            isMinifyEnabled         = true
            isShrinkResources       = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── Core ────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    // ── Compose ─────────────────────────────────────────────
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)

    // ── Navigation ──────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ── Hilt (Dependency Injection) ─────────────────────────
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    kapt(libs.hilt.compiler)

    // ── Room (Offline Database) ─────────────────────────────
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // ── DataStore (Local Settings) ──────────────────────────
    implementation(libs.datastore.preferences)

    // ── Firebase (Backend) ──────────────────────────────────
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // ── Location ────────────────────────────────────────────
    implementation(libs.play.services.location)

    // ── WorkManager (Background Sync) ───────────────────────
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // ── Coroutines & Async Support ──────────────────────────
    implementation(libs.bundles.coroutines)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ── Biometrics ──────────────────────────────────────────
    implementation(libs.biometric)

    // ── Testing ─────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test)
}

// Required for Hilt annotation processing
kapt {
    correctErrorTypes = true
}