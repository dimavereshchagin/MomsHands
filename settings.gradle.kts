// settings.gradle.kts
// Корневой файл настройки проекта для Mom's Hands
// Поддерживает Android и будущее расширение на iOS (KMP)

rootProject.name = "Mom's Hands"

// Подключаем модуль androidApp
include(":androidApp")

// Опционально: в будущем можно добавить
// include(":shared") // для общей логики (Kotlin Multiplatform)
// include(":iosApp") // если будете делать iOS

// Настройка имени проекта для Android
findProject(":androidApp")?.name = "androidApp"