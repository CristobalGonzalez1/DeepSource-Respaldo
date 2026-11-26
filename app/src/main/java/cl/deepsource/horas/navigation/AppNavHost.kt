package cl.deepsource.horas.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.deepsource.horas.appContainer
import cl.deepsource.horas.feature.auth.domain.RolePermissions
import cl.deepsource.horas.feature.auth.domain.UserRole
import cl.deepsource.horas.feature.profile.ProfileRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.deepsource.horas.feature.auth.presentation.forgot.ForgotPasswordRoute
import cl.deepsource.horas.feature.auth.presentation.login.LoginRoute
import cl.deepsource.horas.feature.auth.presentation.request.RequestAccessRoute
import cl.deepsource.horas.feature.home.HomeRoute
import cl.deepsource.horas.feature.workflow.WorkflowRoute
import cl.deepsource.horas.feature.workflow.WorkflowViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
// AppNavHost: Grafo de navegación Compose: conecta Login, Recuperar, Solicitar acceso y Home.
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    externalRoute: String? = null,
    onExternalRouteConsumed: () -> Unit = {},
) {
    // Un ViewModel de flujo compartido entre todas las vistas nuevas.
    // HomeViewModel anterior NO se elimina ni se reemplaza.
    val workflowViewModel: WorkflowViewModel = viewModel(factory = WorkflowViewModel.Factory)
    val authRepository = LocalContext.current.appContainer().authRepository
    val currentUser by authRepository.currentUser.collectAsStateWithLifecycle()
    fun navigateTo(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }
    // Se calcula UNA sola vez al crear el NavHost; no navegar a Login por defecto.
    // Un intent abierto desde widget, notificación o launcher usa la misma sesión restaurada.
    val initialRoute = androidx.compose.runtime.remember {
        val savedUser = authRepository.currentUser.value
        when {
            savedUser == null -> Routes.LOGIN
            externalRoute in setOf(Routes.ALERT, Routes.RUNNING, Routes.HOME, Routes.FINISH) -> externalRoute!!
            savedUser.role == UserRole.ADMIN -> Routes.MANAGER
            else -> Routes.HOME
        }
    }
    // Los nuevos intents (onNewIntent) también respetan la sesión, sin doble navegación.
    LaunchedEffect(externalRoute, currentUser?.id) {
        if (externalRoute in setOf(Routes.ALERT, Routes.RUNNING, Routes.HOME, Routes.FINISH) &&
            currentUser != null && navController.currentDestination?.route != Routes.LOGIN
        ) {
            if (navController.currentDestination?.route != externalRoute) navigateTo(externalRoute!!)
            onExternalRouteConsumed()
        }
    }
    // Si Android invalida la Activity pero no los datos, la sesión se recupera desde SharedPreferences.
    NavHost(navController = navController, startDestination = initialRoute) {
        // Componente que entrega la pantalla de login y sus funciones de navegación.
        composable(Routes.LOGIN) {
            LoginRoute(
                onLoggedIn = {
                    // Login exitoso: abre Home y quita Login del historial para impedir volver con Atrás.
                    // V8: la cuenta ADMIN entra directamente a su panel; NORMAL conserva Inicio.
                    val entry = when {
                        externalRoute in setOf(Routes.ALERT, Routes.RUNNING, Routes.HOME, Routes.FINISH) -> externalRoute!!
                        authRepository.currentUser.value?.role == UserRole.ADMIN -> Routes.MANAGER
                        else -> Routes.HOME
                    }
                    navController.navigate(entry) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                    if (externalRoute != null) onExternalRouteConsumed()
                },
                // El botón Recuperar contraseña abre FORGOT_PASSWORD.
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                // El botón Solicitar acceso abre REQUEST_ACCESS.
                onRequestAccess = { navController.navigate(Routes.REQUEST_ACCESS) },
            )
        }
        // Volver elimina pantalla actual de la pila y retorna al Login.
        composable(Routes.FORGOT_PASSWORD) { ForgotPasswordRoute(onBack = { navController.popBackStack() }) }
        // Volver desde solicitar acceso regresa al Login.
        composable(Routes.REQUEST_ACCESS) { RequestAccessRoute(onBack = { navController.popBackStack() }) }
        // Aquí se crea HomeRoute, con cierre de sesión que devuelve a Login.
        composable(Routes.HOME) {
            HomeRoute(
                // Botones Material del Home usan el NavHost, no crean Activities nuevas.
                onOpenWorkflow = { navigateTo(it) },
                onOpenProjectTasks = { id -> workflowViewModel.selectDetail(id); navigateTo(Routes.DETAIL) },
            )
        }
        composable(Routes.PROFILE) {
            ProfileRoute(
                onNavigate = { navigateTo(it) },
                onSignedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        // Mapeo único: cada ruta se une a una pantalla Compose y al mismo VM.
        listOf(Routes.START, Routes.RUNNING, Routes.ALERT, Routes.FINISH, Routes.SAVED,
            Routes.DETAIL, Routes.MANAGER, Routes.ASSIGNMENTS, Routes.HISTORY,
            Routes.MANUAL, Routes.REPORT, Routes.SETTINGS).forEach { page ->
            composable(page) {
                // Se observa DENTRO de cada destino. El grafo NavHost se recuerda y no debe capturar
                // un usuario obsoleto (por ejemplo, null antes de completar el login).
                val currentUser by authRepository.currentUser.collectAsStateWithLifecycle()
                // Control de rol en el destino: no basta con ocultar el botón del panel.
                if (page in setOf(Routes.MANAGER, Routes.ASSIGNMENTS) && !RolePermissions.canManageAssignments(currentUser)) {
                    LaunchedEffect(page, currentUser?.role) {
                        navController.navigate(if (currentUser == null) Routes.LOGIN else Routes.PROFILE) {
                            popUpTo(page) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                } else {
                    WorkflowRoute(page, workflowViewModel, onNavigate = { navigateTo(it) })
                }
            }
        }
    }
}
