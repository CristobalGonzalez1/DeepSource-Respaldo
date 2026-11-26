package cl.deepsource.horas.feature.workflow

import android.Manifest
import android.app.DatePickerDialog
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Build
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Switch
import cl.deepsource.horas.core.nativefeatures.AppSettingsStore
import cl.deepsource.horas.core.nativefeatures.TaskAlarmScheduler
import cl.deepsource.horas.core.nativefeatures.TaskNotifications
import cl.deepsource.horas.appContainer
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.deepsource.horas.core.ui.components.DsBottomNav
import cl.deepsource.horas.core.ui.components.DsBrandHeader
import cl.deepsource.horas.core.ui.components.DsPrimaryButton
import cl.deepsource.horas.core.ui.components.DsScreenBackground
import cl.deepsource.horas.core.ui.theme.DsBlue
import cl.deepsource.horas.core.ui.theme.DsCyan
import cl.deepsource.horas.core.ui.theme.DsInput
import cl.deepsource.horas.core.ui.theme.DsOutline
import cl.deepsource.horas.core.ui.theme.DsSuccess
import cl.deepsource.horas.core.ui.theme.DsSurface
import cl.deepsource.horas.core.ui.theme.DsTextMuted
import cl.deepsource.horas.core.ui.theme.DsTextPrimary
import cl.deepsource.horas.core.ui.theme.DsViolet
import cl.deepsource.horas.domain.model.Project
import cl.deepsource.horas.domain.model.TimeLog
import cl.deepsource.horas.domain.model.WorkflowDefaults
import cl.deepsource.horas.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Estas funciones únicamente preparan texto; NO modifican los registros.
private fun clock(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    return "%02d:%02d:%02d".format(Locale.US, s / 3600, s % 3600 / 60, s % 60)
}
private fun compact(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    return if (s < 3600) "${s / 60}m ${s % 60}s" else "${s / 3600}h ${s % 3600 / 60}m"
}
private fun difference(seconds: Long): String = (if (seconds >= 0) "+" else "−") + compact(kotlin.math.abs(seconds))
private fun date(epoch: Long): String = SimpleDateFormat("dd MMM yyyy", Locale("es", "CL")).format(Date(epoch))
private fun hour(epoch: Long): String = SimpleDateFormat("HH:mm", Locale("es", "CL")).format(Date(epoch))

/**
 * [FLUJO/RUTA] Punto de entrada de las vistas Material; mismo ViewModel durante todas las rutas.
 * No creamos un repositorio distinto por pantalla: compartimos el de la aplicación.
 */
@Composable
fun WorkflowRoute(page: String, viewModel: WorkflowViewModel, onNavigate: (String) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // V8: como ADMIN comienza directamente en Vista de jefe, también se pregunta aquí
    // en el primer ingreso (antes solo se preguntaba al pasar por Inicio).
    if (page == Routes.MANAGER) {
        val noticeSettings = remember(context) { AppSettingsStore(context) }
        val notifier = remember(context) { TaskNotifications(context) }
        var askNotifications by remember { mutableStateOf(!noticeSettings.notificationQuestionAnswered) }
        val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            noticeSettings.notificationsEnabled = granted
            TaskAlarmScheduler.sync(context, context.appContainer().projectRepository.activeTask.value)
        }
        if (askNotifications) {
            val decline: () -> Unit = {
                noticeSettings.notificationQuestionAnswered = true
                noticeSettings.notificationsEnabled = false
                askNotifications = false
            }
            val accept: () -> Unit = {
                noticeSettings.notificationQuestionAnswered = true
                askNotifications = false
                if (Build.VERSION.SDK_INT >= 33 && !notifier.hasPermission()) {
                    request.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    noticeSettings.notificationsEnabled = true
                    TaskAlarmScheduler.sync(context, context.appContainer().projectRepository.activeTask.value)
                }
            }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = decline,
                title = { Text("¿Activar notificaciones?") },
                text = { Text("DeepSource puede avisarte cuando termina una tarea. Están apagadas inicialmente y puedes configurarlas después en Perfil.") },
                confirmButton = { TextButton(onClick = accept) { Text("Activar") } },
                dismissButton = { TextButton(onClick = decline) { Text("Ahora no") } },
            )
        }
    }
    // Mostrar Toast no es trabajo del repositorio ni del ViewModel.
    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }
    // [V7/V3 RESTAURADO] La pantalla Recordatorio de V3 vuelve a mostrarse al vencer
    // el tiempo con la aplicación abierta. Cambiamos solo una vez por cada extensión;
    // no se usa un reinicio ni se redibuja toda la Activity (sin parpadeo).
    LaunchedEffect(page, state.nowMs, state.activeTask?.id, state.activeTask?.extensionMinutes) {
        val task = state.activeTask
        if (page == Routes.RUNNING && task != null && viewModel.dueAlert(task, state.elapsed)) {
            TaskAlarmScheduler.deliver(context)
            onNavigate(Routes.ALERT)
        }
    }
    // Al pulsar Terminar desde el widget, se abre FINISH sin perder tiempo ni guardar
    // automáticamente. El ViewModel genera el resumen que luego confirma el usuario.
    LaunchedEffect(page, state.activeTask?.id, state.preview?.task?.id) {
        if (page == Routes.FINISH && state.activeTask != null && state.preview == null) {
            viewModel.prepareFinish { }
        }
    }
    WorkflowFrame(page, state.activeTask != null, onNavigate) {
        when (page) {
            Routes.START -> StartPage(state, viewModel, onNavigate)
            Routes.RUNNING -> RunningPage(state, viewModel, onNavigate)
            Routes.ALERT -> AlertPage(state, viewModel, onNavigate)
            Routes.FINISH -> FinishPage(state, viewModel, onNavigate)
            Routes.SAVED -> SavedPage(state, viewModel, onNavigate)
            Routes.DETAIL -> DetailPage(state, viewModel, onNavigate)
            Routes.MANAGER -> ManagerPage(state, viewModel, onNavigate)
            Routes.ASSIGNMENTS -> AssignmentsPage(state, onNavigate)
            Routes.HISTORY -> HistoryPage(state, onNavigate)
            Routes.MANUAL -> ManualPage(state, viewModel, onNavigate)
            Routes.REPORT -> ReportPage(state, onNavigate)
            Routes.SETTINGS -> SettingsPage()
        }
    }
}

