package cl.deepsource.horas.core.nativefeatures

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File

/** Evidencias asociadas al ID de un registro de horas, no al proyecto completo. */
class RecordEvidenceStore(private val context: Context) {
    data class Evidence(val name: String, val mime: String, val file: File)
    private val preferences = context.getSharedPreferences("deepsource_record_evidence", Context.MODE_PRIVATE)
    private val directory get() = File(context.filesDir, "record_evidence").also { it.mkdirs() }

    private fun fileFor(recordId: String): File {
        require(recordId.matches(Regex("[a-zA-Z0-9-]{1,100}")))
        return File(directory, "record_$recordId")
    }

    fun savePhoto(recordId: String, bitmap: Bitmap): Boolean = runCatching {
        val destination = fileFor(recordId)
        val temporary = File(directory, "${destination.name}.tmp")
        try {
            temporary.outputStream().use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream))
            }
            check(temporary.renameTo(destination))
            preferences.edit().putString("name_$recordId", "Fotografía del registro.jpg")
                .putString("mime_$recordId", "image/jpeg").apply()
        } finally { temporary.delete() }
        true
    }.getOrDefault(false)

    // El selector del sistema concede acceso al documento elegido. No se necesita permiso global.
    fun saveDocument(recordId: String, uri: Uri): Boolean = runCatching {
        val resolver = context.contentResolver
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
            ?.take(150) ?: "Documento adjunto"
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val destination = fileFor(recordId)
        val temporary = File(directory, "${destination.name}.tmp")
        try {
            var total = 0L
            resolver.openInputStream(uri)?.use { input ->
                temporary.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        check(total <= 20L * 1024 * 1024) { "Archivo de más de 20 MB" }
                        output.write(buffer, 0, read)
                    }
                }
            } ?: error("No fue posible abrir el archivo")
            check(total > 0L)
            check(temporary.renameTo(destination))
            preferences.edit().putString("name_$recordId", name)
                .putString("mime_$recordId", mime).apply()
        } finally { temporary.delete() }
        true
    }.getOrDefault(false)

    fun get(recordId: String): Evidence? {
        val file = runCatching { fileFor(recordId) }.getOrNull()?.takeIf { it.isFile } ?: return null
        return Evidence(preferences.getString("name_$recordId", "Archivo") ?: "Archivo",
            preferences.getString("mime_$recordId", "application/octet-stream") ?: "application/octet-stream", file)
    }

    fun open(recordId: String): Boolean = runCatching {
        val evidence = get(recordId) ?: return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.evidenceprovider", evidence.file)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, evidence.mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        true
    }.getOrDefault(false)
}
