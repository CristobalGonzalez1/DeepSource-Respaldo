package cl.deepsource.horas.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.deepsource.horas.feature.auth.presentation.forgot.ForgotPasswordRoute
import cl.deepsource.horas.feature.auth.presentation.login.LoginRoute
import cl.deepsource.horas.feature.auth.presentation.request.RequestAccessRoute
import cl.deepsource.horas.feature.home.HomeRoute

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginRoute(
                onLoggedIn = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onRequestAccess = { navController.navigate(Routes.REQUEST_ACCESS) },
            )
        }
        composable(Routes.FORGOT_PASSWORD) { ForgotPasswordRoute(onBack = { navController.popBackStack() }) }
        composable(Routes.REQUEST_ACCESS) { RequestAccessRoute(onBack = { navController.popBackStack() }) }
        composable(Routes.HOME) {
            HomeRoute(
                onSignedOut = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                },
            )
        }
    }
}
