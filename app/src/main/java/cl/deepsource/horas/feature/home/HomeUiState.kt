package cl.deepsource.horas.feature.home

import cl.deepsource.horas.domain.model.Project
import cl.deepsource.horas.domain.model.ProjectWithStats
import cl.deepsource.horas.domain.model.WorkTask

// HomeUiState: Fotografía lógica del Home: proyecto, tarea, tiempo, usuario, reloj y mensajes.
data class HomeUiState(
    // currentProject: Proyecto visible en tarjeta principal.
    val currentProject: Project? = null,
    // currentProjectWeeklyMinutes: Total semanal para el proyecto seleccionado.
    val currentProjectWeeklyMinutes: Long = 0L,
    // projects: Lista con proyectos y su historial resumido.
    val projects: List<ProjectWithStats> = emptyList(),
    // activeTask: Tarea actualmente en ejecución o en pausa.
    val activeTask: WorkTask? = null,
    // isWorkdayActive: Booleano derivado: tarea existe y no está pausada.
    val isWorkdayActive: Boolean = false,
    // activeTaskElapsedSeconds: Segundos a presentar en el cronómetro.
    val activeTaskElapsedSeconds: Long = 0L,
    // currentTimeString: Hora preparada para visualización.
    val currentTimeString: String = "",
    // currentDateString: Fecha preparada para visualización.
    val currentDateString: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userRole: String = "Usuario normal",
    val isLoading: Boolean = false,
    // message: Alerta transitoria que HomeRoute convierte en Toast.
    val message: String? = null,
)
