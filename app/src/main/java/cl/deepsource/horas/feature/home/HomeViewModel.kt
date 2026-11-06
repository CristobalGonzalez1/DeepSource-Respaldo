package cl.deepsource.horas.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.deepsource.horas.appContainer
import cl.deepsource.horas.core.validation.HomeFormValidator
import cl.deepsource.horas.core.nativefeatures.TaskNotifications
import cl.deepsource.horas.domain.model.ProjectWithStats
import cl.deepsource.horas.domain.repository.ProjectRepository
import cl.deepsource.horas.feature.auth.domain.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    private val notifications: TaskNotifications,
) : ViewModel() {

    private val timeTicker = MutableStateFlow(System.currentTimeMillis())
    private val uiMessage = MutableStateFlow<String?>(null)
    private var isTogglingPause = false

    init {
        viewModelScope.launch {
            while (true) {
                timeTicker.value = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        authRepository.currentUser,
        projectRepository.projects,
        projectRepository.currentProject,
        projectRepository.activeTask,
        projectRepository.timeLogs,
    ) { user, projects, currentProj, activeTask, logs ->
        StateSnapshot(user, projects, currentProj, activeTask, logs)
    }.combine(
        combine(timeTicker, uiMessage) { now, msg -> now to msg }
    ) { snapshot, (nowMs, msg) ->

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES"))

        val timeStr = timeFormat.format(Date(nowMs))
        val rawDateStr = dateFormat.format(Date(nowMs))
        val dateStr = rawDateStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }

        val activeTask = snapshot.activeTask
        val isWorkdayActive = activeTask != null && !activeTask.paused

        val activeElapsedSec = if (activeTask != null) {
            activeTask.accumulatedSeconds + if (activeTask.paused) 0L else ((nowMs - activeTask.startEpochMs) / 1000L).coerceAtLeast(0L)
        } else 0L

        val projectsWithStats = snapshot.projects.map { proj ->
            val totalMins = snapshot.logs.filter { it.projectId == proj.id }.sumOf { it.durationMinutes }
            ProjectWithStats(
                project = proj,
                totalLoggedMinutes = totalMins,
                isCurrent = proj.id == snapshot.currentProj?.id,
            )
        }

        val startOfWeekMs = getStartOfWeekMs(nowMs)
        val currentProj = snapshot.currentProj
        val weeklyMinsForCurrent = if (currentProj != null) {
            snapshot.logs.filter { it.projectId == currentProj.id && it.timestampEpochMs >= startOfWeekMs }
                .sumOf { it.durationMinutes }
        } else 0L

        val uName = snapshot.user?.name.orEmpty().ifBlank { "Marina Soto" }
        val uEmail = snapshot.user?.email.orEmpty().ifBlank { "marina.soto@deepsource.cl" }

        HomeUiState(
            currentProject = currentProj,
            currentProjectWeeklyMinutes = weeklyMinsForCurrent,
            projects = projectsWithStats,
            activeTask = activeTask,
            isWorkdayActive = isWorkdayActive,
            activeTaskElapsedSeconds = activeElapsedSec,
            currentTimeString = timeStr,
            currentDateString = dateStr,
            userName = uName,
            userEmail = uEmail,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HomeUiState(),
    )

    fun createProject(name: String, description: String = "") {
        val problem = HomeFormValidator.projectName(name) ?: HomeFormValidator.description(description)
        if (problem != null) { uiMessage.value = problem; return }
        viewModelScope.launch {
            val result = projectRepository.createProject(name, description)
            result.onFailure {
                uiMessage.value = it.localizedMessage ?: "Error al crear proyecto"
            }
        }
    }

    fun selectProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.setCurrentProject(projectId)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
            uiMessage.value = "Proyecto eliminado"
        }
    }

    fun startTask(projectId: String, taskName: String = "Tarea en curso") {
        val problem = HomeFormValidator.selectedProject(projectId) ?: HomeFormValidator.taskName(taskName)
        if (problem != null) { uiMessage.value = problem; return }
        viewModelScope.launch {
            val result = projectRepository.startTask(projectId, taskName)
            result.onSuccess { notifications.post("Tarea iniciada", it.taskName) }
            result.onFailure {
                uiMessage.value = it.localizedMessage ?: "Error al iniciar tarea"
            }
        }
    }

    fun toggleTaskPause() {
        if (isTogglingPause) return
        isTogglingPause = true
        viewModelScope.launch {
            try {
                projectRepository.toggleTaskPause().onFailure {
                    uiMessage.value = it.localizedMessage ?: "Primero debes seleccionar una tarea"
                }.onSuccess { task ->
                    uiMessage.value = if (task.paused) "Tarea aplazada" else "Tarea reanudada"
                }
            } finally {
                isTogglingPause = false
            }
        }
    }

    fun stopActiveTask() {
        viewModelScope.launch {
            projectRepository.stopActiveTask().onSuccess { log ->
                if (log != null) notifications.post("Tarea finalizada", "Tiempo registrado: ${log.durationMinutes} min")
            }.onFailure { uiMessage.value = it.message ?: "No se pudo finalizar" }
        }
    }

    fun logHours(projectId: String, durationMinutes: Long, note: String = "") {
        val problem = HomeFormValidator.selectedProject(projectId) ?: HomeFormValidator.note(note)
        if (problem != null || durationMinutes !in 1..1439) { uiMessage.value = problem ?: "Duración fuera de rango"; return }
        viewModelScope.launch {
            val result = projectRepository.logHours(projectId, durationMinutes, note)
            result.onFailure {
                uiMessage.value = it.localizedMessage ?: "Error al registrar horas"
            }
        }
    }

    fun clearMessage() {
        uiMessage.value = null
    }

    fun onSignOut() {
        authRepository.signOut()
    }

    private fun getStartOfWeekMs(nowMs: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMs
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private data class StateSnapshot(
        val user: cl.deepsource.horas.feature.auth.domain.User?,
        val projects: List<cl.deepsource.horas.domain.model.Project>,
        val currentProj: cl.deepsource.horas.domain.model.Project?,
        val activeTask: cl.deepsource.horas.domain.model.WorkTask?,
        val logs: List<cl.deepsource.horas.domain.model.TimeLog>,
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                HomeViewModel(
                    authRepository = container.authRepository,
                    projectRepository = container.projectRepository,
                    notifications = TaskNotifications(container.context),
                )
            }
        }
    }
}
