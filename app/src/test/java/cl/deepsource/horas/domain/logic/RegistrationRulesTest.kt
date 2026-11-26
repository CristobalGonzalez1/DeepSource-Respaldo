package cl.deepsource.horas.domain.logic

import cl.deepsource.horas.domain.model.TimeLog
import cl.deepsource.horas.domain.model.WorkTask
import org.junit.Assert.*
import org.junit.Test

class RegistrationRulesTest {
    private val work = WorkTask("t1", "p1", "Portal", "Diseño", 1000L)
    private val logs = listOf(
        TimeLog("l1", "p1", 60L, timestampEpochMs = 100L),
        TimeLog("l2", "p2", 120L, timestampEpochMs = 300L),
        TimeLog("l3", "p1", 30L, timestampEpochMs = 400L),
    )

    private fun acceptsOptionalDescription() = assertNull(RegistrationRules.validateManual(90, ""))
    private fun rejectsZero() = assertNotNull(RegistrationRules.validateManual(0, ""))
    private fun rejectsNegative() = assertNotNull(RegistrationRules.validateManual(-1, ""))
    private fun acceptsMaximumMinutes() = assertNull(RegistrationRules.validateManual(1439, ""))
    private fun rejectsExcessMinutes() = assertNotNull(RegistrationRules.validateManual(1440, ""))
    private fun rejectsLongNote() = assertNotNull(RegistrationRules.validateManual(15, "x".repeat(301)))
    private fun accepts300CharacterNote() = assertNull(RegistrationRules.validateManual(15, "x".repeat(300)))
    private fun convertsHoursMinutes() = assertEquals(150L, RegistrationRules.fromHoursAndMinutes(2, 30))
    private fun allowsMinutesOnly() = assertEquals(15L, RegistrationRules.fromHoursAndMinutes(0, 15))
    private fun rejectsInvalidHour() { RegistrationRules.fromHoursAndMinutes(24, 0) }
    private fun rejectsInvalidMinute() { RegistrationRules.fromHoursAndMinutes(0, 60) }
    private fun rejectsEmptyDuration() { RegistrationRules.fromHoursAndMinutes(0, 0) }
    private fun roundsTimerToMinimumOneMinute() {
        assertEquals(1L, RegistrationRules.timerMinutes(0L))
        assertEquals(1L, RegistrationRules.timerMinutes(59L))
        assertEquals(2L, RegistrationRules.timerMinutes(120L))
    }
    private fun noteIsOptionalAtTimerStop() {
        assertEquals("Diseño", RegistrationRules.noteForTimer("  ", work))
        assertEquals("Terminado", RegistrationRules.noteForTimer(" Terminado ", work))
    }
    private fun totalsOnlyRequestedProject() {
        assertEquals(90L, RegistrationRules.projectMinutes("p1", logs))
        assertEquals(0L, RegistrationRules.projectMinutes("inexistente", logs))
    }
    private fun weeklyTotalsIgnoreOlderEntries() = assertEquals(30L, RegistrationRules.weeklyMinutes("p1", 200L, logs))
    private fun projectHistoryIsNewestFirst() {
        assertEquals(listOf("l3", "l1"), RegistrationRules.logsForProject("p1", logs).map { it.id })
    }
    private fun emptyHistoryIsSupported() = assertTrue(RegistrationRules.logsForProject("x", logs).isEmpty())

    // Cada escenario reúne casos relacionados, sin quitar validaciones.
    @Test fun scenario1() {
        acceptsOptionalDescription()
        rejectsZero()
        rejectsNegative()
    }

    @Test fun scenario2() {
        acceptsMaximumMinutes()
        rejectsExcessMinutes()
        rejectsLongNote()
        accepts300CharacterNote()
    }

    @Test fun scenario3() {
        convertsHoursMinutes()
        allowsMinutesOnly()
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { rejectsInvalidHour() }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { rejectsInvalidMinute() }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { rejectsEmptyDuration() }
    }

    @Test fun scenario4() {
        roundsTimerToMinimumOneMinute()
        noteIsOptionalAtTimerStop()
    }

    @Test fun scenario5() {
        totalsOnlyRequestedProject()
        weeklyTotalsIgnoreOlderEntries()
    }

    @Test fun scenario6() {
        projectHistoryIsNewestFirst()
        emptyHistoryIsSupported()
    }
}
