package cl.deepsource.horas.domain.logic

import cl.deepsource.horas.domain.model.TimeLog
import cl.deepsource.horas.domain.model.WorkTask

/** Validaciones y cálculos usados al guardar horas trabajadas. */
object RegistrationRules {
    fun validateManual(minutes: Long, note: String): String? = when {
        minutes !in 1L..1439L -> "La duración debe estar entre 1 y 1439 minutos"
        note.length > 300 -> "Máximo 300 caracteres"
        else -> null
    }

    fun fromHoursAndMinutes(hours: Int, minutes: Int): Long {
        require(hours in 0..23) { "Horas fuera de rango" }
        require(minutes in 0..59) { "Minutos fuera de rango" }
        val total = hours * 60L + minutes
        require(total > 0L) { "Ingresa un tiempo mayor a cero" }
        return total
    }

    fun timerMinutes(elapsedSeconds: Long): Long = (elapsedSeconds / 60L).coerceAtLeast(1L)

    fun noteForTimer(note: String, task: WorkTask): String = note.trim().ifBlank { task.taskName }

    fun projectMinutes(projectId: String, logs: List<TimeLog>): Long =
        logs.filter { it.projectId == projectId }.sumOf { it.durationMinutes }

    fun weeklyMinutes(projectId: String, weekStartEpochMs: Long, logs: List<TimeLog>): Long =
        logs.filter { it.projectId == projectId && it.timestampEpochMs >= weekStartEpochMs }
            .sumOf { it.durationMinutes }

    fun logsForProject(projectId: String, logs: List<TimeLog>): List<TimeLog> =
        logs.filter { it.projectId == projectId }.sortedByDescending { it.timestampEpochMs }
}