/** [FLUJO/COMPONENTE] Marco reutilizable: conserva el logo y los tokens de Theme.kt existentes. */
@Composable
private fun WorkflowFrame(
    page: String, isActive: Boolean, onNavigate: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    DsScreenBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 19.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                // Se reutiliza exactamente el DsBrandHeader anterior, NO se redibuja el logo.
                DsBrandHeader(iconSize = 44.dp)
                Text(if (isActive) "● EN CURSO" else "● DISPONIBLE",
                    color = if (isActive) DsSuccess else DsCyan, fontSize = 10.sp)
            }
            HorizontalDivider(color = DsOutline.copy(alpha = .7f))
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 25.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) { content() }
            // Barra reutilizada desde Home; permanece fija debajo del contenido desplazable.
            DsBottomNav(page, isActive, onNavigate)
        }
    }
}

/** [FLUJO/COMPONENTE] Estilo de tarjetas conserva DsSurface y DsOutline del Theme original. */
@Composable
private fun Panel(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = DsSurface, shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DsOutline)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
private fun Heading(name: String, description: String = "") {
    Text(name, fontSize = 25.sp, color = DsTextPrimary, fontWeight = FontWeight.Bold)
    if (description.isNotBlank()) Text(description, color = DsTextMuted, fontSize = 12.sp)
}

@Composable
private fun MiniLabel(value: String) {
    Text(value.uppercase(Locale.getDefault()), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DsCyan)
}

@Composable
private fun KeyValue(label: String, value: String, accent: Color = DsTextPrimary) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = DsTextMuted, fontSize = 13.sp)
        Text(value, color = accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

/** [FLUJO/COMPONENTE] Menú desplegable Material para seleccionar tareas/proyectos/personas. */
@Composable
private fun Picker(label: String, selected: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        MiniLabel(label)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1C1B39), contentColor = DsTextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6656A7)),
                shape = RoundedCornerShape(9.dp)) {
                Text(options.firstOrNull { it.first == selected }?.second ?: "Seleccionar",
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Start,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("⌄", color = DsCyan)
            }
            // Contenedor morado suave: evita el menú gris/blanco predeterminado de Android.
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF251F47)).heightIn(max = 300.dp),
                containerColor = Color(0xFF251F47),
                shape = RoundedCornerShape(13.dp),
            ) {
                options.forEach { (key, display) ->
                    DropdownMenuItem(text = {
                        Text(display, color = if (key == selected) DsCyan else DsTextPrimary)
                    }, onClick = {
                        expanded = false
                        onSelect(key)
                    })
                }
            }
        }
    }
}

@Composable
private fun ActionRow(first: String, onFirst: () -> Unit, second: String, onSecond: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f)) { DsPrimaryButton(first, onFirst) }
        OutlinedButton(onClick = onSecond, modifier = Modifier.weight(1f).height(56.dp)) { Text(second, fontSize = 12.sp) }
    }
}

