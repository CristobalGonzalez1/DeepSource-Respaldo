package cl.deepsource.horas.widget

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import cl.deepsource.horas.MainActivity
import cl.deepsource.horas.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** V10: fecha y duración exacta desde widget, sin abrir una pantalla grande. */
class WidgetManualEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (appContainer().authRepository.currentUser.value == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        val repo = appContainer().projectRepository
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val project = DeepSourceAppWidget.selectedProject(this, id, repo.projects.value, repo.currentProject.value?.id)
        val task = project?.let { p ->
            DeepSourceAppWidget.selectedTask(this, id, repo.projectTasks.value.filter { it.projectId == p.id })
        }
        if (project == null || task == null) {
            Toast.makeText(this, "Selecciona proyecto y tarea en el widget", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val dp = resources.displayMetrics.density
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((22 * dp).toInt(), (8 * dp).toInt(), (22 * dp).toInt(), 0)
        }
        fun field(title: String, initial: String, numeric: Boolean = false): EditText {
            column.addView(TextView(this).apply { text = title })
            return EditText(this).apply {
                setText(initial)
                inputType = if (numeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                column.addView(this)
            }
        }
        val hours = field("Horas (0 a 23)", "1", true)
        val minutes = field("Minutos (0 a 59)", "0", true)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val calendar = Calendar.getInstance()
        val dateLabel = TextView(this).apply {
            text = "Fecha: ${formatter.format(calendar.time)}   ·   Cambiar fecha"
            textSize = 15f
            setPadding(0, (14 * dp).toInt(), 0, (14 * dp).toInt())
            setOnClickListener {
                DatePickerDialog(this@WidgetManualEntryActivity, { _, y, m, d ->
                    calendar.set(y, m, d, 0, 0, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    text = "Fecha: ${formatter.format(calendar.time)}   ·   Cambiar fecha"
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
                    datePicker.maxDate = System.currentTimeMillis()
                }.show()
            }
        }
        column.addView(dateLabel)
        val note = field("Descripción del trabajo", "Registro manual desde widget")
        var submitting = false
        val dialog = AlertDialog.Builder(this)
            .setTitle("Registrar horas · ${project.name}")
            .setMessage("Tarea: ${task.name}")
            .setView(column)
            .setNegativeButton("Cancelar") { _, _ -> finish() }
            .setPositiveButton("Guardar", null)
            .setOnCancelListener { finish() }
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (submitting) return@setOnClickListener
                val h = hours.text.toString().toIntOrNull()
                val m = minutes.text.toString().toIntOrNull()
                val duration = if (h != null && m != null) h * 60L + m else 0L
                val description = note.text.toString().trim()
                when {
                    h == null || h !in 0..23 || m == null || m !in 0..59 || duration !in 1..1439 -> {
                        hours.error = if (h == null || h !in 0..23) "Entre 0 y 23 horas" else null
                        minutes.error = if (m == null || m !in 0..59) "Entre 0 y 59 minutos; el total debe ser mayor a cero" else null
                    }
                    description.length !in 4..300 -> {
                        note.error = "Escribe una descripción de entre 4 y 300 caracteres"
                    }
                    calendar.timeInMillis > System.currentTimeMillis() -> {
                        dateLabel.error = "Selecciona una fecha válida que no sea futura"
                    }
                    else -> {
                        submitting = true
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
                            val result = withContext(Dispatchers.IO) {
                                repo.logHours(project.id, duration, description, task.name,
                                    "Programada",
                                    calendar.timeInMillis)
                            }
                            result.onSuccess {
                                Toast.makeText(this@WidgetManualEntryActivity, "Horas guardadas en Historial", Toast.LENGTH_SHORT).show()
                                DeepSourceAppWidget.refreshAll(this@WidgetManualEntryActivity)
                                dialog.dismiss()
                                finish()
                            }.onFailure {
                                Toast.makeText(this@WidgetManualEntryActivity, it.message ?: "No se pudo guardar", Toast.LENGTH_LONG).show()
                                submitting = false
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            }
                        }
                    }
                }
            }
        }
        dialog.show()
    }
}
