package cl.deepsource.horas.feature.home
// [GUIA/ARCHIVO] Estado listo para que la vista lo dibuje sin cálculos complejos.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/feature/home/HomeUiState.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import cl.deepsource.horas.domain.model.Project
import cl.deepsource.horas.domain.model.ProjectWithStats
import cl.deepsource.horas.domain.model.WorkTask

// [GUIA/TIPO] HomeUiState: Fotografía lógica del Home: proyecto, tarea, tiempo, usuario, reloj y mensajes.
data class HomeUiState(
    // [GUIA/DATO] currentProject: Proyecto visible en tarjeta principal.
    val currentProject: Project? = null,
    // [GUIA/DATO] currentProjectWeeklyMinutes: Total semanal para el proyecto seleccionado.
    val currentProjectWeeklyMinutes: Long = 0L,
    // [GUIA/DATO] projects: Lista con proyectos y su historial resumido.
    val projects: List<ProjectWithStats> = emptyList(),
    // [GUIA/DATO] activeTask: Tarea actualmente en ejecución o en pausa.
    val activeTask: WorkTask? = null,
    // [GUIA/DATO] isWorkdayActive: Booleano derivado: tarea existe y no está pausada.
    val isWorkdayActive: Boolean = false,
    // [GUIA/DATO] activeTaskElapsedSeconds: Segundos a presentar en el cronómetro.
    val activeTaskElapsedSeconds: Long = 0L,
    // [GUIA/DATO] currentTimeString: Hora preparada para visualización.
    val currentTimeString: String = "",
    // [GUIA/DATO] currentDateString: Fecha preparada para visualización.
    val currentDateString: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userRole: String = "Usuario normal",
    val isLoading: Boolean = false,
    // [GUIA/DATO] message: Alerta transitoria que HomeRoute convierte en Toast.
    val message: String? = null,
)
