package cl.deepsource.horas.data.repository
// [GUIA/ARCHIVO] Persistencia local y StateFlow; no existe BD remota ni Room en este código.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/data/repository/LocalProjectRepository.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import android.content.Context
import android.content.SharedPreferences
import cl.deepsource.horas.domain.model.Project
import cl.deepsource.horas.domain.model.ProjectTask
import cl.deepsource.horas.core.nativefeatures.TaskAlarmScheduler
import cl.deepsource.horas.domain.model.TaskAssignment
import cl.deepsource.horas.domain.model.WorkflowDefaults
import cl.deepsource.horas.domain.model.elapsedSeconds
import cl.deepsource.horas.domain.model.TimeLog
import cl.deepsource.horas.domain.model.WorkTask
import cl.deepsource.horas.domain.repository.ProjectRepository
import cl.deepsource.horas.widget.DeepSourceAppWidget
import cl.deepsource.horas.core.validation.HomeFormValidator
import cl.deepsource.horas.core.nativefeatures.TaskPhotoStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

// [GUIA/TIPO] LocalProjectRepository: Repositorio local: guarda proyectos, tareas y horas en SharedPreferences.
class LocalProjectRepository(private val context: Context) : ProjectRepository {

    private val prefs: SharedPreferences =
        // [GUIA/DETALLE] Archivo de preferencias privado de la app; NO es una base de datos SQL/Room.
        context.getSharedPreferences("deepsource_projects_prefs", Context.MODE_PRIVATE)

    // [GUIA/DETALLE] Lista observable; cuando cambia, HomeViewModel puede recalcular el Home.
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    override val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    // [GUIA/DETALLE] Proyecto seleccionado observable, o null si no hay ninguno.
    private val _currentProject = MutableStateFlow<Project?>(null)
    override val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    // [GUIA/DETALLE] Tarea actual y datos de pausa en tiempo real.
    private val _activeTask = MutableStateFlow<WorkTask?>(null)
    override val activeTask: StateFlow<WorkTask?> = _activeTask.asStateFlow()

    // [GUIA/DETALLE] Historial observable de minutos registrados.
    private val _timeLogs = MutableStateFlow<List<TimeLog>>(emptyList())
    override val timeLogs: StateFlow<List<TimeLog>> = _timeLogs.asStateFlow()

    // [FLUJO/STATEFLOW] La vista de jefe se actualiza sin consultar directamente SharedPreferences.
    private val _assignments = MutableStateFlow<List<TaskAssignment>>(emptyList())
    override val assignments: StateFlow<List<TaskAssignment>> = _assignments.asStateFlow()

    private val _projectTasks = MutableStateFlow<List<ProjectTask>>(emptyList())
    override val projectTasks: StateFlow<List<ProjectTask>> = _projectTasks.asStateFlow()

    init {
        // [GUIA/DETALLE] Al crear repositorio, recupera datos de una ejecución anterior.
        loadData()
    }

