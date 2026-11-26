package cl.deepsource.horas

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import cl.deepsource.horas.core.di.AppContainer

// DeepSourceApp: Application global: crea una sola vez las dependencias compartidas.
class DeepSourceApp : Application() {
    lateinit var container: AppContainer
        // La app puede consultar container, pero no reemplazarlo desde fuera de la clase.
        private set

    // onCreate: Inicializa AppContainer al arrancar el proceso de la app.
    override fun onCreate() {
        super.onCreate()
        // Crea los repositorios y cliente Google una sola vez durante el arranque.
        container = AppContainer(this)
    }
}

/** Acceso al contenedor de dependencias desde un Context (Composables). */
// appContainer: Extensión para obtener repositorios y servicio Google desde Context o CreationExtras.
fun Context.appContainer(): AppContainer = (applicationContext as DeepSourceApp).container

/** Acceso al contenedor de dependencias desde una ViewModel Factory. */
// appContainer: Extensión para obtener repositorios y servicio Google desde Context o CreationExtras.
fun CreationExtras.appContainer(): AppContainer = (this[APPLICATION_KEY] as DeepSourceApp).container
