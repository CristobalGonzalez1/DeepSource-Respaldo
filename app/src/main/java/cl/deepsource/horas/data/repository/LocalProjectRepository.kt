package cl.deepsource.horas.data.repository

import android.content.Context
import android.content.SharedPreferences
import cl.deepsource.horas.domain.model.Project
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

class LocalProjectRepository(private val context: Context) : ProjectRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("deepsource_projects_prefs", Context.MODE_PRIVATE)

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    override val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _currentProject = MutableStateFlow<Project?>(null)
    override val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    private val _activeTask = MutableStateFlow<WorkTask?>(null)
    override val activeTask: StateFlow<WorkTask?> = _activeTask.asStateFlow()

    private val _timeLogs = MutableStateFlow<List<TimeLog>>(emptyList())
    override val timeLogs: StateFlow<List<TimeLog>> = _timeLogs.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val projectsRaw = prefs.getStringSet(KEY_PROJECTS, emptySet()) ?: emptySet()
        val loadedProjects = projectsRaw.mapNotNull { parseProject(it) }
            .sortedByDescending { it.createdAtEpochMs }
        _projects.value = loadedProjects

        val currentProjId = prefs.getString(KEY_CURRENT_PROJECT_ID, null)
        _currentProject.value = loadedProjects.firstOrNull { it.id == currentProjId }
            ?: loadedProjects.firstOrNull()

        val activeTaskRaw = prefs.getString(KEY_ACTIVE_TASK, null)
        _activeTask.value = activeTaskRaw?.let { parseWorkTask(it) }

        val logsRaw = prefs.getStringSet(KEY_TIME_LOGS, emptySet()) ?: emptySet()
        _timeLogs.value = logsRaw.mapNotNull { parseTimeLog(it) }
            .sortedByDescending { it.timestampEpochMs }
    }

    private fun saveData() {
        val projectsSet = _projects.value.map { serializeProject(it) }.toSet()
        val currentProjId = _currentProject.value?.id
        val activeTaskSerialized = _activeTask.value?.let { serializeWorkTask(it) }
        val logsSet = _timeLogs.value.map { serializeTimeLog(it) }.toSet()

        prefs.edit()
            .putStringSet(KEY_PROJECTS, projectsSet)
            .putString(KEY_CURRENT_PROJECT_ID, currentProjId)
            .putString(KEY_ACTIVE_TASK, activeTaskSerialized)
            .putStringSet(KEY_TIME_LOGS, logsSet)
            .apply()

        DeepSourceAppWidget.sendUpdateBroadcast(context)
    }

    override suspend fun createProject(name: String, description: String): Result<Project> {
        val cleanName = name.trim()
        val problem = HomeFormValidator.projectName(cleanName) ?: HomeFormValidator.description(description)
        if (problem != null) return Result.failure(IllegalArgumentException(problem))

        val newProj = Project(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            description = description.trim(),
            createdAtEpochMs = System.currentTimeMillis(),
        )

        val updated = _projects.value + newProj
        _projects.value = updated
        _currentProject.value = newProj

        saveData()
        return Result.success(newProj)
    }

    override suspend fun setCurrentProject(projectId: String?) {
        val selected = _projects.value.firstOrNull { it.id == projectId }
        _currentProject.value = selected
        saveData()
    }

    override suspend fun startTask(projectId: String, taskName: String): Result<WorkTask> {
        val taskProblem = HomeFormValidator.taskName(taskName)
        if (taskProblem != null) return Result.failure(IllegalArgumentException(taskProblem))
        if (_activeTask.value != null) return Result.failure(IllegalStateException("Finaliza la tarea actual antes de iniciar otra"))
        val project = _projects.value.firstOrNull { it.id == projectId }
            ?: return Result.failure(IllegalStateException("El proyecto seleccionado no existe"))

        val newTask = WorkTask(
            id = UUID.randomUUID().toString(),
            projectId = project.id,
            projectName = project.name,
            taskName = taskName.trim().ifBlank { "Tarea en curso" },
            startEpochMs = System.currentTimeMillis(),
        )

        _activeTask.value = newTask
        _currentProject.value = project
        saveData()
        return Result.success(newTask)
    }

    override suspend fun toggleTaskPause(): Result<WorkTask> {
        val task = _activeTask.value ?: return Result.failure(IllegalStateException("No hay una tarea para aplazar"))
        val now = System.currentTimeMillis()
        val updated = if (task.paused) {
            task.copy(paused = false, startEpochMs = now)
        } else {
            task.copy(paused = true, accumulatedSeconds = task.accumulatedSeconds + ((now - task.startEpochMs) / 1000L).coerceAtLeast(0L))
        }
        _activeTask.value = updated
        saveData()
        return Result.success(updated)
    }

    override suspend fun stopActiveTask(): Result<TimeLog?> {
        val task = _activeTask.value ?: return Result.success(null)
        val elapsedSeconds = task.accumulatedSeconds + if (task.paused) 0L else ((System.currentTimeMillis() - task.startEpochMs) / 1000L).coerceAtLeast(0L)
        val durationMinutes = (elapsedSeconds / 60L).coerceAtLeast(1L)

        val log = TimeLog(
            id = UUID.randomUUID().toString(),
            projectId = task.projectId,
            durationMinutes = durationMinutes,
            note = task.taskName,
            timestampEpochMs = System.currentTimeMillis(),
        )

        _timeLogs.value = _timeLogs.value + log
        _activeTask.value = null
        saveData()
        return Result.success(log)
    }

    override suspend fun logHours(projectId: String, durationMinutes: Long, note: String): Result<TimeLog> {
        if (durationMinutes !in 1..1439 || HomeFormValidator.note(note) != null) {
            return Result.failure(IllegalArgumentException("La duración debe ser mayor a 0 minutos"))
        }
        val project = _projects.value.firstOrNull { it.id == projectId }
            ?: return Result.failure(IllegalStateException("El proyecto no existe"))

        val log = TimeLog(
            id = UUID.randomUUID().toString(),
            projectId = project.id,
            durationMinutes = durationMinutes,
            note = note.trim(),
            timestampEpochMs = System.currentTimeMillis(),
        )

        _timeLogs.value = _timeLogs.value + log
        saveData()
        return Result.success(log)
    }

    override suspend fun deleteProject(projectId: String) {
        TaskPhotoStore(context).delete(projectId)
        _projects.value = _projects.value.filterNot { it.id == projectId }
        if (_currentProject.value?.id == projectId) {
            _currentProject.value = _projects.value.firstOrNull()
        }
        if (_activeTask.value?.projectId == projectId) {
            _activeTask.value = null
        }
        _timeLogs.value = _timeLogs.value.filterNot { it.projectId == projectId }
        saveData()
    }

    private fun serializeProject(p: Project): String =
        "${p.id}|${p.name.replace("|", "_")}|${p.description.replace("|", "_")}|${p.createdAtEpochMs}"

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

    private fun serializeWorkTask(t: WorkTask): String =
        "${t.id}|${t.projectId}|${t.projectName.replace("|", "_")}|${t.taskName.replace("|", "_")}|${t.startEpochMs}|${t.paused}|${t.accumulatedSeconds}"

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
        )
    }

    private fun serializeTimeLog(l: TimeLog): String =
        "${l.id}|${l.projectId}|${l.durationMinutes}|${l.note.replace("|", "_")}|${l.timestampEpochMs}"

    private fun parseTimeLog(s: String): TimeLog? {
        val parts = s.split("|")
        if (parts.size < 5) return null
        return TimeLog(
            id = parts[0],
            projectId = parts[1],
            durationMinutes = parts[2].toLongOrNull() ?: 0L,
            note = parts[3],
            timestampEpochMs = parts[4].toLongOrNull() ?: System.currentTimeMillis(),
        )
    }

    private companion object {
        const val KEY_PROJECTS = "key_projects"
        const val KEY_CURRENT_PROJECT_ID = "key_current_project_id"
        const val KEY_ACTIVE_TASK = "key_active_task"
        const val KEY_TIME_LOGS = "key_time_logs"
    }
}
