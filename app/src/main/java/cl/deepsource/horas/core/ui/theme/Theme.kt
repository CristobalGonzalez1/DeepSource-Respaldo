package cl.deepsource.horas.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

// Agrupa colores semánticos de Material 3 para que todos los componentes compartan identidad.
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

// Define tamaños predeterminados de bordes redondeados.
private val DsShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
// DeepSourceTheme: Provee MaterialTheme: paleta oscura, tamaños de letra y formas para todas las pantallas.
fun DeepSourceTheme(content: @Composable () -> Unit) {
    // Aquí se distribuyen paleta, tipografía y formas a los hijos Compose.
    MaterialTheme(
        colorScheme = DsColorScheme,
        typography = DsTypography,
        shapes = DsShapes,
    ) {
        // Aplica color de texto general a los contenidos debajo del tema.
        CompositionLocalProvider(
            LocalContentColor provides DsTextPrimary,
            content = content,
        )
    }
}
