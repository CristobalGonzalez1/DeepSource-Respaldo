import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    jacoco
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
        versionCode = 14
        versionName = "1.14"
        val googleClientId = providers.gradleProperty("GOOGLE_WEB_CLIENT_ID").getOrElse("")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
        // Configurable from gradle.properties or -PAPI_BASE_URL=...
        val apiBase = providers.gradleProperty("API_BASE_URL").getOrElse("http://10.0.2.2:8081/")
        val hoursBase = providers.gradleProperty("HOURS_BASE_URL").getOrElse("http://10.0.2.2:8082/")
        buildConfigField("String", "API_BASE_URL", "\"$apiBase\"")
        buildConfigField("String", "HOURS_BASE_URL", "\"$hoursBase\"")
    }
    buildTypes {
        debug { enableUnitTestCoverage = true }
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


// Informe de cobertura de reglas de negocio. El reporte global de Android se conserva
// por separado; este informe mide únicamente el código lógico explícitamente incluido.
val logicCoverageIncludes = listOf(
    "cl/deepsource/horas/core/validation/**",
    "cl/deepsource/horas/domain/logic/**",
    "cl/deepsource/horas/feature/auth/domain/RolePermissions*"
)

tasks.register<JacocoReport>("businessLogicCoverageReport") {
    dependsOn("testDebugUnitTest")
    val kotlinClasses = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        include(logicCoverageIncludes)
    }
    val javaClasses = fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
        include(logicCoverageIncludes)
    }
    classDirectories.setFrom(kotlinClasses, javaClasses)
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(fileTree(layout.buildDirectory) { include("**/*.exec", "**/*.ec") })
    reports { html.required.set(true); xml.required.set(true); csv.required.set(true) }
}

tasks.register<JacocoCoverageVerification>("verifyBusinessLogic80") {
    dependsOn("businessLogicCoverageReport")
    classDirectories.setFrom(fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        include(logicCoverageIncludes)
    }, fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
        include(logicCoverageIncludes)
    })
    executionData.setFrom(fileTree(layout.buildDirectory) { include("**/*.exec", "**/*.ec") })
    violationRules {
        rule {
            element = "BUNDLE"
            limit { counter = "INSTRUCTION"; value = "COVEREDRATIO"; minimum = "0.80".toBigDecimal() }
        }
    }
}
