package cl.deepsource.horas.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Catálogo tipográfico que se aplica con MaterialTheme.typography.
val DsTypography = Typography(
    // Título de exhibición muy grande (57sp).
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp, color = DsTextPrimary),
    // Título de exhibición mediano (45sp).
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp, color = DsTextPrimary),
    // Título de exhibición pequeño (36sp).
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp, color = DsTextPrimary),
    // Encabezado principal de pantallas como Login.
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp, color = DsTextPrimary),
    // Encabezado de segundo nivel, por ejemplo Mi perfil.
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp, color = DsTextPrimary),
    // Encabezado de tercer nivel.
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, color = DsTextPrimary),
    // Título destacado de tarjetas y diálogos.
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, color = DsTextPrimary),
    // Título secundario de secciones.
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp, color = DsTextPrimary),
    // Título pequeño para componentes compactos.
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = DsTextPrimary),
    // Texto común de lectura (16sp).
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, color = DsTextPrimary),
    // Texto frecuente de 14sp.
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = DsTextPrimary),
    // Texto informativo compacto de 12sp.
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, color = DsTextPrimary),
    // Etiqueta grande y botón principal.
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, color = DsTextPrimary),
    // Etiqueta mediana de campo/sección.
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, color = DsTextPrimary),
    // Etiqueta pequeña en mayúsculas y con separación de letras.
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 1.4.sp, color = DsTextPrimary),
)
