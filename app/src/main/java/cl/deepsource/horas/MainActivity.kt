package cl.deepsource.horas
// [GUIA/ARCHIVO] Activity inicial que carga tema Compose y grafo de navegación.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/MainActivity.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.deepsource.horas.core.ui.theme.DeepSourceTheme
import cl.deepsource.horas.navigation.AppNavHost

// [GUIA/TIPO] MainActivity: Activity principal que Android abre al pulsar el icono de la app.
class MainActivity : ComponentActivity() {
    // [GUIA/FUNCION] onCreate: Punto de entrada: configura las barras del sistema y monta Compose con tema y navegación.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // [GUIA/DETALLE] La pantalla usa toda el área; pinta barras del sistema transparentes.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // [GUIA/DETALLE] Aquí empieza la interfaz Compose (equivale al setContentView de vistas clásicas).
        setContent {
            // [GUIA/DETALLE] Carga estilos globales y el NavHost; desde aquí parte el recorrido de pantallas.
            DeepSourceTheme { AppNavHost() }
        }
    }
}
