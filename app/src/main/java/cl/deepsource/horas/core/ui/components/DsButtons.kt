package cl.deepsource.horas.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cl.deepsource.horas.core.ui.theme.DsBlue
import cl.deepsource.horas.core.ui.theme.DsViolet

@Composable
fun DsPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    loadingText: String? = null,
) {
    val shape = MaterialTheme.shapes.medium
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = shape,
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (enabled || loading) 1f else 0.6f)
                .background(Brush.linearGradient(listOf(Color(0xFF0FB9C9), DsBlue, DsViolet)), shape),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    if (loadingText != null) {
                        Spacer(Modifier.width(12.dp))
                        Text(loadingText, style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            } else {
                Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}