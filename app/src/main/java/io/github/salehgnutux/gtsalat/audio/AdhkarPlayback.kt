package io.github.salehgnutux.gtsalat.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** حالة تشغيل صوت الأذكار (ملفٌّ واحدٌ مضمَّن: صباح/مساء/نوم) — تراقبها شريط المشغّل في القسم. */
data class AdhkarAudioState(
    val active: Boolean = false,
    val key: String = "",        // مفتاح الملفّ الجاري (morning/evening/sleep) لإظهار الشريط في قسمه فقط
    val title: String = "",
    val isPlaying: Boolean = false,
    val loading: Boolean = false,
    val posMs: Int = 0,
    val durMs: Int = 0,
)

object AdhkarPlayback {
    private val _state = MutableStateFlow(AdhkarAudioState())
    val state: StateFlow<AdhkarAudioState> = _state.asStateFlow()
    fun update(block: (AdhkarAudioState) -> AdhkarAudioState) { _state.value = block(_state.value) }
    fun reset() { _state.value = AdhkarAudioState() }
}
