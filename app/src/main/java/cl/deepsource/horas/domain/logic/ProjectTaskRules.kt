package cl.deepsource.horas.domain.logic

import cl.deepsource.horas.domain.model.Project
import cl.deepsource.horas.domain.model.ProjectTask

/** Reglas para crear y seleccionar tareas con estimaciones propias. */
object ProjectTaskRules {
    fun validate(name: String, estimateMinutes: Int): String? = when {
        name.isBlank() -> "Ingresa el nombre de la tarea"
        name.trim().length > 80 -> "El nombre no puede superar 80 caracteres"
        estimateMinutes !in 1..1440 -> "La duración debe estar entre 1 y 1440 minutos"
        else -> null
    }

    fun alreadyExists(projectId: String, name: String, tasks: List<ProjectTask>): Boolean =
        tasks.any { it.projectId == projectId && it.name.equals(name.trim(), ignoreCase = true) }

    fun tasksFor(projectId: String, tasks: List<ProjectTask>): List<ProjectTask> =
        tasks.filter { it.projectId == projectId }.sortedBy { it.createdAtEpochMs }

    fun templateNames(projectName: String): List<String> = when {
        projectName.contains("portal", true) -> listOf("Diseñar pantallas del portal", "Implementar login OAuth", "Validar formularios", "Pruebas de integración")
        projectName.contains("qa", true) || projectName.contains("prueba", true) -> listOf("Preparar plan de pruebas", "Crear casos automatizados", "Ejecutar pruebas", "Documentar hallazgos")
        projectName.contains("mesa", true) || projectName.contains("operacion", true) -> listOf("Revisar incidencias", "Priorizar solicitudes", "Resolver tickets", "Actualizar documentación")
        else -> listOf("Planificar actividades", "Desarrollar funcionalidad", "Realizar pruebas", "Documentar avance")
    }

    fun select(projects: List<Project>, id: String?): Project? =
        projects.firstOrNull { it.id == id }
}
