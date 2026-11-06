package cl.deepsource.horas.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.deepsource.horas.core.ui.components.DsPrimaryButton
import cl.deepsource.horas.core.ui.components.DsTextField
import cl.deepsource.horas.core.ui.theme.DsCyan
import cl.deepsource.horas.core.ui.theme.DsSurface
import cl.deepsource.horas.core.ui.theme.DsTextMuted
import cl.deepsource.horas.core.ui.theme.DsTextPrimary
import cl.deepsource.horas.domain.model.Project
import cl.deepsource.horas.domain.model.ProjectWithStats
import cl.deepsource.horas.core.validation.HomeFormValidator

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DsSurface,
        title = {
            Text(
                text = "Cargar proyecto",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DsTextPrimary,
            )
        },
        text = {
            Column {
                DsTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = "Nombre del proyecto",
                    placeholder = "Ej: Portal Cliente AFC",
                    errorMessage = errorText,
                )
                Spacer(Modifier.height(12.dp))
                DsTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Descripción (opcional)",
                    placeholder = "Breve descripción",
                )
            }
        },
        confirmButton = {
            DsPrimaryButton(
                text = "Guardar",
                onClick = {
                    val problem = HomeFormValidator.projectName(name) ?: HomeFormValidator.description(description)
                    if (problem != null) {
                        errorText = problem
                    } else {
                        onCreate(name, description)
                        onDismiss()
                    }
                },
                modifier = Modifier.width(120.dp),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = DsTextMuted)
            }
        },
    )
}

@Composable
fun ConfirmDeleteProjectDialog(
    projectName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DsSurface,
        title = {
            Text(
                text = "Eliminar proyecto",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DsTextPrimary,
            )
        },
        text = {
            Text(
                text = "¿Estás seguro de que deseas eliminar el proyecto \"$projectName\"? Esta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = DsTextMuted,
            )
        },
        confirmButton = {
            DsPrimaryButton(
                text = "Eliminar",
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                modifier = Modifier.width(120.dp),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = DsTextMuted)
            }
        },
    )
}

@Composable
fun StartTaskDialog(
    projects: List<ProjectWithStats>,
    selectedProject: Project?,
    onDismiss: () -> Unit,
    onStart: (projectId: String, taskName: String) -> Unit,
) {
    var chosenProjectId by remember { mutableStateOf(selectedProject?.id ?: projects.firstOrNull()?.project?.id.orEmpty()) }
    var taskName by remember { mutableStateOf("") }
    var taskError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DsSurface,
        title = {
            Text(
                text = "Iniciar nueva tarea",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DsTextPrimary,
            )
        },
        text = {
            Column {
                Text("Selecciona un proyecto:", style = MaterialTheme.typography.labelMedium, color = DsTextMuted)
                Spacer(Modifier.height(8.dp))
                projects.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { chosenProjectId = item.project.id }
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = item.project.id == chosenProjectId,
                            onClick = { chosenProjectId = item.project.id },
                            colors = RadioButtonDefaults.colors(selectedColor = DsCyan),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(item.project.name, style = MaterialTheme.typography.bodyLarge, color = DsTextPrimary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                DsTextField(
                    value = taskName,
                    onValueChange = { taskName = it; taskError = null },
                    label = "Nombre de la tarea (opcional)",
                    placeholder = "Ej: Reunión de avance",
                    errorMessage = taskError,
                )
            }
        },
        confirmButton = {
            DsPrimaryButton(
                text = "Iniciar",
                onClick = {
                    val problem = HomeFormValidator.selectedProject(chosenProjectId) ?: HomeFormValidator.taskName(taskName)
                    if (problem != null) { taskError = problem } else {
                        onStart(chosenProjectId, taskName)
                        onDismiss()
                    }
                },
                modifier = Modifier.width(120.dp),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = DsTextMuted)
            }
        },
    )
}

