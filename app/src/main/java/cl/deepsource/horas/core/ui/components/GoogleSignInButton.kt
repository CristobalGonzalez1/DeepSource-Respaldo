package cl.deepsource.horas.core.ui.components

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
fun GoogleSignInButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, GoogleColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = GoogleColors.Surface,
            contentColor = GoogleColors.Text,
            disabledContainerColor = GoogleColors.Surface,
            disabledContentColor = GoogleColors.Text,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(24.dp), color = GoogleColors.Blue, strokeWidth = 2.dp)
        } else {
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
