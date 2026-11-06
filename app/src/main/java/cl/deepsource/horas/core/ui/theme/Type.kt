package cl.deepsource.horas.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DsTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp, color = DsTextPrimary),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp, color = DsTextPrimary),
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp, color = DsTextPrimary),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp, color = DsTextPrimary),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp, color = DsTextPrimary),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, color = DsTextPrimary),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, color = DsTextPrimary),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp, color = DsTextPrimary),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = DsTextPrimary),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, color = DsTextPrimary),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = DsTextPrimary),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, color = DsTextPrimary),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, color = DsTextPrimary),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, color = DsTextPrimary),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 1.4.sp, color = DsTextPrimary),
)