// Iniciar tarea -> ViewModel.begin() -> repositorio.startTask() -> widget y reloj.
@Composable
private fun StartPage(s: WorkflowUiState, vm: WorkflowViewModel, go: (String) -> Unit) {
    Heading("Iniciar tarea", "Selecciona qué vas a medir")
    Picker("Proyecto", s.selectedProject?.id ?: "", s.projects.map { it.id to it.name }, vm::chooseProject)
    Picker("Tarea del proyecto", s.form.taskName.takeIf { it in s.selectedTaskNames }
        ?: s.selectedTaskNames.firstOrNull().orEmpty(), s.selectedTaskNames.map { it to it }, vm::chooseTask)
    if (s.selectedTaskNames.isEmpty()) Text("Aún no hay tareas en este proyecto.", color = DsTextMuted)
    AddTaskEditor(projectId = s.selectedProject?.id, vm = vm)
    Panel {
        KeyValue("Duración estimada de la tarea", "${s.projectTasks.firstOrNull { it.projectId == s.selectedProject?.id && it.name == (s.form.taskName.takeIf { name -> name in s.selectedTaskNames } ?: s.selectedTaskNames.firstOrNull()) }?.estimateMinutes ?: 90} min", DsSuccess)
        Text("La hora de inicio se toma del reloj local del dispositivo.", color = DsTextMuted, fontSize = 12.sp)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = s.form.demoReminder, onCheckedChange = vm::setDemo)
        Text("Aviso de prueba a los 15 segundos", fontSize = 12.sp, color = DsTextMuted)
    }
    if (s.activeTask != null) {
        Text("Ya existe una tarea activa; solo puede haber una a la vez.", color = DsSuccess)
        DsPrimaryButton("Ver tarea activa", onClick = { go(Routes.RUNNING) })
    } else {
        DsPrimaryButton("Iniciar tarea", onClick = { vm.begin { go(Routes.RUNNING) } },
            enabled = s.projects.isNotEmpty())
        if (s.projects.isEmpty()) TextButton(onClick = { go(Routes.HOME) }) { Text("Crear proyecto en Inicio") }
    }
    Text("Proyecto → Tarea → Horas", color = DsTextMuted, fontSize = 11.sp,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

// Reloj calculado desde timestamp + segundos acumulados: sigue contando al minimizar la app.
@Composable
private fun RunningPage(s: WorkflowUiState, vm: WorkflowViewModel, go: (String) -> Unit) {
    val task = s.activeTask
    if (task == null) {
        Heading("Sin tarea activa")
        DsPrimaryButton("Iniciar una tarea", onClick = { go(Routes.START) }); return
    }
    Heading("Tarea en curso", "Registro automático del tiempo")
    Text(if (task.paused) "● EN PAUSA" else "● EN CURSO", color = if (task.paused) DsTextMuted else DsSuccess)
    Text(task.taskName, color = DsTextPrimary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
    Text(task.projectName, color = DsTextMuted, fontSize = 13.sp)
    var previousExtension by remember(task.id) { mutableStateOf(task.extensionMinutes) }
    var extensionNotice by remember(task.id) { mutableStateOf(false) }
    LaunchedEffect(task.extensionMinutes) {
        if (task.extensionMinutes > previousExtension) {
            extensionNotice = true
            delay(2300L)
            extensionNotice = false
        }
        previousExtension = task.extensionMinutes
    }
    AnimatedVisibility(extensionNotice, enter = fadeIn(tween(240)), exit = fadeOut(tween(300))) {
        Surface(color = DsSuccess.copy(alpha = .14f), shape = RoundedCornerShape(12.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = DsSuccess)
                Text("  Plazo ampliado 30 minutos", color = DsSuccess)
            }
        }
    }
    Panel {
        MiniLabel("Tiempo transcurrido")
        Text(clock(s.elapsed), fontSize = 38.sp, fontWeight = FontWeight.Black, color = DsTextPrimary,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("Inicio: ${hour(task.originalStartEpochMs)}", color = DsTextMuted,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 12.sp)
        HorizontalDivider(color = DsOutline)
        KeyValue("Estimado", compact(task.estimateMinutes * 60L))
        AnimatedContent(targetState = task.extensionMinutes,
            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) }, label = "plazo") { extension ->
            KeyValue("Límite con aplazamiento", compact((task.estimateMinutes + extension) * 60L))
        }
        KeyValue("Restante", clock(s.limit - s.elapsed), DsCyan)
        val smoothProgress by animateFloatAsState(
            targetValue = if (s.limit > 0) (s.elapsed.toFloat() / s.limit).coerceIn(0f,1f) else 0f,
            animationSpec = tween(420), label = "avance")
        LinearProgressIndicator(progress = { smoothProgress }, modifier = Modifier.fillMaxWidth(), color = DsCyan)
    }
    if (task.demoReminder) Text("● Aviso de prueba a los 15 segundos", color = DsSuccess, fontSize = 11.sp)
    ActionRow("Finalizar", { vm.prepareFinish { go(Routes.FINISH) } }, "+30 min", { vm.extend { } })
    // Este control complementa Aplazar del HTML; se preserva pausa real de la app original.
    OutlinedButton(onClick = vm::togglePause, modifier = Modifier.fillMaxWidth()) {
        Icon(if (task.paused) Icons.Default.PlayArrow else Icons.Default.Pause, null)
        Text(if (task.paused) " Reanudar cronómetro" else " Pausar cronómetro")
    }
    Text("El cronómetro no mide teclado, cámara ni ubicación.", color = DsTextMuted, fontSize = 11.sp)
}

