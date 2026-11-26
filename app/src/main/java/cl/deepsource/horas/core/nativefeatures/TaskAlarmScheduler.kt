package cl.deepsource.horas.core.nativefeatures

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import cl.deepsource.horas.appContainer
import cl.deepsource.horas.domain.model.WorkTask
import cl.deepsource.horas.domain.model.elapsedSeconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * V7: mantiene la lógica de aplazar 30 min de V3 y el receptor externo de V6.
 * Las alarmas Android no exactas pueden retrasarse en modo ahorro de batería.
 */
object TaskAlarmScheduler {
    private const val REQUEST_CODE = 7402
    private const val EXTEND_REQUEST_CODE = 7403
    const val ACTION_DUE = "cl.deepsource.horas.ACTION_TASK_DUE"
    const val ACTION_EXTEND = "cl.deepsource.horas.ACTION_EXTEND_30"
    const val EXTRA_TASK_ID = "task_id"
    private fun preferences(context: Context) = context.getSharedPreferences("deepsource_alarm_v5", Context.MODE_PRIVATE)
    private fun marker(task: WorkTask): String = "${task.id}:${task.extensionMinutes}:${task.nextReminderAtSeconds}"
    fun isDelivered(context: Context, task: WorkTask): Boolean =
        preferences(context).getString("lastDelivered", null) == marker(task)
    private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE, Intent(context, TaskDeadlineReceiver::class.java).apply { action = ACTION_DUE },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    /** Botón de +30 min incluso desde pantalla bloqueada. Se comprueba el ID de la tarea. */
    fun extendPendingIntent(context: Context, task: WorkTask): PendingIntent = PendingIntent.getBroadcast(
        context, EXTEND_REQUEST_CODE,
        Intent(context, TaskDeadlineReceiver::class.java).apply {
            action = ACTION_EXTEND
            putExtra(EXTRA_TASK_ID, task.id)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun stopSound(context: Context) {
        try { context.stopService(Intent(context, TaskAlarmSoundService::class.java)) }
        catch (_: Exception) { /* servicio ausente o restringido */ }
    }

    fun sync(context: Context, task: WorkTask?) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val deadline = pending(context)
        manager.cancel(deadline)
        val settings = AppSettingsStore(context)
        val notifications = TaskNotifications(context)
        if (task == null || !settings.notificationsEnabled || !notifications.hasPermission()) {
            notifications.clearTaskNotifications()
            stopSound(context)
            return
        }
        if (task.paused) {
            stopSound(context)
            notifications.showOngoingTask(task)
            return
        }
        if (preferences(context).getString("lastDelivered", null) == marker(task)) {
            // La alarma sigue pendiente de que el usuario pulse Finalizar o +30 min.
            notifications.showDeadline(task, silent = true)
            return
        }
        // Una tarea nueva o extendida nunca debe heredar el tono de la anterior.
        stopSound(context)
        notifications.showOngoingTask(task)
        val limit = task.nextReminderAtSeconds.takeIf { it > 0L }
            ?: (task.estimateMinutes + task.extensionMinutes) * 60L
        val delayMs = ((limit - task.elapsedSeconds()) * 1000L).coerceAtLeast(1000L)
        val target = System.currentTimeMillis() + delayMs
        // Exacta solo si Android concedió "Alarmas y recordatorios". Si no, respaldo inexacto.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, deadline)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, deadline)
        } else {
            manager.set(AlarmManager.RTC_WAKEUP, target, deadline)
        }
    }

    /** El receptor y la pantalla abierta comparten esta función. Solo suena una vez por extensión. */
    fun deliver(context: Context) {
        val task = context.appContainer().projectRepository.activeTask.value ?: return
        val settings = AppSettingsStore(context)
        val notifications = TaskNotifications(context)
        if (!settings.notificationsEnabled || !notifications.hasPermission() || task.paused) return
        val currentMarker = marker(task)
        if (preferences(context).getString("lastDelivered", null) == currentMarker) return
        val limit = task.nextReminderAtSeconds.takeIf { it > 0L }
            ?: (task.estimateMinutes + task.extensionMinutes) * 60L
        if (task.elapsedSeconds() < limit) { sync(context, task); return }
        preferences(context).edit().putString("lastDelivered", currentMarker).apply()
        // Primero se publica aviso nativo; si Android no deja arrancar un FGS desde el fondo,
        // el canal del sistema seguirá proporcionando la notificación y su sonido inicial.
        notifications.showDeadline(task, silent = false)
        try {
            ContextCompat.startForegroundService(context,
                Intent(context, TaskAlarmSoundService::class.java).apply {
                    action = TaskAlarmSoundService.ACTION_RING
                    putExtra(EXTRA_TASK_ID, task.id)
                })
        } catch (_: Exception) {
            // Restricciones de inicio en segundo plano (Android 12+).
        }
    }

}

class TaskDeadlineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            TaskAlarmScheduler.ACTION_DUE -> TaskAlarmScheduler.deliver(context)
            TaskAlarmScheduler.ACTION_EXTEND -> {
                val requestedId = intent.getStringExtra(TaskAlarmScheduler.EXTRA_TASK_ID) ?: return
                val result = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = context.appContainer().projectRepository
                        val task = repo.activeTask.value
                        if (task != null && task.id == requestedId) {
                            TaskAlarmScheduler.stopSound(context)
                            repo.extendActiveTask(30)
                        }
                    } finally { result.finish() }
                }
            }
        }
    }
}

/** Tras reiniciar, recupera la notificación/alarma de una tarea persistida. */
class TaskBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                intent?.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) {
            TaskAlarmScheduler.sync(context, context.appContainer().projectRepository.activeTask.value)
        }
    }
}
