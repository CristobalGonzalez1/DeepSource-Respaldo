// [GUIA/ARCHIVO] Configuración de compilación: settings.gradle.kts. Lee estos comentarios antes de modificar valores.
// [GUIA/CONFIG] Fuentes de plugins necesarios para compilar el proyecto.
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
// [GUIA/CONFIG] Fuentes de donde Gradle obtiene las librerías.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
// [GUIA/CONFIG] Nombre con que Android Studio identifica el proyecto.
rootProject.name = "DeepSourceHoras"
// [GUIA/CONFIG] Incluye el módulo de aplicación, donde está el código Kotlin.
include(":app")
