package cl.deepsource.horas.core.util
// [GUIA/ARCHIVO] Reglas de email, password y textos obligatorios para autenticación.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/core/util/Validators.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import android.util.Patterns
import androidx.annotation.StringRes
import cl.deepsource.horas.R

/** Validaciones de formularios. Devuelven el recurso del error, o null si el valor es válido. */
// [GUIA/TIPO] Validators: Valida credenciales/campos de autenticación y devuelve un ID de string de error o null.
object Validators {
    @StringRes
    // [GUIA/FUNCION] email: Comprueba el formato de correo con Patterns.EMAIL_ADDRESS.
    fun email(value: String): Int? =
        if (Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()) null else R.string.error_email_invalid

    @StringRes
    // [GUIA/FUNCION] password: Comprueba largo mínimo de contraseña (6 según el código, aunque un texto indica 8).
    fun password(value: String): Int? =
        if (value.length >= MIN_PASSWORD_LENGTH) null else R.string.error_password_short

    @StringRes
    // [GUIA/FUNCION] required: Rechaza texto vacío o solo espacios.
    fun required(value: String): Int? =
        if (value.isNotBlank()) null else R.string.error_required

    private const val MIN_PASSWORD_LENGTH = 6
}
