package cl.deepsource.horas.core.nativefeatures

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File

/**
 * [V4/REPOSITORIO DE ARCHIVOS] Guarda un adjunto por proyecto sin cambiar ProjectRepository.
 * Usa SAF/OpenDocument: no requiere acceso global al almacenamiento del usuario.
 * Los archivos nunca salen de filesDir salvo que el usuario pulse "Abrir archivo".
 */
class ProjectEvidenceStore(private val context: Context) {
    data class Attachment(val originalName: String, val mime: String, val file: File)
    private val prefs = context.getSharedPreferences("deepsource_project_evidence", Context.MODE_PRIVATE)
    private val directory: File get() = File(context.filesDir, "project_attachments").also { it.mkdirs() }

    // El ID proviene del modelo Project y se valida antes de construir una ruta.
    private fun file(id: String): File {
        require(id.matches(Regex("[a-zA-Z0-9-]{1,64}")))
        return File(directory, "project_$id")
    }

    /** [V4/ARCHIVOS] Copia hasta 20 MiB, con límite real durante streaming. */
    fun import(projectId: String, uri: Uri): Attachment? = runCatching {
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?.take(150)?.substringAfterLast('/') ?: "Archivo adjunto"
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val dest = file(projectId)
        val temporary = File(directory, "${dest.name}.tmp")
        try {
            var count = 0L
            context.contentResolver.openInputStream(uri)?.use { source ->
                temporary.outputStream().use { output ->
                    val bytes = ByteArray(8192)
                    while (true) {
                        val read = source.read(bytes)
                        if (read < 0) break
                        count += read
                        if (count > 20L * 1024L * 1024L) error("Archivo supera los 20 MB")
                        output.write(bytes, 0, read)
                    }
                }
            } ?: error("No se pudo leer el archivo")
            check(count > 0) { "Archivo vacío" }
            check(temporary.renameTo(dest)) { "No se pudo guardar" }
            prefs.edit().putString("name_$projectId", name).putString("mime_$projectId", mime).apply()
            Attachment(name, mime, dest)
        } finally {
            temporary.delete()
        }
    }.getOrNull()

    /** [V4/CONSULTA] Obtiene el último archivo sin mantener URI externos persistentes. */
    fun latest(projectId: String): Attachment? = runCatching {
        val existing = file(projectId).takeIf { it.isFile } ?: return null
        Attachment(prefs.getString("name_$projectId", "Archivo") ?: "Archivo",
            prefs.getString("mime_$projectId", "application/octet-stream") ?: "application/octet-stream", existing)
    }.getOrNull()

    /** [V4/BOTON] Comparte temporalmente el archivo con visor externo vía FileProvider. */
    fun open(projectId: String): Boolean = runCatching {
        val attachment = latest(projectId) ?: return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.evidenceprovider", attachment.file)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, attachment.mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        true
    }.getOrDefault(false)

    /** [V4/BORRADO] Cuando se elimina un proyecto, también desaparece el documento privado. */
    fun delete(projectId: String) {
        runCatching { file(projectId).delete() }
        prefs.edit().remove("name_$projectId").remove("mime_$projectId").apply()
    }
}
