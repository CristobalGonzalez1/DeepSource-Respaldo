package cl.deepsource.horas.data.repository

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
import cl.deepsource.horas.data.remote.RemoteApi
import cl.deepsource.horas.data.remote.RemoteProject
import cl.deepsource.horas.data.remote.RemoteTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// LocalProjectRepository: Repositorio local: guarda proyectos, tareas y horas en SharedPreferences.
class LocalProjectRepository(private val context: Context) : ProjectRepository {

    private val prefs: SharedPreferences =
        // Archivo de preferencias privado de la app; NO es una base de datos SQL/Room.
        context.getSharedPreferences("deepsource_projects_prefs", Context.MODE_PRIVATE)

    // Lista observable; cuando cambia, HomeViewModel puede recalcular el Home.
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    override val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    // Proyecto seleccionado observable, o null si no hay ninguno.
    private val _currentProject = MutableStateFlow<Project?>(null)
    override val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    // Tarea actual y datos de pausa en tiempo real.
    private val _activeTask = MutableStateFlow<WorkTask?>(null)
    override val activeTask: StateFlow<WorkTask?> = _activeTask.asStateFlow()

    // Historial observable de minutos registrados.
    private val _timeLogs = MutableStateFlow<List<TimeLog>>(emptyList())
    override val timeLogs: StateFlow<List<TimeLog>> = _timeLogs.asStateFlow()

    // La vista de jefe se actualiza sin consultar directamente SharedPreferences.
    private val _assignments = MutableStateFlow<List<TaskAssignment>>(emptyList())
    override val assignments: StateFlow<List<TaskAssignment>> = _assignments.asStateFlow()

    private val _projectTasks = MutableStateFlow<List<ProjectTask>>(emptyList())
    override val projectTasks: StateFlow<List<ProjectTask>> = _projectTasks.asStateFlow()

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncLock = Mutex()

    init {
        loadData()
        // Al iniciar, recuperamos proyectos y tareas del servidor sin impedir el uso sin conexión.
        syncScope.launch { syncWithApi() }
    }

    private fun syncLater() {
        syncScope.launch { syncWithApi() }
    }

    /** Retrofit consulta los proyectos y tareas de nuestro servidor Spring Boot.
     *  Si todavía no hay datos remotos, migramos los que ya tenía la persona en el teléfono.
     *  En caso de no tener conexión se conserva la copia local y se reintenta al editar.
     */
    suspend fun syncWithApi(): Boolean = withContext(Dispatchers.IO) {
        syncLock.withLock {
            try {
                // Aplicamos los borrados pendientes antes de combinar los datos con el servidor.
                val deletedTasks = prefs.getStringSet("pending_deleted_tasks", emptySet()).orEmpty().toSet()
                for (id in deletedTasks) {
                    val remote = RemoteApi.projects.tasks().firstOrNull { (it.clientId ?: "tarea-servidor-${it.id}") == id }
                    if (remote?.id != null) RemoteApi.projects.deleteTask(remote.id)
                    prefs.edit().putStringSet("pending_deleted_tasks",
                        prefs.getStringSet("pending_deleted_tasks", emptySet()).orEmpty().minus(id)).apply()
                }
                val deletedProjects = prefs.getStringSet("pending_deleted_projects", emptySet()).orEmpty().toSet()
                for (id in deletedProjects) {
                    val remote = RemoteApi.projects.projects().firstOrNull { (it.clientId ?: "servidor-${it.id}") == id }
                    if (remote?.id != null) RemoteApi.projects.deleteProject(remote.id)
                    prefs.edit().putStringSet("pending_deleted_projects",
                        prefs.getStringSet("pending_deleted_projects", emptySet()).orEmpty().minus(id)).apply()
                }
                var serverProjects = RemoteApi.projects.projects()
                val known = serverProjects.map { it.clientId ?: "servidor-${it.id}" }.toSet()
                for (project in _projects.value.filter { it.id !in known }) {
                    RemoteApi.projects.addProject(RemoteProject(name = project.name,
                        description = project.description, clientId = project.id))
                }
                serverProjects = RemoteApi.projects.projects()
                var serverTasks = RemoteApi.projects.tasks()
                val taskIds = serverTasks.map { it.clientId ?: "tarea-servidor-${it.id}" }.toSet()
                for (task in _projectTasks.value.filter { it.id !in taskIds }) {
                    val remoteProject = serverProjects.firstOrNull { (it.clientId ?: "servidor-${it.id}") == task.projectId } ?: continue
                    RemoteApi.projects.addTask(RemoteTask(projectId = remoteProject.id ?: continue,
                        name = task.name, clientId = task.id, estimateMinutes = task.estimateMinutes))
                }
                serverTasks = RemoteApi.projects.tasks()
                val mergedProjects = serverProjects.mapNotNull { remote ->
                    val id = remote.clientId ?: "servidor-${remote.id}"
                    val previous = _projects.value.firstOrNull { it.id == id }
                    Project(id, remote.name, remote.description, previous?.createdAtEpochMs ?: System.currentTimeMillis())
                }
                if (mergedProjects.isNotEmpty()) {
                    _projects.value = mergedProjects
                    val chosenId = _currentProject.value?.id
                    _currentProject.value = mergedProjects.firstOrNull { it.id == chosenId } ?: mergedProjects.first()
                }
                val mergedTasks = serverTasks.mapNotNull { remote ->
                    val projectId = serverProjects.firstOrNull { it.id == remote.projectId }?.let { it.clientId ?: "servidor-${it.id}" } ?: return@mapNotNull null
                    ProjectTask(remote.clientId ?: "tarea-servidor-${remote.id}", projectId, remote.name,
                        estimateMinutes = (remote.estimateMinutes).coerceIn(1, 1440))
                }
                _projectTasks.value = mergedTasks
                saveData()
                true
            } catch (_: Exception) {
                // La aplicación sigue operativa con la información guardada en el dispositivo.
                false
            }
        }
    }


