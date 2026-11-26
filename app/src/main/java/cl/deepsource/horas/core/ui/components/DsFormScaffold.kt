package cl.deepsource.horas.core.ui.components
// [GUIA/ARCHIVO] Base visual compartida de los formularios Recuperar y Solicitar acceso.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/core/ui/components/DsFormScaffold.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cl.deepsource.horas.R
import cl.deepsource.horas.core.ui.theme.DsCyan
import cl.deepsource.horas.core.ui.theme.DsOutline
import cl.deepsource.horas.core.ui.theme.DsSurface
import androidx.compose.foundation.BorderStroke

/** Estructura común para pantallas secundarias con formulario (recuperar contraseña, solicitar acceso...). */
@Composable
// [GUIA/FUNCION] DsFormScaffold: Estructura reutilizable de formularios secundarios con botón Volver, título, subtítulo y contenido.
fun DsFormScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    DsScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 16.dp),
        ) {
            // [GUIA/EVENTO] onClick: se ejecuta al tocar este botón o control; delega en onBack.
            // [GUIA/ESTILO] Botón Volver: ejecuta callback que hace popBackStack en AppNavHost.
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = DsCyan)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_back), color = DsCyan)
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = DsSurface.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, DsOutline),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                // [GUIA/ESTILO] Dentro se inserta el contenido específico de la pantalla secundaria.
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
            }
        }
    }
}
