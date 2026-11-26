package cl.deepsource.horas.domain.logic

import cl.deepsource.horas.domain.model.Project
import cl.deepsource.horas.domain.model.ProjectTask
import org.junit.Assert.*
import org.junit.Test

class ProjectTaskRulesTest {
    private val projects = listOf(Project("p1", "Portal"), Project("p2", "Mesa"))
    private val tasks = listOf(
        ProjectTask("t1", "p1", "OAuth", 200L),
        ProjectTask("t2", "p2", "Atención", 100L),
        ProjectTask("t3", "p1", "Diseño", 100L)
    )
    private fun acceptsValidNameAndEstimate() = assertNull(ProjectTaskRules.validate("OAuth", 90))
    private fun rejectsBlankTask() = assertNotNull(ProjectTaskRules.validate("  ", 90))
    private fun rejectsVeryLongName() = assertNotNull(ProjectTaskRules.validate("x".repeat(81), 90))
    private fun acceptsMaximumName() = assertNull(ProjectTaskRules.validate("x".repeat(80), 90))
    private fun rejectsZeroEstimate() = assertNotNull(ProjectTaskRules.validate("OAuth", 0))
    private fun rejectsOverDayEstimate() = assertNotNull(ProjectTaskRules.validate("OAuth", 1441))
    private fun acceptsOneDayEstimate() = assertNull(ProjectTaskRules.validate("OAuth", 1440))
    private fun findsDuplicateInSameProjectCaseInsensitive() = assertTrue(ProjectTaskRules.alreadyExists("p1", " oauth ", tasks))
    private fun allowsSameTaskInDifferentProject() = assertFalse(ProjectTaskRules.alreadyExists("p2", "OAuth", tasks))
    private fun listsProjectTasksChronologically() = assertEquals(listOf("t3", "t1"), ProjectTaskRules.tasksFor("p1", tasks).map { it.id })
    private fun selectsExistingProject() = assertEquals("p2", ProjectTaskRules.select(projects, "p2")?.id)
    private fun missingProjectReturnsNull() = assertNull(ProjectTaskRules.select(projects, "missing"))
    private fun portalHasOAuthTask() = assertTrue(ProjectTaskRules.templateNames("Portal Cliente").contains("Implementar login OAuth"))
    private fun qaHasAutomatedCases() = assertTrue(ProjectTaskRules.templateNames("Automatización QA").contains("Crear casos automatizados"))
    private fun testProjectsHaveQAPlan() = assertTrue(ProjectTaskRules.templateNames("Pruebas internas").contains("Preparar plan de pruebas"))
    private fun operationDeskHasTickets() = assertTrue(ProjectTaskRules.templateNames("Mesa Operaciones").contains("Resolver tickets"))
    private fun genericProjectsGetGenericTasks() = assertTrue(ProjectTaskRules.templateNames("Otro").contains("Planificar actividades"))

    // Cada escenario reúne casos relacionados, sin quitar validaciones.
    @Test fun scenario1() {
        acceptsValidNameAndEstimate()
        rejectsBlankTask()
        rejectsVeryLongName()
        acceptsMaximumName()
    }

    @Test fun scenario2() {
        rejectsZeroEstimate()
        rejectsOverDayEstimate()
        acceptsOneDayEstimate()
    }

    @Test fun scenario3() {
        findsDuplicateInSameProjectCaseInsensitive()
        allowsSameTaskInDifferentProject()
    }

    @Test fun scenario4() {
        listsProjectTasksChronologically()
        selectsExistingProject()
        missingProjectReturnsNull()
    }

    @Test fun scenario5() {
        portalHasOAuthTask()
        qaHasAutomatedCases()
        testProjectsHaveQAPlan()
    }

    @Test fun scenario6() {
        operationDeskHasTickets()
        genericProjectsGetGenericTasks()
    }
}
