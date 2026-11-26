package cl.deepsource.horas.feature.home

import android.widget.Toast
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import cl.deepsource.horas.core.nativefeatures.TaskPhotoStore
import cl.deepsource.horas.core.nativefeatures.ProjectEvidenceStore
import cl.deepsource.horas.core.nativefeatures.AppSettingsStore
import cl.deepsource.horas.core.nativefeatures.TaskAlarmScheduler
import cl.deepsource.horas.core.nativefeatures.TaskNotifications
import cl.deepsource.horas.appContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.deepsource.horas.core.ui.components.DsBottomNav
import cl.deepsource.horas.core.ui.components.DsBrandHeader
import cl.deepsource.horas.core.ui.components.DsScreenBackground
import cl.deepsource.horas.feature.home.components.CreateProjectDialog
import cl.deepsource.horas.feature.home.components.CurrentProjectCard
import cl.deepsource.horas.feature.home.components.CurrentTimeSection
import cl.deepsource.horas.feature.home.components.HomeActions
import cl.deepsource.horas.feature.home.components.LimitCard
import cl.deepsource.horas.feature.home.components.LimitInfoDialog
import cl.deepsource.horas.navigation.Routes
import cl.deepsource.horas.feature.home.components.LogHoursDialog
import cl.deepsource.horas.feature.home.components.ProjectList
import cl.deepsource.horas.feature.home.components.StartTaskDialog

@Composable
// HomeRoute: Une estado y métodos de HomeViewModel con la pantalla y recursos Android de cámara/permisos.
fun HomeRoute(
    onOpenWorkflow: (String) -> Unit = {},
    onOpenProjectTasks: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    // Observa ViewModel con conciencia del ciclo de vida para actualizar Home.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationSettings = androidx.compose.runtime.remember(context) { AppSettingsStore(context) }
    val notifier = androidx.compose.runtime.remember(context) { TaskNotifications(context) }
    var showNotificationQuestion by androidx.compose.runtime.remember { mutableStateOf(false) }
    LaunchedEffect(uiState.userEmail) {
        // Se pregunta al entrar tras login, solo la primera vez. El permiso de Android va después de aceptar.
        if (uiState.userEmail.isNotBlank() && !notificationSettings.notificationQuestionAnswered)
            showNotificationQuestion = true
    }
    val syncDeadline: () -> Unit = {
        TaskAlarmScheduler.sync(context, context.appContainer().projectRepository.activeTask.value)
    }
    // Servicio que guarda imágenes JPEG de cada proyecto en memoria interna.
    val photoStore = androidx.compose.runtime.remember(context) { TaskPhotoStore(context) }
    // Archivos PDF, imágenes y documentos se almacenan PRIVADAMENTE por proyecto.
    val evidenceStore = androidx.compose.runtime.remember(context) { ProjectEvidenceStore(context) }
    var attachmentRevision by rememberSaveable { mutableStateOf(0) }
    // Contador que fuerza releer miniatura tras fotografiar otra vez.
    var photoRevision by rememberSaveable { mutableStateOf(0) }
    // La foto se asocia al proyecto actual; no a una tarea individual.
    val currentProjectId = uiState.currentProject?.id
    // Lee ruta de la foto existente para proyecto elegido.
    // La ficha no necesita permanecer abierta para conservar la evidencia.
    val attachment = androidx.compose.runtime.remember(currentProjectId, attachmentRevision) {
        currentProjectId?.let { evidenceStore.latest(it) }
    }
    val photo = androidx.compose.runtime.remember(currentProjectId, photoRevision) {
        currentProjectId?.let { photoStore.latest(it)?.absolutePath }
    }
    // Contrato TakePicturePreview abre cámara del dispositivo y devuelve Bitmap miniatura.
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null && currentProjectId != null) {
            // Persiste la foto JPEG y actualiza la pantalla si se guardó.
            val saved = photoStore.save(currentProjectId, bitmap)
            if (saved != null) {
                photoRevision++
                Toast.makeText(context, "Fotografía guardada en este proyecto", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(context, "No se pudo guardar la fotografía", Toast.LENGTH_SHORT).show()
        }
    }
    // Selector nativo del sistema: admite imágenes, PDF y otros archivos.
    // No solicitamos permisos de almacenamiento porque OpenDocument da acceso al URI escogido.
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && currentProjectId != null) {
            val stored = evidenceStore.import(currentProjectId, uri)
            if (stored != null) {
                attachmentRevision++
                Toast.makeText(context, "Archivo adjuntado al proyecto", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(context, "No se pudo adjuntar (máximo 20 MB)", Toast.LENGTH_LONG).show()
        }
    }
        // En Android 13+ consulta permiso POST_NOTIFICATIONS.
    var hasNotifications by rememberSaveable { mutableStateOf(
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    ) }
    // Lanza diálogo Android solicitando permiso de notificaciones cuando haga falta.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotifications = granted
        notificationSettings.notificationsEnabled = granted
        syncDeadline()
        Toast.makeText(context, if (granted) "Notificaciones activadas" else "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
    }
    val acceptNotifications: () -> Unit = {
        notificationSettings.notificationQuestionAnswered = true
        showNotificationQuestion = false
        if (Build.VERSION.SDK_INT >= 33 && !notifier.hasPermission()) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotifications = true
            notificationSettings.notificationsEnabled = true
            syncDeadline()
        }
    }
    // Muestra errores/confirmaciones con Toast y borra mensaje para no repetirlo.
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }
    HomeScreen(
        uiState = uiState,
        onOpenWorkflow = onOpenWorkflow,
        onOpenProjectTasks = onOpenProjectTasks,
        // Modal Crear proyecto -> HomeViewModel.createProject -> repositorio -> SharedPreferences.
        onCreateProject = viewModel::createProject,
        // Clic en fila de lista -> cambia proyecto actual.
        onSelectProject = viewModel::selectProject,
        // Modal Iniciar -> valida y crea tarea en repositorio.
        onStartTask = viewModel::startTask,
        // Botón Detener -> cierra cronómetro y registra minutos.
        onStopTask = viewModel::stopActiveTask,
        // Mantiene callback anterior de pausa para compatibilidad; ahora la pausa real está en RunningPage.
        onTogglePause = viewModel::toggleTaskPause,
        // Modal Registro manual -> guarda TimeLog.
        onLogHours = viewModel::logHours,
        // Confirmar borrar -> elimina proyecto y datos relacionados.
        onDeleteProject = { id ->
            // Al eliminar proyecto también se borra su documento adjunto.
            evidenceStore.delete(id)
            viewModel.deleteProject(id)
        },
        photoPath = photo,
        photoRevision = photoRevision,
        attachmentName = attachment?.originalName,
        onPickAttachment = {
            if (currentProjectId != null) documentPicker.launch(arrayOf("*/*"))
            else Toast.makeText(context, "Selecciona un proyecto", Toast.LENGTH_SHORT).show()
        },
        onOpenAttachment = {
            if (currentProjectId != null && !evidenceStore.open(currentProjectId))
                Toast.makeText(context, "No hay una aplicación para abrir este archivo", Toast.LENGTH_SHORT).show()
        },
        notificationsEnabled = hasNotifications && notificationSettings.notificationsEnabled,
        // Botón Cámara -> inicia TakePicturePreview si existe proyecto seleccionado.
        onCapturePhoto = {
            if (currentProjectId != null) camera.launch(null)
            else Toast.makeText(context, "Selecciona un proyecto", Toast.LENGTH_SHORT).show()
        },
        // Botón Activar notificaciones -> solicita permiso del sistema si corresponde.
        onEnableNotifications = acceptNotifications,
    )
    if (showNotificationQuestion) {
        val declineNotifications: () -> Unit = {
            notificationSettings.notificationQuestionAnswered = true
            notificationSettings.notificationsEnabled = false
            syncDeadline()
            showNotificationQuestion = false
        }
        AlertDialog(
            onDismissRequest = declineNotifications,
            title = { Text("¿Activar notificaciones?") },
            text = { Text("DeepSource puede avisarte con sonido y vibración cuando se cumpla el tiempo de una tarea, incluso fuera de la aplicación. Comienzan desactivadas y puedes cambiarlo después en Perfil → Configuración.") },
            confirmButton = { TextButton(onClick = acceptNotifications) { Text("Activar") } },
            dismissButton = { TextButton(onClick = declineNotifications) { Text("Ahora no") } },
        )
    }
}

