package cl.deepsource.horas.core.ui.components
// [GUIA/ARCHIVO] Botón de Google; la ejecución de la cuenta se implementa en LoginRoute.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/core/ui/components/GoogleSignInButton.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.deepsource.horas.R
import cl.deepsource.horas.core.ui.theme.GoogleColors


@Composable
// [GUIA/FUNCION] GoogleSignInButton: Botón visual de Google con colores oficiales; dispara el callback que le entrega LoginRoute.
fun GoogleSignInButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    OutlinedButton(
        // [GUIA/EVENTO] onClick: se ejecuta al tocar este botón o control; delega en onClick.
        onClick = onClick,
        enabled = !loading,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, GoogleColors.Border),
        // [GUIA/ESTILO] Fondo blanco y texto oscuro para imitar el diseño del botón Google.
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = GoogleColors.Surface,
            contentColor = GoogleColors.Text,
            disabledContainerColor = GoogleColors.Surface,
            disabledContentColor = GoogleColors.Text,
        ),
    ) {
        // [GUIA/ESTILO] Muestra spinner mientras Google elige cuenta.
        if (loading) {
            CircularProgressIndicator(Modifier.size(24.dp), color = GoogleColors.Blue, strokeWidth = 2.dp)
        } else {
            // [GUIA/ESTILO] Carga logo del recurso ic_google.xml.
            Image(painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))

            Text(
                text,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge,
                color = GoogleColors.Text,
            )
        }
    }
}
