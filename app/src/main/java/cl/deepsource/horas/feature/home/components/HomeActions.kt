package cl.deepsource.horas.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.deepsource.horas.core.ui.components.DsPrimaryButton
import cl.deepsource.horas.core.ui.theme.DsOutline
import cl.deepsource.horas.core.ui.theme.DsSurface
import cl.deepsource.horas.core.ui.theme.DsTextPrimary

@Composable
fun HomeActions(
    onStartTaskClick: () -> Unit,
    onLogHoursClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            DsPrimaryButton(
                text = "Iniciar tarea",
                onClick = onStartTaskClick,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            OutlinedButton(
                onClick = onLogHoursClick,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, DsOutline),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = DsSurface,
                    contentColor = DsTextPrimary,
                ),
            ) {
                Text(
                    text = "Registrar horas",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
