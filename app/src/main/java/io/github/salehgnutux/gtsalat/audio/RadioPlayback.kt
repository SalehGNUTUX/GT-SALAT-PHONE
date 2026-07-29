package io.github.salehgnutux.gtsalat.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** حالة تشغيل الإذاعة الجاريّة (StateFlow تراقبها الواجهة). */
data class RadioState(
    val active: Boolean = false,
    val name: String = "",
    val url: String = "",
    val isPlaying: Boolean = false,
    val loading: Boolean = false,
)

object RadioPlayback {
    private val _state = MutableStateFlow(RadioState())
    val state: StateFlow<RadioState> = _state.asStateFlow()
    fun update(block: (RadioState) -> RadioState) { _state.value = block(_state.value) }
    fun reset() { _state.value = RadioState() }
}
