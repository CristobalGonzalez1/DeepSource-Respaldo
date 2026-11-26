import cl.deepsource.horas.domain.model.*

fun main() {
    val one = Project("id-1", "Proyecto Uno")
    val two = Project("id-2", "Proyecto Dos")
    val backlog = listOf(ProjectTask("t1", one.id, "Diseñar"), ProjectTask("t2", two.id, "Probar"))
    check(backlog.filter { it.projectId == one.id }.map { it.name } == listOf("Diseñar"))
    check(backlog.filter { it.projectId == two.id }.map { it.name } == listOf("Probar"))
    val task = WorkTask("w", one.id, one.name, "Diseñar", System.currentTimeMillis() - 4_000L)
    check(task.elapsedSeconds() >= 3)
    val extended = task.copy(extensionMinutes = 30, nextReminderAtSeconds = (90 + 30) * 60L)
    check(extended.estimateMinutes == 90 && extended.extensionMinutes == 30)
    println("PASS: catálogo aislado por proyecto, reloj real y extensión sin modificar duración")
}
