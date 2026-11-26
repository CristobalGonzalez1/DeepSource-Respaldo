// [GUIA/ARCHIVO] Configuración de compilación: app/build.gradle.kts. Lee estos comentarios antes de modificar valores.
// [GUIA/CONFIG] Habilita plugin Android, Kotlin y compilador Compose.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// [GUIA/CONFIG] Configura Android SDK, identidad de app y opciones de compilación.
android {
    // [GUIA/CONFIG] Paquete lógico de código y recursos generados.
    namespace = "cl.deepsource.horas"
    // [GUIA/CONFIG] Versión del SDK usada para compilar.
    compileSdk = 35

    // [GUIA/CONFIG] Parámetros de la APK: ID, soporte de versiones y versión pública.
    defaultConfig {
        // [GUIA/CONFIG] Identificador final de instalación en Android.
        applicationId = "cl.deepsource.horas"
        // [GUIA/CONFIG] Versión mínima: Android 8.0 (API 26).
        minSdk = 26
        // [GUIA/CONFIG] Versión de comportamiento Android objetivo.
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

        // [GUIA/CONFIG] Lee el ID web de Google de propiedades Gradle; sin él, Google login devuelve error.
        val googleClientId = providers.gradleProperty("GOOGLE_WEB_CLIENT_ID").getOrElse("")
        // [GUIA/CONFIG] Publica el ID como BuildConfig.GOOGLE_WEB_CLIENT_ID para AppContainer.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
    }

    // [GUIA/CONFIG] Ajustes de builds release/debug.
    buildTypes {
        release { isMinifyEnabled = false }
    }
    // [GUIA/CONFIG] Fuente y bytecode Java configurados para Java 17.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // [GUIA/CONFIG] Objetivo JVM 17 para Kotlin.
    kotlinOptions { jvmTarget = "17" }
    // [GUIA/CONFIG] Activa Compose y clase BuildConfig.
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// [GUIA/CONFIG] Librerías: Compose, Lifecycle, Navigation, Credential Manager, GoogleID y Coroutines.
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    // [GUIA/CONFIG] La navegación entre Login/Home usa esta librería.
    implementation(libs.androidx.navigation.compose)
    // [GUIA/CONFIG] Permite selector nativo de credenciales Google.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.google.googleid)
    // [GUIA/CONFIG] Corrutinas para operaciones y actualización periódica de la interfaz.
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
