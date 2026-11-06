package cl.deepsource.horas.domain.model

data class Project(
    val id: String,
    val name: String,
    val description: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

data class ProjectWithStats(
    val project: Project,
    val totalLoggedMinutes: Long,
    val isCurrent: Boolean,
)

data class WorkTask(
    val id: String,
    val projectId: String,
    val projectName: String,
    val taskName: String,
    val startEpochMs: Long,
    val paused: Boolean = false,
    val accumulatedSeconds: Long = 0L,
)

data class TimeLog(
    val id: String,
    val projectId: String,
    val durationMinutes: Long,
    val note: String = "",
    val timestampEpochMs: Long = System.currentTimeMillis(),
)
