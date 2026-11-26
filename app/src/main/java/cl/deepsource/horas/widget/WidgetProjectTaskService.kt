package cl.deepsource.horas.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.graphics.Color
import cl.deepsource.horas.R
import cl.deepsource.horas.appContainer

/** Adapter RemoteViews de Android 8+: colección de filas desplazables en el launcher. */
class WidgetProjectTaskService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        ProjectTaskFactory(applicationContext,
            intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID))
}

private data class WidgetRow(val label: String, val type: String = "", val id: String = "",
    val selected: Boolean = false)

private class ProjectTaskFactory(private val context: Context, private val widgetId: Int) :
    RemoteViewsService.RemoteViewsFactory {
    private var items = emptyList<WidgetRow>()

    override fun onCreate() { load() }
    override fun onDataSetChanged() { load() }
    override fun onDestroy() { items = emptyList() }
    override fun getCount() = items.size
    override fun getViewTypeCount() = 1
    override fun hasStableIds() = true
    override fun getItemId(position: Int): Long = items.getOrNull(position)?.let {
        (it.type + it.id).hashCode().toLong()
    } ?: position.toLong()
    override fun getLoadingView(): RemoteViews? = null

    override fun getViewAt(position: Int): RemoteViews? {
        val row = items.getOrNull(position) ?: return null
        val views = RemoteViews(context.packageName, R.layout.widget_project_task_item)
        views.setTextViewText(R.id.widget_collection_row, row.label)
        views.setTextColor(R.id.widget_collection_row, when {
            row.type.isEmpty() -> Color.rgb(34, 211, 238)
            row.selected -> Color.rgb(68, 240, 211)
            else -> Color.WHITE
        })
        if (row.type.isNotEmpty()) {
            views.setOnClickFillInIntent(R.id.widget_collection_row, Intent().apply {
                putExtra(DeepSourceAppWidget.EXTRA_ITEM_TYPE, row.type)
                putExtra(DeepSourceAppWidget.EXTRA_ITEM_ID, row.id)
            })
        }
        return views
    }

    private fun load() {
        if (context.appContainer().authRepository.currentUser.value == null) {
            items = listOf(WidgetRow("Inicia sesión para ver proyectos y tareas"))
            return
        }
        val repository = context.appContainer().projectRepository
        val projects = repository.projects.value
        val selected = DeepSourceAppWidget.selectedProject(context, widgetId,
            projects, repository.currentProject.value?.id)
        val tasks = repository.projectTasks.value.filter { it.projectId == selected?.id }
        val selectedTask = DeepSourceAppWidget.selectedTask(context, widgetId, tasks)
        val rows = mutableListOf(WidgetRow("PROYECTOS · desplaza para elegir"))
        if (projects.isEmpty()) rows.add(WidgetRow("Crea tu primer proyecto en DeepSource"))
        projects.forEach {
            rows.add(WidgetRow(if (it.id == selected?.id) "✓ ${it.name}" else "▸ ${it.name}",
                "project", it.id, it.id == selected?.id))
        }
        rows.add(WidgetRow("TAREAS · ${selected?.name ?: "sin proyecto"}"))
        if (tasks.isEmpty()) rows.add(WidgetRow("Pulsa Nueva tarea debajo"))
        tasks.forEach {
            rows.add(WidgetRow(if (it.id == selectedTask?.id) "✓ ${it.name}" else "○ ${it.name}",
                "task", it.id, it.id == selectedTask?.id))
        }
        items = rows
    }
}
