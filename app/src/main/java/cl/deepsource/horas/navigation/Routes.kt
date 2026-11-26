package cl.deepsource.horas.navigation
// [GUIA/ARCHIVO] Constantes con nombres de las rutas; AppNavHost define qué pantalla muestra cada una.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/navigation/Routes.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

object Routes {
    // [GUIA/DETALLE] Nombre interno de ruta para la pantalla de ingreso.
    const val LOGIN = "login"
    // [GUIA/DETALLE] Ruta que utiliza el enlace ¿Olvidaste tu contraseña?.
    const val FORGOT_PASSWORD = "forgot_password"
    // [GUIA/DETALLE] Ruta de pantalla Solicitar acceso.
    const val REQUEST_ACCESS = "request_access"
    // [GUIA/DETALLE] Ruta de la pantalla principal después del ingreso.
    const val HOME = "home"

    // [FLUJO/RUTAS] Vistas nuevas Material v2, conservando las rutas anteriores.
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
}
