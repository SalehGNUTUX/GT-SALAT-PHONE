package io.github.salehgnutux.gtsalat.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** وضع التشغيل: آية-بآية (متزامن مع النصّ) أو سورة كاملة (القرآن المسموع). */
enum class QuranMode { AYAH, SURAH }

/** لقطةٌ من حالة تشغيل القرآن، تراقبها الواجهات لتظليل الآية الجاريّة وتحديث الأزرار. */
data class QuranPlayState(
    val active: Boolean = false,
    val mode: QuranMode = QuranMode.AYAH,
    val surah: Int = 0,
    val surahName: String = "",
    val ayah: Int = 0,               // الآية الجاريّة (0 = بسملة/تحميل)
    val reciterId: String = "",
    val reciterName: String = "",
    val isPlaying: Boolean = false,
    val loading: Boolean = false,
)

/**
 * حالةٌ عالميّةٌ مشتركةٌ بين [QuranAudioService] والواجهات (StateFlow).
 * الخدمة تكتب، والشاشات تقرأ لتظليل الآية والتمرير التلقائيّ.
 */
object QuranPlayback {
    private val _state = MutableStateFlow(QuranPlayState())
    val state: StateFlow<QuranPlayState> = _state.asStateFlow()

    fun update(block: (QuranPlayState) -> QuranPlayState) {
        _state.value = block(_state.value)
    }

    fun reset() { _state.value = QuranPlayState() }
}
