package io.github.salehgnutux.gtsalat.ui.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class TasbihUi(
    val count: Int = 0,
    val target: Int = 33,          // 0 = بلا حدّ
    val dhikrIndex: Int = 0,
) {
    val dhikr: String get() = DHIKR_LIST[dhikrIndex]
    val laps: Int get() = if (target > 0) count / target else 0
    val inLap: Int get() = if (target > 0) count % target else count
    /** هل بلغ العدّ مضاعفاً للهدف الآن (لحظة الاهتزاز)؟ */
    fun justReachedTarget(): Boolean = target > 0 && count > 0 && count % target == 0

    companion object {
        val DHIKR_LIST = listOf(
            "سُبْحَانَ اللهِ",
            "الْحَمْدُ لِلَّهِ",
            "لَا إِلَهَ إِلَّا اللهُ",
            "اللهُ أَكْبَرُ",
            "أَسْتَغْفِرُ اللهَ",
            "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ",
            "سُبْحَانَ اللهِ وَبِحَمْدِهِ",
        )
        val TARGETS = listOf(33, 99, 100, 0)
    }
}

@HiltViewModel
class TasbihViewModel @Inject constructor() : ViewModel() {

    private val _ui = MutableStateFlow(TasbihUi())
    val ui: StateFlow<TasbihUi> = _ui.asStateFlow()

    fun increment() {
        _ui.value = _ui.value.copy(count = _ui.value.count + 1)
    }

    fun reset() {
        _ui.value = _ui.value.copy(count = 0)
    }

    fun setTarget(t: Int) {
        _ui.value = _ui.value.copy(target = t)
    }

    fun setDhikr(index: Int) {
        _ui.value = _ui.value.copy(dhikrIndex = index, count = 0)
    }
}
