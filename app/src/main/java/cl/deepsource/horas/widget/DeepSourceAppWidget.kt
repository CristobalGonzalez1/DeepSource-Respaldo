package cl.deepsource.horas.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import cl.deepsource.horas.MainActivity
import cl.deepsource.horas.R

class DeepSourceAppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetView(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, DeepSourceAppWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        for (appWidgetId in appWidgetIds) {
            updateWidgetView(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidgetView(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences("deepsource_projects_prefs", Context.MODE_PRIVATE)

        val activeTaskRaw = prefs.getString("key_active_task", null)
        val activeTaskName = activeTaskRaw?.split("|")?.getOrNull(3)
        val isPaused = activeTaskRaw?.split("|")?.getOrNull(5)?.toBooleanStrictOrNull() ?: false

        val currentProjId = prefs.getString("key_current_project_id", null)
        val projectsRaw = prefs.getStringSet("key_projects", emptySet()) ?: emptySet()
        val currentProjName = projectsRaw.mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 2 && parts[0] == currentProjId) parts[1] else null
        }.firstOrNull() ?: projectsRaw.firstOrNull()?.split("|")?.getOrNull(1)

        val views = RemoteViews(context.packageName, R.layout.widget_deepsource)

        val projText = currentProjName?.let { "Proyecto: $it" } ?: "Sin proyecto seleccionado"
        views.setTextViewText(R.id.tv_widget_project_name, projText)

        val taskStatus = when {
            activeTaskRaw == null -> "Sin tarea activa"
            isPaused -> "Aplazada: ${activeTaskName ?: "Tarea"}"
            else -> "En curso: ${activeTaskName ?: "Tarea"}"
        }
        views.setTextViewText(R.id.tv_widget_task_status, taskStatus)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        fun sendUpdateBroadcast(context: Context) {
            val intent = Intent(context, DeepSourceAppWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, DeepSourceAppWidget::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }

}
