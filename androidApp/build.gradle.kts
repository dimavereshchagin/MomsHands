// build.gradle.kts
// Сборка Android-приложения для Mom's Hands
// Clean Architecture + MVVM + Room + MPAndroidChart + PDF + Темная тема + Мультиязычность

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.moms.hands"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.moms.hands"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Поддержка нескольких детей
        // Данные будут храниться с childId
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        // Используем современный Java 17
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        // Kotlin для Android
        jvmTarget = "17"
    }

    buildFeatures {
        // Включаем View Binding — безопасная работа с UI
        viewBinding = true

        // В будущем можно добавить:
        // compose = true // для Jetpack Compose
    }

    // Поддержка нескольких файлов ресурсов (для мультиязычности)
    resourcePrefix = "main_"

    // Настройка APK
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")

    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Архитектура: MVVM + Clean Architecture
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // Локальная база данных (Room)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Фоновые задачи — умные напоминания
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // Графики: круговые, линейные, тепловые
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Экспорт в PDF
    implementation("androidx.print:print:1.0.0")

    // Шифрование базы данных (резервное копирование)
    implementation("net.sqlcipher:android-database-sqlcipher:4.5.3@aar")

    // Тестирование
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}