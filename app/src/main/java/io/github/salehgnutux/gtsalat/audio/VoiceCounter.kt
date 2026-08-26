package io.github.salehgnutux.gtsalat.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * عدّادٌ صوتيٌّ يعمل **دون إنترنت**: يراقب سعة الميكروفون، فكلّ **نطقةٍ** (ارتفاعٌ فوق العتبة ثمّ هبوط)
 * تُحسَب مرّةً — لا تعرّفٌ على كلمةٍ بعينها، بل استجابةٌ لصوت المستخدم عند التسبيح/التكبير…
 * يحتاج إذن RECORD_AUDIO. الحساسيّة تضبط العتبة (أعلى حساسيّة = عتبةٌ أدنى).
 */
class VoiceCounter {
    private var job: Job? = null
    val isRunning get() = job?.isActive == true

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope, sensitivity: Int, onCount: () -> Unit): Boolean {
        if (isRunning) return true
        // sensitivity 0..100 → عتبة RMS من ~3800 (منخفضة) إلى ~700 (عالية).
        val s = sensitivity.coerceIn(0, 100)
        val threshold = 3800.0 - (3800.0 - 700.0) * (s / 100.0)
        job = scope.launch(Dispatchers.IO) {
            val rate = 16000
            val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minBuf <= 0) return@launch
            val bufSize = maxOf(minBuf, rate / 5 * 2)   // ~200مل
            val record = runCatching {
                AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize)
            }.getOrNull() ?: return@launch
            if (record.state != AudioRecord.STATE_INITIALIZED) { runCatching { record.release() }; return@launch }
            val buf = ShortArray(bufSize / 2)
            runCatching { record.startRecording() }
            // العدّ عند **نهاية** النطقة، بشرط أن يستمرّ الصوت مدّةً منطقيّةً لنطق عبارة (لا صوتاً عابراً).
            val minMs = 220L    // أدنى مدّةٍ لعبارةٍ قصيرة (يُقصي النقرات/الطرق العابرة)
            val maxMs = 3000L   // أقصى مدّة (يُقصي الضجيج المستمرّ الطويل)
            var above = false
            var aboveStartMs = 0L
            var lastCountMs = 0L
            try {
                while (isActive) {
                    val n = record.read(buf, 0, buf.size)
                    if (n <= 0) continue
                    var sum = 0.0
                    for (i in 0 until n) { val v = buf[i].toDouble(); sum += v * v }
                    val rms = sqrt(sum / n)
                    val now = System.currentTimeMillis()
                    if (!above && rms > threshold) {
                        above = true
                        aboveStartMs = now
                    } else if (above && rms < threshold * 0.55) {
                        above = false
                        val dur = now - aboveStartMs
                        if (dur in minMs..maxMs && now - lastCountMs > 300) {
                            lastCountMs = now
                            launch(Dispatchers.Main) { onCount() }
                        }
                    }
                }
            } finally {
                runCatching { record.stop() }
                runCatching { record.release() }
            }
        }
        return true
    }

    fun stop() { job?.cancel(); job = null }
}
