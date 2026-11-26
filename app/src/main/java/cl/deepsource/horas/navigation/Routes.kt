package cl.deepsource.horas.navigation

object Routes {
    // Nombre interno de ruta para la pantalla de ingreso.
    const val LOGIN = "login"
    // Ruta que utiliza el enlace ¿Olvidaste tu contraseña?.
    const val FORGOT_PASSWORD = "forgot_password"
    // Ruta de pantalla Solicitar acceso.
    const val REQUEST_ACCESS = "request_access"
    // Ruta de la pantalla principal después del ingreso.
    const val HOME = "home"

    // Vistas nuevas Material v2, conservando las rutas anteriores.
    const val START = "start_task"
    const val RUNNING = "running_task"
    const val ALERT = "task_alert"
    const val FINISH = "finish_task"
    const val SAVED = "saved_log"
    const val DETAIL = "project_detail"
    const val MANAGER = "manager"
    const val ASSIGNMENTS = "assignments"
    const val HISTORY = "history"
    const val MANUAL = "manual_log"
    const val REPORT = "time_report"
    const val SETTINGS = "app_settings"
    const val PROFILE = "user_profile"
}
