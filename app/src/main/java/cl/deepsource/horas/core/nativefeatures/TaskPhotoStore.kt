package cl.deepsource.horas.core.nativefeatures
// [GUIA/ARCHIVO] Guarda, busca y elimina fotos JPEG privadas por proyecto.
// [GUIA/UBICACION] app/src/main/java/cl/deepsource/horas/core/nativefeatures/TaskPhotoStore.kt
// [GUIA/LECTURA] Sigue los comentarios de funcion, datos, botones y flujo; son explicativos y no ejecutables.

import android.content.Context
import android.graphics.Bitmap
import java.io.File

/** Evidencia fotográfica privada, conservada en el almacenamiento interno de la app. */
// [GUIA/TIPO] TaskPhotoStore: Almacén privado para una foto JPEG por proyecto.
class TaskPhotoStore(private val context: Context) {
    private val directory: File get() = File(context.filesDir, "task_evidence").also { it.mkdirs() }
    // [GUIA/FUNCION] path: Devuelve ruta interna de la foto del proyecto, identificada por su ID.
    private fun path(projectId: String): File = File(directory, "project_${projectId}.jpg")

    // [GUIA/FUNCION] save: Valida ID, comprime el Bitmap a JPEG y sustituye la foto previa de forma temporal.
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

    // [GUIA/FUNCION] latest: Localiza la foto existente del proyecto; null si todavía no hay.
    fun latest(projectId: String): File? = runCatching {
        val file = path(projectId)
        file.takeIf { it.isFile }
    }.getOrNull()

    // [GUIA/FUNCION] delete: Borra la evidencia de un proyecto cuando este se elimina.
    fun delete(projectId: String) { runCatching { path(projectId).delete() } }
}
