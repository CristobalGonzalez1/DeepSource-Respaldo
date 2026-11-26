package cl.deepsource.horas.domain.repository

import cl.deepsource.horas.domain.model.Project
import cl.deepsource.horas.domain.model.TimeLog
import cl.deepsource.horas.domain.model.WorkTask
import cl.deepsource.horas.domain.model.ProjectTask
import cl.deepsource.horas.domain.model.TaskAssignment
import kotlinx.coroutines.flow.StateFlow

// ProjectRepository: Contrato entre ViewModel y persistencia: expone StateFlow y operaciones de proyectos y tareas.
interface ProjectRepository {
    val projects: StateFlow<List<Project>>
    val currentProject: StateFlow<Project?>
    val activeTask: StateFlow<WorkTask?>
    val timeLogs: StateFlow<List<TimeLog>>
    // Vista de jefe y lista de asignaciones observables.
    val assignments: StateFlow<List<TaskAssignment>>
    // Cada proyecto mantiene un catálogo persistente e independiente.
    val projectTasks: StateFlow<List<ProjectTask>>
    suspend fun addProjectTask(projectId: String, name: String, estimateMinutes: Int = 90): Result<ProjectTask>
    suspend fun deleteProjectTask(taskId: String): Result<Unit>

    // createProject: Contrato para crear proyecto validado.
    suspend fun createProject(name: String, description: String = ""): Result<Project>
    // setCurrentProject: Contrato para cambiar el proyecto actual.
    suspend fun setCurrentProject(projectId: String?)
    // startTask: Contrato para activar un cronómetro de tarea.
    suspend fun startTask(projectId: String, taskName: String = "Tarea en curso", difficulty: String = "Normal", estimateMinutes: Int = 90, demoReminder: Boolean = false): Result<WorkTask>
    // toggleTaskPause: Contrato para pausar/reanudar tarea.
    suspend fun toggleTaskPause(): Result<WorkTask>
    // stopActiveTask: Contrato para finalizar y guardar tiempo registrado.
    suspend fun stopActiveTask(note: String = ""): Result<TimeLog?>
    // +30 minutos: amplía el límite sin alterar tiempo transcurrido.
    suspend fun extendActiveTask(minutes: Int = 30): Result<WorkTask>
    // logHours: Contrato para añadir un registro manual de horas.
    suspend fun logHours(projectId: String, durationMinutes: Long, note: String = "", taskName: String = "Registro manual", difficulty: String = "Normal", timestampEpochMs: Long = System.currentTimeMillis()): Result<TimeLog>
    // deleteProject: Contrato para eliminar un proyecto y datos asociados.
    suspend fun deleteProject(projectId: String)
    // Asignar: comprueba horas disponibles antes de guardar la asignación.
    suspend fun assignTask(employeeId: String, projectId: String, taskName: String, difficulty: String, estimateMinutes: Int): Result<TaskAssignment>
}
