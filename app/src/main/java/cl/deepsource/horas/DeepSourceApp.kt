package cl.deepsource.horas
// [GUIA/ARCHIVO] Application que crea y entrega AppContainer a toda la aplicación.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/DeepSourceApp.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import cl.deepsource.horas.core.di.AppContainer

// [GUIA/TIPO] DeepSourceApp: Application global: crea una sola vez las dependencias compartidas.
class DeepSourceApp : Application() {
    lateinit var container: AppContainer
        // [GUIA/DETALLE] La app puede consultar container, pero no reemplazarlo desde fuera de la clase.
        private set

    // [GUIA/FUNCION] onCreate: Inicializa AppContainer al arrancar el proceso de la app.
    override fun onCreate() {
        super.onCreate()
        // [GUIA/DETALLE] Crea los repositorios y cliente Google una sola vez durante el arranque.
        container = AppContainer(this)
    }
}

/** Acceso al contenedor de dependencias desde un Context (Composables). */
// [GUIA/FUNCION] appContainer: Extensión para obtener repositorios y servicio Google desde Context o CreationExtras.
fun Context.appContainer(): AppContainer = (applicationContext as DeepSourceApp).container

/** Acceso al contenedor de dependencias desde una ViewModel Factory. */
// [GUIA/FUNCION] appContainer: Extensión para obtener repositorios y servicio Google desde Context o CreationExtras.
fun CreationExtras.appContainer(): AppContainer = (this[APPLICATION_KEY] as DeepSourceApp).container
