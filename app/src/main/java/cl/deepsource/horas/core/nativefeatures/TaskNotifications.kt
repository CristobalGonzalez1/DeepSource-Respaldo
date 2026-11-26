package cl.deepsource.horas.core.nativefeatures

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import cl.deepsource.horas.MainActivity
import cl.deepsource.horas.R
import cl.deepsource.horas.domain.model.WorkTask
import cl.deepsource.horas.domain.model.elapsedSeconds
import cl.deepsource.horas.navigation.Routes

/** Notificaciones del cronómetro y del plazo vencido, con acción +30 min desde afuera de la app. */
class TaskNotifications(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val settings = AppSettingsStore(context)
    companion object {
        const val DEADLINE_ID = 72500
        const val RUNNING_ID = 72501
        private const val RUNNING_CHANNEL = "active_timer_v7"
    }

    fun hasPermission(): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Android controla los sonidos de un canal existente; no cambiamos los ajustes elegidos por el usuario. */
    fun ensureDeadlineChannel(): String {
        val id = settings.channelId
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(id) == null) {
            val channel = NotificationChannel(id, "Alarmas de tareas DeepSource", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Avisos cuando una tarea llega a su límite de tiempo"
                enableVibration(settings.vibrationEnabled)
                if (settings.vibrationEnabled) vibrationPattern = longArrayOf(0, 280, 180, 280, 800)
                if (settings.soundEnabled) {
                    // Tono de sistema para el caso en que Android impida el servicio de alarma persistente.
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                } else setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }
        return id
    }

    private fun ensureRunningChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(RUNNING_CHANNEL) == null) {
            manager.createNotificationChannel(NotificationChannel(
                RUNNING_CHANNEL, "Cronómetro en curso", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Muestra el tiempo restante y el botón para extender 30 minutos"
                setSound(null, null)
                enableVibration(false)
            })
        }
    }

    fun soundSettingsIntent(): Intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        putExtra(Settings.EXTRA_CHANNEL_ID, ensureDeadlineChannel())
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun openTaskIntent(route: String): PendingIntent = PendingIntent.getActivity(
        context, if (route == Routes.ALERT) 8332 else 8331,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NOTIFICATION_ROUTE, route)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun extendAction(task: WorkTask) = NotificationCompat.Action.Builder(
        R.drawable.ic_notification, "+30 min", TaskAlarmScheduler.extendPendingIntent(context, task)).build()

    /** El sistema renderiza el conteo regresivo sin que Compose permanezca abierto. */
    fun showOngoingTask(task: WorkTask) {
        if (!settings.notificationsEnabled || !hasPermission()) return
        ensureRunningChannel()
        val remaining = (task.nextReminderAtSeconds.takeIf { it > 0L }
            ?: (task.estimateMinutes + task.extensionMinutes) * 60L) - task.elapsedSeconds()
        val builder = NotificationCompat.Builder(context, RUNNING_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (task.paused) "DeepSource · Cronómetro pausado" else "DeepSource · Tiempo en curso")
            .setContentText("${task.projectName}: ${task.taskName}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${task.projectName} · ${task.taskName}"))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openTaskIntent(Routes.RUNNING))
            .addAction(extendAction(task))
            .setOngoing(true).setAutoCancel(false).setOnlyAlertOnce(true).setSilent(true)
        if (!task.paused) {
            builder.setWhen(System.currentTimeMillis() + remaining.coerceAtLeast(0L) * 1000L)
                .setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
        } else builder.setShowWhen(false)
        manager.cancel(DEADLINE_ID)
        manager.notify(RUNNING_ID, builder.build())
    }

    /** Aviso del plazo con botón +30 y apertura de la pantalla V3 al pulsarlo. */
    fun buildDeadline(task: WorkTask, silent: Boolean): android.app.Notification {
        ensureDeadlineChannel()
        val builder = NotificationCompat.Builder(context, settings.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("DeepSource · ¡Tiempo estimado alcanzado!")
            .setContentText("${task.taskName} · Finaliza o extiende 30 minutos")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${task.projectName}: ${task.taskName}. Finaliza o extiende 30 minutos."))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openTaskIntent(Routes.ALERT))
            .addAction(extendAction(task))
            .setOngoing(true).setOnlyAlertOnce(true).setAutoCancel(false)
            .setWhen(task.originalStartEpochMs)
            .setUsesChronometer(true)
        if (silent) builder.setSilent(true)
        return builder.build()
    }

    fun showDeadline(task: WorkTask, silent: Boolean) {
        if (!settings.notificationsEnabled || !hasPermission()) return
        manager.cancel(RUNNING_ID)
        manager.notify(DEADLINE_ID, buildDeadline(task, silent))
    }

    fun clearTaskNotifications() {
        manager.cancel(RUNNING_ID)
        manager.cancel(DEADLINE_ID)
    }

    // Mantener compatibilidad con Home y Settings de V6.
    fun postDeadline(taskName: String) = notify("Tiempo estimado alcanzado", "Revisa la tarea: $taskName", true)
    fun post(title: String, detail: String) = notify(title, detail, false)

    private fun notify(title: String, detail: String, deadline: Boolean) {
        if (!settings.notificationsEnabled || !hasPermission()) return
        val channel = if (deadline) ensureDeadlineChannel() else "task_updates"
        if (!deadline && Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(channel) == null) {
            manager.createNotificationChannel(NotificationChannel(channel, "Actividad de tareas", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val notif = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(detail)
            .setCategory(if (deadline) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_STATUS)
            .setPriority(if (deadline) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openTaskIntent(if (deadline) Routes.ALERT else Routes.HOME))
            .setAutoCancel(true)
            .build()
        manager.notify(if (deadline) 72503 else (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notif)
    }
}
