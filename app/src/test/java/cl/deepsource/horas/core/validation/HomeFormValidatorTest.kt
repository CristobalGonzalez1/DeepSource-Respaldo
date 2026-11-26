package cl.deepsource.horas.core.validation

import org.junit.Assert.*
import org.junit.Test

class HomeFormValidatorTest {
 private fun projectNames() {
  assertNotNull(HomeFormValidator.projectName(""))
  assertNotNull(HomeFormValidator.projectName("ab"))
  assertNotNull(HomeFormValidator.projectName("a".repeat(61)))
  assertNull(HomeFormValidator.projectName("Portal Cliente AFC"))
 }
 private fun descriptions() {
  assertNotNull(HomeFormValidator.description("x".repeat(301)))
  assertNull(HomeFormValidator.description(""))
 }
 private fun taskNames() {
  assertNotNull(HomeFormValidator.taskName("x".repeat(81)))
  assertNull(HomeFormValidator.taskName("Tarea"))
 }
 private fun selectProject() {
  assertNotNull(HomeFormValidator.selectedProject(""))
  assertNull(HomeFormValidator.selectedProject("p-1"))
 }
 private fun hoursRange() {
  assertNull(HomeFormValidator.validateHours("23"))
  assertNotNull(HomeFormValidator.validateHours("24"))
  assertNotNull(HomeFormValidator.validateHours("texto"))
 }
 private fun minuteRange() {
  assertNull(HomeFormValidator.validateMinutes("59"))
  assertNotNull(HomeFormValidator.validateMinutes("60"))
 }
 private fun duration() {
  assertNotNull(HomeFormValidator.duration("0","0"))
  assertNotNull(HomeFormValidator.duration("25","0"))
  assertNull(HomeFormValidator.duration("1","30"))
 }
 private fun notes() {
  assertNotNull(HomeFormValidator.note("x".repeat(301)))
  assertNull(HomeFormValidator.note("bien"))
 }

    // Cada escenario reúne casos relacionados, sin quitar validaciones.
    @Test fun scenario1() {
        projectNames()
        descriptions()
    }

    @Test fun scenario2() {
        taskNames()
        selectProject()
    }

    @Test fun scenario3() {
        hoursRange()
        minuteRange()
    }

    @Test fun scenario4() {
        duration()
        notes()
    }
}
