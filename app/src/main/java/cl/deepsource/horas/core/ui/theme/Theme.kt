package cl.deepsource.horas.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

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

private val DsShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun DeepSourceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DsColorScheme,
        typography = DsTypography,
        shapes = DsShapes,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides DsTextPrimary,
            content = content,
        )
    }
}
