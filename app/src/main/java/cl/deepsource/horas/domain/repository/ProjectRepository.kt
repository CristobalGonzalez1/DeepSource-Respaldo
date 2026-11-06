package cl.deepsource.horas.domain.repository

import cl.deepsource.horas.domain.model.Project
import cl.deepsource.horas.domain.model.TimeLog
import cl.deepsource.horas.domain.model.WorkTask
import kotlinx.coroutines.flow.StateFlow

interface ProjectRepository {
    val projects: StateFlow<List<Project>>
    val currentProject: StateFlow<Project?>
    val activeTask: StateFlow<WorkTask?>
    val timeLogs: StateFlow<List<TimeLog>>

    suspend fun createProject(name: String, description: String = ""): Result<Project>
    suspend fun setCurrentProject(projectId: String?)
    suspend fun startTask(projectId: String, taskName: String = "Tarea en curso"): Result<WorkTask>
    suspend fun toggleTaskPause(): Result<WorkTask>
    suspend fun stopActiveTask(): Result<TimeLog?>
    suspend fun logHours(projectId: String, durationMinutes: Long, note: String = ""): Result<TimeLog>
    suspend fun deleteProject(projectId: String)
}