    // loadData: Reconstruye flujos de proyectos/tareas/registros a partir de lo guardado en el teléfono.
    // Al crear repositorio, recupera datos de una ejecución anterior.
    private fun loadData() {
        // Lee la colección persistida de proyectos como cadenas.
        // Solo una instalación nueva recibe los tres proyectos del HTML.
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

        // Recupera cronómetro persistido, incluso si la aplicación se cerró.
        val activeTaskRaw = prefs.getString(KEY_ACTIVE_TASK, null)
        _activeTask.value = activeTaskRaw?.let { parseWorkTask(it) }

        // Recupera los registros históricos de tiempo.
        val logsRaw = prefs.getStringSet(KEY_TIME_LOGS, emptySet()) ?: emptySet()
        _timeLogs.value = logsRaw.mapNotNull { parseTimeLog(it) }
            .sortedByDescending { it.timestampEpochMs }

        // Recupera asignaciones: sobreviven al cierre de la aplicación.
        _assignments.value = (prefs.getStringSet(KEY_ASSIGNMENTS, emptySet()) ?: emptySet())
            .mapNotNull(::parseAssignment).sortedByDescending { it.assignedAtEpochMs }
        // Sembrar tareas en proyectos existentes una única vez, sin borrar sus datos.
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

    // saveData: Serializa el estado a SharedPreferences y comunica al widget que debe actualizarse.
    private fun saveData() {
        val projectsSet = _projects.value.map { serializeProject(it) }.toSet()
        val currentProjId = _currentProject.value?.id
        val activeTaskSerialized = _activeTask.value?.let { serializeWorkTask(it) }
        val logsSet = _timeLogs.value.map { serializeTimeLog(it) }.toSet()

        // Escribe simultáneamente claves de proyectos, selección, tarea activa e historial.
        prefs.edit()
            .putStringSet(KEY_PROJECTS, projectsSet)
            .putString(KEY_CURRENT_PROJECT_ID, currentProjId)
            .putString(KEY_ACTIVE_TASK, activeTaskSerialized)
            .putStringSet(KEY_TIME_LOGS, logsSet)
            .putStringSet(KEY_ASSIGNMENTS, _assignments.value.map(::serializeAssignment).toSet())
            .putStringSet(KEY_PROJECT_TASKS, _projectTasks.value.map(::serializeProjectTask).toSet())
            .apply()

        // El widget se actualiza tras cualquier cambio persistido.
        DeepSourceAppWidget.sendUpdateBroadcast(context)
        // Programa o cancela el aviso también desde el widget y al pausar.
        TaskAlarmScheduler.sync(context, _activeTask.value)
    }

    // createProject: Valida nombre, crea ID UUID y proyecto nuevo, lo selecciona y lo persiste.
    override suspend fun createProject(name: String, description: String): Result<Project> {
        val cleanName = name.trim()
        val problem = HomeFormValidator.projectName(cleanName) ?: HomeFormValidator.description(description)
        if (problem != null) return Result.failure(IllegalArgumentException(problem))

        val newProj = Project(
            // ID único para diferenciar proyectos/tareas/registros.
            id = UUID.randomUUID().toString(),
            name = cleanName,
            description = description.trim(),
            createdAtEpochMs = System.currentTimeMillis(),
        )

        val updated = _projects.value + newProj
        // Publica nueva lista reactiva de proyectos.
        _projects.value = updated
        // Al crear, el proyecto nuevo pasa a ser el seleccionado.
        _currentProject.value = newProj
        _projectTasks.value = _projectTasks.value + defaultTasks(newProj)

        saveData()
        syncLater()
        return Result.success(newProj)
    }

    // setCurrentProject: Marca el proyecto elegido y guarda esa elección.
    override suspend fun setCurrentProject(projectId: String?) {
        val selected = _projects.value.firstOrNull { it.id == projectId }
        _currentProject.value = selected
        saveData()
    }

    // startTask: Evita dos tareas simultáneas, crea tarea con hora de inicio y la persiste.
    override suspend fun startTask(
        projectId: String, taskName: String, difficulty: String,
        estimateMinutes: Int, demoReminder: Boolean,
    ): Result<WorkTask> {
        val taskProblem = HomeFormValidator.taskName(taskName)
        if (estimateMinutes !in 1..1440) {
            return Result.failure(IllegalArgumentException("Duración estimada no válida"))
        }
        if (taskProblem != null) return Result.failure(IllegalArgumentException(taskProblem))
        // Regla: solo puede ejecutarse una tarea a la vez.
        if (_activeTask.value != null) return Result.failure(IllegalStateException("Finaliza la tarea actual antes de iniciar otra"))
        val project = _projects.value.firstOrNull { it.id == projectId }
            ?: return Result.failure(IllegalStateException("El proyecto seleccionado no existe"))

        val startedAt = System.currentTimeMillis()
        val newTask = WorkTask(
            // ID único para diferenciar proyectos/tareas/registros.
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

    // toggleTaskPause: Pausa acumulando segundos o reanuda desde el instante actual; no pierde el tiempo anterior.
    override suspend fun toggleTaskPause(): Result<WorkTask> {
        val task = _activeTask.value ?: return Result.failure(IllegalStateException("No hay una tarea para aplazar"))
        val now = System.currentTimeMillis()
        // Si estaba en pausa, vuelve a correr; si corría, acumula segundos y pausa.
        val updated = if (task.paused) {
            task.copy(paused = false, startEpochMs = now)
        } else {
            task.copy(paused = true, accumulatedSeconds = task.accumulatedSeconds + ((now - task.startEpochMs) / 1000L).coerceAtLeast(0L))
        }
        _activeTask.value = updated
        saveData()
        return Result.success(updated)
    }

    // +30 min: actualiza la estimación límite, no suma trabajo no realizado.
    override suspend fun extendActiveTask(minutes: Int): Result<WorkTask> {
        if (minutes !in 1..240) return Result.failure(IllegalArgumentException("Extensión fuera de rango"))
        val task = _activeTask.value ?: return Result.failure(IllegalStateException("No hay tarea activa"))
        val updated = task.copy(
            extensionMinutes = task.extensionMinutes + minutes,
            // En modo demo, el siguiente aviso llega 15 s después del aplazamiento.
            nextReminderAtSeconds = if (task.demoReminder) task.elapsedSeconds() + 15L
                                    else (task.estimateMinutes + task.extensionMinutes + minutes) * 60L,
        )
        _activeTask.value = updated
        saveData()
        return Result.success(updated)
    }

    // stopActiveTask: Calcula tiempo transcurrido, crea TimeLog, quita tarea activa y guarda.
    // Guardar registro llama a este método con la nota del usuario.
    override suspend fun stopActiveTask(note: String): Result<TimeLog?> {
        val task = _activeTask.value ?: return Result.success(null)
        val elapsedSeconds = task.elapsedSeconds()
        // Historial nuevo guarda segundos reales; minutos es la compatibilidad con Home.
        val durationMinutes = (elapsedSeconds / 60L).coerceAtLeast(1L)

        val log = TimeLog(
            // ID único para diferenciar proyectos/tareas/registros.
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

    // logHours: Registra manualmente una duración en minutos con nota y fecha.
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
            // ID único para diferenciar proyectos/tareas/registros.
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

    // deleteProject: Elimina proyecto, su foto, sus horas y cualquier tarea activa asociada; actualiza selección.
    override suspend fun deleteProject(projectId: String) {
        // Primero solicitamos el borrado a Spring Boot; si no hay red mantenemos la operación local.
        val pending = prefs.getStringSet("pending_deleted_projects", emptySet()).orEmpty().plus(projectId)
        val pendingTasks = prefs.getStringSet("pending_deleted_tasks", emptySet()).orEmpty() +
            _projectTasks.value.filter { it.projectId == projectId }.map { it.id }
        prefs.edit().putStringSet("pending_deleted_projects", pending)
            .putStringSet("pending_deleted_tasks", pendingTasks).apply()
        // Elimina la fotografía asociada al borrar el proyecto.
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
        syncLater()
    }

    // Asignar tarea: 8 h por persona menos carga de ejemplo,
    // registros propios y asignaciones existentes. No hay servidor ni empleados reales.
    override suspend fun assignTask(
        employeeId: String, projectId: String, taskName: String,
        difficulty: String, estimateMinutes: Int,
    ): Result<TaskAssignment> {
        val member = WorkflowDefaults.members.firstOrNull { it.id == employeeId }
            ?: return Result.failure(IllegalArgumentException("Empleado no válido"))
        val project = _projects.value.firstOrNull { it.id == projectId }
            ?: return Result.failure(IllegalArgumentException("Proyecto no válido"))
        if (taskName.isBlank() || estimateMinutes <= 0) {
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
            project.createdAtEpochMs + index, estimateMinutes = 60 + 30 * (index % 4)) }
    }

    override suspend fun addProjectTask(projectId: String, name: String, estimateMinutes: Int): Result<ProjectTask> {
        if (estimateMinutes !in 1..1440)
            return Result.failure(IllegalArgumentException("La duración debe estar entre 1 y 1440 minutos"))
        if (_projects.value.none { it.id == projectId }) return Result.failure(IllegalArgumentException("Proyecto no encontrado"))
        val clean = name.trim()
        val problem = HomeFormValidator.taskName(clean)
        if (problem != null) return Result.failure(IllegalArgumentException(problem))
        if (_projectTasks.value.any { it.projectId == projectId && it.name.equals(clean, true) })
            return Result.failure(IllegalArgumentException("La tarea ya existe en este proyecto"))
        val task = ProjectTask(UUID.randomUUID().toString(), projectId, clean, estimateMinutes = estimateMinutes)
        _projectTasks.value = _projectTasks.value + task
        saveData()
        syncLater()
        return Result.success(task)
    }

    override suspend fun deleteProjectTask(taskId: String): Result<Unit> {
        val pending = prefs.getStringSet("pending_deleted_tasks", emptySet()).orEmpty().plus(taskId)
        prefs.edit().putStringSet("pending_deleted_tasks", pending).apply()
        if (_projectTasks.value.none { it.id == taskId }) return Result.failure(IllegalArgumentException("Tarea no encontrada"))
        _projectTasks.value = _projectTasks.value.filterNot { it.id == taskId }
        saveData()
        syncLater()
        return Result.success(Unit)
    }

    private fun serializeProjectTask(t: ProjectTask) =
        listOf(t.id, t.projectId, t.name.replace("|", "_"), t.createdAtEpochMs.toString(), t.estimateMinutes.toString()).joinToString("|")

    private fun parseProjectTask(raw: String): ProjectTask? {
        val p = raw.split("|")
        if (p.size < 4) return null
        return ProjectTask(p[0], p[1], p[2], p[3].toLongOrNull() ?: 0L,
            (p.getOrNull(4)?.toIntOrNull() ?: 90).coerceIn(1, 1440))
    }

    // serializeProject: Convierte Project a texto con separador | para guardarlo en SharedPreferences.
    private fun serializeProject(p: Project): String =
        "${p.id}|${p.name.replace("|", "_")}|${p.description.replace("|", "_")}|${p.createdAtEpochMs}"

    // parseProject: Reconstruye Project desde el formato de texto guardado.
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

    // serializeWorkTask: Convierte WorkTask a una cadena con su pausa y segundos acumulados.
    private fun serializeWorkTask(t: WorkTask): String =
        "${t.id}|${t.projectId}|${t.projectName.replace("|", "_")}|${t.taskName.replace("|", "_")}|${t.startEpochMs}|${t.paused}|${t.accumulatedSeconds}|${t.difficulty}|${t.estimateMinutes}|${t.extensionMinutes}|${t.originalStartEpochMs}|${t.demoReminder}|${t.nextReminderAtSeconds}"

    // parseWorkTask: Recrea WorkTask desde el dato guardado, aplicando valores por defecto si faltan campos.
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

    // serializeTimeLog: Transforma el registro de horas a texto persistible.
    private fun serializeTimeLog(l: TimeLog): String =
        "${l.id}|${l.projectId}|${l.durationMinutes}|${l.note.replace("|", "_")}|${l.timestampEpochMs}|${l.durationSeconds}|${l.taskName.replace("|", "_")}|${l.difficulty}|${l.estimateMinutes}|${l.source}|${l.employeeName.replace("|", "_")}"

    // parseTimeLog: Reconstruye registro de horas guardado.
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

    // Las asignaciones se serializan aparte para no tocar proyectos antiguos.
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

    // Claves que utiliza el almacenamiento y consulta también el widget.
    private companion object {
        const val KEY_PROJECTS = "key_projects"
        const val KEY_CURRENT_PROJECT_ID = "key_current_project_id"
        const val KEY_ACTIVE_TASK = "key_active_task"
        const val KEY_TIME_LOGS = "key_time_logs"
        const val KEY_ASSIGNMENTS = "key_assignments"
        const val KEY_PROJECT_TASKS = "key_project_tasks_v5"
    }
}