@Composable
fun LogHoursDialog(
    projects: List<ProjectWithStats>,
    selectedProject: Project?,
    onDismiss: () -> Unit,
    onLog: (projectId: String, durationMinutes: Long, note: String) -> Unit,
) {
    var chosenProjectId by remember { mutableStateOf(selectedProject?.id ?: projects.firstOrNull()?.project?.id.orEmpty()) }
    var hoursText by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DsSurface,
        title = {
            Text(
                text = "Registrar horas manualmente",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DsTextPrimary,
            )
        },
        text = {
            Column {
                Text("Selecciona proyecto:", style = MaterialTheme.typography.labelMedium, color = DsTextMuted)
                Spacer(Modifier.height(4.dp))
                projects.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { chosenProjectId = item.project.id }
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = item.project.id == chosenProjectId,
                            onClick = { chosenProjectId = item.project.id },
                            colors = RadioButtonDefaults.colors(selectedColor = DsCyan),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(item.project.name, style = MaterialTheme.typography.bodyLarge, color = DsTextPrimary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    DsTextField(
                        value = hoursText,
                        onValueChange = {
                            hoursText = it
                            errorText = null
                        },
                        label = "Horas",
                        errorMessage = errorText?.takeIf { HomeFormValidator.validateHours(hoursText) != null },
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    DsTextField(
                        value = minutesText,
                        onValueChange = {
                            minutesText = it
                            errorText = null
                        },
                        label = "Minutos",
                        errorMessage = errorText?.takeIf { HomeFormValidator.validateMinutes(minutesText) != null },
                        placeholder = "30",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (errorText != null) {
                    Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
                DsTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = "Nota o descripción",
                    placeholder = "Ej: Avance módulo de reportes",
                )
            }
        },
        confirmButton = {
            DsPrimaryButton(
                text = "Guardar",
                onClick = {
                    val problem = HomeFormValidator.selectedProject(chosenProjectId)
                        ?: HomeFormValidator.duration(hoursText, minutesText)
                        ?: HomeFormValidator.note(noteText)
                    if (problem != null) { errorText = problem } else {
                        val totalMinutes = (hoursText.toLongOrNull() ?: 0L) * 60L + (minutesText.toLongOrNull() ?: 0L)
                        onLog(chosenProjectId, totalMinutes, noteText)
                        onDismiss()
                    }
                },
                modifier = Modifier.width(120.dp),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = DsTextMuted)
            }
        },
    )
}

@Composable
fun LimitInfoDialog(
    projectCount: Int,
    weeklyMinutes: Long,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DsSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Plan Sin Límites",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DsTextPrimary,
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Tu cuenta cuenta con acceso completo e ilimitado a todas las características:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DsTextMuted,
                )
                Spacer(Modifier.height(12.dp))
                FeatureCheckRow("Creación de proyectos sin restricciones")
                FeatureCheckRow("Registro de horas manual y temporizador en vivo")
                FeatureCheckRow("Persistencia local automática de datos")
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(DsCyan.copy(alpha = 0.1f))
                        .padding(12.dp),
                ) {
                    Column {
                        Text(
                            text = "ESTADÍSTICAS ACTUALES",
                            style = MaterialTheme.typography.labelSmall,
                            color = DsCyan,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "• Proyectos creados: $projectCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = DsTextPrimary,
                        )
                        Text(
                            text = "• Horas esta semana: ${formatHoursMinutes(weeklyMinutes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DsTextPrimary,
                        )
                    }
                }
            }
        },
        confirmButton = {
            DsPrimaryButton(
                text = "Entendido",
                onClick = onDismiss,
                modifier = Modifier.width(120.dp),
            )
        },
    )
}

@Composable
private fun FeatureCheckRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 3.dp),
    ) {
        Text(text = "✓  ", color = DsCyan, fontWeight = FontWeight.Bold)
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = DsTextPrimary)
    }
}

private fun formatHoursMinutes(minutes: Long): String {
    val hrs = minutes / 60
    val mins = minutes % 60
    return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
}
