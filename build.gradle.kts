// [GUIA/ARCHIVO] Configuración de compilación: build.gradle.kts. Lee estos comentarios antes de modificar valores.
// [GUIA/CONFIG] Declara plugins generales, sin aplicarlos automáticamente a todos los módulos.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