    // [GUIA/FUNCION] loadData: Reconstruye flujos de proyectos/tareas/registros a partir de lo guardado en el teléfono.
    // [GUIA/DETALLE] Al crear repositorio, recupera datos de una ejecución anterior.
    private fun loadData() {
        // [GUIA/DETALLE] Lee la colección persistida de proyectos como cadenas.
        // [FLUJO/SEMILLAS] Solo una instalación nueva recibe los tres proyectos del HTML.
        // Si la clave ya existe, no sobrescribimos los proyectos creados por la persona.
        if (!prefs.contains(KEY_PROJECTS)) {
            val samples = listOf("Portal Cliente AFC", "Automatización QA", "Mesa Operaciones")
                .map { Project(UUID.randomUUID().toString(), it) }
            prefs.edit()
                .putStringSet(KEY_PROJECTS, samples.map(::serializeProject).toSet())
                .putString(KEY_CURRENT_PROJECT_ID, samples.first().id)
                .apply()
        }
        val projectsRaw = prefs.getStringSet(KEY_PROJECTS, emptySet()) ?: emptySet()
        val loadedProjects = projectsRaw.mapNotNull { parseProject(it) }
            .sortedByDescending { it.createdAtEpochMs }
        _projects.value = loadedProjects

        val currentProjId = prefs.getString(KEY_CURRENT_PROJECT_ID, null)
        _currentProject.value = loadedProjects.firstOrNull { it.id == currentProjId }
            ?: loadedProjects.firstOrNull()

        // [GUIA/DETALLE] Recupera cronómetro persistido, incluso si la aplicación se cerró.
        val activeTaskRaw = prefs.getString(KEY_ACTIVE_TASK, null)
        _activeTask.value = activeTaskRaw?.let { parseWorkTask(it) }

        // [GUIA/DETALLE] Recupera los registros históricos de tiempo.
        val logsRaw = prefs.getStringSet(KEY_TIME_LOGS, emptySet()) ?: emptySet()
        _timeLogs.value = logsRaw.mapNotNull { parseTimeLog(it) }
            .sortedByDescending { it.timestampEpochMs }

        // [FLUJO/PERSISTENCIA] Recupera asignaciones: sobreviven al cierre de la aplicación.
        _assignments.value = (prefs.getStringSet(KEY_ASSIGNMENTS, emptySet()) ?: emptySet())
            .mapNotNull(::parseAssignment).sortedByDescending { it.assignedAtEpochMs }
        // [V5/MIGRACIÓN] Sembrar tareas en proyectos existentes una única vez, sin borrar sus datos.
        if (!prefs.contains(KEY_PROJECT_TASKS)) {
            _projectTasks.value = loadedProjects.flatMap(::defaultTasks)
            prefs.edit().putStringSet(KEY_PROJECT_TASKS, _projectTasks.value.map(::serializeProjectTask).toSet()).apply()
        } else {
            _projectTasks.value = (prefs.getStringSet(KEY_PROJECT_TASKS, emptySet()) ?: emptySet())
                .mapNotNull(::parseProjectTask).filter { task -> loadedProjects.any { it.id == task.projectId } }
                .sortedBy { it.createdAtEpochMs }
        }
        TaskAlarmScheduler.sync(context, _activeTask.value)
    }

    // [GUIA/FUNCION] saveData: Serializa el estado a SharedPreferences y comunica al widget que debe actualizarse.
    private fun saveData() {
        val projectsSet = _projects.value.map { serializeProject(it) }.toSet()
        val currentProjId = _currentProject.value?.id
        val activeTaskSerialized = _activeTask.value?.let { serializeWorkTask(it) }
        val logsSet = _timeLogs.value.map { serializeTimeLog(it) }.toSet()

        // [GUIA/DETALLE] Escribe simultáneamente claves de proyectos, selección, tarea activa e historial.
        prefs.edit()
            .putStringSet(KEY_PROJECTS, projectsSet)
            .putString(KEY_CURRENT_PROJECT_ID, currentProjId)
            .putString(KEY_ACTIVE_TASK, activeTaskSerialized)
            .putStringSet(KEY_TIME_LOGS, logsSet)
            .putStringSet(KEY_ASSIGNMENTS, _assignments.value.map(::serializeAssignment).toSet())
            .putStringSet(KEY_PROJECT_TASKS, _projectTasks.value.map(::serializeProjectTask).toSet())
            .apply()

        // [GUIA/DETALLE] El widget se actualiza tras cualquier cambio persistido.
        DeepSourceAppWidget.sendUpdateBroadcast(context)
        // [V5/ALARMAS] Programa o cancela el aviso también desde el widget y al pausar.
        TaskAlarmScheduler.sync(context, _activeTask.value)
    }

    // [GUIA/FUNCION] createProject: Valida nombre, crea ID UUID y proyecto nuevo, lo selecciona y lo persiste.
    override suspend fun createProject(name: String, description: String): Result<Project> {
        val cleanName = name.trim()
        val problem = HomeFormValidator.projectName(cleanName) ?: HomeFormValidator.description(description)
        if (problem != null) return Result.failure(IllegalArgumentException(problem))

        val newProj = Project(
            // [GUIA/DETALLE] ID único para diferenciar proyectos/tareas/registros.
            id = UUID.randomUUID().toString(),
            name = cleanName,
            description = description.trim(),
            createdAtEpochMs = System.currentTimeMillis(),
        )

        val updated = _projects.value + newProj
        // [GUIA/DETALLE] Publica nueva lista reactiva de proyectos.
        _projects.value = updated
        // [GUIA/DETALLE] Al crear, el proyecto nuevo pasa a ser el seleccionado.
        _currentProject.value = newProj
        _projectTasks.value = _projectTasks.value + defaultTasks(newProj)

        saveData()
        return Result.success(newProj)
    }

