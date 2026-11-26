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

/** Notificaciones Android: canal configurable, sonido del sistema y vibración opcional. */
class TaskNotifications(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val settings = AppSettingsStore(context)

    fun hasPermission(): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Cada combinación usa su canal; Android no permite editar canales ya creados desde código. */
    fun ensureDeadlineChannel(): String {
        val id = settings.channelId
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(id) == null) {
            val channel = NotificationChannel(id, "Avisos de tiempo DeepSource", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notificaciones cuando una tarea alcanza su tiempo estimado"
                enableVibration(settings.vibrationEnabled)
                if (settings.vibrationEnabled) vibrationPattern = longArrayOf(0, 250, 140, 300)
                if (settings.soundEnabled) {
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
                } else setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }
        return id
    }

    /** Abre el canal del sistema: se puede elegir un sonido distinto por app. */
    fun soundSettingsIntent(): Intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        putExtra(Settings.EXTRA_CHANNEL_ID, ensureDeadlineChannel())
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun postDeadline(taskName: String) = notify("Tiempo estimado alcanzado", "Revisa la tarea: $taskName", true)
    fun post(title: String, detail: String) = notify(title, detail, false)

    private fun notify(title: String, detail: String, deadline: Boolean) {
        if (!settings.notificationsEnabled || !hasPermission()) return
        val channel = if (deadline) ensureDeadlineChannel() else "task_updates"
        if (!deadline && Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(channel) == null) {
            manager.createNotificationChannel(NotificationChannel(channel, "Actividad de tareas", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setCategory(if (deadline) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_STATUS)
            .setPriority(if (deadline) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(if (deadline) 72500 else (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}
