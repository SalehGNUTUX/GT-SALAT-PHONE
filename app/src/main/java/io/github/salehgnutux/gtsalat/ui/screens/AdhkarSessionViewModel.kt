package io.github.salehgnutux.gtsalat.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.domain.MorningEveningAdhkar
import io.github.salehgnutux.gtsalat.domain.MorningEveningAdhkar.Dhikr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AdhkarSessionViewModel @Inject constructor(
    savedState: SavedStateHandle,
) : ViewModel() {

    val type: String = savedState["type"] ?: "morning"
    val isEvening: Boolean = type == "evening"
    val items: List<Dhikr> = MorningEveningAdhkar.forType(type)

    /** المتبقّي من كلّ ذكر (عدٌّ تنازليّ يبدأ من العدد المأثور، والصفر يعني اكتماله). */
    private val _remaining = MutableStateFlow(items.map { it.count })
    val remaining: StateFlow<List<Int>> = _remaining.asStateFlow()

    fun tap(index: Int) {
        val cur = _remaining.value.toMutableList()
        if (index in cur.indices && cur[index] > 0) {
            cur[index] = cur[index] - 1
            _remaining.value = cur
        }
    }

    fun reset() {
        _remaining.value = items.map { it.count }
    }
}
