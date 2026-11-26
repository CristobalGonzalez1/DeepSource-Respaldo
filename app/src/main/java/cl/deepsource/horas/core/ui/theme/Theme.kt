package cl.deepsource.horas.core.ui.theme
// [GUIA/ARCHIVO] Configura MaterialTheme, colores y esquinas redondeadas globalmente.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/core/ui/theme/Theme.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

// [GUIA/ESTILO] Agrupa colores semánticos de Material 3 para que todos los componentes compartan identidad.
private val DsColorScheme = darkColorScheme(
    primary = DsCyan,
    onPrimary = DsBackground,
    secondary = DsBlue,
    tertiary = DsViolet,
    background = DsBackground,
    onBackground = DsTextPrimary,
    surface = DsSurface,
    onSurface = DsTextPrimary,
    surfaceVariant = DsInput,
    onSurfaceVariant = DsTextMuted,
    outline = DsOutline,
    error = DsError,
)

// [GUIA/ESTILO] Define tamaños predeterminados de bordes redondeados.
private val DsShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
// [GUIA/FUNCION] DeepSourceTheme: Provee MaterialTheme: paleta oscura, tamaños de letra y formas para todas las pantallas.
fun DeepSourceTheme(content: @Composable () -> Unit) {
    // [GUIA/ESTILO] Aquí se distribuyen paleta, tipografía y formas a los hijos Compose.
    MaterialTheme(
        colorScheme = DsColorScheme,
        typography = DsTypography,
        shapes = DsShapes,
    ) {
        // [GUIA/ESTILO] Aplica color de texto general a los contenidos debajo del tema.
        CompositionLocalProvider(
            LocalContentColor provides DsTextPrimary,
            content = content,
        )
    }
}
