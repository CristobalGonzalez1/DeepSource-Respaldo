package cl.deepsource.horas.navigation
// [GUIA/ARCHIVO] Mapa de navegación: dónde está cada pantalla y qué botón cambia de ruta.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/navigation/AppNavHost.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import androidx.compose.runtime.Composable
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
// [GUIA/FUNCION] AppNavHost: Grafo de navegación Compose: conecta Login, Recuperar, Solicitar acceso y Home.
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    // [FLUJO/MVVM] Un ViewModel de flujo compartido entre todas las vistas nuevas.
    // HomeViewModel anterior NO se elimina ni se reemplaza.
    val workflowViewModel: WorkflowViewModel = viewModel(factory = WorkflowViewModel.Factory)
    fun navigateTo(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }
    // [GUIA/DETALLE] La aplicación inicia siempre en Login; no hay restauración automática de sesión.
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        // [GUIA/DETALLE] Componente que entrega la pantalla de login y sus funciones de navegación.
        composable(Routes.LOGIN) {
            LoginRoute(
                onLoggedIn = {
                    // [GUIA/DETALLE] Login exitoso: abre Home y quita Login del historial para impedir volver con Atrás.
                    navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                // [GUIA/DETALLE] El botón Recuperar contraseña abre FORGOT_PASSWORD.
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                // [GUIA/DETALLE] El botón Solicitar acceso abre REQUEST_ACCESS.
                onRequestAccess = { navController.navigate(Routes.REQUEST_ACCESS) },
            )
        }
        // [GUIA/DETALLE] Volver elimina pantalla actual de la pila y retorna al Login.
        composable(Routes.FORGOT_PASSWORD) { ForgotPasswordRoute(onBack = { navController.popBackStack() }) }
        // [GUIA/DETALLE] Volver desde solicitar acceso regresa al Login.
        composable(Routes.REQUEST_ACCESS) { RequestAccessRoute(onBack = { navController.popBackStack() }) }
        // [GUIA/DETALLE] Aquí se crea HomeRoute, con cierre de sesión que devuelve a Login.
        composable(Routes.HOME) {
            HomeRoute(
                // [FLUJO/ACCESOS] Botones Material del Home usan el NavHost, no crean Activities nuevas.
                onOpenWorkflow = { navigateTo(it) },
                onOpenProjectTasks = { id -> workflowViewModel.selectDetail(id); navigateTo(Routes.DETAIL) },
                onSignedOut = {
                    // [GUIA/DETALLE] Al cerrar sesión se vuelve a Login y se elimina Home del historial.
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                },
            )
        }
        // [FLUJO/NAVEGACIÓN] Mapeo único: cada ruta se une a una pantalla Compose y al mismo VM.
        listOf(Routes.START, Routes.RUNNING, Routes.ALERT, Routes.FINISH, Routes.SAVED,
            Routes.DETAIL, Routes.MANAGER, Routes.ASSIGNMENTS, Routes.HISTORY,
            Routes.MANUAL, Routes.REPORT, Routes.SETTINGS).forEach { page ->
            composable(page) {
                WorkflowRoute(page, workflowViewModel, onNavigate = { navigateTo(it) })
            }
        }
    }
}
