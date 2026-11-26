package cl.deepsource.horas.core.ui.theme
// [GUIA/ARCHIVO] Paleta de colores usada por componentes DeepSource y por el botón de Google.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/core/ui/theme/Color.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import androidx.compose.ui.graphics.Color

// [GUIA/ESTILO] Fondo azul muy oscuro de toda la app.
val DsBackground = Color(0xFF050A1A)
// [GUIA/ESTILO] Superficie de tarjetas y formularios.
val DsSurface = Color(0xFF0C1530)
// [GUIA/ESTILO] Fondo interno de los TextField.
val DsInput = Color(0xFF0E1938)
// [GUIA/ESTILO] Bordes y separadores.
val DsOutline = Color(0xFF1F2D55)
// [GUIA/ESTILO] Texto principal de alto contraste.
val DsTextPrimary = Color(0xFFF2F5FF)
// [GUIA/ESTILO] Texto secundario / descripciones.
val DsTextMuted = Color(0xFF7D8BB5)
// [GUIA/ESTILO] Acento cyan de enlaces y encabezados.
val DsCyan = Color(0xFF22D3EE)
// [GUIA/ESTILO] Azul del degradado de botones.
val DsBlue = Color(0xFF2F7BFF)
// [GUIA/ESTILO] Violeta del degradado y fondos.
val DsViolet = Color(0xFF5B3DF5)
// [GUIA/ESTILO] Color de error en validaciones.
val DsError = Color(0xFFFF6B7A)
// [GUIA/ESTILO] Color de confirmación positiva.
val DsSuccess = Color(0xFF34D399)


// [GUIA/ESTILO] Paleta visual del botón Google: la autorización ocurre en CredentialManager.
object GoogleColors {
    // [GUIA/ESTILO] Azul oficial de Google.
    val Blue = Color(0xFF4285F4)
    // [GUIA/ESTILO] Rojo oficial de Google.
    val Red = Color(0xFFEA4335)
    // [GUIA/ESTILO] Amarillo oficial de Google.
    val Yellow = Color(0xFFFBBC05)
    // [GUIA/ESTILO] Verde oficial de Google.
    val Green = Color(0xFF34A853)
    // [GUIA/ESTILO] Fondo blanco para el botón Google.
    val Surface = Color(0xFFFFFFFF)
    // [GUIA/ESTILO] Borde gris del botón Google.
    val Border = Color(0xFFDADCE0)
    // [GUIA/ESTILO] Texto oscuro del botón Google.
    val Text = Color(0xFF1F1F1F)
}
