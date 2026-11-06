package cl.deepsource.horas.feature.home

import cl.deepsource.horas.domain.model.Project
import cl.deepsource.horas.domain.model.ProjectWithStats
import cl.deepsource.horas.domain.model.WorkTask

data class HomeUiState(
    val currentProject: Project? = null,
    val currentProjectWeeklyMinutes: Long = 0L,
    val projects: List<ProjectWithStats> = emptyList(),
    val activeTask: WorkTask? = null,
    val isWorkdayActive: Boolean = false,
    val activeTaskElapsedSeconds: Long = 0L,
    val currentTimeString: String = "",
    val currentDateString: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
)
