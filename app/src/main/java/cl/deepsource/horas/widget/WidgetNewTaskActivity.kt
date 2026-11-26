package cl.deepsource.horas.widget

import android.app.AlertDialog
import android.content.Intent
import cl.deepsource.horas.MainActivity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import cl.deepsource.horas.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Diálogo mínimo con teclado para crear una tarea desde el widget: no borra nada. */
class WidgetNewTaskActivity : ComponentActivity() {
    companion object { const val EXTRA_PROJECT_ID = "widget_new_task_project_id" }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (appContainer().authRepository.currentUser.value == null) {
            Toast.makeText(this, "Inicia sesión para crear tareas", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        val repository = appContainer().projectRepository
        val projectId = intent.getStringExtra(EXTRA_PROJECT_ID)
        val project = repository.projects.value.firstOrNull { it.id == projectId }
        if (project == null) {
            Toast.makeText(this, "Selecciona un proyecto primero", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val input = EditText(this).apply {
            hint = "Nombre de la tarea"
            setSingleLine(true)
        }
        AlertDialog.Builder(this)
            .setTitle("Nueva tarea · ${project.name}")
            .setMessage("Se guardará en el catálogo de tareas de este proyecto")
            .setView(input)
            .setNegativeButton("Cancelar") { _, _ -> finish() }
            .setPositiveButton("Guardar") { _, _ ->
                val name = input.text.toString()
                CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
                    val result = withContext(Dispatchers.IO) { repository.addProjectTask(project.id, name) }
                    result.onSuccess { task ->
                        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                            AppWidgetManager.INVALID_APPWIDGET_ID)
                        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            DeepSourceAppWidget.widgetPrefs(this@WidgetNewTaskActivity).edit()
                                .putString("task_$widgetId", task.id).apply()
                        }
                        Toast.makeText(this@WidgetNewTaskActivity, "Tarea guardada", Toast.LENGTH_SHORT).show()
                        DeepSourceAppWidget.refreshAll(this@WidgetNewTaskActivity)
                    }.onFailure {
                        Toast.makeText(this@WidgetNewTaskActivity,
                            it.message ?: "No se pudo guardar", Toast.LENGTH_LONG).show()
                    }
                    finish()
                }
            }
            .setOnCancelListener { finish() }
            .show()
    }
}