    // [GUIA/FUNCION] setCurrentProject: Marca el proyecto elegido y guarda esa elección.
    override suspend fun setCurrentProject(projectId: String?) {
        val selected = _projects.value.firstOrNull { it.id == projectId }
        _currentProject.value = selected
        saveData()
    }

    // [GUIA/FUNCION] startTask: Evita dos tareas simultáneas, crea tarea con hora de inicio y la persiste.
    override suspend fun startTask(
        projectId: String, taskName: String, difficulty: String,
        estimateMinutes: Int, demoReminder: Boolean,
    ): Result<WorkTask> {
        val taskProblem = HomeFormValidator.taskName(taskName)
        if (difficulty !in WorkflowDefaults.estimates || estimateMinutes !in 1..1440) {
            return Result.failure(IllegalArgumentException("Dificultad o estimación no válida"))
        }
        if (taskProblem != null) return Result.failure(IllegalArgumentException(taskProblem))
        // [GUIA/DETALLE] Regla: solo puede ejecutarse una tarea a la vez.
        if (_activeTask.value != null) return Result.failure(IllegalStateException("Finaliza la tarea actual antes de iniciar otra"))
        val project = _projects.value.firstOrNull { it.id == projectId }
            ?: return Result.failure(IllegalStateException("El proyecto seleccionado no existe"))

        val startedAt = System.currentTimeMillis()
        val newTask = WorkTask(
            // [GUIA/DETALLE] ID único para diferenciar proyectos/tareas/registros.
            id = UUID.randomUUID().toString(),
            projectId = project.id,
            projectName = project.name,
            taskName = taskName.trim().ifBlank { "Tarea en curso" },
            startEpochMs = startedAt,
            difficulty = difficulty,
            estimateMinutes = estimateMinutes,
            originalStartEpochMs = startedAt,
            demoReminder = demoReminder,
            nextReminderAtSeconds = if (demoReminder) 15L else estimateMinutes * 60L,
        )

        _activeTask.value = newTask
        _currentProject.value = project
        saveData()
        return Result.success(newTask)
    }

    // [GUIA/FUNCION] toggleTaskPause: Pausa acumulando segundos o reanuda desde el instante actual; no pierde el tiempo anterior.
    override suspend fun toggleTaskPause(): Result<WorkTask> {
        val task = _activeTask.value ?: return Result.failure(IllegalStateException("No hay una tarea para aplazar"))
        val now = System.currentTimeMillis()
        // [GUIA/DETALLE] Si estaba en pausa, vuelve a correr; si corría, acumula segundos y pausa.
        val updated = if (task.paused) {
            task.copy(paused = false, startEpochMs = now)
        } else {
            task.copy(paused = true, accumulatedSeconds = task.accumulatedSeconds + ((now - task.startEpochMs) / 1000L).coerceAtLeast(0L))
        }
        _activeTask.value = updated
        saveData()
        return Result.success(updated)
    }

    // [FLUJO/BOTÓN] +30 min: actualiza la estimación límite, no suma trabajo no realizado.
    override suspend fun extendActiveTask(minutes: Int): Result<WorkTask> {
        if (minutes !in 1..240) return Result.failure(IllegalArgumentException("Extensión fuera de rango"))
        val task = _activeTask.value ?: return Result.failure(IllegalStateException("No hay tarea activa"))
        val updated = task.copy(
            extensionMinutes = task.extensionMinutes + minutes,
            // [FLUJO/RECORDATORIO] En modo demo, el siguiente aviso llega 15 s después del aplazamiento.
            nextReminderAtSeconds = if (task.demoReminder) task.elapsedSeconds() + 15L
                                    else (task.estimateMinutes + task.extensionMinutes + minutes) * 60L,
        )
        _activeTask.value = updated
        saveData()
        return Result.success(updated)
    }