@Composable
// Dibuja solo Inicio, proyectos y modales: Perfil vive en su propia ruta.
fun HomeScreen(
    uiState: HomeUiState,
    onOpenWorkflow: (String) -> Unit = {},
    onOpenProjectTasks: (String) -> Unit = {},
    onCreateProject: (String, String) -> Unit,
    onSelectProject: (String) -> Unit,
    onStartTask: (String, String) -> Unit,
    onStopTask: () -> Unit,
    onTogglePause: () -> Unit,
    onLogHours: (String, Long, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    photoPath: String? = null,
    photoRevision: Int = 0,
    attachmentName: String? = null,
    onPickAttachment: () -> Unit = {},
    onOpenAttachment: () -> Unit = {},
    notificationsEnabled: Boolean = false,
    onCapturePhoto: () -> Unit = {},
    onEnableNotifications: () -> Unit = {},
) {
    // Estado Compose que controla si aparece modal Cargar proyecto.
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    // Estado Compose de modal Iniciar nueva tarea.
    var showStartDialog by rememberSaveable { mutableStateOf(false) }
    // Estado Compose de modal Registrar horas manualmente.
    var showLogDialog by rememberSaveable { mutableStateOf(false) }
    // Modal de Plan Sin Límites: corrige la antigua acción confusa de pausar.
    // La sección de evidencias inicia cerrada; se abre a petición del usuario.
    var evidenceExpanded by rememberSaveable { mutableStateOf(false) }

    DsScreenBackground {
        // [V4/DISEÑO] Column exterior fija barra inferior; el contenido del Home se desplaza aparte.
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Column(
            Modifier.weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DsBrandHeader(iconSize = 44.dp)

            // Sin desplegable de usuario: acceso exclusivo en la cuarta pestaña Perfil.
                // Reloj visible alimentado por los datos de HomeViewModel.
                CurrentTimeSection(
                    timeString = uiState.currentTimeString,
                    dateString = uiState.currentDateString,
                )

                // Muestra proyecto activo y cronómetro; tiene Detener/Crear proyecto.
                CurrentProjectCard(
                    currentProject = uiState.currentProject,
                    weeklyMinutes = uiState.currentProjectWeeklyMinutes,
                    activeTask = uiState.activeTask,
                    activeElapsedSeconds = uiState.activeTaskElapsedSeconds,
                    // Envía acción Detener al botón situado en CurrentProjectCard.
                    onStopTask = onStopTask,
                    // Muestra ventana de nuevo proyecto.
                    onCreateProject = { showCreateDialog = true },
                )

                // [FLUJO/BOTONES PRINCIPALES] HomeActions CONSERVA su diseño original.
                // La acción ahora abre formularios completos Material v2; quedan los diálogos rápidos abajo.
                HomeActions(
                    onStartTaskClick = { onOpenWorkflow(if (uiState.activeTask != null) Routes.RUNNING else Routes.START) },
                    onLogHoursClick = { onOpenWorkflow(Routes.MANUAL) },
                )

                // Se eliminó el bloque duplicado "Herramientas de tiempo y equipo".
                // Historial y Perfil están disponibles en la barra inferior; administración solo desde Perfil con rol ADMIN.

                // Tarjeta plegable: ocupa una sola fila hasta que el usuario la abre.
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(14.dp).animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Alterna entre desplegar/plegar. No borra archivos ni altera el ViewModel.
                        OutlinedButton(onClick = { evidenceExpanded = !evidenceExpanded },
                            modifier = Modifier.fillMaxWidth()) {
                            Text(if (evidenceExpanded) "⌃  Ocultar archivos y fotos" else "⌄  Adjuntar archivos o fotos")
                        }
                        AnimatedVisibility(visible = evidenceExpanded, enter = fadeIn(), exit = fadeOut()) {
                            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text("Evidencias del proyecto", style = MaterialTheme.typography.titleMedium)
                                Text("Puedes tomar una fotografía o importar un archivo desde tu teléfono.",
                                    style = MaterialTheme.typography.bodySmall)
                                // Abre explorador de documentos, copia el archivo al espacio privado.
                                OutlinedButton(onClick = onPickAttachment,
                                    enabled = uiState.currentProject != null, modifier = Modifier.fillMaxWidth()) {
                                    Text("📎 Subir archivo o imagen")
                                }
                                // Cámara antigua conservada: guarda la foto JPEG del proyecto.
                                OutlinedButton(onClick = onCapturePhoto,
                                    enabled = uiState.currentProject != null, modifier = Modifier.fillMaxWidth()) {
                                    Text("📷 Tomar fotografía")
                                }
                                if (attachmentName != null) {
                                    Text("Último archivo: $attachmentName", style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = onOpenAttachment) { Text("Abrir archivo adjunto") }
                                }
                                // Previsualización de la fotografía obtenida con cámara.
                                AnimatedVisibility(visible = photoPath != null, enter = fadeIn(), exit = fadeOut()) {
                                    val imageBitmap = androidx.compose.runtime.remember(photoPath, photoRevision) {
                                        photoPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
                                    }
                                    if (imageBitmap != null) Image(
                                        bitmap = imageBitmap,
                                        contentDescription = "Última fotografía del proyecto",
                                        modifier = Modifier.fillMaxWidth().height(150.dp),
                                        contentScale = ContentScale.Fit,
                                    )
                                }
                                if (!notificationsEnabled) {
                                    OutlinedButton(onClick = onEnableNotifications,
                                        modifier = Modifier.fillMaxWidth()) { Text("Activar notificaciones") }
                                } else Text("✓ Avisos habilitados", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Presenta proyectos con seleccionar, añadir y eliminar.
                ProjectList(
                    projects = uiState.projects,
                    onSelectProject = onSelectProject,
                    // Muestra ventana de nuevo proyecto.
                    onCreateProject = { showCreateDialog = true },
                    onDeleteProject = onDeleteProject,
                    onOpenProjectTasks = onOpenProjectTasks,
                )
        }
        // Barra visible desde el primer Home tras el login, nunca al final del scroll.
        DsBottomNav(Routes.HOME, uiState.activeTask != null, onOpenWorkflow)
        }
    }

    // Monta CreateProjectDialog cuando se pulsa Crear/Cargar proyecto.
    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = onCreateProject,
        )
    }

    // Monta StartTaskDialog cuando se pulsa Iniciar tarea.
    if (showStartDialog) {
        StartTaskDialog(
            projects = uiState.projects,
            selectedProject = uiState.currentProject,
            onDismiss = { showStartDialog = false },
            onStart = onStartTask,
        )
    }

    // Monta LogHoursDialog cuando se pulsa Registrar horas.
    if (showLogDialog) {
        LogHoursDialog(
            projects = uiState.projects,
            selectedProject = uiState.currentProject,
            onDismiss = { showLogDialog = false },
            onLog = onLogHours,
        )
    }
}
