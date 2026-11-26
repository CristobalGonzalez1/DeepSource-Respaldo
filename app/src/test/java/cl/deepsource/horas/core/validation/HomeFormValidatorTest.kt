package cl.deepsource.horas.core.validation

import org.junit.Assert.*
import org.junit.Test

class HomeFormValidatorTest {
 @Test fun projectNames() {
  assertNotNull(HomeFormValidator.projectName(""))
  assertNotNull(HomeFormValidator.projectName("ab"))
  assertNotNull(HomeFormValidator.projectName("a".repeat(61)))
  assertNull(HomeFormValidator.projectName("Portal Cliente AFC"))
 }
 @Test fun descriptions() {
  assertNotNull(HomeFormValidator.description("x".repeat(301)))
  assertNull(HomeFormValidator.description(""))
 }
 @Test fun taskNames() {
  assertNotNull(HomeFormValidator.taskName("x".repeat(81)))
  assertNull(HomeFormValidator.taskName("Tarea"))
 }
 @Test fun selectProject() {
  assertNotNull(HomeFormValidator.selectedProject(""))
  assertNull(HomeFormValidator.selectedProject("p-1"))
 }
 @Test fun hoursRange() {
  assertNull(HomeFormValidator.validateHours("23"))
  assertNotNull(HomeFormValidator.validateHours("24"))
  assertNotNull(HomeFormValidator.validateHours("texto"))
 }
 @Test fun minuteRange() {
  assertNull(HomeFormValidator.validateMinutes("59"))
  assertNotNull(HomeFormValidator.validateMinutes("60"))
 }
 @Test fun duration() {
  assertNotNull(HomeFormValidator.duration("0","0"))
  assertNotNull(HomeFormValidator.duration("25","0"))
  assertNull(HomeFormValidator.duration("1","30"))
 }
 @Test fun notes() {
  assertNotNull(HomeFormValidator.note("x".repeat(301)))
  assertNull(HomeFormValidator.note("bien"))
 }
}
