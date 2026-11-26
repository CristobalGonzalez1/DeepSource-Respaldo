package cl.deepsource.horas.data.remote

import cl.deepsource.horas.domain.model.TimeLog
import org.junit.Assert.*
import org.junit.Test

class HoursSyncMapperTest {
    @Test fun mantieneIdFechaYDuracionAlEnviar() {
        val log = TimeLog("registro-1", "proyecto-local", 75, "", 1795680000000L,
            durationSeconds = 4523L, taskName = "Revisar formularios", source = "timer")
        val remote = HoursSyncMapper.toRemote(log, 5L)
        assertEquals(5L, remote.projectId)
        assertEquals("registro-1", remote.clientId)
        assertEquals("Revisar formularios", remote.taskName)
        assertEquals(4523L, remote.durationSeconds)
        assertEquals("timer", remote.source)
        assertTrue(remote.workedOn.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test fun recuperaRegistroDesdeServidorSinDuplicarIdentidad() {
        val remote = RemoteHours(9L, 5L, "Revisar formularios", 75L,
            "2026-11-26", "Revisión", "registro-1", "timer", 4523L, 1795680000000L)
        val log = HoursSyncMapper.toLocal(remote, "proyecto-local")
        assertEquals("registro-1", log.id)
        assertEquals("proyecto-local", log.projectId)
        assertEquals(4523L, log.durationSeconds)
        assertEquals("timer", log.source)
    }
}
