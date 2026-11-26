package cl.deepsource.horas.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.deepsource.horas.appContainer
import cl.deepsource.horas.feature.auth.domain.RolePermissions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.deepsource.horas.core.ui.theme.DsCyan
import cl.deepsource.horas.core.ui.theme.DsSurface
import cl.deepsource.horas.core.ui.theme.DsTextMuted
import cl.deepsource.horas.navigation.Routes

/** [V4/NAVEGACION] Barra inferior común al Home y al resto de pantallas después del login.
 * Se declara en un solo componente para que sus colores, orden y acciones sean idénticos.
 * Cada botón delega navegación al NavHost: no modifica los datos directamente.
 */
@Composable
fun DsBottomNav(page: String, hasActiveTask: Boolean, onNavigate: (String) -> Unit) {
    val user by LocalContext.current.appContainer().authRepository.currentUser.collectAsStateWithLifecycle()
    val entries = mutableListOf(
        Triple(Routes.HOME, Icons.Default.Home, "Inicio"),
        Triple(Routes.START, Icons.Default.Timer, "Tarea"),
        Triple(Routes.HISTORY, Icons.Default.History, "Historial"),
        Triple(Routes.PROFILE, Icons.Default.Person, "Perfil"),
    )
    if (RolePermissions.canManageAssignments(user)) {
        entries.add(Triple(Routes.MANAGER, Icons.Default.AdminPanelSettings, "Admin"))
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(DsSurface).padding(vertical = 5.dp, horizontal = 3.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        entries.forEach { (route, icon, title) ->
            val active = page == route ||
                (route == Routes.START && page in listOf(Routes.RUNNING, Routes.ALERT, Routes.FINISH, Routes.SAVED)) ||
                (route == Routes.PROFILE && page == Routes.SETTINGS) ||
                (route == Routes.MANAGER && page == Routes.ASSIGNMENTS)
            Column(
                modifier = Modifier.weight(1f).clickable {
                    // Si hay cronómetro activo, Tarea abre su vista en curso.
                    onNavigate(if (route == Routes.START && hasActiveTask) Routes.RUNNING else route)
                }.padding(7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(icon, contentDescription = title, modifier = Modifier.size(21.dp),
                    tint = if (active) DsCyan else DsTextMuted)
                Text(title, fontSize = 10.sp, color = if (active) DsCyan else DsTextMuted)
            }
        }
    }
}
