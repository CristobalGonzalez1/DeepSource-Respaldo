package cl.deepsource.horas.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import cl.deepsource.horas.MainActivity
import cl.deepsource.horas.R
import cl.deepsource.horas.appContainer
import cl.deepsource.horas.domain.model.Project
import cl.deepsource.horas.domain.model.ProjectTask
import cl.deepsource.horas.domain.model.elapsedSeconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * V8: Widget doble SIN borrar sus funciones originales.
 * Modo Cronómetro: seleccionar proyecto/tarea desplazando la lista, dificultad, iniciar,
 * pausar, reanudar y extender 30 minutos. Modo Registrar: registrar horas de hoy
 * directamente en el MISMO ProjectRepository que alimenta Historial.
 *
 * Los widgets Android RemoteViews no admiten campos de texto libre ni gestos horizontales
 * fiables entre paneles (el launcher intercepta el deslizamiento); se usan pestañas
 * y una lista vertical desplazable de proyectos + tareas del proyecto seleccionado.
 */
class DeepSourceAppWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager,
        appWidgetId: Int, newOptions: android.os.Bundle) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, newOptions)
        render(context, manager, appWidgetId)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        super.onDeleted(context, ids)
        val edit = widgetPrefs(context).edit()
        ids.forEach {
            edit.remove("project_$it").remove("difficulty_$it").remove("task_$it")
                .remove("mode_$it").remove("minutes_$it").remove("picker_$it")
        }
        edit.apply()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action !in ACTIONS) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID)
                if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return@launch
                if (context.appContainer().authRepository.currentUser.value == null) {
                    showToast(context, "Inicia sesión en DeepSource para usar el widget")
                    refreshAll(context)
                    return@launch
                }
                val repository = context.appContainer().projectRepository
                val prefs = widgetPrefs(context)
                val edit = prefs.edit()
                when (intent.action) {
                    ACTION_SELECT -> {
                        // La fila proporciona el ID real del proyecto/tarea, nunca su posición.
                        val itemId = intent.getStringExtra(EXTRA_ITEM_ID).orEmpty()
                        val manualMode = prefs.getString("mode_$widgetId", "timer") == "manual"
                        if (repository.activeTask.value != null && !manualMode) {
                            showToast(context, "Cronómetro activo: usa Registrar para elegir otro proyecto")
                        } else when (intent.getStringExtra(EXTRA_ITEM_TYPE)) {
                            "project" -> {
                                if (repository.projects.value.any { it.id == itemId }) {
                                    edit.putString("project_$widgetId", itemId)
                                        .remove("task_$widgetId").apply()
                                }
                            }
                            "task" -> {
                                val selectedProject = selectedProject(context, widgetId,
                                    repository.projects.value, repository.currentProject.value?.id)
                                val projectTask = repository.projectTasks.value.firstOrNull {
                                    it.id == itemId && it.projectId == selectedProject?.id
                                }
                                if (projectTask != null) edit.putString("task_$widgetId", itemId).putBoolean("picker_$widgetId", false).apply()
                            }
                        }
                    }
                    ACTION_PICKER -> {
                        val wasOpen = prefs.getBoolean("picker_$widgetId", false)
                        edit.putBoolean("picker_$widgetId", !wasOpen).apply()
                    }
                    ACTION_MODE_TIMER -> edit.putString("mode_$widgetId", "timer").apply()
                    ACTION_MODE_MANUAL -> edit.putString("mode_$widgetId", "manual").apply()
                    ACTION_NEXT_DIFFICULTY -> {
                        if (repository.activeTask.value == null || prefs.getString("mode_$widgetId", "timer") == "manual") {
                            val index = LEVELS.indexOf(selectedDifficulty(context, widgetId))
                            edit.putString("difficulty_$widgetId", LEVELS[(index + 1) % LEVELS.size]).apply()
                        }
                    }
                    ACTION_START -> {
                        if (repository.activeTask.value == null) {
                            val project = selectedProject(context, widgetId,
                                repository.projects.value, repository.currentProject.value?.id)
                            val task = project?.let {
                                selectedTask(context, widgetId,
                                    repository.projectTasks.value.filter { t -> t.projectId == it.id })
                            }
                            if (project == null || task == null) {
                                showToast(context, "Selecciona un proyecto y una tarea")
                            } else {
                                repository.startTask(project.id, task.name, "Programada",
                                    task.estimateMinutes, false)
                                    .onFailure { showToast(context, it.message ?: "No se pudo iniciar") }
                            }
                        }
                    }
                    ACTION_TOGGLE -> repository.toggleTaskPause()
                        .onFailure { showToast(context, it.message ?: "No se pudo pausar") }
                    ACTION_EXTEND -> repository.extendActiveTask(30)
                        .onSuccess { showToast(context, "Plazo extendido en 30 minutos") }
                        .onFailure { showToast(context, it.message ?: "Sin tarea activa") }
                    ACTION_HPLUS, ACTION_HMINUS, ACTION_MPLUS, ACTION_MMINUS -> {
                        val delta = when (intent.action) {
                            ACTION_HPLUS -> 60
                            ACTION_HMINUS -> -60
                            ACTION_MPLUS -> 5
                            else -> -5
                        }
                        val previous = prefs.getInt("minutes_$widgetId", 60)
                        // Respeta la validación 1..1439 minutos del repositorio.
                        edit.putInt("minutes_$widgetId", (previous + delta).coerceIn(0, 1439)).apply()
                    }
                    ACTION_SAVE -> {
                        if (!saving.compareAndSet(false, true)) return@launch
                        try {
                            val project = selectedProject(context, widgetId,
                                repository.projects.value, repository.currentProject.value?.id)
                            val selected = project?.let { p -> selectedTask(context, widgetId,
                                repository.projectTasks.value.filter { it.projectId == p.id }) }
                            val minutes = prefs.getInt("minutes_$widgetId", 60)
                            when {
                                project == null || selected == null -> showToast(context, "Selecciona proyecto y tarea")
                                minutes !in 1..1439 -> showToast(context, "Agrega al menos 5 minutos")
                                else -> repository.logHours(project.id, minutes.toLong(),
                                    "Registro rápido desde widget", selected.name,
                                    selectedDifficulty(context, widgetId), System.currentTimeMillis())
                                    .onSuccess {
                                        showToast(context, "${minutes} min registrados en Historial")
                                        edit.putInt("minutes_$widgetId", 60).apply()
                                    }
                                    .onFailure { showToast(context, it.message ?: "No se pudo registrar") }
                            }
                        } finally { saving.set(false) }
                    }
                }
                refreshAll(context)
            } finally { pending.finish() }
        }
    }

    private fun render(context: Context, manager: AppWidgetManager, id: Int) {
        val repository = context.appContainer().projectRepository
        val task = repository.activeTask.value
        val projects = repository.projects.value
        val selected = selectedProject(context, id, projects, repository.currentProject.value?.id)
        val selectedTask = selected?.let { p -> selectedTask(context, id,
            repository.projectTasks.value.filter { it.projectId == p.id }) }
        val estimate = task?.estimateMinutes ?: selectedTask?.estimateMinutes ?: 90
        val isManual = widgetPrefs(context).getString("mode_$id", "timer") == "manual"
        val manualMinutes = widgetPrefs(context).getInt("minutes_$id", 60).coerceIn(0, 1439)
        val height = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 320)
        val expanded = height >= 180
        val pickerOpen = widgetPrefs(context).getBoolean("picker_$id", false) && expanded
        val views = RemoteViews(context.packageName, R.layout.widget_deepsource)
        if (context.appContainer().authRepository.currentUser.value == null) {
            // Después de Cerrar sesión, no mostrar proyectos/tareas ni aceptar acciones del widget.
            views.setTextViewText(R.id.tv_widget_badge, "● SIN SESIÓN")
            views.setTextViewText(R.id.tv_widget_project_name, "Inicia sesión para continuar")
            views.setTextViewText(R.id.tv_widget_selected_task, "Tu perfil se recuperará al ingresar")
            views.setViewVisibility(R.id.widget_tabs, View.GONE)
            views.setViewVisibility(R.id.widget_collection_container, View.GONE)
            views.setViewVisibility(R.id.widget_expand_hint, View.GONE)
            views.setViewVisibility(R.id.widget_timer_panel, View.GONE)
            views.setViewVisibility(R.id.widget_manual_panel, View.GONE)
            views.setViewVisibility(R.id.widget_action_add_task, View.GONE)
            // Acceso persistente incluso con la sesión cerrada.
            views.setViewVisibility(R.id.widget_open_logged_out, View.VISIBLE)
            views.setOnClickPendingIntent(R.id.widget_open_logged_out,
                PendingIntent.getActivity(context, 980000 + id,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        data = Uri.parse("deepsource://widget/login/$id")
                    }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            manager.updateAppWidget(id, views)
            return
        }
        views.setViewVisibility(R.id.widget_open_logged_out, View.GONE)

        views.setTextViewText(R.id.tv_widget_project_name,
            if (!isManual && task != null) task.projectName else
                selected?.name ?: "Sin proyectos: crea uno en DeepSource")
        views.setTextViewText(R.id.tv_widget_selected_task,
            if (!isManual && task != null) task.taskName else
                selectedTask?.name ?: "Sin tarea: añade una nueva")
        views.setTextViewText(R.id.tv_widget_badge,
            when { task == null -> "● DISPONIBLE"; task.paused -> "● PAUSA"; else -> "● MIDIENDO" })
        views.setTextViewText(R.id.tv_widget_difficulty,
            "Duración: ${estimate + (task?.extensionMinutes ?: 0)} min")
        views.setTextViewText(R.id.tv_widget_task_status,
            when { task == null -> "Elige una tarea de la lista";
                task.paused -> "Pausada: ${task.taskName}"; else -> "En curso: ${task.taskName}" })
        views.setViewVisibility(R.id.widget_manual_difficulty, View.GONE)
        views.setTextViewText(R.id.widget_tab_timer, if (isManual) "⏱ Cronómetro" else "● Cronómetro")
        views.setTextViewText(R.id.widget_tab_manual, if (isManual) "● Registrar" else "✎ Registrar")
        views.setViewVisibility(R.id.widget_timer_panel, if (isManual) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_manual_panel, if (isManual && height >= 225) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_collection_container, if (pickerOpen) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.widget_action_picker, if (pickerOpen) "▴ Cerrar selección" else "⌄ Cambiar proyecto o tarea")
        views.setViewVisibility(R.id.widget_action_picker, if (expanded) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_expand_hint, if (expanded) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_action_add_task, if (height >= 295) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.tv_widget_task_status, if (height < 310) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_difficulty_row, View.GONE)
        views.setViewVisibility(R.id.widget_action_extend, if (task != null) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.widget_action_primary,
            when { task == null -> "▶ Iniciar"; task.paused -> "▶ Reanudar"; else -> "Ⅱ Pausar" })
        val elapsed = task?.elapsedSeconds() ?: 0L
        views.setChronometer(R.id.widget_chronometer,
            SystemClock.elapsedRealtime() - elapsed * 1000L, null, task != null && !task.paused)

        // Lista vertical real: todos los proyectos, más tareas del proyecto elegido.
        val adapterIntent = Intent(context, WidgetProjectTaskService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            data = Uri.parse("deepsource://widget/list/$id")
        }
        views.setRemoteAdapter(R.id.widget_project_task_list, adapterIntent)
        val template = Intent(context, DeepSourceAppWidget::class.java).apply {
            action = ACTION_SELECT
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            data = Uri.parse("deepsource://widget/click/$id")
        }
        views.setPendingIntentTemplate(R.id.widget_project_task_list,
            PendingIntent.getBroadcast(context, 7000 + id, template,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE))

        val prefix = id * 40
        val commands = listOf(
            R.id.widget_action_picker to ACTION_PICKER,
            R.id.widget_tab_timer to ACTION_MODE_TIMER,
            R.id.widget_tab_manual to ACTION_MODE_MANUAL,
            
            R.id.widget_action_primary to if (task == null) ACTION_START else ACTION_TOGGLE,
            R.id.widget_action_extend to ACTION_EXTEND,
        )
        commands.forEachIndexed { index, (viewId, action) ->
            views.setOnClickPendingIntent(viewId, broadcast(context, id, prefix + index, action))
        }

        // Registro manual con fecha y duración exactas: diálogo liviano con teclado.
        val manualIntent = Intent(context, WidgetManualEntryActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            data = Uri.parse("deepsource://widget/manual/$id")
        }
        views.setOnClickPendingIntent(R.id.widget_action_manual_entry,
            PendingIntent.getActivity(context, prefix + 24, manualIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        // Finalizar abre directamente la pantalla de revisión y Guardar registro,
        // nunca persiste el trabajo sin la confirmación del usuario.
        val finishIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NOTIFICATION_ROUTE, cl.deepsource.horas.navigation.Routes.FINISH)
            data = Uri.parse("deepsource://widget/finish/$id")
        }
        views.setOnClickPendingIntent(R.id.widget_action_finish,
            PendingIntent.getActivity(context, prefix + 25, finishIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        views.setViewVisibility(R.id.widget_action_finish,
            if (!isManual && task != null) View.VISIBLE else View.GONE)
        // Crear una tarea requiere teclado: abre un diálogo mínimo, con proyecto preseleccionado.
        val addIntent = Intent(context, WidgetNewTaskActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            putExtra(WidgetNewTaskActivity.EXTRA_PROJECT_ID, selected?.id)
            data = Uri.parse("deepsource://widget/add/$id")
        }
        views.setOnClickPendingIntent(R.id.widget_action_add_task,
            PendingIntent.getActivity(context, prefix + 22, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        views.setOnClickPendingIntent(R.id.widget_action_open,
            PendingIntent.getActivity(context, prefix + 23,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    data = Uri.parse("deepsource://widget/open/$id")
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        manager.updateAppWidget(id, views)
        if (expanded) manager.notifyAppWidgetViewDataChanged(id, R.id.widget_project_task_list)
    }

    private fun broadcast(context: Context, id: Int, requestCode: Int, actionValue: String): PendingIntent {
        val intent = Intent(context, DeepSourceAppWidget::class.java).apply {
            action = actionValue
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            data = Uri.parse("deepsource://widget/action/$id/$actionValue")
        }
        return PendingIntent.getBroadcast(context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val ACTION_SELECT = "cl.deepsource.horas.widget.SELECT_ITEM"
        const val EXTRA_ITEM_TYPE = "widget_item_type"
        const val EXTRA_ITEM_ID = "widget_item_id"
        private const val ACTION_PICKER = "widget.PICKER"
        private const val ACTION_MODE_TIMER = "widget.TIMER"
        private const val ACTION_MODE_MANUAL = "widget.MANUAL"
        private const val ACTION_NEXT_DIFFICULTY = "widget.DIFFICULTY"
        private const val ACTION_START = "widget.START"
        private const val ACTION_TOGGLE = "widget.TOGGLE"
        private const val ACTION_EXTEND = "widget.EXTEND30"
        private const val ACTION_HPLUS = "widget.HPLUS"
        private const val ACTION_HMINUS = "widget.HMINUS"
        private const val ACTION_MPLUS = "widget.MPLUS"
        private const val ACTION_MMINUS = "widget.MMINUS"
        private const val ACTION_SAVE = "widget.SAVE"
        private val ACTIONS = setOf(ACTION_SELECT, ACTION_PICKER, ACTION_MODE_TIMER, ACTION_MODE_MANUAL,
            ACTION_NEXT_DIFFICULTY, ACTION_START, ACTION_TOGGLE, ACTION_EXTEND,
            ACTION_HPLUS, ACTION_HMINUS, ACTION_MPLUS, ACTION_MMINUS, ACTION_SAVE)
        private val LEVELS = listOf("Normal", "Media", "Compleja")
        private val ESTIMATES = mapOf("Normal" to 90, "Media" to 120, "Compleja" to 240)
        private val saving = AtomicBoolean(false)

        fun widgetPrefs(context: Context) =
            context.getSharedPreferences("deepsource_widget_v4", Context.MODE_PRIVATE)

        fun selectedProject(context: Context, widgetId: Int, projects: List<Project>, currentId: String?): Project? {
            val saved = widgetPrefs(context).getString("project_$widgetId", null)
            return projects.firstOrNull { it.id == saved } ?: projects.firstOrNull { it.id == currentId }
                ?: projects.firstOrNull()
        }
        fun selectedTask(context: Context, widgetId: Int, tasks: List<ProjectTask>): ProjectTask? {
            val saved = widgetPrefs(context).getString("task_$widgetId", null)
            return tasks.firstOrNull { it.id == saved } ?: tasks.firstOrNull()
        }
        fun selectedDifficulty(context: Context, widgetId: Int): String {
            val raw = widgetPrefs(context).getString("difficulty_$widgetId", "Normal")
            return raw?.takeIf { it in LEVELS } ?: "Normal"
        }
        fun sendUpdateBroadcast(context: Context) {
            context.sendBroadcast(Intent(context, DeepSourceAppWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    AppWidgetManager.getInstance(context).getAppWidgetIds(
                        ComponentName(context, DeepSourceAppWidget::class.java)))
            })
        }
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DeepSourceAppWidget::class.java))
            DeepSourceAppWidget().onUpdate(context, manager, ids)
        }
        private fun showToast(context: Context, message: String) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