// Notificación contextual del prototipo; botones llevan al mismo ViewModel.
@Composable
private fun AlertPage(s: WorkflowUiState, vm: WorkflowViewModel, go: (String) -> Unit) {
    val task = s.activeTask
    Heading("Recordatorio", "Revisa el tiempo de tu tarea")
    if (task == null) { DsPrimaryButton("Volver", { go(Routes.START) }); return }
    Panel {
        Icon(Icons.Default.Warning, "Recordatorio", tint = DsCyan, modifier = Modifier.size(45.dp))
        Text(if (task.demoReminder) "Aviso de prueba" else "¡Tiempo estimado alcanzado!",
            fontSize = 21.sp, fontWeight = FontWeight.Bold, color = DsTextPrimary)
        Text(task.projectName, color = DsTextMuted, fontSize = 13.sp)
        Text(task.taskName, color = DsTextPrimary, fontWeight = FontWeight.SemiBold)
        Text("¿Deseas continuar 30 minutos más o finalizar?", color = DsTextMuted)
        KeyValue("Tiempo transcurrido", clock(s.elapsed), DsSuccess)
        KeyValue("Límite actual", compact(s.limit), DsCyan)
        ActionRow("Finalizar", { vm.prepareFinish { go(Routes.FINISH) } }, "+30 min", { vm.extend { go(Routes.RUNNING) } })
    }
    OutlinedButton(onClick = { go(Routes.RUNNING) }, modifier = Modifier.fillMaxWidth()) {
        Text("Ver cronómetro")
    }
    Text("La alarma deja de sonar cuando finalizas o aplazas; puedes regresar a ver el cronómetro.",
        color = DsTextMuted, fontSize = 11.sp)
}

// Confirmación antes de persistir: calcula diferencia estimado - real.
@Composable
private fun FinishPage(s: WorkflowUiState, vm: WorkflowViewModel, go: (String) -> Unit) {
    val preview = s.preview
    Heading("Finalizar tarea", "Revisa el tiempo antes de guardar")
    if (preview == null) { DsPrimaryButton("Regresar a tarea", { go(Routes.RUNNING) }); return }
    Panel {
        Text(preview.task.taskName, color = DsTextPrimary, fontWeight = FontWeight.Bold)
        KeyValue("Estimado", compact(preview.task.estimateMinutes * 60L))
        KeyValue("Real", clock(preview.seconds), DsSuccess)
        KeyValue("Diferencia", difference(preview.seconds - preview.task.estimateMinutes * 60L))
    }
    MiniLabel("¿Qué realizaste?")
    OutlinedTextField(value = s.form.note, onValueChange = { vm.edit { f -> f.copy(note = it) } },
        modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("Resumen de trabajo") })
    // Solo aquí termina el registro de forma definitiva.
    DsPrimaryButton("Guardar registro", { vm.saveFinished { go(Routes.SAVED) } })
    TextButton(onClick = { go(Routes.RUNNING) }) { Text("Volver al cronómetro (en pausa)") }
}

// Guardado confirmado; toma el registro real del repositorio, no valores simulados.
@Composable
private fun SavedPage(s: WorkflowUiState, vm: WorkflowViewModel, go: (String) -> Unit) {
    val record = s.logs.firstOrNull { it.id == s.lastSavedId } ?: s.logs.firstOrNull()
    Icon(Icons.Default.CheckCircle, null, tint = DsSuccess, modifier = Modifier.size(40.dp))
    Heading("¡Registro guardado!", "Los datos quedaron almacenados en este teléfono")
    if (record != null) Panel {
        Text(record.taskName, color = DsTextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(s.projects.firstOrNull { it.id == record.projectId }?.name ?: "Proyecto", color = DsTextMuted)
        KeyValue("Tiempo real", clock(record.durationSeconds), DsSuccess)
        KeyValue("Estimado", compact(record.estimateMinutes * 60L))
        KeyValue("Diferencia", difference(record.durationSeconds - record.estimateMinutes * 60L))
        if (record.note.isNotBlank()) Text(record.note, color = DsTextMuted, fontSize = 12.sp)
    }
    DsPrimaryButton("Ver detalle de proyecto", onClick = {
        record?.projectId?.let(vm::selectDetail)
        go(Routes.DETAIL)
    })
    OutlinedButton(onClick = { go(Routes.HISTORY) }, modifier = Modifier.fillMaxWidth()) { Text("Ver historial") }
}

// Detalle de proyecto: los tiempos vienen de registros reales.
@Composable
private fun DetailPage(s: WorkflowUiState, vm: WorkflowViewModel, go: (String) -> Unit) {
    val project = s.detailProject
    Heading("Detalle de proyecto", "Información y estado")
    if (project == null) { Text("Selecciona un proyecto en Inicio"); return }
    val logs = s.logs.filter { it.projectId == project.id }
    // Porcentaje visual de Figma, explícitamente ilustrativo; no se inventa progreso real.
    val sampleProgress = when (project.name) { "Portal Cliente AFC" -> .35f; "Automatización QA" -> .58f; else -> .22f }
    Panel {
        KeyValue("Estado", "ACTIVO", DsSuccess)
        Text(project.name, color = DsTextPrimary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        if (project.description.isNotBlank()) Text(project.description, color = DsTextMuted)
        Text("Avance de ejemplo (no calculado): ${(sampleProgress * 100).toInt()}%", color = DsTextMuted, fontSize = 11.sp)
        LinearProgressIndicator(progress = { sampleProgress }, modifier = Modifier.fillMaxWidth(), color = DsBlue)
    }
    Panel {
        MiniLabel("Tiempo registrado")
        KeyValue("Registros", "${logs.size}")
        KeyValue("Estimado", compact(logs.sumOf { it.estimateMinutes * 60L }))
        KeyValue("Tiempo real", compact(logs.sumOf { it.durationSeconds }), DsSuccess)
    }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Assignment, null, tint = DsCyan)
            Text("  Tareas de este proyecto", color = DsTextPrimary, fontWeight = FontWeight.Bold)
        }
        if (s.detailProjectTasks.isEmpty()) Text("No hay tareas configuradas", color = DsTextMuted)
        s.detailProjectTasks.forEach { task ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = DsTextMuted, modifier = Modifier.size(17.dp))
                Text("  ${task.name}", modifier = Modifier.weight(1f), color = DsTextPrimary, fontSize = 12.sp)
                androidx.compose.material3.IconButton(onClick = { vm.removeTask(task.id) }) {
                    Icon(Icons.Default.DeleteOutline, "Eliminar ${task.name}", tint = DsTextMuted)
                }
            }
        }
        AddTaskEditor(projectId = project.id, vm = vm)
    }
    DsPrimaryButton("Iniciar tarea de este proyecto", {
        vm.chooseProject(project.id)
        go(Routes.START)
    })
    OutlinedButton(onClick = { go(Routes.HOME) }, modifier = Modifier.fillMaxWidth()) { Text("Volver al inicio") }
}

