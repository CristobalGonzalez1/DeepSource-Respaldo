package cl.deepsource.horas

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import cl.deepsource.horas.core.di.AppContainer

class DeepSourceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Acceso al contenedor de dependencias desde un Context (Composables). */
fun Context.appContainer(): AppContainer = (applicationContext as DeepSourceApp).container

/** Acceso al contenedor de dependencias desde una ViewModel Factory. */
fun CreationExtras.appContainer(): AppContainer = (this[APPLICATION_KEY] as DeepSourceApp).container
