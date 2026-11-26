package cl.deepsource.horas.domain.model

/**
 * [FLUJO/MODELO] Datos nuevos del HTML Material v2. No sustituyen Project, WorkTask ni TimeLog.
 * Sus valores se gestionan desde ViewModel -> ProjectRepository -> SharedPreferences.
 */
object WorkflowDefaults {
    // Las tres duraciones están tomadas del HTML de referencia.
    val estimates = linkedMapOf("Normal" to 90, "Media" to 120, "Compleja" to 240)
    val sampleTasks = listOf(
        "Implementar login OAuth", "Revisar incidencia pendiente",
        "Pruebas de calidad", "Actualizar documentación",
    )
    val members = listOf(
        TeamMember("luis", "Luis Gómez", 180),
        TeamMember("ana", "Ana Pérez", 360),
        TeamMember("marta", "Marta Ruiz", 480),
    )
}

/** [FLUJO/MODELO] Miembro local ilustrativo, NO una cuenta remota ni una nómina real. */
data class TeamMember(val id: String, val name: String, val initialLoadMinutes: Int)

/** [FLUJO/MODELO] Asignación hecha en Vista de jefe y persistida localmente. */
data class TaskAssignment(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val projectId: String,
    val projectName: String,
    val taskName: String,
    val difficulty: String,
    val estimateMinutes: Int,
    val assignedAtEpochMs: Long,
)

/** [FLUJO/UTIL] Un único cálculo sirve tanto a la pantalla como al widget. */
fun WorkTask.elapsedSeconds(now: Long = System.currentTimeMillis()): Long =
    cl.deepsource.horas.domain.logic.TimerRules.elapsedSeconds(this, now)
