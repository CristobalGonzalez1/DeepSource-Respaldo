package cl.deepsource.horas.feature.workflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.deepsource.horas.appContainer
import cl.deepsource.horas.core.nativefeatures.TaskNotifications
import cl.deepsource.horas.domain.model.Project
import cl.deepsource.horas.domain.model.ProjectTask
import cl.deepsource.horas.domain.model.TaskAssignment
import cl.deepsource.horas.domain.model.TimeLog
import cl.deepsource.horas.domain.model.WorkTask
import cl.deepsource.horas.domain.model.WorkflowDefaults
import cl.deepsource.horas.domain.model.elapsedSeconds
import cl.deepsource.horas.domain.repository.ProjectRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * [FLUJO/MVVM] Estado de campos editables. Es INDEPENDIENTE de los modelos persistidos.
 * Los Composables nunca escriben directamente en SharedPreferences.
 */
data class WorkflowForm(
    val projectId: String = "",
    val taskName: String = WorkflowDefaults.sampleTasks.first(),
    val difficulty: String = "Normal",
    val demoReminder: Boolean = false,
    val note: String = "",
    val manualProjectId: String = "",
    val manualTaskName: String = WorkflowDefaults.sampleTasks.first(),
    val manualDifficulty: String = "Normal",
    val manualDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date()),
    val manualHours: String = "1",
    val manualMinutes: String = "0",
    val manualNote: String = "",
    val employeeId: String = "luis",
    val managerProjectId: String = "",
    val managerTaskName: String = WorkflowDefaults.sampleTasks.first(),
    val managerDifficulty: String = "Normal",
    val detailProjectId: String = "",
)

/** [FLUJO/MVVM] Instantánea tomada al tocar Finalizar. Se confirma recién en Guardar. */
data class FinishPreview(val task: WorkTask, val seconds: Long)

/** [FLUJO/MVVM] Datos necesarios para renderizar todas las pantallas nuevas. */
data class WorkflowUiState(
    val projects: List<Project> = emptyList(),
    val currentProject: Project? = null,
    val activeTask: WorkTask? = null,
    val logs: List<TimeLog> = emptyList(),
    val assignments: List<TaskAssignment> = emptyList(),
    val projectTasks: List<ProjectTask> = emptyList(),
    val form: WorkflowForm = WorkflowForm(),
    val nowMs: Long = System.currentTimeMillis(),
    val preview: FinishPreview? = null,
    val lastSavedId: String? = null,
    val message: String? = null,
) {
    // [FLUJO/RELOJ] Pared de tiempo real; no depende del número de recomposiciones Compose.
    val elapsed: Long get() = activeTask?.elapsedSeconds(nowMs) ?: 0L
    val limit: Long get() = activeTask?.let { (it.estimateMinutes + it.extensionMinutes) * 60L } ?: 0L
    val selectedProject: Project? get() = projects.firstOrNull { it.id == form.projectId }
        ?: currentProject ?: projects.firstOrNull()
    val selectedTaskNames: List<String> get() = projectTasks.filter { it.projectId == selectedProject?.id }.map { it.name }
    val detailProjectTasks: List<ProjectTask> get() = projectTasks.filter { it.projectId == detailProject?.id }
    val detailProject: Project? get() = projects.firstOrNull { it.id == form.detailProjectId }
        ?: currentProject ?: projects.firstOrNull()
}

/**
 * [FLUJO/MVVM] Único intermediario de las pantallas Material v2.
 * Pantalla -> función de este ViewModel -> ProjectRepository -> StateFlow -> Pantalla.
 * Mantiene el mismo repositorio y formato previo, para NO perder datos antiguos.
 */
