package cl.deepsource.horas.core.validation

// HomeFormValidator: Reglas para proyectos, tareas, duración y notas; independientes de las pantallas.
object HomeFormValidator {
    // projectName: Valida nombre de proyecto: no vacío y longitud de 3 a 60 caracteres.
    fun projectName(name: String): String? = when {
        name.isBlank() -> "Ingresa el nombre del proyecto"
        name.trim().length < 3 -> "El nombre debe tener al menos 3 caracteres"
        name.trim().length > 60 -> "El nombre no puede superar 60 caracteres"
        else -> null
    }

    // description: Limita descripción de proyecto a 300 caracteres.
    fun description(description: String): String? =
        if (description.length > 300) "Máximo 300 caracteres" else null

    // taskName: Limita el título de tarea a 80 caracteres.
    fun taskName(name: String): String? =
        if (name.length > 80) "El nombre no puede superar 80 caracteres" else null

    // selectedProject: Exige ID no vacío antes de operar sobre proyectos.
    fun selectedProject(id: String): String? =
        if (id.isBlank()) "Debes seleccionar un proyecto" else null

    // validateHours: Acepta de 0 a 23 horas o campo vacío; rechaza caracteres no numéricos.
    fun validateHours(hours: String): String? = when {
        hours.isBlank() -> null
        hours.toLongOrNull() == null -> "Ingresa horas válidas"
        hours.toLong() !in 0..23 -> "Usa entre 0 y 23 horas"
        else -> null
    }

    // validateMinutes: Acepta de 0 a 59 minutos o campo vacío.
    fun validateMinutes(minutes: String): String? = when {
        minutes.isBlank() -> null
        minutes.toLongOrNull() == null -> "Ingresa minutos válidos"
        minutes.toLong() !in 0..59 -> "Usa entre 0 y 59 minutos"
        else -> null
    }

    // duration: Valida horas y minutos y rechaza tiempo total igual a 0.
    fun duration(hours: String, minutes: String): String? = when {
        validateHours(hours) != null -> validateHours(hours)
        validateMinutes(minutes) != null -> validateMinutes(minutes)
        (hours.toLongOrNull() ?: 0L) * 60L + (minutes.toLongOrNull() ?: 0L) == 0L -> "Ingresa un tiempo mayor a 0 minutos"
        else -> null
    }

    // note: Restringe observaciones a 300 caracteres.
    fun note(note: String): String? = if (note.length > 300) "Máximo 300 caracteres" else null
}
