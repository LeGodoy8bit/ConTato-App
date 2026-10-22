package contato.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File

/**
 * Foto de perfil do Comunicador, guardada só neste celular (pasta privada do app).
 * A imagem é reduzida para no máximo 512 px: fica leve e não guarda a foto original inteira.
 */
class ProfilePhotoStore(context: Context) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "profile_photo.jpg")

    fun load(): Bitmap? = if (file.exists()) runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull() else null

    /** Copia a foto escolhida (roda fora da thread principal). Devolve null se não deu para ler. */
    fun save(uri: Uri): Bitmap? = runCatching {
        val source = ImageDecoder.createSource(appContext.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val scale = (MAX_SIZE.toFloat() / maxOf(w, h)).coerceAtMost(1f)
            decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
            decoder.isMutableRequired = false
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bitmap
    }.getOrNull()

    fun delete() {
        file.delete()
    }

    private companion object {
        const val MAX_SIZE = 512
    }
}
