package io.github.salehgnutux.gtsalat.ui.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** خطوةٌ في التسبيح المختلط (دبر الصلاة): الذِّكر وعدده الوارد في السنّة. */
data class MixedStep(val dhikr: String, val count: Int)

data class TasbihUi(
    val count: Int = 0,            // العدّ داخل اللفّة (عاديّ) أو داخل الخطوة (مختلط)
    val target: Int = 33,          // 0 = بلا حدّ (الوضع العاديّ)
    val dhikrIndex: Int = 0,
    val mixed: Boolean = false,    // وضع دبر الصلاة (سبحان/حمد/تكبير/تهليل بعدد السنّة)
    val mixedStep: Int = 0,
    val mixedDone: Boolean = false,
    val pulse: Int = 0,            // يتغيّر عند بلوغ حدٍّ (للاهتزاز مرّةً)
) {
    val dhikr: String get() = if (mixed) MIXED[mixedStep].dhikr else DHIKR_LIST[dhikrIndex]
    val stepTarget: Int get() = if (mixed) MIXED[mixedStep].count else target
    val laps: Int get() = if (!mixed && target > 0) count / target else 0
    val inLap: Int get() = if (mixed) count else if (target > 0) count % target else count
    /** المجموع الكلّيّ في الوضع المختلط (من 0 إلى 100). */
    val mixedOverall: Int get() = MIXED.take(mixedStep).sumOf { it.count } + count

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

        // تسبيح دبر الصلاة كما ورد (مسلم): 33 + 33 + 33 ثمّ تهليلةٌ تُتمّ المئة.
        val MIXED = listOf(
            MixedStep("سُبْحَانَ اللهِ", 33),
            MixedStep("الْحَمْدُ لِلَّهِ", 33),
            MixedStep("اللهُ أَكْبَرُ", 33),
            MixedStep("لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ", 1),
        )
        const val MIXED_TOTAL = 100
    }
}

@HiltViewModel
class TasbihViewModel @Inject constructor() : ViewModel() {

    private val _ui = MutableStateFlow(TasbihUi())
    val ui: StateFlow<TasbihUi> = _ui.asStateFlow()

    fun increment() {
        val s = _ui.value
        if (!s.mixed) {
            val c = s.count + 1
            val reached = s.target > 0 && c % s.target == 0
            _ui.value = s.copy(count = c, pulse = if (reached) s.pulse + 1 else s.pulse)
            return
        }
        if (s.mixedDone) return
        val c = s.count + 1
        if (c >= TasbihUi.MIXED[s.mixedStep].count) {
            // اكتملت الخطوة: ننتقل للتالية أو نُنهي عند الوصول للمئة.
            if (s.mixedStep < TasbihUi.MIXED.lastIndex) {
                _ui.value = s.copy(mixedStep = s.mixedStep + 1, count = 0, pulse = s.pulse + 1)
            } else {
                _ui.value = s.copy(count = c, mixedDone = true, pulse = s.pulse + 1)
            }
        } else {
            _ui.value = s.copy(count = c)
        }
    }

    fun reset() {
        _ui.value = _ui.value.copy(count = 0, mixedStep = 0, mixedDone = false)
    }

    fun setTarget(t: Int) {
        _ui.value = _ui.value.copy(target = t)
    }

    fun setDhikr(index: Int) {
        _ui.value = _ui.value.copy(dhikrIndex = index, count = 0)
    }

    fun setMixed(v: Boolean) {
        _ui.value = _ui.value.copy(mixed = v, count = 0, mixedStep = 0, mixedDone = false)
    }
}
