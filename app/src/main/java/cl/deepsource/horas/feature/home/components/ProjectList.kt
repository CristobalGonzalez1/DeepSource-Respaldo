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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.deepsource.horas.core.ui.theme.DsBlue
import cl.deepsource.horas.core.ui.theme.DsCyan
import cl.deepsource.horas.core.ui.theme.DsOutline
import cl.deepsource.horas.core.ui.theme.DsSurface
import cl.deepsource.horas.core.ui.theme.DsTextMuted
import cl.deepsource.horas.core.ui.theme.DsTextPrimary
import cl.deepsource.horas.core.ui.theme.DsViolet
import cl.deepsource.horas.domain.model.Project
import cl.deepsource.horas.domain.model.ProjectWithStats

@Composable
fun ProjectList(
    projects: List<ProjectWithStats>,
    onSelectProject: (String) -> Unit,
    onCreateProject: () -> Unit,
    onDeleteProject: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "PROYECTOS (${projects.size})",
                style = MaterialTheme.typography.labelSmall,
                color = DsTextMuted,
                letterSpacing = 1.2.sp,
            )
            TextButton(onClick = onCreateProject) {
                Text("+ Cargar proyecto", color = DsCyan, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (projects.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = DsSurface),
                border = BorderStroke(1.dp, DsOutline),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No tienes proyectos todavía",
                        style = MaterialTheme.typography.bodyLarge,
                        color = DsTextMuted,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onCreateProject,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, DsCyan),
                    ) {
                        Text("Cargar primer proyecto", color = DsCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                projects.forEach { item ->
                    ProjectItemRow(
                        item = item,
                        onClick = { onSelectProject(item.project.id) },
                        onDeleteClick = { projectToDelete = item.project },
                    )
                }
            }
        }
    }

    projectToDelete?.let { proj ->
        ConfirmDeleteProjectDialog(
            projectName = proj.name,
            onDismiss = { projectToDelete = null },
            onConfirm = { onDeleteProject(proj.id) },
        )
    }
}

@Composable
private fun ProjectItemRow(
    item: ProjectWithStats,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCurrent) DsSurface.copy(alpha = 0.95f) else DsSurface,
        ),
        border = BorderStroke(
            width = if (item.isCurrent) 1.5.dp else 1.dp,
            brush = if (item.isCurrent) {
                Brush.horizontalGradient(listOf(DsCyan, DsBlue, DsViolet))
            } else {
                Brush.horizontalGradient(listOf(DsOutline, DsOutline))
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                if (item.isCurrent) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(DsCyan)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "SELECCIONADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = item.project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (item.isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = DsTextPrimary,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (item.isCurrent) DsCyan.copy(alpha = 0.15f) else DsOutline.copy(alpha = 0.3f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = formatDuration(item.totalLoggedMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.isCurrent) DsCyan else DsTextMuted,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar proyecto",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

private fun formatDuration(minutes: Long): String {
    val hrs = minutes / 60
    val mins = minutes % 60
    return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
}
