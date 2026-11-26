plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "cl.deepsource.horas"
    compileSdk = 35
    defaultConfig {
        applicationId = "cl.deepsource.horas"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "1.12"
        val googleClientId = providers.gradleProperty("GOOGLE_WEB_CLIENT_ID").getOrElse("")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
        // Configurable from gradle.properties or -PAPI_BASE_URL=...
        val apiBase = providers.gradleProperty("API_BASE_URL").getOrElse("http://10.0.2.2:8081/")
        val hoursBase = providers.gradleProperty("HOURS_BASE_URL").getOrElse("http://10.0.2.2:8082/")
        buildConfigField("String", "API_BASE_URL", "\"$apiBase\"")
        buildConfigField("String", "HOURS_BASE_URL", "\"$hoursBase\"")
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
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
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.google.googleid)
    implementation(libs.kotlinx.coroutines.android)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    debugImplementation(libs.androidx.compose.ui.tooling)
}