    // [GUIA/FUNCION] stopActiveTask: Calcula tiempo transcurrido, crea TimeLog, quita tarea activa y guarda.
    // [FLUJO/BOTÓN] Guardar registro llama a este método con la nota del usuario.
    override suspend fun stopActiveTask(note: String): Result<TimeLog?> {
        val task = _activeTask.value ?: return Result.success(null)
        val elapsedSeconds = task.elapsedSeconds()
        // [FLUJO/PRECISIÓN] Historial nuevo guarda segundos reales; minutos es la compatibilidad con Home.
        val durationMinutes = (elapsedSeconds / 60L).coerceAtLeast(1L)

        val log = TimeLog(
            // [GUIA/DETALLE] ID único para diferenciar proyectos/tareas/registros.
            id = UUID.randomUUID().toString(),
            projectId = task.projectId,
            durationMinutes = durationMinutes,
            note = note.trim().ifBlank { task.taskName },
            timestampEpochMs = System.currentTimeMillis(),
            durationSeconds = elapsedSeconds,
            taskName = task.taskName,
            difficulty = task.difficulty,
            estimateMinutes = task.estimateMinutes,
            source = "timer",
        )

        _timeLogs.value = _timeLogs.value + log
        _activeTask.value = null
        saveData()
        return Result.success(log)
    }

    // [GUIA/FUNCION] logHours: Registra manualmente una duración en minutos con nota y fecha.
    override suspend fun logHours(
        projectId: String, durationMinutes: Long, note: String,
        taskName: String, difficulty: String, timestampEpochMs: Long,
    ): Result<TimeLog> {
        if (durationMinutes !in 1..1439) {
            return Result.failure(IllegalArgumentException("La duración debe estar entre 1 y 1439 minutos"))
        }
        HomeFormValidator.note(note)?.let { problem ->
            return Result.failure(IllegalArgumentException(problem))
        }
        val project = _projects.value.firstOrNull { it.id == projectId }
            ?: return Result.failure(IllegalStateException("El proyecto no existe"))

        val log = TimeLog(
            // [GUIA/DETALLE] ID único para diferenciar proyectos/tareas/registros.
            id = UUID.randomUUID().toString(),
            projectId = project.id,
            durationMinutes = durationMinutes,
            note = note.trim(),
            timestampEpochMs = timestampEpochMs,
            durationSeconds = durationMinutes * 60,
            taskName = taskName.trim().ifBlank { "Registro manual" },
            difficulty = difficulty,
            estimateMinutes = WorkflowDefaults.estimates[difficulty] ?: 90,
            source = "manual",
        )

        _timeLogs.value = _timeLogs.value + log
        saveData()
        return Result.success(log)
    }

    // [GUIA/FUNCION] deleteProject: Elimina proyecto, su foto, sus horas y cualquier tarea activa asociada; actualiza selección.
    override suspend fun deleteProject(projectId: String) {
        // [GUIA/DETALLE] Elimina la fotografía asociada al borrar el proyecto.
        TaskPhotoStore(context).delete(projectId)
        _projects.value = _projects.value.filterNot { it.id == projectId }
        if (_currentProject.value?.id == projectId) {
            _currentProject.value = _projects.value.firstOrNull()
        }
        if (_activeTask.value?.projectId == projectId) {
            _activeTask.value = null
        }
        _timeLogs.value = _timeLogs.value.filterNot { it.projectId == projectId }
        _assignments.value = _assignments.value.filterNot { it.projectId == projectId }
        _projectTasks.value = _projectTasks.value.filterNot { it.projectId == projectId }
        saveData()
    }

    // [FLUJO/BOTÓN] Asignar tarea: 8 h por persona menos carga de ejemplo,
    // registros propios y asignaciones existentes. No hay servidor ni empleados reales.
    override suspend fun assignTask(
        employeeId: String, projectId: String, taskName: String,
        difficulty: String, estimateMinutes: Int,
    ): Result<TaskAssignment> {
        val member = WorkflowDefaults.members.firstOrNull { it.id == employeeId }
            ?: return Result.failure(IllegalArgumentException("Empleado no válido"))
        val project = _projects.value.firstOrNull { it.id == projectId }
            ?: return Result.failure(IllegalArgumentException("Proyecto no válido"))
        if (taskName.isBlank() || difficulty !in WorkflowDefaults.estimates || estimateMinutes <= 0) {
            return Result.failure(IllegalArgumentException("Completa los datos de la tarea"))
        }
        val alreadyAssigned = _assignments.value.filter { it.employeeId == employeeId }.sumOf { it.estimateMinutes }
        val logged = if (employeeId == "luis") _timeLogs.value.sumOf { it.durationMinutes.toInt() } else 0
        if (member.initialLoadMinutes + alreadyAssigned + logged + estimateMinutes > 480) {
            return Result.failure(IllegalStateException("No hay horas disponibles para esta asignación"))
        }
        val item = TaskAssignment(UUID.randomUUID().toString(), member.id, member.name,
            project.id, project.name, taskName.trim(), difficulty, estimateMinutes, System.currentTimeMillis())
        _assignments.value = listOf(item) + _assignments.value
        saveData()
        return Result.success(item)
    }

