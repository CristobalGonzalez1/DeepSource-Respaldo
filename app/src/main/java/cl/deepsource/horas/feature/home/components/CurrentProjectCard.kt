package cl.deepsource.horas.feature.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.deepsource.horas.core.ui.theme.DsBlue
import cl.deepsource.horas.core.ui.theme.DsCyan
import cl.deepsource.horas.core.ui.theme.DsError
import cl.deepsource.horas.core.ui.theme.DsOutline
import cl.deepsource.horas.core.ui.theme.DsSurface
import cl.deepsource.horas.core.ui.theme.DsTextMuted
import cl.deepsource.horas.core.ui.theme.DsTextPrimary
import cl.deepsource.horas.core.ui.theme.DsViolet
import cl.deepsource.horas.domain.model.Project
import cl.deepsource.horas.domain.model.WorkTask

@Composable
fun CurrentProjectCard(
    currentProject: Project?,
    weeklyMinutes: Long,
    activeTask: WorkTask?,
    activeElapsedSeconds: Long,
    onStopTask: () -> Unit,
    onCreateProject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = DsSurface),
        border = BorderStroke(
            width = 1.dp,
            brush = if (currentProject != null) {
                Brush.horizontalGradient(listOf(DsCyan, DsBlue, DsViolet))
            } else {
                Brush.horizontalGradient(listOf(DsOutline, DsOutline))
            },
        ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        if (currentProject != null) {
                            Brush.horizontalGradient(listOf(DsCyan, DsBlue, DsViolet))
                        } else {
                            Brush.horizontalGradient(listOf(DsOutline, DsOutline))
                        },
                    ),
            )

            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "PROYECTO ACTUAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = DsTextMuted,
                        letterSpacing = 1.2.sp,
                    )

                    if (currentProject != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DsCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "SELECCIONADO",
                                style = MaterialTheme.typography.labelSmall,
                                color = DsCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (currentProject != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(DsCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = DsCyan,
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentProject.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = DsTextPrimary,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Horas esta semana: ${formatHoursMinutes(weeklyMinutes)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DsCyan,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = activeTask != null && activeTask.projectId == currentProject.id,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        if (activeTask != null) {
                            Column {
                                Spacer(Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .background(DsCyan.copy(alpha = 0.08f))
                                        .padding(12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column {
                                            Text(
                                                text = "${if (activeTask.paused) "Aplazada" else "En curso"}: ${activeTask.taskName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DsTextMuted,
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = formatSecondsToHms(activeElapsedSeconds),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = FontFamily.Monospace,
                                                color = DsCyan,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = onStopTask,
                                            shape = MaterialTheme.shapes.small,
                                            border = BorderStroke(1.dp, DsError),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = DsError,
                                            ),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Stop,
                                                contentDescription = null,
                                                tint = DsError,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Detener", color = DsError, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No hay un proyecto activo seleccionado",
                        style = MaterialTheme.typography.bodyLarge,
                        color = DsTextMuted,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onCreateProject,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, DsCyan),
                    ) {
                        Text("Crear proyecto", color = DsCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatHoursMinutes(minutes: Long): String {
    val hrs = minutes / 60
    val mins = minutes % 60
    return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
}

private fun formatSecondsToHms(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
}
