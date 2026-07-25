package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.ContentRepository
import io.github.salehgnutux.gtsalat.domain.TafsirAyah
import io.github.salehgnutux.gtsalat.domain.TafsirSurah
import io.github.salehgnutux.gtsalat.ui.theme.AmiriQuran
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ===== قائمة السور ===== */

@HiltViewModel
class TafsirViewModel @Inject constructor(private val repo: ContentRepository) : ViewModel() {
    private val _surahs = MutableStateFlow<List<TafsirSurah>>(emptyList())
    val surahs: StateFlow<List<TafsirSurah>> = _surahs.asStateFlow()
    init { viewModelScope.launch { _surahs.value = repo.tafsirSurahs() } }
}

@Composable
fun TafsirScreen(onOpen: (Int) -> Unit, onBack: () -> Unit, vm: TafsirViewModel = hiltViewModel()) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("التفسير الميسّر", onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(surahs, key = { it.n }) { s ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(s.n) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${s.n}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(s.name, fontFamily = AmiriQuran, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("${s.type} · ${s.count} آية", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

/* ===== آيات سورةٍ بتفسيرها ===== */

@HiltViewModel
class TafsirSurahViewModel @Inject constructor(
    savedState: SavedStateHandle,
    repo: ContentRepository,
) : ViewModel() {
    private val n: Int = (savedState.get<String>("n") ?: "1").toIntOrNull() ?: 1
    private val _surah = MutableStateFlow<TafsirSurah?>(null)
    val surah: StateFlow<TafsirSurah?> = _surah.asStateFlow()
    init { viewModelScope.launch { _surah.value = repo.tafsirSurah(n) } }
}

@Composable
fun TafsirSurahScreen(onBack: () -> Unit, vm: TafsirSurahViewModel = hiltViewModel()) {
    val surah by vm.surah.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("سورة ${surah?.name ?: ""}".trim(), onBack)
        val ayahs = surah?.ayahs ?: emptyList()
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(ayahs, key = { it.n }) { a -> AyahCard(a) }
        }
    }
}

@Composable
private fun AyahCard(a: TafsirAyah) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${a.n}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(
                    a.text,
                    fontFamily = AmiriQuran,
                    fontSize = 22.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
            if (a.tafsir.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    a.tafsir,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
