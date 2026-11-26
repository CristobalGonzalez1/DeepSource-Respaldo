package cl.deepsource.horas.core.nativefeatures

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import cl.deepsource.horas.appContainer
import cl.deepsource.horas.domain.model.WorkTask
import cl.deepsource.horas.domain.model.elapsedSeconds

/**
 * [V5/ALARMA] Usa AlarmManager para avisar incluso cuando la Activity está cerrada.
 * Sin el permiso especial de alarmas exactas, Android puede diferir el aviso en modo ahorro/Doze.
 * Cada pausa, reanudación, aplazamiento y finalización cancela/recalcula su próxima ejecución.
 */
object TaskAlarmScheduler {
    private const val REQUEST_CODE = 7402
    const val ACTION_DUE = "cl.deepsource.horas.ACTION_TASK_DUE"
    private fun preferences(context: Context) = context.getSharedPreferences("deepsource_alarm_v5", Context.MODE_PRIVATE)
    private fun marker(task: WorkTask): String = "${task.id}:${task.extensionMinutes}:${task.nextReminderAtSeconds}"
    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE, Intent(context, TaskDeadlineReceiver::class.java).apply { action = ACTION_DUE },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun sync(context: Context, task: WorkTask?) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pending(context)
        manager.cancel(pending)
        if (task == null || task.paused || !AppSettingsStore(context).notificationsEnabled) return
        if (preferences(context).getString("lastDelivered", null) == marker(task)) return
        val limit = task.nextReminderAtSeconds.takeIf { it > 0L }
            ?: (task.estimateMinutes + task.extensionMinutes) * 60L
        val delayMs = ((limit - task.elapsedSeconds()) * 1000L).coerceAtLeast(1000L)
        val target = System.currentTimeMillis() + delayMs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pending)
        } else {
            manager.set(AlarmManager.RTC_WAKEUP, target, pending)
        }
    }

    fun deliver(context: Context) {
        val task = context.appContainer().projectRepository.activeTask.value ?: return
        val marker = marker(task)
        if (task.paused || preferences(context).getString("lastDelivered", null) == marker) return
        val limit = task.nextReminderAtSeconds.takeIf { it > 0L }
            ?: (task.estimateMinutes + task.extensionMinutes) * 60L
        if (task.elapsedSeconds() < limit) { sync(context, task); return }
        preferences(context).edit().putString("lastDelivered", marker).apply()
        sync(context, task)
        TaskNotifications(context).postDeadline(task.taskName)
    }
}

/** Receptor Android independiente de pantallas Compose y del proceso visible. */
class TaskDeadlineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == TaskAlarmScheduler.ACTION_DUE) TaskAlarmScheduler.deliver(context)
    }
}

/** Tras reiniciar el celular se reprograma el aviso de la tarea persistida. */
class TaskBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            TaskAlarmScheduler.sync(context, context.appContainer().projectRepository.activeTask.value)
        }
    }
}
