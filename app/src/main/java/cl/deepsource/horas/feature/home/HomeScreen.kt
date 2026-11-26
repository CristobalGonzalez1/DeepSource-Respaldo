package cl.deepsource.horas.feature.home
// [GUIA/ARCHIVO] Vista Home + Route: el Route conecta ViewModel, permisos y cámara; HomeScreen solo dibuja.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/feature/home/HomeScreen.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

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
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import cl.deepsource.horas.feature.home.components.HomeHeader
import cl.deepsource.horas.feature.home.components.LimitCard
import cl.deepsource.horas.feature.home.components.LimitInfoDialog
import cl.deepsource.horas.navigation.Routes
import cl.deepsource.horas.feature.home.components.LogHoursDialog
import cl.deepsource.horas.feature.home.components.ProjectList
import cl.deepsource.horas.feature.home.components.StartTaskDialog

@Composable
// [GUIA/FUNCION] HomeRoute: Une estado y métodos de HomeViewModel con la pantalla y recursos Android de cámara/permisos.
fun HomeRoute(
    onSignedOut: () -> Unit,
    onOpenWorkflow: (String) -> Unit = {},
    onOpenProjectTasks: (String) -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    // [GUIA/DETALLE] Observa ViewModel con conciencia del ciclo de vida para actualizar Home.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // [GUIA/DETALLE] Servicio que guarda imágenes JPEG de cada proyecto en memoria interna.
    val photoStore = androidx.compose.runtime.remember(context) { TaskPhotoStore(context) }
    // [V4/ARCHIVOS] Archivos PDF, imágenes y documentos se almacenan PRIVADAMENTE por proyecto.
    val evidenceStore = androidx.compose.runtime.remember(context) { ProjectEvidenceStore(context) }
    var attachmentRevision by rememberSaveable { mutableStateOf(0) }
    // [GUIA/DETALLE] Contador que fuerza releer miniatura tras fotografiar otra vez.
    var photoRevision by rememberSaveable { mutableStateOf(0) }
    // [GUIA/DETALLE] La foto se asocia al proyecto actual; no a una tarea individual.
    val currentProjectId = uiState.currentProject?.id
    // [GUIA/DETALLE] Lee ruta de la foto existente para proyecto elegido.
    // [V4/ARCHIVOS] La ficha no necesita permanecer abierta para conservar la evidencia.
    val attachment = androidx.compose.runtime.remember(currentProjectId, attachmentRevision) {
        currentProjectId?.let { evidenceStore.latest(it) }
    }
    val photo = androidx.compose.runtime.remember(currentProjectId, photoRevision) {
        currentProjectId?.let { photoStore.latest(it)?.absolutePath }
    }
    // [GUIA/DETALLE] Contrato TakePicturePreview abre cámara del dispositivo y devuelve Bitmap miniatura.
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null && currentProjectId != null) {
            // [GUIA/DETALLE] Persiste la foto JPEG y actualiza la pantalla si se guardó.
            val saved = photoStore.save(currentProjectId, bitmap)
            if (saved != null) {
                photoRevision++
                Toast.makeText(context, "Fotografía guardada en este proyecto", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(context, "No se pudo guardar la fotografía", Toast.LENGTH_SHORT).show()
        }
    }
    // [V4/ARCHIVOS] Selector nativo del sistema: admite imágenes, PDF y otros archivos.
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
        // [GUIA/DETALLE] En Android 13+ consulta permiso POST_NOTIFICATIONS.
    var hasNotifications by rememberSaveable { mutableStateOf(
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    ) }
    // [GUIA/DETALLE] Lanza diálogo Android solicitando permiso de notificaciones cuando haga falta.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotifications = granted
        Toast.makeText(context, if (granted) "Notificaciones activadas" else "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
    }
    // [GUIA/DETALLE] Muestra errores/confirmaciones con Toast y borra mensaje para no repetirlo.
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
        // [GUIA/DETALLE] Botón Cerrar sesión: limpia repositorio de auth y navega a Login.
        onSignOut = { viewModel.onSignOut(); onSignedOut() },
        // [GUIA/DETALLE] Modal Crear proyecto -> HomeViewModel.createProject -> repositorio -> SharedPreferences.
        onCreateProject = viewModel::createProject,
        // [GUIA/DETALLE] Clic en fila de lista -> cambia proyecto actual.
        onSelectProject = viewModel::selectProject,
        // [GUIA/DETALLE] Modal Iniciar -> valida y crea tarea en repositorio.
        onStartTask = viewModel::startTask,
        // [GUIA/DETALLE] Botón Detener -> cierra cronómetro y registra minutos.
        onStopTask = viewModel::stopActiveTask,
        // [FLUJO/LEGADO] Mantiene callback anterior de pausa para compatibilidad; ahora la pausa real está en RunningPage.
        onTogglePause = viewModel::toggleTaskPause,
        // [GUIA/DETALLE] Modal Registro manual -> guarda TimeLog.
        onLogHours = viewModel::logHours,
        // [GUIA/DETALLE] Confirmar borrar -> elimina proyecto y datos relacionados.
        onDeleteProject = { id ->
            // [V4/ARCHIVOS] Al eliminar proyecto también se borra su documento adjunto.
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
        notificationsEnabled = hasNotifications,
        // [GUIA/DETALLE] Botón Cámara -> inicia TakePicturePreview si existe proyecto seleccionado.
        onCapturePhoto = {
            if (currentProjectId != null) camera.launch(null)
            else Toast.makeText(context, "Selecciona un proyecto", Toast.LENGTH_SHORT).show()
        },
        // [GUIA/DETALLE] Botón Activar notificaciones -> solicita permiso del sistema si corresponde.
        onEnableNotifications = {
            if (Build.VERSION.SDK_INT >= 33 && !hasNotifications) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else Toast.makeText(context, "Las notificaciones están habilitadas", Toast.LENGTH_SHORT).show()
        },
    )
}

