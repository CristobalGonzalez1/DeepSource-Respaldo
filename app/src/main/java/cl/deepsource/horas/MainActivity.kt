package cl.deepsource.horas

import android.graphics.Color
import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.deepsource.horas.core.ui.theme.DeepSourceTheme
import cl.deepsource.horas.navigation.AppNavHost

// MainActivity: Activity principal que Android abre al pulsar el icono de la app.
class MainActivity : ComponentActivity() {
    companion object { const val EXTRA_NOTIFICATION_ROUTE = "deepsource_notification_route" }
    private val externalRoute = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalRoute.value = intent.getStringExtra(EXTRA_NOTIFICATION_ROUTE)
    }
    // onCreate: Punto de entrada: configura las barras del sistema y monta Compose con tema y navegación.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalRoute.value = intent?.getStringExtra(EXTRA_NOTIFICATION_ROUTE)
        // La pantalla usa toda el área; pinta barras del sistema transparentes.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // Aquí empieza la interfaz Compose (equivale al setContentView de vistas clásicas).
        setContent {
            // Carga estilos globales y el NavHost; desde aquí parte el recorrido de pantallas.
            DeepSourceTheme {
                AppNavHost(externalRoute = externalRoute.value,
                    onExternalRouteConsumed = {
                        externalRoute.value = null
                        intent?.removeExtra(EXTRA_NOTIFICATION_ROUTE)
                    })
            }
        }
    }
}