// Las horas están agrupadas por proyecto para que puedas revisar su detalle sin saturar la pantalla.
@Composable
private fun HistoryPage(s: WorkflowUiState, go: (String) -> Unit) {
    Heading("Historial", "Revisa tus horas por proyecto")
    Panel {
        KeyValue("Registros", "${s.logs.size}")
        KeyValue("Tiempo total", compact(s.logs.sumOf { it.durationSeconds }), DsSuccess)
    }
    if (s.logs.isEmpty()) Panel { Text("Todavía no has registrado horas.", color = DsTextMuted) }
    s.projects.forEach { project ->
        val logs = s.logs.filter { it.projectId == project.id }.sortedByDescending { it.timestampEpochMs }
        var expanded by remember(project.id) { mutableStateOf(false) }
        Panel {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(project.name, fontWeight = FontWeight.Bold, color = DsTextPrimary)
                    Text("${logs.size} registros · ${compact(logs.sumOf { it.durationSeconds })}",
                        color = DsTextMuted, fontSize = 12.sp)
                }
                Text(if (expanded) "Ocultar −" else "Ver detalle +", color = DsCyan, fontSize = 12.sp)
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = DsOutline)
                    if (logs.isEmpty()) Text("Aún no hay horas registradas en este proyecto.", color = DsTextMuted)
                    logs.forEach { entry ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            KeyValue(entry.taskName, clock(entry.durationSeconds), DsCyan)
                            Text("${date(entry.timestampEpochMs)} · ${if (entry.source == "manual") "Registro manual" else "Cronómetro"}",
                                color = DsTextMuted, fontSize = 11.sp)
                            if (entry.note.isNotBlank()) Text(entry.note, color = DsTextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
    ActionRow("Iniciar tarea", { go(Routes.START) }, "Registrar manual", { go(Routes.MANUAL) })
}

// Selección de fecha con calendario y errores debajo de los campos afectados.
@Composable
private fun ManualPage(s: WorkflowUiState, vm: WorkflowViewModel, go: (String) -> Unit) {
    val f = s.form
    val context = LocalContext.current
    var attempted by remember { mutableStateOf(false) }
    Heading("Registro manual", "Anota las horas dedicadas a una tarea")
    val manualProject = s.projects.firstOrNull { it.id == f.manualProjectId } ?: s.selectedProject
    val manualTasks = s.projectTasks.filter { it.projectId == manualProject?.id }.map { it.name }
    Picker("Proyecto", f.manualProjectId.ifBlank { s.selectedProject?.id ?: "" },
        s.projects.map { it.id to it.name }) { selected ->
        vm.edit { data -> data.copy(manualProjectId = selected, manualTaskName = "") }
    }
    if (attempted && manualProject == null) Text("Selecciona un proyecto para continuar", color = Color(0xFFFF627A))
    Picker("Tarea", f.manualTaskName.takeIf { it in manualTasks } ?: manualTasks.firstOrNull().orEmpty(),
        manualTasks.map { it to it }) { selected -> vm.edit { it.copy(manualTaskName = selected) } }
    if (attempted && manualTasks.isEmpty()) Text("Este proyecto aún no tiene tareas. Crea una antes de registrar horas.", color = Color(0xFFFF627A))

    MiniLabel("Fecha del registro")
    val parsedDate = remember(f.manualDate) {
        runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(f.manualDate) }.getOrNull()
    }
    val dateError = attempted && (parsedDate == null || parsedDate.time > System.currentTimeMillis())
    OutlinedButton(onClick = {
        val c = java.util.Calendar.getInstance()
        if (parsedDate != null) c.time = parsedDate
        DatePickerDialog(context, { _, year, month, day ->
            vm.edit { it.copy(manualDate = "%04d-%02d-%02d".format(Locale.US, year, month + 1, day)) }
        }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }, modifier = Modifier.fillMaxWidth().then(if (dateError) Modifier.border(1.dp, Color(0xFFFF627A), RoundedCornerShape(12.dp)) else Modifier)) {
        Icon(Icons.Default.Schedule, contentDescription = null)
        Text("  ${f.manualDate}  ·  Elegir en calendario")
    }
    if (dateError) Text("Selecciona una fecha válida que no sea futura", color = Color(0xFFFF627A), fontSize = 12.sp)

    val hours = f.manualHours.toIntOrNull()
    val minutes = f.manualMinutes.toIntOrNull()
    val hourError = attempted && (hours == null || hours !in 0..23)
    val minuteError = attempted && (minutes == null || minutes !in 0..59 || ((hours ?: 0) * 60 + (minutes ?: 0) == 0))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = f.manualHours, onValueChange = { value -> vm.edit { it.copy(manualHours = value) } },
            label = { Text("Horas") }, modifier = Modifier.weight(1f), singleLine = true,
            isError = hourError,
            supportingText = { if (hourError) Text("Entre 0 y 23 horas") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(value = f.manualMinutes, onValueChange = { value -> vm.edit { it.copy(manualMinutes = value) } },
            label = { Text("Minutos") }, modifier = Modifier.weight(1f), singleLine = true,
            isError = minuteError,
            supportingText = { if (minuteError) Text("Entre 0 y 59; total mayor a cero") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
    val noteError = attempted && f.manualNote.trim().length !in 4..300
    OutlinedTextField(value = f.manualNote, onValueChange = { value -> vm.edit { it.copy(manualNote = value) } },
        modifier = Modifier.fillMaxWidth(), minLines = 2, label = { Text("¿Qué trabajo realizaste?") },
        isError = noteError,
        supportingText = { if (noteError) Text("Describe tu trabajo usando entre 4 y 300 caracteres") })
    DsPrimaryButton("Guardar horas", {
        attempted = true
        if (manualProject != null && manualTasks.isNotEmpty() && !dateError &&
            hours != null && hours in 0..23 && minutes != null && minutes in 0..59 &&
            hours * 60 + minutes > 0 && f.manualNote.trim().length in 4..300 &&
            parsedDate != null && parsedDate.time <= System.currentTimeMillis()) {
            vm.saveManual { go(Routes.SAVED) }
        }
    }, enabled = s.projects.isNotEmpty())
}

// Reporte: suma segundos reales y estimaciones de cada proyecto; no inventa valores.
@Composable
private fun ReportPage(s: WorkflowUiState, go: (String) -> Unit) {
    Heading("Tiempos", "Comparación de trabajo declarado")
    Panel {
        MiniLabel("Tiempo total registrado")
        Text(clock(s.logs.sumOf { it.durationSeconds }), fontSize = 31.sp,
            color = DsTextPrimary, fontWeight = FontWeight.Black)
        Text("${s.logs.size} registros", color = DsTextMuted, fontSize = 12.sp)
    }
    MiniLabel("Por proyecto")
    s.projects.forEach { project ->
        val logs = s.logs.filter { it.projectId == project.id }
        val estimated = logs.sumOf { it.estimateMinutes * 60L }
        val actual = logs.sumOf { it.durationSeconds }
        Panel {
            KeyValue(project.name, "${logs.size} registros", DsCyan)
            KeyValue("Estimado", compact(estimated))
            KeyValue("Real", compact(actual), DsSuccess)
            KeyValue("Diferencia", difference(actual - estimated))
        }
    }
    Text("El reporte se basa en tiempos que la persona registró; no supervisa su actividad.", color = DsTextMuted, fontSize = 11.sp)
    DsPrimaryButton("Ver historial", { go(Routes.HISTORY) })
}

// Carga ficticia inicial del HTML + asignaciones + registros propios.
private fun loadMinutes(s: WorkflowUiState, employeeId: String): Int {
    val member = WorkflowDefaults.members.first { it.id == employeeId }
    val assigned = s.assignments.filter { it.employeeId == employeeId }.sumOf { it.estimateMinutes }
    val logged = if (employeeId == "luis") s.logs.sumOf { it.durationMinutes.toInt() } else 0
    return member.initialLoadMinutes + assigned + logged
}

// Vista de jefe: equipo ilustrativo, guardado de asignaciones REAL en el dispositivo.
@Composable
private fun ManagerPage(s: WorkflowUiState, vm: WorkflowViewModel, go: (String) -> Unit) {
    val f = s.form
    val selected = WorkflowDefaults.members.firstOrNull { it.id == f.employeeId } ?: WorkflowDefaults.members.first()
    val estimate = s.projectTasks.firstOrNull { it.name == f.managerTaskName && it.projectId == (f.managerProjectId.ifBlank { s.selectedProject?.id.orEmpty() }) }?.estimateMinutes ?: 90
    val free = (480 - loadMinutes(s, selected.id)).coerceAtLeast(0)
    Heading("Vista de jefe", "Asigna tareas sin sobrepasar las horas disponibles")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        MiniLabel("Carga y disponibilidad")
        TextButton(onClick = { go(Routes.ASSIGNMENTS) }) { Text("Asignaciones", color = DsCyan) }
    }
    WorkflowDefaults.members.forEach { member ->
        val used = loadMinutes(s, member.id)
        Panel {
            KeyValue(member.name, "${(used.coerceAtMost(480)) / 60}h / 8h", DsTextPrimary)
            Text(if (used >= 480) "Sin disponibilidad" else "${480 - used} min libres",
                color = if (used >= 480) Color(0xFFFF7188) else DsSuccess, fontSize = 11.sp)
            LinearProgressIndicator(progress = { (used / 480f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(), color = if (used >= 480) Color(0xFFFF7188) else DsBlue)
        }
    }
    MiniLabel("Nueva asignación")
    Picker("Empleado", f.employeeId, WorkflowDefaults.members.map { it.id to it.name }) {
        vm.edit { state -> state.copy(employeeId = it) }
    }
    Picker("Proyecto", f.managerProjectId.ifBlank { s.selectedProject?.id ?: "" },
        s.projects.map { it.id to it.name }) { id -> vm.edit { it.copy(managerProjectId = id) } }
    Picker("Tarea", f.managerTaskName, s.projectTasks.filter { it.projectId == f.managerProjectId.ifBlank { s.selectedProject?.id.orEmpty() } }.map { it.name to it.name }) { task ->
        vm.edit { it.copy(managerTaskName = task) }
    }
    Panel {
        KeyValue("Estimado", "$estimate min")
        KeyValue("Disponibles después", "${free - estimate} min", if (free >= estimate) DsSuccess else Color(0xFFFF7188))
    }
    DsPrimaryButton("Asignar tarea", onClick = { vm.assign { go(Routes.ASSIGNMENTS) } },
        enabled = free >= estimate && s.projects.isNotEmpty())
    Text("Miembros y carga inicial de ejemplo; no es un sistema remoto de recursos humanos.", color = DsTextMuted, fontSize = 11.sp)
}

// Historial de asignaciones; se conserva al cerrar/abrir la app.
@Composable
private fun AssignmentsPage(s: WorkflowUiState, go: (String) -> Unit) {
    Heading("Asignaciones", "Tareas del equipo")
    if (s.assignments.isEmpty()) Panel { Text("Sin asignaciones. Asigna una tarea desde Equipo.", color = DsTextMuted) }
    s.assignments.forEach { item ->
        Panel {
            KeyValue(item.taskName, compact(item.estimateMinutes * 60L), DsCyan)
            Text("${item.employeeName} · ${item.projectName}", color = DsTextMuted, fontSize = 12.sp)
            KeyValue("Fecha", date(item.assignedAtEpochMs))
        }
    }
    DsPrimaryButton("Nueva asignación", { go(Routes.MANAGER) })
}

/** [V5/TAREAS] Entrada reutilizada en el formulario de inicio y en el detalle del proyecto. */
@Composable
private fun AddTaskEditor(projectId: String?, vm: WorkflowViewModel) {
    var expanded by remember(projectId) { mutableStateOf(false) }
    var name by remember(projectId) { mutableStateOf("") }
    var planned by remember(projectId) { mutableStateOf("90") }
    var attempted by remember(projectId) { mutableStateOf(false) }
    OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth(),
        enabled = projectId != null) {
        Icon(Icons.Default.AddTask, contentDescription = null)
        Text(if (expanded) "  Cancelar nueva tarea" else "  Añadir tarea al proyecto")
    }
    AnimatedVisibility(expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Nombre de la nueva tarea") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                isError = attempted && (name.isBlank() || name.length > 80),
                supportingText = { if (attempted && (name.isBlank() || name.length > 80)) Text("Escribe un nombre de hasta 80 caracteres") })
            OutlinedTextField(value = planned, onValueChange = { planned = it.filter(Char::isDigit).take(4) },
                label = { Text("Duración prevista (minutos)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = attempted && (planned.toIntOrNull() ?: 0) !in 1..1440,
                supportingText = { if (attempted && (planned.toIntOrNull() ?: 0) !in 1..1440) Text("Indica entre 1 y 1440 minutos") })
            DsPrimaryButton("Guardar tarea", onClick = {
                attempted = true
                if (name.isNotBlank() && name.length <= 80 && (planned.toIntOrNull() ?: 0) in 1..1440) {
                    projectId?.let { id -> vm.addTask(id, name, planned.toIntOrNull() ?: 90) {
                        name = ""; attempted = false; expanded = false
                    } }
                }
            })
        }
    }
}

/** [V5/AJUSTES] Centro de preferencias accesible desde Mi perfil; nunca desde barra principal. */
@Composable
private fun SettingsPage() {
    val context = LocalContext.current
    val store = remember(context) { AppSettingsStore(context) }
    val notifier = remember(context) { TaskNotifications(context) }
    var enabled by remember { mutableStateOf(store.notificationsEnabled && notifier.hasPermission()) }
    var vibrate by remember { mutableStateOf(store.vibrationEnabled) }
    var sound by remember { mutableStateOf(store.soundEnabled) }
    var classicAlarm by remember { mutableStateOf(store.classicAlarmTone) }
    var permission by remember { mutableStateOf(notifier.hasPermission()) }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permission = granted
        enabled = granted
        store.notificationsEnabled = granted
        store.notificationQuestionAnswered = true
        TaskAlarmScheduler.sync(context, context.appContainer().projectRepository.activeTask.value)
    }
    val refreshAlarm: () -> Unit = {
        TaskAlarmScheduler.sync(context, context.appContainer().projectRepository.activeTask.value)
    }
    Heading("Configuración", "Ajusta cómo DeepSource te avisa y acompaña tu trabajo")
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsActive, null, tint = DsCyan)
            Text("  Notificaciones", color = DsTextPrimary, modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = { wantsEnabled ->
                store.notificationQuestionAnswered = true
                if (wantsEnabled && Build.VERSION.SDK_INT >= 33 && !notifier.hasPermission()) {
                    permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    enabled = wantsEnabled
                    store.notificationsEnabled = wantsEnabled
                    refreshAlarm()
                }
            })
        }
        Text(if (permission) "Permiso del sistema concedido" else "El permiso de Android está pendiente",
            color = DsTextMuted, fontSize = 12.sp)
        if (!permission && Build.VERSION.SDK_INT >= 33) {
            OutlinedButton(onClick = { permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth()) { Text("Solicitar permiso de notificaciones") }
        }
    }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VolumeUp, null, tint = DsCyan)
            Text("  Aviso con sonido", color = DsTextPrimary, modifier = Modifier.weight(1f))
            Switch(checked = sound, onCheckedChange = { sound = it; store.soundEnabled = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Vibration, null, tint = DsCyan)
            Text("  Vibración", color = DsTextPrimary, modifier = Modifier.weight(1f))
            Switch(checked = vibrate, onCheckedChange = { vibrate = it; store.vibrationEnabled = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VolumeUp, null, tint = DsCyan)
            Text("  Bip-bip clásico repetitivo", color = DsTextPrimary, modifier = Modifier.weight(1f))
            Switch(checked = classicAlarm, onCheckedChange = {
                classicAlarm = it
                store.classicAlarmTone = it
            })
        }
        Text("Desactivarlo usa el tono predeterminado de alarma. La opción siguiente cambia el sonido de la notificación de respaldo.",
            color = DsTextMuted, fontSize = 12.sp)
        OutlinedButton(onClick = {
            try { context.startActivity(notifier.soundSettingsIntent()) }
            catch (_: Exception) { Toast.makeText(context, "Abre los ajustes de notificaciones del teléfono", Toast.LENGTH_LONG).show() }
        }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Settings, null); Text("  Cambiar sonido del aviso") }
        OutlinedButton(onClick = { notifier.postDeadline("Notificación de prueba") },
            enabled = enabled && permission, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.NotificationsActive, null)
            Text("  Probar sonido y vibración")
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = remember(context) { context.getSystemService(AlarmManager::class.java) }
        if (!alarmManager.canScheduleExactAlarms()) {
            Panel {
                MiniLabel("Precisión de la alarma")
                Text("Para avisos más puntuales, Android puede solicitar el permiso especial de alarmas y recordatorios.",
                    color = DsTextMuted, fontSize = 12.sp)
                OutlinedButton(onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${context.packageName}")))
                    } catch (_: Exception) {
                        Toast.makeText(context, "Abre Alarmas y recordatorios desde Ajustes de Android", Toast.LENGTH_LONG).show()
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Permitir alarmas precisas (opcional)") }
            }
        }
    }
    Panel {
        MiniLabel("Recordatorios")
        Text("Mientras la tarea está activa, Android muestra un cronómetro y el botón +30 min. Al vencer, vuelve el Recordatorio de V3 con bip-bip y vibración hasta aplazar o finalizar (según los permisos y límites del sistema).",
            color = DsTextMuted, fontSize = 12.sp)
        Text("La prueba rápida de 15 segundos se mantiene disponible en Iniciar tarea.",
            color = DsTextMuted, fontSize = 12.sp)
    }
}
