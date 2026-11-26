package cl.deepsource.horas.data.remote

import cl.deepsource.horas.domain.model.TimeLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Convierte un registro local al formato JSON que entiende hours-service. */
object HoursSyncMapper {
    private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

    fun toRemote(log: TimeLog, remoteProjectId: Long) = RemoteHours(
        projectId = remoteProjectId,
        taskName = log.taskName,
        durationMinutes = log.durationMinutes,
        workedOn = dateFormat().format(Date(log.timestampEpochMs)),
        note = log.note,
        clientId = log.id,
        source = log.source,
        durationSeconds = log.durationSeconds,
        timestampEpochMs = log.timestampEpochMs,
    )

    fun toLocal(remote: RemoteHours, localProjectId: String): TimeLog {
        val day = runCatching { dateFormat().parse(remote.workedOn)?.time }.getOrNull()
        return TimeLog(
            id = remote.clientId ?: "horas-servidor-${remote.id}",
            projectId = localProjectId,
            durationMinutes = remote.durationMinutes,
            note = remote.note,
            timestampEpochMs = remote.timestampEpochMs ?: day ?: System.currentTimeMillis(),
            durationSeconds = remote.durationSeconds ?: remote.durationMinutes * 60,
            taskName = remote.taskName,
            source = remote.source,
        )
    }
}