class WorkflowViewModel(
    private val repository: ProjectRepository,
    private val notifications: TaskNotifications,
) : ViewModel() {
    private val form = MutableStateFlow(WorkflowForm())
    private val now = MutableStateFlow(System.currentTimeMillis())
    private val preview = MutableStateFlow<FinishPreview?>(null)
    private val lastSavedId = MutableStateFlow<String?>(null)
    private val message = MutableStateFlow<String?>(null)
    // [FLUJO/RECORDATORIO] Esta llave impide abrir el mismo aviso en cada segundo.
    private val deliveredAlerts = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            while (true) {
                now.value = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    private data class Stored(
        val projects: List<Project>, val current: Project?, val task: WorkTask?,
        val logs: List<TimeLog>, val assignments: List<TaskAssignment>,
        val projectTasks: List<ProjectTask> = emptyList(),
    )

    // [FLUJO/STATEFLOW] Combina datos de repositorio, formulario, reloj y confirmación.
    private val data = combine(repository.projects, repository.currentProject,
        repository.activeTask, repository.timeLogs, repository.assignments) { projects, current, task, logs, assignments ->
        Stored(projects, current, task, logs, assignments)
    }.combine(repository.projectTasks) { stored, tasks -> stored.copy(projectTasks = tasks) }

    val uiState: StateFlow<WorkflowUiState> = combine(data, form, now, preview, lastSavedId) {
            stored, fields, time, pending, savedId ->
        WorkflowUiState(stored.projects, stored.current, stored.task, stored.logs,
            stored.assignments, stored.projectTasks, fields, time, pending, savedId)
    }.combine(message) { dataState, notice -> dataState.copy(message = notice) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), WorkflowUiState())

    fun clearMessage() { message.value = null }
    fun edit(change: (WorkflowForm) -> WorkflowForm) { form.value = change(form.value) }

    // [FLUJO/BOTÓN] Proyecto: cambia solo selección de formulario; la selección actual se guarda al iniciar.
    fun chooseProject(id: String) = edit { old ->
        val first = repository.projectTasks.value.firstOrNull { it.projectId == id }?.name.orEmpty()
        old.copy(projectId = id, taskName = first)
    }
    fun chooseDifficulty(value: String) = edit { it.copy(difficulty = value) }
    fun chooseTask(value: String) = edit { it.copy(taskName = value) }
    fun setDemo(value: Boolean) = edit { it.copy(demoReminder = value) }
    fun selectDetail(projectId: String) = edit { it.copy(detailProjectId = projectId) }

    /** [V5/TAREAS] Añadir tareas antes de iniciar; repositorio persiste la lista por proyecto. */
    fun addTask(projectId: String, name: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.addProjectTask(projectId, name)
                .onSuccess { task -> form.value = form.value.copy(projectId = projectId, taskName = task.name); onSuccess() }
                .onFailure { message.value = it.message ?: "No se pudo añadir la tarea" }
        }
    }

    fun removeTask(id: String) = viewModelScope.launch {
        repository.deleteProjectTask(id).onFailure { message.value = it.message }
    }

    // [FLUJO/BOTÓN] Iniciar: impide tareas duplicadas; los 90/120/240 min vienen del nivel.
    fun begin(onSuccess: () -> Unit) {
        val state = uiState.value
        val project = state.selectedProject ?: run { message.value = "Crea o selecciona un proyecto"; return }
        val fields = state.form
        val taskName = state.selectedTaskNames.firstOrNull { it == fields.taskName }
            ?: state.selectedTaskNames.firstOrNull()
            ?: run { message.value = "Agrega una tarea a este proyecto antes de iniciar"; return }
        viewModelScope.launch {
            repository.startTask(project.id, taskName, fields.difficulty,
                WorkflowDefaults.estimates[fields.difficulty] ?: 90, fields.demoReminder)
                .onSuccess {
                    notifications.post("Tarea iniciada", it.taskName)
                    onSuccess()
                }.onFailure { message.value = it.message ?: "No se pudo iniciar" }
        }
    }

    // [FLUJO/BOTÓN] Pausar/reanudar: mantiene la medición previa y avisa al widget.
    fun togglePause() = viewModelScope.launch {
        repository.toggleTaskPause().onFailure { message.value = it.message }
    }

    // [FLUJO/BOTÓN] +30 min: NO agrega tiempo ficticio al registro, solo extiende el límite.
    fun extend(onSuccess: () -> Unit) = viewModelScope.launch {
        repository.extendActiveTask(30).onSuccess { onSuccess() }
            .onFailure { message.value = it.message }
    }

    // [FLUJO/BOTÓN] Finalizar: pausa el reloj y prepara resumen; aún no lo guarda.
    fun prepareFinish(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val previous = repository.activeTask.value ?: return@launch
            if (!previous.paused) repository.toggleTaskPause()
            val frozen = repository.activeTask.value ?: return@launch
            preview.value = FinishPreview(frozen, frozen.elapsedSeconds())
            form.value = form.value.copy(note = "")
            onSuccess()
        }
    }

    // [FLUJO/BOTÓN] Guardar registro: añade el resumen al TimeLog y deja el cronómetro inactivo.
    fun saveFinished(onSuccess: () -> Unit) {
        val note = form.value.note.trim()
        if (note.length !in 4..300) { message.value = "La descripción debe tener entre 4 y 300 caracteres"; return }
        viewModelScope.launch {
            repository.stopActiveTask(note).onSuccess { log ->
                if (log != null) {
                    lastSavedId.value = log.id
                    preview.value = null
                    notifications.post("Tarea finalizada", "Tiempo registrado: ${log.durationMinutes} min")
                    onSuccess()
                }
            }.onFailure { message.value = it.message }
        }
    }

    // [FLUJO/BOTÓN] Guardar manual: valida fecha y duración, sin pasar por un cronómetro.
    fun saveManual(onSuccess: () -> Unit) {
        val state = uiState.value
        val f = state.form
        val project = state.projects.firstOrNull { it.id == f.manualProjectId }
            ?: state.selectedProject ?: run { message.value = "Selecciona un proyecto"; return }
        val hours = f.manualHours.toIntOrNull()
        val minutes = f.manualMinutes.toIntOrNull()
        val duration = if (hours != null && minutes != null) hours * 60L + minutes else 0L
        if (hours == null || minutes == null || hours !in 0..23 || minutes !in 0..59 || duration !in 1..1439) {
            message.value = "Ingresa una duración válida entre 1 minuto y 23 h 59 min"; return
        }
        if (f.manualNote.trim().length !in 4..300) { message.value = "La descripción debe tener entre 4 y 300 caracteres"; return }
        val parsed = try { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(f.manualDate) } catch (_: Exception) { null }
        if (parsed == null || parsed.time > System.currentTimeMillis()) { message.value = "Selecciona una fecha válida que no sea futura"; return }
        val allowed = repository.projectTasks.value.filter { it.projectId == project.id }.map { it.name }
        val chosenTask = f.manualTaskName.takeIf { it in allowed } ?: allowed.firstOrNull()
            ?: run { message.value = "Agrega una tarea a este proyecto"; return }
        viewModelScope.launch {
            repository.logHours(project.id, duration, f.manualNote.trim(), chosenTask,
                f.manualDifficulty, parsed.time)
                .onSuccess { log -> lastSavedId.value = log.id; onSuccess() }
                .onFailure { message.value = it.message ?: "No se pudo guardar" }
        }
    }

    // [FLUJO/BOTÓN] Asignar: validación definitiva vive en Repository; actualiza lista y carga.
    fun assign(onSuccess: () -> Unit) {
        val state = uiState.value
        val f = state.form
        val project = state.projects.firstOrNull { it.id == f.managerProjectId }
            ?: state.selectedProject ?: run { message.value = "Falta proyecto"; return }
        viewModelScope.launch {
            repository.assignTask(f.employeeId, project.id, f.managerTaskName, f.managerDifficulty,
                WorkflowDefaults.estimates[f.managerDifficulty] ?: 90)
                .onSuccess { onSuccess() }
                .onFailure { message.value = it.message ?: "No fue posible asignar" }
        }
    }

    // [FLUJO/AVISO] En esta versión el aviso contextual se dispara con la app abierta.
    // NO promete alarma exacta cuando Android suspende la app. Demo: 15 s; real: tiempo estimado + extensión.
    fun dueAlert(task: WorkTask, seconds: Long): Boolean {
        val threshold = task.nextReminderAtSeconds.takeIf { it > 0L }
            ?: if (task.demoReminder) 15L else (task.estimateMinutes + task.extensionMinutes) * 60L
        val id = "${task.id}:${task.extensionMinutes}"
        if (seconds < threshold || id in deliveredAlerts) return false
        deliveredAlerts.add(id)
        notifications.post("Recordatorio DeepSource", "¿Continuar o finalizar ${task.taskName}?")
        return true
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                WorkflowViewModel(container.projectRepository, TaskNotifications(container.context))
            }
        }
    }
}
