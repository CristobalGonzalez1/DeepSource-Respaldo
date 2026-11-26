package cl.deepsource.horas.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.deepsource.horas.appContainer
import cl.deepsource.horas.core.validation.HomeFormValidator
import cl.deepsource.horas.domain.logic.RegistrationRules
import cl.deepsource.horas.domain.logic.TimerRules
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

// HomeViewModel: Procesa acciones del Home, combina StateFlows y calcula métricas que muestran los Composables.
class HomeViewModel(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    private val notifications: TaskNotifications,
) : ViewModel() {

    // Reloj reactivo: emite milisegundos actuales para recalcular segundos de tarea cada segundo.
    private val timeTicker = MutableStateFlow(System.currentTimeMillis())
    // Canal de mensajes breves que HomeRoute presenta como Toast.
    private val uiMessage = MutableStateFlow<String?>(null)
    // Candado sencillo que evita varios toques de pausa a la vez.
    private var isTogglingPause = false

    init {
        viewModelScope.launch {
            // Tick cada segundo mientras vive el ViewModel.
            while (true) {
                timeTicker.value = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    // Combina sesión, proyectos, proyecto seleccionado, tarea, registros, hora y mensajes.
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

        // Tiempo activo = tiempo acumulado + segmento desde startEpochMs si no está en pausa.
        val activeElapsedSec = if (activeTask != null) {
            TimerRules.elapsedSeconds(activeTask, nowMs)
        } else 0L

        // Para cada proyecto suma los minutos registrados en su historial.
        val projectsWithStats = snapshot.projects.map { proj ->
            val totalMins = RegistrationRules.projectMinutes(proj.id, snapshot.logs)
            ProjectWithStats(
                project = proj,
                totalLoggedMinutes = totalMins,
                isCurrent = proj.id == snapshot.currentProj?.id,
            )
        }

        // Límite inferior para calcular minutos de esta semana.
        val startOfWeekMs = getStartOfWeekMs(nowMs)
        val currentProj = snapshot.currentProj
        // Suma TimeLog solo del proyecto actual desde inicio de semana.
        val weeklyMinsForCurrent = if (currentProj != null) {
            RegistrationRules.weeklyMinutes(currentProj.id, startOfWeekMs, snapshot.logs)
        } else 0L

        val uName = snapshot.user?.name.orEmpty().ifBlank { "Marina Soto" }
        val uEmail = snapshot.user?.email.orEmpty().ifBlank { "marina.soto@deepsource.cl" }

        // Construye datos listos para pintar en HomeScreen sin procesar allí el negocio.
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
            userRole = if (snapshot.user?.role == cl.deepsource.horas.feature.auth.domain.UserRole.ADMIN)
                "Administrador (modo demostración)" else "Usuario normal",
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        // Mantiene flujo activo al observarlo; pausa recolección tras salir de la pantalla.
        started = SharingStarted.WhileSubscribed(5000L),
        // Construye datos listos para pintar en HomeScreen sin procesar allí el negocio.
        initialValue = HomeUiState(),
    )

    // createProject: Valida y crea proyecto mediante el repositorio; reporta errores al Home.
    fun createProject(name: String, description: String = "") {
        // Valida antes de lanzar la operación en el repositorio.
        val problem = HomeFormValidator.projectName(name) ?: HomeFormValidator.description(description)
        if (problem != null) { uiMessage.value = problem; return }
        viewModelScope.launch {
            // Punto de procesamiento y guardado real del nuevo proyecto.
            val result = projectRepository.createProject(name, description)
            result.onFailure {
                uiMessage.value = it.localizedMessage ?: "Error al crear proyecto"
            }
        }
    }

    // selectProject: Envía al repositorio el ID elegido desde la lista.
    fun selectProject(projectId: String) {
        viewModelScope.launch {
            // Actualiza elección y widget mediante guardado del repositorio.
            projectRepository.setCurrentProject(projectId)
        }
    }

    // deleteProject: Borra el proyecto y presenta mensaje de confirmación.
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            // Ejecuta eliminación definitiva de datos del proyecto.
            projectRepository.deleteProject(projectId)
            uiMessage.value = "Proyecto eliminado"
        }
    }

    // startTask: Valida e inicia cronómetro, notificando al usuario si se confirma.
    fun startTask(projectId: String, taskName: String = "Tarea en curso") {
        val problem = HomeFormValidator.selectedProject(projectId) ?: HomeFormValidator.taskName(taskName)
        if (problem != null) { uiMessage.value = problem; return }
        viewModelScope.launch {
            // Crea WorkTask y marca hora de inicio en persistencia local.
            val result = projectRepository.startTask(projectId, taskName)
            // Después de éxito confirmado, manda notificación; no notifica si hay error.
            result.onSuccess { notifications.post("Tarea iniciada", it.taskName) }
            result.onFailure {
                uiMessage.value = it.localizedMessage ?: "Error al iniciar tarea"
            }
        }
    }

    // toggleTaskPause: Evita doble toque simultáneo y alterna pausa/reanudación.
    fun toggleTaskPause() {
        if (isTogglingPause) return
        isTogglingPause = true
        viewModelScope.launch {
            try {
                // Pausa o reanuda manteniendo los segundos acumulados.
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

    // stopActiveTask: Finaliza tarea, persiste tiempo y envía notificación local.
    fun stopActiveTask() {
        viewModelScope.launch {
            // Transforma tarea activa en TimeLog, guardando el resultado.
            projectRepository.stopActiveTask().onSuccess { log ->
                // Notifica con duración obtenida del registro terminado.
                if (log != null) notifications.post("Tarea finalizada", "Tiempo registrado: ${log.durationMinutes} min")
            }.onFailure { uiMessage.value = it.message ?: "No se pudo finalizar" }
        }
    }

    // logHours: Valida duración (1..1439 min) y solicita registro manual al repositorio.
    fun logHours(projectId: String, durationMinutes: Long, note: String = "") {
        val problem = HomeFormValidator.selectedProject(projectId) ?: HomeFormValidator.note(note)
        if (problem != null || durationMinutes !in 1..1439) { uiMessage.value = problem ?: "Duración fuera de rango"; return }
        viewModelScope.launch {
            // Crea un registro histórico manual en SharedPreferences.
            val result = projectRepository.logHours(projectId, durationMinutes, note)
            result.onFailure {
                uiMessage.value = it.localizedMessage ?: "Error al registrar horas"
            }
        }
    }

    // clearMessage: Quita mensaje tras mostrarlo en Toast; evita repetirlo.
    fun clearMessage() {
        uiMessage.value = null
    }

    // onSignOut: Cierra sesión en el repositorio de autenticación.
    fun onSignOut() {
        // Vacía sesión del repositorio fake; HomeRoute además navega a Login.
        authRepository.signOut()
    }

    // getStartOfWeekMs: Calcula inicio de semana para totalizar minutos semanales.
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

    // StateSnapshot: Agrupa los flujos base antes de formar el estado que observa HomeScreen.
    // Paso intermedio interno para combinar cinco flujos de datos base.
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
