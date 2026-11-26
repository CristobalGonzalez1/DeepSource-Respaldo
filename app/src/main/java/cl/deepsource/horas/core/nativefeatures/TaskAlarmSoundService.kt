package cl.deepsource.horas.core.nativefeatures

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import cl.deepsource.horas.R
import cl.deepsource.horas.appContainer
import cl.deepsource.horas.domain.model.elapsedSeconds

/**
 * Repite un bip-bip clásico y vibración hasta que el usuario use +30 min o Finalizar.
 * No fuerza una Activity encima de la pantalla bloqueada: solo notificación del sistema.
 */
class TaskAlarmSoundService : Service() {
    companion object { const val ACTION_RING = "cl.deepsource.horas.RING_DEADLINE" }
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val task = appContainer().projectRepository.activeTask.value
        val settings = AppSettingsStore(this)
        if (task == null || task.paused || !settings.notificationsEnabled ||
            !TaskNotifications(this).hasPermission() ||
            !TaskAlarmScheduler.isDelivered(this, task) ||
            task.elapsedSeconds() < (task.nextReminderAtSeconds.takeIf { it > 0L }
                ?: (task.estimateMinutes + task.extensionMinutes) * 60L) ||
            (intent?.getStringExtra(TaskAlarmScheduler.EXTRA_TASK_ID) != null &&
             intent.getStringExtra(TaskAlarmScheduler.EXTRA_TASK_ID) != task.id)) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val notification = TaskNotifications(this).buildDeadline(task, silent = true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(TaskNotifications.DEADLINE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else startForeground(TaskNotifications.DEADLINE_ID, notification)
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (settings.soundEnabled && player == null) {
            try {
                val url = if (settings.classicAlarmTone)
                    Uri.parse("android.resource://$packageName/${R.raw.deepsource_classic_beep}")
                else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                player = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    setDataSource(this@TaskAlarmSoundService, url)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (_: Exception) { player?.release(); player = null }
        }
        if (settings.vibrationEnabled && vibrator == null) {
            try {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    (getSystemService(VIBRATOR_SERVICE) as Vibrator)
                }
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 170, 250, 850), 0))
            } catch (_: Exception) { }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        try { player?.stop() } catch (_: Exception) { }
        try { player?.release() } catch (_: Exception) { }
        player = null
        try { vibrator?.cancel() } catch (_: Exception) { }
        vibrator = null
        super.onDestroy()
    }
}
