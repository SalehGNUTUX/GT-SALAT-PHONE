package io.github.salehgnutux.gtsalat.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** حالة تشغيل الرقية المسموعة (قائمة تشغيلٍ من الآيات) — تراقبها شاشة «أستمع» للتظليل والتحكّم. */
data class RuqyahState(
    val active: Boolean = false,
    val index: Int = 0,
    val total: Int = 0,
    val surah: Int = 0,
    val ayah: Int = 0,
    val label: String = "",
    val isPlaying: Boolean = false,
    val loading: Boolean = false,
    val repeatMode: Int = 0,   // 0 = بلا تكرار · 1 = تكرار المقطع · 2 = تكرار الكلّ
    val posMs: Int = 0,
    val durMs: Int = 0,
)

object RuqyahPlayback {
    private val _state = MutableStateFlow(RuqyahState())
    val state: StateFlow<RuqyahState> = _state.asStateFlow()
    fun update(block: (RuqyahState) -> RuqyahState) { _state.value = block(_state.value) }
    fun reset() { _state.value = RuqyahState() }
}
