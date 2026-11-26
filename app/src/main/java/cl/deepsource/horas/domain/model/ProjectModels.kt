package cl.deepsource.horas.domain.model

// Project: Datos básicos de un proyecto.
data class Project(
    // id: Identificador único (UUID o texto) para poder relacionar proyectos y tareas.
    val id: String,
    val name: String,
    val description: String = "",
    // createdAtEpochMs: Fecha de creación como milisegundos desde epoch.
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

// ProjectWithStats: Combina un proyecto con sus minutos totales y si está seleccionado.
data class ProjectWithStats(
    val project: Project,
    val totalLoggedMinutes: Long,
    val isCurrent: Boolean,
)

// WorkTask: Estado de cronómetro: proyecto, nombre, instante de inicio, pausa y segundos acumulados.
data class WorkTask(
    // id: Identificador único (UUID o texto) para poder relacionar proyectos y tareas.
    val id: String,
    val projectId: String,
    val projectName: String,
    val taskName: String,
    val startEpochMs: Long,
    // paused: Indica si hay cronómetro en pausa.
    val paused: Boolean = false,
    // accumulatedSeconds: Segundos ya contados antes de la última pausa.
    val accumulatedSeconds: Long = 0L,
    // Opciones nuevas; los cronómetros antiguos usan los valores por defecto.
    val difficulty: String = "Normal",
    val estimateMinutes: Int = 90,
    val extensionMinutes: Int = 0,
    val originalStartEpochMs: Long = startEpochMs,
    val demoReminder: Boolean = false,
    // Segundo objetivo del siguiente aviso. Se reinicia al usar +30 min.
    val nextReminderAtSeconds: Long = 0L,
)

// TimeLog: Registro histórico de minutos cargados manualmente o al detener un cronómetro.
data class TimeLog(
    // id: Identificador único (UUID o texto) para poder relacionar proyectos y tareas.
    val id: String,
    val projectId: String,
    // durationMinutes: Duración consolidada que se guarda en el historial.
    val durationMinutes: Long,
    val note: String = "",
    val timestampEpochMs: Long = System.currentTimeMillis(),
    // Se conserva durationMinutes para las pantallas originales.
    val durationSeconds: Long = durationMinutes * 60L,
    val taskName: String = "Registro de horas",
    val difficulty: String = "Normal",
    val estimateMinutes: Int = 90,
    val source: String = "manual",
    val employeeName: String = "Luis Gómez",
)

/** [V5/TAREAS] Catálogo propio de tareas por proyecto; no representa una sesión de cronómetro. */
data class ProjectTask(
    val id: String,
    val projectId: String,
    val name: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val estimateMinutes: Int = 90,
)
