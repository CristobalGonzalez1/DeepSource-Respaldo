package cl.deepsource.horas.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import cl.deepsource.horas.MainActivity
import cl.deepsource.horas.R
import cl.deepsource.horas.appContainer
import cl.deepsource.horas.domain.model.Project
import cl.deepsource.horas.domain.model.elapsedSeconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * [V4/WIDGET-MVVM] El widget es un cliente Android del mismo ProjectRepository que los ViewModels.
 * Mantiene SOLO la selección visual (proyecto/dificultad) en SharedPreferences por instancia.
 * Cronómetro, tareas y registros siguen perteneciendo al repositorio original: no se duplican.
 */
class DeepSourceAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
    }

    /** [V5/RESPONSIVE] Android llama al estirar/reducir el widget en la pantalla de inicio. */
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager,
                                           appWidgetId: Int, newOptions: android.os.Bundle) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions)
        render(context, manager, appWidgetId)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        super.onDeleted(context, ids)
        val editor = widgetPrefs(context).edit()
        ids.forEach { editor.remove("project_$it").remove("difficulty_$it").remove("task_$it") }
        editor.apply()
    }

    /** [V4/WIDGET-BOTONES] Broadcasts: cambiar proyecto, dificultad o controlar cronómetro. */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_NEXT_PROJECT, ACTION_NEXT_DIFFICULTY, ACTION_NEXT_TASK, ACTION_START, ACTION_TOGGLE -> {
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val repository = context.appContainer().projectRepository
                        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                        val prefs = widgetPrefs(context)
                        when (intent.action) {
                            ACTION_NEXT_PROJECT -> {
                                // [V4/PROYECTOS] Rota entre proyectos reales cargados, no una lista falsa.
                                if (repository.activeTask.value == null) {
                                    val projects = repository.projects.value
                                    if (projects.isNotEmpty()) {
                                        val current = selectedProject(context, widgetId, projects, repository.currentProject.value?.id)
                                        val idx = projects.indexOfFirst { it.id == current?.id }
                                        prefs.edit().putString("project_$widgetId", projects[(idx + 1) % projects.size].id)
                                            .remove("task_$widgetId").apply()
                                    }
                                }
                            }
                            ACTION_NEXT_DIFFICULTY -> {
                                // [V4/DIFICULTAD] Normal 90 min -> Media 120 min -> Compleja 240 min.
                                if (repository.activeTask.value == null) {
                                    val current = selectedDifficulty(context, widgetId)
                                    val index = LEVELS.indexOf(current)
                                    prefs.edit().putString("difficulty_$widgetId", LEVELS[(index + 1) % LEVELS.size]).apply()
                                }
                            }
                            ACTION_NEXT_TASK -> {
                                if (repository.activeTask.value == null) {
                                    val project = selectedProject(context, widgetId, repository.projects.value, repository.currentProject.value?.id)
                                    val tasks = repository.projectTasks.value.filter { it.projectId == project?.id }
                                    if (tasks.isNotEmpty()) {
                                        val current = prefs.getString("task_$widgetId", null)
                                        val idx = tasks.indexOfFirst { it.id == current }
                                        prefs.edit().putString("task_$widgetId", tasks[(idx + 1) % tasks.size].id).apply()
                                    }
                                }
                            }
                            ACTION_START -> {
                                // [V4/INICIAR] El proyecto y dificultad elegidos se pasan al mismo repositorio que usa Compose.
                                if (repository.activeTask.value == null) {
                                    val project = selectedProject(context, widgetId, repository.projects.value, repository.currentProject.value?.id)
                                    if (project != null) {
                                        val level = selectedDifficulty(context, widgetId)
                                        val estimate = ESTIMATES.getValue(level)
                                        repository.setCurrentProject(project.id)
                                        val chosen = selectedTask(context, widgetId, repository.projectTasks.value.filter { it.projectId == project.id })
                                        repository.startTask(project.id, chosen?.name ?: "Tarea rápida", level, estimate, false)
                                    }
                                }
                            }
                            ACTION_TOGGLE -> {
                                // [V4/PAUSA] Se aprovecha la pausa existente. No se crea otro contador.
                                if (repository.activeTask.value != null) repository.toggleTaskPause()
                            }
                        }
                        refreshAll(context)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    /** [V4/WIDGET-VISTA] Escribe el estado real del repositorio en RemoteViews. */
    private fun render(context: Context, manager: AppWidgetManager, id: Int) {
        val repository = context.appContainer().projectRepository
        val task = repository.activeTask.value
        val projects = repository.projects.value
        val project = if (task != null) projects.firstOrNull { it.id == task.projectId }
            else selectedProject(context, id, projects, repository.currentProject.value?.id)
        val difficulty = task?.difficulty ?: selectedDifficulty(context, id)
        val selectedTask = if (task == null) selectedTask(context, id,
            repository.projectTasks.value.filter { it.projectId == project?.id }) else null
        val minutes = if (task != null) task.estimateMinutes + task.extensionMinutes else ESTIMATES.getValue(difficulty)
        val views = RemoteViews(context.packageName, R.layout.widget_deepsource)

        // [V4/DESPLEGABLE-COMPACTO] RemoteViews no ofrece DropdownMenu de Compose.
        // Cada selector táctil muestra el valor actual y pasa al siguiente al tocar 'Cambiar'.
        views.setTextViewText(R.id.tv_widget_project_name,
            when { project != null -> project.name; projects.isEmpty() -> "Crea un proyecto en la app"; else -> "Selecciona un proyecto" })
        views.setTextViewText(R.id.tv_widget_difficulty, "$difficulty · $minutes min")
        views.setTextViewText(R.id.tv_widget_selected_task, task?.taskName ?: selectedTask?.name ?: "Crea una tarea en la app")
        views.setTextViewText(R.id.widget_action_task_change, if (task == null) "↻ Tarea" else "En uso")
        views.setTextViewText(R.id.tv_widget_task_status,
            when { task == null -> "Configura e inicia tu tarea"; task.paused -> "Pausada: ${task.taskName}"; else -> "En curso: ${task.taskName}" })
        views.setTextViewText(R.id.tv_widget_badge,
            when { task == null -> "● DISPONIBLE"; task.paused -> "● PAUSA"; else -> "● MIDIENDO" })

        // [V4/RELOJ] Android Chronometer anima sin wake-lock ni actualizaciones manuales por segundo.
        val elapsed = task?.elapsedSeconds() ?: 0L
        views.setChronometer(R.id.widget_chronometer, SystemClock.elapsedRealtime() - elapsed * 1000L,
            null, task != null && !task.paused)
        views.setTextViewText(R.id.widget_action_primary,
            when { task == null -> "▶ Iniciar"; task.paused -> "▶ Reanudar"; else -> "Ⅱ Pausar" })
        views.setTextViewText(R.id.widget_action_project_change, if (task == null) "Cambiar ›" else "En uso")
        views.setTextViewText(R.id.widget_action_difficulty_change, if (task == null) "Cambiar ›" else "En uso")

        // [V5/TAMAÑO] Modos pequeño/medio/grande: sin recortar cronómetro ni acciones.
        val options = manager.getAppWidgetOptions(id)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 280)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
        val compact = height < 200
        val tiny = height < 165
        views.setViewVisibility(R.id.widget_project_row, if (compact) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_difficulty_row, if (compact) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_task_row, if (height < 255) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.tv_widget_task_status, if (tiny) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_action_open, if (width < 210) View.GONE else View.VISIBLE)

        // [V4/PENDING-INTENT] requestCode distinto por widget y botón, evitando conflictos con otro widget.
        val prefix = id * 20
        views.setOnClickPendingIntent(R.id.widget_action_project_change,
            broadcast(context, id, prefix + 1, ACTION_NEXT_PROJECT))
        views.setOnClickPendingIntent(R.id.widget_action_difficulty_change,
            broadcast(context, id, prefix + 2, ACTION_NEXT_DIFFICULTY))
        views.setOnClickPendingIntent(R.id.widget_action_task_change,
            broadcast(context, id, prefix + 3, ACTION_NEXT_TASK))
        views.setOnClickPendingIntent(R.id.widget_action_primary,
            broadcast(context, id, prefix + 4, if (task == null) ACTION_START else ACTION_TOGGLE))

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val open = PendingIntent.getActivity(context, prefix + 5, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_action_open, open)
        // No enlazamos el root completo: los selectores deben recibir su propio click.
        manager.updateAppWidget(id, views)
    }

    private fun broadcast(context: Context, widgetId: Int, requestCode: Int, command: String): PendingIntent {
        val intent = Intent(context, DeepSourceAppWidget::class.java).apply {
            action = command
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        return PendingIntent.getBroadcast(context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        private val LEVELS = listOf("Normal", "Media", "Compleja")
        private val ESTIMATES = mapOf("Normal" to 90, "Media" to 120, "Compleja" to 240)
        private const val ACTION_NEXT_PROJECT = "cl.deepsource.horas.widget.NEXT_PROJECT"
        private const val ACTION_NEXT_DIFFICULTY = "cl.deepsource.horas.widget.NEXT_DIFFICULTY"
        private const val ACTION_NEXT_TASK = "cl.deepsource.horas.widget.NEXT_TASK"
        private const val ACTION_START = "cl.deepsource.horas.widget.START_TASK"
        private const val ACTION_TOGGLE = "cl.deepsource.horas.widget.TOGGLE_TASK"

        private fun widgetPrefs(context: Context) =
            context.getSharedPreferences("deepsource_widget_v4", Context.MODE_PRIVATE)

        /** [V4/SELECCION] Si se eliminó un proyecto, vuelve al actual o al primero disponible. */
        private fun selectedProject(context: Context, widgetId: Int, projects: List<Project>, currentId: String?): Project? {
            val saved = widgetPrefs(context).getString("project_$widgetId", null)
            return projects.firstOrNull { it.id == saved }
                ?: projects.firstOrNull { it.id == currentId }
                ?: projects.firstOrNull()
        }
        private fun selectedTask(context: Context, widgetId: Int, tasks: List<cl.deepsource.horas.domain.model.ProjectTask>): cl.deepsource.horas.domain.model.ProjectTask? {
            val id = widgetPrefs(context).getString("task_$widgetId", null)
            return tasks.firstOrNull { it.id == id } ?: tasks.firstOrNull()
        }
        private fun selectedDifficulty(context: Context, widgetId: Int): String {
            val name = widgetPrefs(context).getString("difficulty_$widgetId", "Normal")
            return name?.takeIf { it in LEVELS } ?: "Normal"
        }

        /** [V4/ACTUALIZAR] Llamado por el repositorio tras cambios persistidos. */
        fun sendUpdateBroadcast(context: Context) {
            context.sendBroadcast(Intent(context, DeepSourceAppWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    AppWidgetManager.getInstance(context)
                        .getAppWidgetIds(ComponentName(context, DeepSourceAppWidget::class.java)))
            })
        }
        private fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DeepSourceAppWidget::class.java))
            DeepSourceAppWidget().onUpdate(context, manager, ids)
        }
    }
}
