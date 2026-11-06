package cl.deepsource.horas.core.nativefeatures

import android.content.Context
import android.graphics.Bitmap
import java.io.File

/** Evidencia fotográfica privada, conservada en el almacenamiento interno de la app. */
class TaskPhotoStore(private val context: Context) {
    private val directory: File get() = File(context.filesDir, "task_evidence").also { it.mkdirs() }
    private fun path(projectId: String): File = File(directory, "project_${projectId}.jpg")

    fun save(projectId: String, image: Bitmap): File? = runCatching {
        require(projectId.matches(Regex("[a-zA-Z0-9-]{1,64}")))
        val target = path(projectId)
        val temporary = File(directory, "${target.name}.tmp")
        temporary.outputStream().use { out ->
            check(image.compress(Bitmap.CompressFormat.JPEG, 85, out))
        }
        check(temporary.renameTo(target)) { "No se pudo guardar la fotografía" }
        target
    }.getOrNull()

    fun latest(projectId: String): File? = runCatching {
        val file = path(projectId)
        file.takeIf { it.isFile }
    }.getOrNull()

    fun delete(projectId: String) { runCatching { path(projectId).delete() } }
}
