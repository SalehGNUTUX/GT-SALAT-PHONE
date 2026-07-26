package io.github.salehgnutux.gtsalat.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.R
import io.github.salehgnutux.gtsalat.data.settings.AdhanType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * معاينة الأذان داخل شاشة الإعدادات (تجربة سريعة) — مشغّلٌ خفيفٌ بلا خدمة مقدّمة،
 * فالتطبيق في المقدّمة أثناء المعاينة. يبثّ أيّ نوعٍ قيد التشغيل الآن لتبديل زرّ التشغيل/الإيقاف.
 */
@Singleton
class AdhanPreviewer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var player: MediaPlayer? = null

    private val _playing = MutableStateFlow<AdhanType?>(null)
    /** النوع الجاري تشغيله الآن، أو null إن كانت المعاينة متوقّفة. */
    val playing: StateFlow<AdhanType?> = _playing.asStateFlow()

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    /** يشغّل معاينة النوع المطلوب؛ إن كان النوع نفسه يعمل الآن أوقفه (تبديل). */
    fun toggle(type: AdhanType, customUri: String?) {
        if (_playing.value == type) { stop(); return }
        stop()
        val uri = when (type) {
            AdhanType.SHORT -> resUri(R.raw.adhan_short)
            AdhanType.FULL -> resUri(R.raw.adhan_full)
            AdhanType.CUSTOM -> customUri?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return
        }
        player = MediaPlayer().apply {
            setAudioAttributes(attrs)
            val ok = runCatching { setDataSource(context, uri) }.isSuccess
            if (!ok) { stop(); return }
            setOnCompletionListener { stop() }
            setOnErrorListener { _, _, _ -> stop(); true }
            setOnPreparedListener { start() }
            prepareAsync()
        }
        _playing.value = type
    }

    fun stop() {
        player?.run { runCatching { if (isPlaying) stop() }; release() }
        player = null
        _playing.value = null
    }

    /** معاينة صوتٍ بمساره (يتوقّف تلقائيّاً عند الانتهاء). */
    fun previewSound(uri: Uri) {
        stop()
        player = MediaPlayer().apply {
            setAudioAttributes(attrs)
            val ok = runCatching { setDataSource(context, uri) }.isSuccess
            if (!ok) { stop(); return }
            setOnCompletionListener { stop() }
            setOnErrorListener { _, _, _ -> stop(); true }
            setOnPreparedListener { start() }
            prepareAsync()
        }
    }

    fun previewRes(resId: Int) = previewSound(resUri(resId))
    fun previewTone() = previewSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)

    private fun resUri(resId: Int): Uri = Uri.parse("android.resource://${context.packageName}/$resId")
}
