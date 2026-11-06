package cl.deepsource.horas.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.deepsource.horas.core.ui.components.DsBrandHeader
import cl.deepsource.horas.core.ui.theme.DsCyan
import cl.deepsource.horas.core.ui.theme.DsOutline
import cl.deepsource.horas.core.ui.theme.DsSurface
import cl.deepsource.horas.core.ui.theme.DsTextMuted
import cl.deepsource.horas.core.ui.theme.DsTextPrimary

@Composable
fun HomeHeader(
    userName: String,
    userEmail: String,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UserCard(userName, userEmail, onOpenProfile, modifier)
}
@Composable
fun UserCard(
    userName: String,
    userEmail: String,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nameToShow = userName.ifBlank { "Marina Soto" }
    val emailToShow = userEmail.ifBlank { "marina.soto@deepsource.cl" }
    val initials = getInitials(nameToShow)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Usuario",
            style = MaterialTheme.typography.labelMedium,
            color = DsTextMuted,
            fontSize = 13.sp,
        )

        Spacer(Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenProfile),
            color = androidx.compose.ui.graphics.Color(0xFF1C2639),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DsOutline.copy(alpha = 0.6f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DsCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initials,
                        color = DsCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nameToShow,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DsTextPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = emailToShow,
                        style = MaterialTheme.typography.bodySmall,
                        color = DsTextMuted,
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Abrir mi perfil",
                    tint = DsTextMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private fun getInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
        parts.isNotEmpty() && parts[0].length >= 2 -> parts[0].take(2).uppercase()
        parts.isNotEmpty() -> parts[0].take(1).uppercase()
        else -> "MS"
    }
}

