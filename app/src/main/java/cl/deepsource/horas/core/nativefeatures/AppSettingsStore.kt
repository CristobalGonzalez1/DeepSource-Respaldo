package cl.deepsource.horas.core.nativefeatures

import android.content.Context

/** [V5/AJUSTES] Preferencias locales: no requieren Internet ni cuenta remota. */
class AppSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("deepsource_settings_v5", Context.MODE_PRIVATE)
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications", true)
        set(value) { prefs.edit().putBoolean("notifications", value).apply() }
    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration", true)
        set(value) { prefs.edit().putBoolean("vibration", value).apply() }
    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) { prefs.edit().putBoolean("sound", value).apply() }
    val channelId: String get() = "deadline_${if (soundEnabled) "sound" else "silent"}_${if (vibrationEnabled) "vibrate" else "still"}"
}
