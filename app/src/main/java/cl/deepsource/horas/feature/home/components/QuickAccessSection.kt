package cl.deepsource.horas.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cl.deepsource.horas.core.ui.theme.DsCyan
import cl.deepsource.horas.core.ui.theme.DsOutline
import cl.deepsource.horas.core.ui.theme.DsSurface
import cl.deepsource.horas.core.ui.theme.DsTextMuted
import cl.deepsource.horas.core.ui.theme.DsTextPrimary
import cl.deepsource.horas.domain.model.Project

@Composable
fun QuickAccessSection(
    currentProject: Project?,
    onQuickStartTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEnabled = currentProject != null

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Acceso rápido",
            style = MaterialTheme.typography.labelSmall,
            color = DsTextMuted,
        )
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = isEnabled, onClick = onQuickStartTask),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = if (isEnabled) DsSurface else DsSurface.copy(alpha = 0.5f),
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isEnabled) DsCyan.copy(alpha = 0.6f) else DsOutline,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isEnabled) DsCyan.copy(alpha = 0.15f) else DsOutline.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isEnabled) DsCyan else DsTextMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentProject != null) "Iniciar tarea en ${currentProject.name}" else "Iniciar tarea desde el proyecto actual",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isEnabled) DsTextPrimary else DsTextMuted.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (isEnabled) "Acceso directo rápido" else "Selecciona un proyecto activo primero",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEnabled) DsCyan else DsTextMuted.copy(alpha = 0.5f),
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (isEnabled) DsCyan else DsTextMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
