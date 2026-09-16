plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}

android {
    namespace = "com.jarvis.calendar"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.jarvis.calendar"
        minSdk = 26
        targetSdk = 34
        versionCode = 12
        versionName = "6.0-Final"
    }
    buildTypes { 
        release { 
            isMinifyEnabled = false 
        } 
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { 
        jvmTarget = "1.8" 
    }
    buildFeatures { 
        compose = true 
    }
    composeOptions { 
        kotlinCompilerExtensionVersion = "1.5.5" 
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.json:json:20231013")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // 🎯 СЕРЕБРЯНАЯ ПУЛЯ: Готовая prebuilt библиотека llama.cpp для Android
    // Не требует NDK, работает из коробки, содержит нативные .so файлы
    implementation("dev.ffmpegkit-maintained:llama-android:0.1.1")
}
