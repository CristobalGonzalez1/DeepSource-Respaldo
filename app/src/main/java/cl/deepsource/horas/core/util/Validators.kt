package cl.deepsource.horas.core.util

import android.util.Patterns
import androidx.annotation.StringRes
import cl.deepsource.horas.R

/** Validaciones de formularios. Devuelven el recurso del error, o null si el valor es válido. */
// Validators: Valida credenciales/campos de autenticación y devuelve un ID de string de error o null.
object Validators {
    @StringRes
    // email: Comprueba el formato de correo con Patterns.EMAIL_ADDRESS.
    fun email(value: String): Int? =
        if (Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()) null else R.string.error_email_invalid

    @StringRes
    // password: Comprueba largo mínimo de contraseña (6 según el código, aunque un texto indica 8).
    fun password(value: String): Int? =
        if (value.length >= MIN_PASSWORD_LENGTH) null else R.string.error_password_short

    @StringRes
    // required: Rechaza texto vacío o solo espacios.
    fun required(value: String): Int? =
        if (value.isNotBlank()) null else R.string.error_required

    private const val MIN_PASSWORD_LENGTH = 6
}
