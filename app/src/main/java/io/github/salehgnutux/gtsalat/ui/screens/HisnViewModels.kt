package io.github.salehgnutux.gtsalat.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.ContentRepository
import io.github.salehgnutux.gtsalat.domain.HisnCategory
import io.github.salehgnutux.gtsalat.domain.HisnDhikr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** قائمة أبواب حصن المسلم الـ132. */
@HiltViewModel
class HisnViewModel @Inject constructor(private val repo: ContentRepository) : ViewModel() {
    private val _categories = MutableStateFlow<List<HisnCategory>>(emptyList())
    val categories: StateFlow<List<HisnCategory>> = _categories.asStateFlow()
    init { viewModelScope.launch { _categories.value = repo.hisnCategories() } }
}

/** جلسة بابٍ واحد: أذكاره بعدٍّ تنازليّ يبدأ من عدد التكرار المأثور. */
@HiltViewModel
class HisnCategoryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: ContentRepository,
) : ViewModel() {

    val categoryId: Int = (savedState.get<String>("id") ?: "1").toIntOrNull() ?: 1
    private val id: Int get() = categoryId

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _items = MutableStateFlow<List<HisnDhikr>>(emptyList())
    val items: StateFlow<List<HisnDhikr>> = _items.asStateFlow()

    private val _remaining = MutableStateFlow<List<Int>>(emptyList())
    val remaining: StateFlow<List<Int>> = _remaining.asStateFlow()

    init {
        viewModelScope.launch {
            val cat = repo.hisnCategory(id)
            if (cat != null) {
                _name.value = cat.name
                _items.value = cat.items
                _remaining.value = cat.items.map { it.count }
            }
        }
    }

    fun tap(index: Int) {
        val cur = _remaining.value.toMutableList()
        if (index in cur.indices && cur[index] > 0) {
            cur[index] = cur[index] - 1
            _remaining.value = cur
        }
    }

    fun reset() {
        _remaining.value = _items.value.map { it.count }
    }
}
