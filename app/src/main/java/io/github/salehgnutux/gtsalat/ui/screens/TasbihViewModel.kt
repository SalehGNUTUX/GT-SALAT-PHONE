package io.github.salehgnutux.gtsalat.ui.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** نوعٌ من التسبيح المختلط: اسمٌ وقائمة كلماتٍ **تتناوب** واحدةً بعد الأخرى (لا تتكرّر نفسها). */
data class MixedType(val name: String, val phrases: List<String>)

data class TasbihUi(
    val count: Int = 0,            // العدّ الكلّيّ (تتناوب الكلمة معه في المختلط)
    val target: Int = 33,          // 0 = بلا حدّ
    val dhikrIndex: Int = 0,
    val mixed: Boolean = false,    // الوضع المختلط: كلماتٌ تتناوب واحدةً بعد الأخرى
    val mixedType: Int = 0,        // 0 = تسبيح · 1 = الباقيات الصالحات
    val pulse: Int = 0,            // يتغيّر عند بلوغ حدٍّ (للاهتزاز مرّةً)
) {
    private val phrases: List<String> get() = MIXED_TYPES[mixedType.coerceIn(0, MIXED_TYPES.lastIndex)].phrases
    // في المختلط تتناوب الكلمة مع كلّ عدّة؛ وفي العاديّ ذِكرٌ واحدٌ ثابت.
    val dhikr: String get() = if (mixed) phrases[count % phrases.size] else DHIKR_LIST[dhikrIndex]
    val stepTarget: Int get() = target
    val laps: Int get() = if (target > 0) count / target else 0
    val inLap: Int get() = if (target > 0) count % target else count

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

        /** أنواع المختلط: كلماتُ كلّ نوعٍ تتناوب واحدةً بعد الأخرى بشكلٍ متتالٍ مختلط. */
        val MIXED_TYPES = listOf(
            MixedType(
                "تسبيح",
                listOf("سُبْحَانَ اللهِ وَبِحَمْدِهِ", "سُبْحَانَ اللهِ الْعَظِيمِ"),
            ),
            MixedType(
                "الباقيات الصالحات",
                listOf(
                    "سُبْحَانَ اللهِ",
                    "الْحَمْدُ لِلَّهِ",
                    "لَا إِلَهَ إِلَّا اللهُ",
                    "اللهُ أَكْبَرُ",
                    "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللهِ",
                ),
            ),
        )
    }
}

@HiltViewModel
class TasbihViewModel @Inject constructor() : ViewModel() {

    private val _ui = MutableStateFlow(TasbihUi())
    val ui: StateFlow<TasbihUi> = _ui.asStateFlow()

    // عدٌّ موحّد: في المختلط تتناوب الكلمة تلقائيّاً حسب العدّ (dhikr = phrases[count % size]).
    fun increment() {
        val s = _ui.value
        val c = s.count + 1
        val reached = s.target > 0 && c % s.target == 0
        _ui.value = s.copy(count = c, pulse = if (reached) s.pulse + 1 else s.pulse)
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

    fun setMixed(v: Boolean) {
        _ui.value = _ui.value.copy(mixed = v, count = 0)
    }

    fun setMixedType(index: Int) {
        _ui.value = _ui.value.copy(mixedType = index, count = 0)
    }
}
