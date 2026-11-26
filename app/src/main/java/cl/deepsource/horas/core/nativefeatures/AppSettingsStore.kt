package cl.deepsource.horas.core.nativefeatures

import android.content.Context

/** [V5/AJUSTES] Preferencias locales: no requieren Internet ni cuenta remota. */
class AppSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("deepsource_settings_v5", Context.MODE_PRIVATE)
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications", false)
        set(value) { prefs.edit().putBoolean("notifications", value).apply() }
    /** Pregunta solo tras el primer inicio de sesión; un no no activa avisos. */
    var notificationQuestionAnswered: Boolean
        get() = prefs.getBoolean("notification_question_answered", false)
        set(value) { prefs.edit().putBoolean("notification_question_answered", value).apply() }
    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration", true)
        set(value) { prefs.edit().putBoolean("vibration", value).apply() }
    var classicAlarmTone: Boolean
        get() = prefs.getBoolean("classic_alarm", true)
        set(value) { prefs.edit().putBoolean("classic_alarm", value).apply() }
    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) { prefs.edit().putBoolean("sound", value).apply() }
    val channelId: String get() = "deadline_v7_${if (soundEnabled) "sound" else "silent"}_${if (vibrationEnabled) "vibrate" else "still"}"
}
