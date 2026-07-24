package io.github.salehgnutux.gtsalat.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.ContentRepository
import io.github.salehgnutux.gtsalat.domain.AsmaName
import io.github.salehgnutux.gtsalat.domain.DuaCategory
import io.github.salehgnutux.gtsalat.domain.HadithCollection
import io.github.salehgnutux.gtsalat.domain.HikamCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HadithViewModel @Inject constructor(private val repo: ContentRepository) : ViewModel() {
    private val _collections = MutableStateFlow<List<HadithCollection>>(emptyList())
    val collections: StateFlow<List<HadithCollection>> = _collections.asStateFlow()
    init { viewModelScope.launch { _collections.value = repo.hadithCollections() } }
}

@HiltViewModel
class DuasViewModel @Inject constructor(private val repo: ContentRepository) : ViewModel() {
    private val _categories = MutableStateFlow<List<DuaCategory>>(emptyList())
    val categories: StateFlow<List<DuaCategory>> = _categories.asStateFlow()
    init { viewModelScope.launch { _categories.value = repo.duas() } }
}

@HiltViewModel
class HikamViewModel @Inject constructor(private val repo: ContentRepository) : ViewModel() {
    private val _categories = MutableStateFlow<List<HikamCategory>>(emptyList())
    val categories: StateFlow<List<HikamCategory>> = _categories.asStateFlow()
    init { viewModelScope.launch { _categories.value = repo.hikamCategories() } }
}

@HiltViewModel
class AsmaViewModel @Inject constructor(private val repo: ContentRepository) : ViewModel() {
    private val _items = MutableStateFlow<List<AsmaName>>(emptyList())
    val items: StateFlow<List<AsmaName>> = _items.asStateFlow()
    init { viewModelScope.launch { _items.value = repo.asma() } }
}
