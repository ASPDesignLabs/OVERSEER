plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.snakesan.overseer"
    compileSdk = 34 // 36 is experimental; 34/35 is safer for current WearOS

    defaultConfig {
        applicationId = "com.snakesan.overseer"
        minSdk = 30 // Lowered slightly to ensure broad Watch 4/5/6 compatibility
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("shared_config") {
            // Ensure this file actually exists at project root
            storeFile = file("${rootProject.projectDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Enable shrinking for WearOS apps
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("shared_config")
        }
        debug {
            signingConfig = signingConfigs.getByName("shared_config")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // General Android
    implementation(libs.core.splashscreen)
    implementation("androidx.activity:activity-compose:1.8.2")

    // Play Services
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Wear Compose (The Vitals)
    val composeWearVersion = "1.3.0"
    implementation("androidx.wear.compose:compose-foundation:$composeWearVersion")
    implementation("androidx.wear.compose:compose-material:$composeWearVersion")
    implementation("androidx.wear.compose:compose-navigation:$composeWearVersion")
    implementation("androidx.wear:wear:1.3.0")

    // Standard Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)

    // Tools
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}