    /** [V5/TAREAS] Tareas iniciales diferenciadas: editar y añadir desde cada proyecto. */
    private fun defaultTasks(project: Project): List<ProjectTask> {
        val names = when {
            project.name.contains("portal", true) -> listOf("Diseñar pantallas del portal", "Implementar login OAuth", "Validar formularios", "Pruebas de integración")
            project.name.contains("qa", true) || project.name.contains("prueba", true) -> listOf("Preparar plan de pruebas", "Crear casos automatizados", "Ejecutar pruebas", "Documentar hallazgos")
            project.name.contains("mesa", true) || project.name.contains("operacion", true) -> listOf("Revisar incidencias", "Priorizar solicitudes", "Resolver tickets", "Actualizar documentación")
            else -> listOf("Planificar actividades", "Desarrollar funcionalidad", "Realizar pruebas", "Documentar avance")
        }
        return names.mapIndexed { index, name -> ProjectTask(UUID.randomUUID().toString(), project.id, name,
            project.createdAtEpochMs + index) }
    }

    override suspend fun addProjectTask(projectId: String, name: String): Result<ProjectTask> {
        if (_projects.value.none { it.id == projectId }) return Result.failure(IllegalArgumentException("Proyecto no encontrado"))
        val clean = name.trim()
        val problem = HomeFormValidator.taskName(clean)
        if (problem != null) return Result.failure(IllegalArgumentException(problem))
        if (_projectTasks.value.any { it.projectId == projectId && it.name.equals(clean, true) })
            return Result.failure(IllegalArgumentException("La tarea ya existe en este proyecto"))
        val task = ProjectTask(UUID.randomUUID().toString(), projectId, clean)
        _projectTasks.value = _projectTasks.value + task
        saveData()
        return Result.success(task)
    }

    override suspend fun deleteProjectTask(taskId: String): Result<Unit> {
        if (_projectTasks.value.none { it.id == taskId }) return Result.failure(IllegalArgumentException("Tarea no encontrada"))
        _projectTasks.value = _projectTasks.value.filterNot { it.id == taskId }
        saveData()
        return Result.success(Unit)
    }

    private fun serializeProjectTask(t: ProjectTask) =
        listOf(t.id, t.projectId, t.name.replace("|", "_"), t.createdAtEpochMs.toString()).joinToString("|")

    private fun parseProjectTask(raw: String): ProjectTask? {
        val p = raw.split("|")
        if (p.size != 4) return null
        return ProjectTask(p[0], p[1], p[2], p[3].toLongOrNull() ?: 0L)
    }

    // [GUIA/FUNCION] serializeProject: Convierte Project a texto con separador | para guardarlo en SharedPreferences.
    private fun serializeProject(p: Project): String =
        "${p.id}|${p.name.replace("|", "_")}|${p.description.replace("|", "_")}|${p.createdAtEpochMs}"

    // [GUIA/FUNCION] parseProject: Reconstruye Project desde el formato de texto guardado.
    private fun parseProject(s: String): Project? {
        val parts = s.split("|")
        if (parts.size < 4) return null
        return Project(
            id = parts[0],
            name = parts[1],
            description = parts[2],
            createdAtEpochMs = parts[3].toLongOrNull() ?: System.currentTimeMillis(),
        )
    }

    // [GUIA/FUNCION] serializeWorkTask: Convierte WorkTask a una cadena con su pausa y segundos acumulados.
    private fun serializeWorkTask(t: WorkTask): String =
        "${t.id}|${t.projectId}|${t.projectName.replace("|", "_")}|${t.taskName.replace("|", "_")}|${t.startEpochMs}|${t.paused}|${t.accumulatedSeconds}|${t.difficulty}|${t.estimateMinutes}|${t.extensionMinutes}|${t.originalStartEpochMs}|${t.demoReminder}|${t.nextReminderAtSeconds}"

