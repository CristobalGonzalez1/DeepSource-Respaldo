package cl.deepsource.horas.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.deepsource.horas.appContainer
import cl.deepsource.horas.core.ui.components.DsBottomNav
import cl.deepsource.horas.core.ui.components.DsBrandHeader
import cl.deepsource.horas.core.ui.components.DsScreenBackground
import cl.deepsource.horas.core.ui.theme.DsCyan
import cl.deepsource.horas.core.ui.theme.DsOutline
import cl.deepsource.horas.core.ui.theme.DsSurface
import cl.deepsource.horas.core.ui.theme.DsTextMuted
import cl.deepsource.horas.core.ui.theme.DsTextPrimary
import cl.deepsource.horas.feature.auth.domain.RolePermissions
import cl.deepsource.horas.navigation.Routes
import cl.deepsource.horas.widget.DeepSourceAppWidget

/** [V6/PERFIL] Pantalla independiente con sesión reactiva y herramientas según rol. */
@Composable
fun ProfileRoute(onNavigate: (String) -> Unit, onSignedOut: () -> Unit) {
    val container = LocalContext.current.appContainer()
    val user by container.authRepository.currentUser.collectAsStateWithLifecycle()
    val projects by container.projectRepository.projects.collectAsStateWithLifecycle()
    val activeTask by container.projectRepository.activeTask.collectAsStateWithLifecycle()
    val isAdmin = RolePermissions.canManageAssignments(user)

    DsScreenBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Mismo símbolo, tamaño y tipografía que Inicio, Login y las demás pantallas.
                DsBrandHeader(iconSize = 44.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = DsCyan)
                    Text("  Mi perfil", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = DsTextPrimary)
                }
                Surface(
                    shape = RoundedCornerShape(15.dp), color = DsSurface,
                    border = BorderStroke(1.dp, DsOutline), modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        Text("NOMBRE", color = DsTextMuted, fontSize = 11.sp)
                        Text(user?.name ?: "Sin sesión", color = DsTextPrimary)
                        HorizontalDivider(color = DsOutline)
                        Text("CORREO ELECTRÓNICO", color = DsTextMuted, fontSize = 11.sp)
                        Text(user?.email ?: "—", color = DsTextPrimary)
                        HorizontalDivider(color = DsOutline)
                        Text("TIPO DE CUENTA", color = DsTextMuted, fontSize = 11.sp)
                        Text(if (isAdmin) "Administrador" else "Usuario normal", color = DsCyan)
                        HorizontalDivider(color = DsOutline)
                        Text("Proyectos cargados: ${projects.size}", color = DsTextPrimary)
                    }
                }
                OutlinedButton(onClick = { onNavigate(Routes.SETTINGS) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = DsCyan)
                    Text("  Configuración y notificaciones")
                }
                if (isAdmin) {
                    // Únicamente ADMIN ve los accesos; el grafo y el ViewModel también verifican el rol.
                    Surface(
                        shape = RoundedCornerShape(15.dp), color = DsSurface,
                        border = BorderStroke(1.dp, DsCyan.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = DsCyan)
                                Text("  Administración", color = DsTextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Text("Gestión de equipo y asignaciones · solo administrador",
                                color = DsTextMuted, fontSize = 12.sp)
                            OutlinedButton(onClick = { onNavigate(Routes.MANAGER) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = DsCyan)
                                Text("  Vista de jefe")
                            }
                            OutlinedButton(onClick = { onNavigate(Routes.ASSIGNMENTS) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = DsCyan)
                                Text("  Asignaciones del equipo")
                            }
                        }
                    }
                }
                OutlinedButton(onClick = {
                    container.authRepository.signOut()
                    DeepSourceAppWidget.sendUpdateBroadcast(container.context)
                    onSignedOut()
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Text("  Cerrar sesión")
                }
            }
            DsBottomNav(Routes.PROFILE, activeTask != null, onNavigate)
        }
    }
}
