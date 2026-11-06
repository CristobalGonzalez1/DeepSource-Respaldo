package cl.deepsource.horas.core.util

import android.util.Patterns
import androidx.annotation.StringRes
import cl.deepsource.horas.R

/** Validaciones de formularios. Devuelven el recurso del error, o null si el valor es válido. */
object Validators {
    @StringRes
    fun email(value: String): Int? =
        if (Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()) null else R.string.error_email_invalid

    @StringRes
    fun password(value: String): Int? =
        if (value.length >= MIN_PASSWORD_LENGTH) null else R.string.error_password_short

    @StringRes
    fun required(value: String): Int? =
        if (value.isNotBlank()) null else R.string.error_required

    private const val MIN_PASSWORD_LENGTH = 6
}
