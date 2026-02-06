// Имя файла: build.gradle.kts
// (Корневой файл проекта - тот самый, где была ошибка)
// --- НАЧАЛО ФАЙЛА ---
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hiltAndroid) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}
// --- КОНЕЦ ФАЙЛА ---
// Имя файла: build.gradle.kts