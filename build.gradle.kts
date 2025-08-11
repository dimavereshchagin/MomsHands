// Корневой build.gradle.kts — настройка всего проекта
plugins {
    // Подключаем Android Gradle Plugin
    val agp_version by extra("8.6.0")
}

// Настраиваем все подпроекты
allprojects {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal() // Для сторонних плагинов
    }
}

// Настройка зависимостей для сборки
buildscript {
    val agp_version by extra("8.6.0")
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Основной плагин для Android
        classpath("com.android.tools.build:gradle:$agp_version")
        // Плагин для Kotlin
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
    }
}