    // [GUIA/FUNCION] parseWorkTask: Recrea WorkTask desde el dato guardado, aplicando valores por defecto si faltan campos.
    private fun parseWorkTask(s: String): WorkTask? {
        val parts = s.split("|")
        if (parts.size < 5) return null
        return WorkTask(
            id = parts[0],
            projectId = parts[1],
            projectName = parts[2],
            taskName = parts[3],
            startEpochMs = parts[4].toLongOrNull() ?: System.currentTimeMillis(),
            paused = parts.getOrNull(5)?.toBooleanStrictOrNull() ?: false,
            accumulatedSeconds = parts.getOrNull(6)?.toLongOrNull() ?: 0L,
            difficulty = parts.getOrNull(7) ?: "Normal",
            estimateMinutes = parts.getOrNull(8)?.toIntOrNull() ?: 90,
            extensionMinutes = parts.getOrNull(9)?.toIntOrNull() ?: 0,
            originalStartEpochMs = parts.getOrNull(10)?.toLongOrNull() ?: (parts[4].toLongOrNull() ?: System.currentTimeMillis()),
            demoReminder = parts.getOrNull(11)?.toBooleanStrictOrNull() ?: false,
            nextReminderAtSeconds = parts.getOrNull(12)?.toLongOrNull() ?: 0L,
        )
    }

    // [GUIA/FUNCION] serializeTimeLog: Transforma el registro de horas a texto persistible.
    private fun serializeTimeLog(l: TimeLog): String =
        "${l.id}|${l.projectId}|${l.durationMinutes}|${l.note.replace("|", "_")}|${l.timestampEpochMs}|${l.durationSeconds}|${l.taskName.replace("|", "_")}|${l.difficulty}|${l.estimateMinutes}|${l.source}|${l.employeeName.replace("|", "_")}"

    // [GUIA/FUNCION] parseTimeLog: Reconstruye registro de horas guardado.
    private fun parseTimeLog(s: String): TimeLog? {
        val parts = s.split("|")
        if (parts.size < 5) return null
        return TimeLog(
            id = parts[0],
            projectId = parts[1],
            durationMinutes = parts[2].toLongOrNull() ?: 0L,
            note = parts[3],
            timestampEpochMs = parts[4].toLongOrNull() ?: System.currentTimeMillis(),
            durationSeconds = parts.getOrNull(5)?.toLongOrNull() ?: ((parts[2].toLongOrNull() ?: 0L) * 60L),
            taskName = parts.getOrNull(6) ?: parts[3],
            difficulty = parts.getOrNull(7) ?: "Normal",
            estimateMinutes = parts.getOrNull(8)?.toIntOrNull() ?: 90,
            source = parts.getOrNull(9) ?: "manual",
            employeeName = parts.getOrNull(10) ?: "Luis Gómez",
        )
    }

    // [FLUJO/PERSISTENCIA] Las asignaciones se serializan aparte para no tocar proyectos antiguos.
    private fun serializeAssignment(a: TaskAssignment): String = listOf(
        a.id, a.employeeId, a.employeeName, a.projectId, a.projectName,
        a.taskName, a.difficulty, a.estimateMinutes.toString(), a.assignedAtEpochMs.toString(),
    ).joinToString("|") { it.replace("|", "_") }

    private fun parseAssignment(raw: String): TaskAssignment? {
        val p = raw.split("|")
        if (p.size < 9) return null
        return TaskAssignment(p[0], p[1], p[2], p[3], p[4], p[5], p[6],
            p[7].toIntOrNull() ?: return null, p[8].toLongOrNull() ?: return null)
    }

    // [GUIA/DETALLE] Claves que utiliza el almacenamiento y consulta también el widget.
    private companion object {
        const val KEY_PROJECTS = "key_projects"
        const val KEY_CURRENT_PROJECT_ID = "key_current_project_id"
        const val KEY_ACTIVE_TASK = "key_active_task"
        const val KEY_TIME_LOGS = "key_time_logs"
        const val KEY_ASSIGNMENTS = "key_assignments"
        const val KEY_PROJECT_TASKS = "key_project_tasks_v5"
    }
}