@Composable
// [GUIA/FUNCION] HomeScreen: Dibuja Home, perfil, tarjeta actual, controles, proyectos y modales, usando callbacks.
fun HomeScreen(
    uiState: HomeUiState,
    onOpenWorkflow: (String) -> Unit = {},
    onOpenProjectTasks: (String) -> Unit = {},
    onSignOut: () -> Unit,
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
    // [GUIA/DETALLE] Estado Compose que controla si aparece modal Cargar proyecto.
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    // [GUIA/DETALLE] Estado Compose de modal Iniciar nueva tarea.
    var showStartDialog by rememberSaveable { mutableStateOf(false) }
    // [GUIA/DETALLE] Estado Compose de modal Registrar horas manualmente.
    var showLogDialog by rememberSaveable { mutableStateOf(false) }
    // [FLUJO/ESTADO] Modal de Plan Sin Límites: corrige la antigua acción confusa de pausar.
    var showPlanDialog by rememberSaveable { mutableStateOf(false) }
    // [V4/DESPLEGABLE] La sección de evidencias inicia cerrada; se abre a petición del usuario.
    var evidenceExpanded by rememberSaveable { mutableStateOf(false) }
    // [GUIA/DETALLE] Estado visual: alterna pantalla principal y perfil sin una ruta nueva.
    var showProfile by rememberSaveable { mutableStateOf(false) }
    // [GUIA/DETALLE] El botón Atrás físico vuelve desde perfil a inicio.
    BackHandler(enabled = showProfile) { showProfile = false }

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

            // [GUIA/DETALLE] Rama de perfil; incluye nombres, correo y botón Cerrar sesión.
            if (showProfile) {
                // [GUIA/DETALLE] Botón Volver a inicio: cierra vista de perfil.
                TextButton(onClick = { showProfile = false }) { Text("← Volver a inicio") }
                Text("Mi perfil", style = MaterialTheme.typography.headlineMedium)
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Nombre", style = MaterialTheme.typography.labelMedium)
                        Text(uiState.userName, style = MaterialTheme.typography.bodyLarge)
                        HorizontalDivider()
                        Text("Correo electrónico", style = MaterialTheme.typography.labelMedium)
                        Text(uiState.userEmail, style = MaterialTheme.typography.bodyLarge)
                        HorizontalDivider()
                        Text("Tipo de cuenta", style = MaterialTheme.typography.labelMedium)
                        Text(uiState.userRole, style = MaterialTheme.typography.bodyLarge)
                        HorizontalDivider()
                        Text("Proyectos cargados: ${uiState.projects.size}")
                    }
                }
                // [V5/PERFIL] Ajustes accesibles sin añadir otra pestaña a la barra inferior.
                OutlinedButton(onClick = { onOpenWorkflow(Routes.SETTINGS) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Text("  Configuración y notificaciones")
                }
                // [GUIA/DETALLE] Botón Cerrar sesión: ejecuta callback de HomeRoute.
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar sesión")
                }
            } else {
                // [GUIA/DETALLE] Tarjeta de usuario; al tocarla pone showProfile=true.
                HomeHeader(
                    userName = uiState.userName,
                    userEmail = uiState.userEmail,
                    // [GUIA/DETALLE] Acción de abrir perfil desde avatar/encabezado.
                    onOpenProfile = { showProfile = true },
                )

                // [GUIA/DETALLE] Reloj visible alimentado por los datos de HomeViewModel.
                CurrentTimeSection(
                    timeString = uiState.currentTimeString,
                    dateString = uiState.currentDateString,
                )

                // [GUIA/DETALLE] Muestra proyecto activo y cronómetro; tiene Detener/Crear proyecto.
                CurrentProjectCard(
                    currentProject = uiState.currentProject,
                    weeklyMinutes = uiState.currentProjectWeeklyMinutes,
                    activeTask = uiState.activeTask,
                    activeElapsedSeconds = uiState.activeTaskElapsedSeconds,
                    // [GUIA/DETALLE] Envía acción Detener al botón situado en CurrentProjectCard.
                    onStopTask = onStopTask,
                    // [GUIA/DETALLE] Muestra ventana de nuevo proyecto.
                    onCreateProject = { showCreateDialog = true },
                )

                // [FLUJO/BOTONES PRINCIPALES] HomeActions CONSERVA su diseño original.
                // La acción ahora abre formularios completos Material v2; quedan los diálogos rápidos abajo.
                HomeActions(
                    onStartTaskClick = { onOpenWorkflow(if (uiState.activeTask != null) Routes.RUNNING else Routes.START) },
                    onLogHoursClick = { onOpenWorkflow(Routes.MANUAL) },
                )

                // [V4/LIMPIEZA] Se eliminó el bloque duplicado "Herramientas de tiempo y equipo".
                // Historial y Equipo siguen disponibles permanentemente en la barra de navegación.

                // [V4/EVIDENCIAS] Tarjeta plegable: ocupa una sola fila hasta que el usuario la abre.
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(14.dp).animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // [V4/BOTON] Alterna entre desplegar/plegar. No borra archivos ni altera el ViewModel.
                        OutlinedButton(onClick = { evidenceExpanded = !evidenceExpanded },
                            modifier = Modifier.fillMaxWidth()) {
                            Text(if (evidenceExpanded) "⌃  Ocultar archivos y fotos" else "⌄  Adjuntar archivos o fotos")
                        }
                        AnimatedVisibility(visible = evidenceExpanded, enter = fadeIn(), exit = fadeOut()) {
                            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text("Evidencias del proyecto", style = MaterialTheme.typography.titleMedium)
                                Text("Puedes tomar una fotografía o importar un archivo desde tu teléfono.",
                                    style = MaterialTheme.typography.bodySmall)
                                // [V4/BOTON] Abre explorador de documentos, copia el archivo al espacio privado.
                                OutlinedButton(onClick = onPickAttachment,
                                    enabled = uiState.currentProject != null, modifier = Modifier.fillMaxWidth()) {
                                    Text("📎 Subir archivo o imagen")
                                }
                                // [GUIA/BOTON] Cámara antigua conservada: guarda la foto JPEG del proyecto.
                                OutlinedButton(onClick = onCapturePhoto,
                                    enabled = uiState.currentProject != null, modifier = Modifier.fillMaxWidth()) {
                                    Text("📷 Tomar fotografía")
                                }
                                if (attachmentName != null) {
                                    Text("Último archivo: $attachmentName", style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = onOpenAttachment) { Text("Abrir archivo adjunto") }
                                }
                                // [GUIA/DETALLE] Previsualización de la fotografía obtenida con cámara.
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

                // [FLUJO/PLAN] La tarjeta conserva su apariencia; al tocar abre INFORMACIÓN del plan.
                // Pausar y reanudar ahora están en la pantalla Tarea en curso.
                LimitCard(
                    onClick = { showPlanDialog = true },
                    planName = "Plan Sin Límites",
                )

                // [GUIA/DETALLE] Presenta proyectos con seleccionar, añadir y eliminar.
                ProjectList(
                    projects = uiState.projects,
                    onSelectProject = onSelectProject,
                    // [GUIA/DETALLE] Muestra ventana de nuevo proyecto.
                    onCreateProject = { showCreateDialog = true },
                    onDeleteProject = onDeleteProject,
                    onOpenProjectTasks = onOpenProjectTasks,
                )
            }
        }
        // [V4/NAVEGACION] Barra visible desde el primer Home tras el login, nunca al final del scroll.
        DsBottomNav(Routes.HOME, uiState.activeTask != null, onOpenWorkflow)
        }
    }

    if (showPlanDialog) {
        // [FLUJO/MODAL] Se reutiliza LimitInfoDialog que ya existía sin usarse.
        LimitInfoDialog(
            projectCount = uiState.projects.size,
            weeklyMinutes = uiState.currentProjectWeeklyMinutes,
            onDismiss = { showPlanDialog = false },
        )
    }

    // [GUIA/DETALLE] Monta CreateProjectDialog cuando se pulsa Crear/Cargar proyecto.
    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = onCreateProject,
        )
    }

    // [GUIA/DETALLE] Monta StartTaskDialog cuando se pulsa Iniciar tarea.
    if (showStartDialog) {
        StartTaskDialog(
            projects = uiState.projects,
            selectedProject = uiState.currentProject,
            onDismiss = { showStartDialog = false },
            onStart = onStartTask,
        )
    }

    // [GUIA/DETALLE] Monta LogHoursDialog cuando se pulsa Registrar horas.
    if (showLogDialog) {
        LogHoursDialog(
            projects = uiState.projects,
            selectedProject = uiState.currentProject,
            onDismiss = { showLogDialog = false },
            onLog = onLogHours,
        )
    }
}
