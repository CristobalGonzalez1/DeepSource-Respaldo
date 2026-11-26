package cl.deepsource.horas.domain.logic

import cl.deepsource.horas.domain.model.WorkTask

/** Reglas del cronómetro compartidas por la app y el widget, sin depender de Android. */
object TimerRules {
    fun elapsedSeconds(task: WorkTask, nowMs: Long): Long =
        task.accumulatedSeconds + if (task.paused) 0L else
            ((nowMs - task.startEpochMs) / 1000L).coerceAtLeast(0L)

    fun togglePause(task: WorkTask, nowMs: Long): WorkTask =
        if (task.paused) task.copy(paused = false, startEpochMs = nowMs)
        else task.copy(paused = true, accumulatedSeconds = elapsedSeconds(task, nowMs))

    fun extend(task: WorkTask, minutes: Int, nowMs: Long): WorkTask {
        require(minutes in 1..240) { "Extensión fuera de rango" }
        return task.copy(
            extensionMinutes = task.extensionMinutes + minutes,
            nextReminderAtSeconds = if (task.demoReminder) elapsedSeconds(task, nowMs) + 15L
                else (task.estimateMinutes + task.extensionMinutes + minutes) * 60L,
        )
    }

    fun reminderSeconds(task: WorkTask): Long =
        if (task.nextReminderAtSeconds > 0L) task.nextReminderAtSeconds
        else (task.estimateMinutes + task.extensionMinutes).toLong() * 60L

    fun isDue(task: WorkTask, nowMs: Long): Boolean =
        elapsedSeconds(task, nowMs) >= reminderSeconds(task)

    fun remainingSeconds(task: WorkTask, nowMs: Long): Long =
        (reminderSeconds(task) - elapsedSeconds(task, nowMs)).coerceAtLeast(0L)
}
