package cl.deepsource.horas.core.validation

object HomeFormValidator {
    fun projectName(name: String): String? = when {
        name.isBlank() -> "Ingresa el nombre del proyecto"
        name.trim().length < 3 -> "El nombre debe tener al menos 3 caracteres"
        name.trim().length > 60 -> "El nombre no puede superar 60 caracteres"
        else -> null
    }

    fun description(description: String): String? =
        if (description.length > 300) "Máximo 300 caracteres" else null

    fun taskName(name: String): String? =
        if (name.length > 80) "El nombre no puede superar 80 caracteres" else null

    fun selectedProject(id: String): String? =
        if (id.isBlank()) "Debes seleccionar un proyecto" else null

    fun validateHours(hours: String): String? = when {
        hours.isBlank() -> null
        hours.toLongOrNull() == null -> "Ingresa horas válidas"
        hours.toLong() !in 0..23 -> "Usa entre 0 y 23 horas"
        else -> null
    }

    fun validateMinutes(minutes: String): String? = when {
        minutes.isBlank() -> null
        minutes.toLongOrNull() == null -> "Ingresa minutos válidos"
        minutes.toLong() !in 0..59 -> "Usa entre 0 y 59 minutos"
        else -> null
    }

    fun duration(hours: String, minutes: String): String? = when {
        validateHours(hours) != null -> validateHours(hours)
        validateMinutes(minutes) != null -> validateMinutes(minutes)
        (hours.toLongOrNull() ?: 0L) * 60L + (minutes.toLongOrNull() ?: 0L) == 0L -> "Ingresa un tiempo mayor a 0 minutos"
        else -> null
    }

    fun note(note: String): String? = if (note.length > 300) "Máximo 300 caracteres" else null
}
