package cl.deepsource.horas.domain.logic

import cl.deepsource.horas.domain.model.WorkTask
import org.junit.Assert.*
import org.junit.Test

class TimerRulesTest {
    private fun task(paused: Boolean = false, demo: Boolean = false) = WorkTask(
        id = "t1", projectId = "p1", projectName = "Portal", taskName = "OAuth",
        startEpochMs = 1_000L, paused = paused, accumulatedSeconds = 30L,
        estimateMinutes = 90, demoReminder = demo
    )

    private fun countsRunningSeconds() = assertEquals(35L, TimerRules.elapsedSeconds(task(), 6_000L))
    private fun pausesClockWithoutAddingTime() = assertEquals(30L, TimerRules.elapsedSeconds(task(paused = true), 900_000L))
    private fun ignoresNegativeClockShift() = assertEquals(30L, TimerRules.elapsedSeconds(task(), 0L))
    private fun pausingAccumulatesTime() {
        val paused = TimerRules.togglePause(task(), 21_000L)
        assertTrue(paused.paused)
        assertEquals(50L, paused.accumulatedSeconds)
    }
    private fun resumeUsesNewStartTime() {
        val resumed = TimerRules.togglePause(task(paused = true), 12_000L)
        assertFalse(resumed.paused)
        assertEquals(12_000L, resumed.startEpochMs)
        assertEquals(30L, resumed.accumulatedSeconds)
    }
    private fun extendThirtyMinutesDoesNotChangeWorkedSeconds() {
        val result = TimerRules.extend(task(), 30, 61_000L)
        assertEquals(30, result.extensionMinutes)
        assertEquals(7_200L, result.nextReminderAtSeconds)
        assertEquals(30L, result.accumulatedSeconds)
    }
    private fun subsequentExtensionAddsToTheDeadline() {
        val t = TimerRules.extend(task(), 30, 61_000L)
        val extended = TimerRules.extend(t, 30, 62_000L)
        assertEquals(60, extended.extensionMinutes)
        assertEquals(9_000L, extended.nextReminderAtSeconds)
    }
    private fun demoExtensionSchedulesAfterFifteenMoreSeconds() {
        val t = TimerRules.extend(task(demo = true), 30, 11_000L)
        assertEquals(55L, t.nextReminderAtSeconds)
    }
    private fun rejectsZeroExtension() { TimerRules.extend(task(), 0, 1_000L) }
    private fun rejectsTooLongExtension() { TimerRules.extend(task(), 241, 1_000L) }
    private fun defaultsToEstimateWhenReminderNotSet() = assertEquals(5_400L, TimerRules.reminderSeconds(task()))
    private fun overdueAndRemainingTime() {
        val t = task().copy(nextReminderAtSeconds = 35L)
        assertTrue(TimerRules.isDue(t, 6_000L))
        assertFalse(TimerRules.isDue(t, 5_000L))
        assertEquals(1L, TimerRules.remainingSeconds(t, 5_000L))
        assertEquals(0L, TimerRules.remainingSeconds(t, 6_000L))
    }

    // Cada escenario reúne casos relacionados, sin quitar validaciones.
    @Test fun scenario1() {
        countsRunningSeconds()
        pausesClockWithoutAddingTime()
        ignoresNegativeClockShift()
    }

    @Test fun scenario2() {
        pausingAccumulatesTime()
        resumeUsesNewStartTime()
    }

    @Test fun scenario3() {
        extendThirtyMinutesDoesNotChangeWorkedSeconds()
        subsequentExtensionAddsToTheDeadline()
        demoExtensionSchedulesAfterFifteenMoreSeconds()
    }

    @Test fun scenario4() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { rejectsZeroExtension() }
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) { rejectsTooLongExtension() }
    }

    @Test fun scenario5() {
        defaultsToEstimateWhenReminderNotSet()
        overdueAndRemainingTime()
    }
}
