package cl.deepsource.horas.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import cl.deepsource.horas.core.ui.theme.DsCyan
import cl.deepsource.horas.core.ui.theme.DsViolet

@Composable
fun DsScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(DsViolet.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.1f),
                        radius = size.width * 0.8f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(DsCyan.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.95f),
                        radius = size.width * 0.8f,
                    ),
                )
            },
        content = content,
    )
}
