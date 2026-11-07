package io.github.salehgnutux.gtsalat.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * صورة خلفيّةٍ يختارها المستخدم من هاتفه: تُصغَّر وتُحفظ داخل التطبيق مع نسخةٍ مموَّهةٍ للزجاج ومصغّرةٍ للقائمة،
 * فلا يتعلّق التطبيق بملفّ المعرض الأصليّ ولا يحتاج إذناً للوصول إليه.
 */
@Singleton
class CustomBackgroundStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** يستورد الصورة من [uri]؛ يعيد true عند النجاح. */
    suspend fun import(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            // Coil يطبّق اتّجاه EXIF ويفكّ الترميز بحجمٍ مناسب.
            val result = context.imageLoader.execute(
                ImageRequest.Builder(context).data(uri).size(MAX_W, MAX_H).scale(Scale.FIT).allowHardware(false).build(),
            ) as? SuccessResult ?: return@runCatching false
            val src = result.drawable.toBitmap()
            val dir = dir(context).apply { mkdirs() }
            val sharp = scaleToWidth(src, minOf(src.width, MAX_W))
            save(sharp, File(dir, SHARP))
            save(scaleToWidth(src, THUMB_W), File(dir, THUMB))
            // تمويهٌ ناعمٌ رخيص: تصغيرٌ شديدٌ ثمّ تكبيرٌ بالترشيح، كالنسخ المموَّهة المضمَّنة.
            val tiny = scaleToWidth(src, 24)
            save(scaleToWidth(scaleToWidth(tiny, 80), BLUR_W), File(dir, BLUR))
            true
        }.getOrDefault(false)
    }

    private fun scaleToWidth(b: Bitmap, w: Int): Bitmap {
        val h = (b.height.toLong() * w / b.width).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(b, w.coerceAtLeast(1), h, true)
    }

    private fun save(b: Bitmap, f: File) {
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        tmp.renameTo(f)
    }

    companion object {
        private const val MAX_W = 1080
        private const val MAX_H = 2400
        private const val THUMB_W = 216
        private const val BLUR_W = 240
        private const val SHARP = "custom.jpg"
        private const val BLUR = "custom_blur.jpg"
        private const val THUMB = "custom_thumb.jpg"

        fun dir(context: Context) = File(context.filesDir, "backgrounds")
        fun sharp(context: Context) = File(dir(context), SHARP)
        fun blur(context: Context) = File(dir(context), BLUR)
        fun thumb(context: Context) = File(dir(context), THUMB)
        fun exists(context: Context) = sharp(context).let { it.exists() && it.length() > 0 }
    }
}
