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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import cl.deepsource.horas.core.ui.components.DsBrandHeader
import cl.deepsource.horas.core.ui.components.DsScreenBackground
import cl.deepsource.horas.feature.home.components.CreateProjectDialog
import cl.deepsource.horas.feature.home.components.CurrentProjectCard
import cl.deepsource.horas.feature.home.components.CurrentTimeSection
import cl.deepsource.horas.feature.home.components.HomeActions
import cl.deepsource.horas.feature.home.components.HomeHeader
import cl.deepsource.horas.feature.home.components.LimitCard
import cl.deepsource.horas.feature.home.components.LogHoursDialog
import cl.deepsource.horas.feature.home.components.ProjectList
import cl.deepsource.horas.feature.home.components.StartTaskDialog

@Composable
fun HomeRoute(
    onSignedOut: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val photoStore = androidx.compose.runtime.remember(context) { TaskPhotoStore(context) }
    var photoRevision by rememberSaveable { mutableStateOf(0) }
    val currentProjectId = uiState.currentProject?.id
    val photo = androidx.compose.runtime.remember(currentProjectId, photoRevision) {
        currentProjectId?.let { photoStore.latest(it)?.absolutePath }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null && currentProjectId != null) {
            val saved = photoStore.save(currentProjectId, bitmap)
            if (saved != null) {
                photoRevision++
                Toast.makeText(context, "Fotografía guardada en este proyecto", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(context, "No se pudo guardar la fotografía", Toast.LENGTH_SHORT).show()
        }
    }
    var hasNotifications by rememberSaveable { mutableStateOf(
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    ) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotifications = granted
        Toast.makeText(context, if (granted) "Notificaciones activadas" else "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
    }
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }
    HomeScreen(
        uiState = uiState,
        onSignOut = { viewModel.onSignOut(); onSignedOut() },
        onCreateProject = viewModel::createProject,
        onSelectProject = viewModel::selectProject,
        onStartTask = viewModel::startTask,
        onStopTask = viewModel::stopActiveTask,
        onTogglePause = viewModel::toggleTaskPause,
        onLogHours = viewModel::logHours,
        onDeleteProject = viewModel::deleteProject,
        photoPath = photo,
        photoRevision = photoRevision,
        notificationsEnabled = hasNotifications,
        onCapturePhoto = {
            if (currentProjectId != null) camera.launch(null)
            else Toast.makeText(context, "Selecciona un proyecto", Toast.LENGTH_SHORT).show()
        },
        onEnableNotifications = {
            if (Build.VERSION.SDK_INT >= 33 && !hasNotifications) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else Toast.makeText(context, "Las notificaciones están habilitadas", Toast.LENGTH_SHORT).show()
        },
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
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
    notificationsEnabled: Boolean = false,
    onCapturePhoto: () -> Unit = {},
    onEnableNotifications: () -> Unit = {},
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showStartDialog by rememberSaveable { mutableStateOf(false) }
    var showLogDialog by rememberSaveable { mutableStateOf(false) }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showProfile) { showProfile = false }

    DsScreenBackground {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DsBrandHeader(iconSize = 44.dp)

            if (showProfile) {
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
                        Text("Proyectos cargados: ${uiState.projects.size}")
                    }
                }
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar sesión")
                }
            } else {
                HomeHeader(
                    userName = uiState.userName,
                    userEmail = uiState.userEmail,
                    onOpenProfile = { showProfile = true },
                )

                CurrentTimeSection(
                    timeString = uiState.currentTimeString,
                    dateString = uiState.currentDateString,
                )

                CurrentProjectCard(
                    currentProject = uiState.currentProject,
                    weeklyMinutes = uiState.currentProjectWeeklyMinutes,
                    activeTask = uiState.activeTask,
                    activeElapsedSeconds = uiState.activeTaskElapsedSeconds,
                    onStopTask = onStopTask,
                    onCreateProject = { showCreateDialog = true },
                )

                HomeActions(
                    onStartTaskClick = {
                        if (uiState.projects.isEmpty()) showCreateDialog = true else showStartDialog = true
                    },
                    onLogHoursClick = {
                        if (uiState.projects.isEmpty()) showCreateDialog = true else showLogDialog = true
                    },
                )

                // Recursos nativos: cámara del sistema y notificaciones Android.
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Evidencias de trabajo", style = MaterialTheme.typography.titleMedium)
                        Text("Adjunta una fotografía del avance al proyecto actual.", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = onCapturePhoto, enabled = uiState.currentProject != null, modifier = Modifier.fillMaxWidth()) {
                            Text("📷 Tomar fotografía")
                        }
                        AnimatedVisibility(visible = photoPath != null, enter = fadeIn(), exit = fadeOut()) {
                            val imageBitmap = androidx.compose.runtime.remember(photoPath, photoRevision) { photoPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() } }
                            if (imageBitmap != null) Image(
                                bitmap = imageBitmap,
                                contentDescription = "Última evidencia fotográfica del proyecto",
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        if (!notificationsEnabled) {
                            OutlinedButton(onClick = onEnableNotifications, modifier = Modifier.fillMaxWidth()) { Text("Activar notificaciones") }
                        } else Text("✓ Avisos de inicio y fin de tareas habilitados", style = MaterialTheme.typography.bodySmall)
                    }
                }

                LimitCard(
                    onClick = onTogglePause,
                    planName = "Plan Sin Límites",
                )

                ProjectList(
                    projects = uiState.projects,
                    onSelectProject = onSelectProject,
                    onCreateProject = { showCreateDialog = true },
                    onDeleteProject = onDeleteProject,
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = onCreateProject,
        )
    }

    if (showStartDialog) {
        StartTaskDialog(
            projects = uiState.projects,
            selectedProject = uiState.currentProject,
            onDismiss = { showStartDialog = false },
            onStart = onStartTask,
        )
    }

    if (showLogDialog) {
        LogHoursDialog(
            projects = uiState.projects,
            selectedProject = uiState.currentProject,
            onDismiss = { showLogDialog = false },
            onLog = onLogHours,
        )
    }
}
