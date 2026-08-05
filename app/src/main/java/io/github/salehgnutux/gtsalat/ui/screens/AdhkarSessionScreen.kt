package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import io.github.salehgnutux.gtsalat.ui.theme.AmiriQuran

@Composable
fun AdhkarSessionScreen(onBack: () -> Unit, vm: AdhkarSessionViewModel = hiltViewModel()) {
    val remaining by vm.remaining.collectAsStateWithLifecycle()
    val title = if (vm.isEvening) "أذكار المساء" else "أذكار الصباح"
    val doneCount = remaining.count { it == 0 }
    val total = vm.items.size
    val cards by androidx.hilt.navigation.compose.hiltViewModel<ThemeToggleViewModel>().adhkarCardView.collectAsStateWithLifecycle()
    // حالة الـpager مرفوعةٌ للأعلى ليعيدها زرّ «تصفير» إلى البطاقة الأولى.
    val pager = androidx.compose.foundation.pager.rememberPagerState(pageCount = { vm.items.size })
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        // زرّ البطاقات في actions فيصير بجانب زرّ السِمة المدمج في الترويسة (لا تكرار).
        SubScreenHeader(title, onBack, actions = {
            TextButton(onClick = { vm.reset(); scope.launch { pager.scrollToPage(0) } }) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text("تصفير")
            }
            AdhkarViewToggleButton()
        })

        // شريط تقدّم الجلسة
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                "أكملتَ $doneCount من $total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else doneCount.toFloat() / total },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }

        if (cards) {
            // عرضٌ بطاقيّ (سلايد) بأسلوب أسماء الله الحسنى — بطاقةٌ لكلّ ذكرٍ تُمرَّر يميناً/يساراً.
            androidx.compose.foundation.pager.HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 28.dp),
                pageSpacing = 12.dp,
            ) { page ->
                val dhikr = vm.items[page]
                val left = remaining.getOrElse(page) { dhikr.count }
                val done = left == 0
                AdhkarCard(text = dhikr.text, count = dhikr.count, left = left, done = done, onTap = {
                    vm.tap(page)
                    // عند إكمال عدد الذكر بهذه النقرة، ننتقل تلقائيّاً للبطاقة التالية.
                    if (left <= 1 && page < vm.items.lastIndex) {
                        scope.launch { kotlinx.coroutines.delay(350); pager.animateScrollToPage(page + 1) }
                    }
                })
            }
            Text(
                "${pager.currentPage + 1} / ${vm.items.size}",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(vm.items) { i, dhikr ->
                    val left = remaining.getOrElse(i) { dhikr.count }
                    val done = left == 0
                    Card(
                        Modifier.fillMaxWidth().clickable(enabled = !done) { vm.tap(i) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (done) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                dhikr.text,
                                fontFamily = AmiriQuran,
                                fontSize = 22.sp,
                                lineHeight = 40.sp,
                                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text(
                                    if (dhikr.count > 1) "العدد المأثور: ${dhikr.count}" else "مرّة واحدة",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                                CounterBadge(left = left, total = dhikr.count, done = done)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** بطاقة ذكرٍ للعرض السلايد: نصٌّ بخطّ أميري + عدّاد تنازليّ، تُنقَر للعدّ. */
@Composable
internal fun AdhkarCard(text: String, count: Int, left: Int, done: Boolean, onTap: () -> Unit) {
    Card(
        Modifier.fillMaxSize().padding(vertical = 16.dp).clickable(enabled = !done, onClick = onTap),
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val scroll = rememberScrollState()
            Text(
                text,
                fontFamily = AmiriQuran,
                fontSize = if (text.length > 220) 22.sp else 26.sp,
                lineHeight = if (text.length > 220) 42.sp else 48.sp,
                fontWeight = FontWeight.Bold,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll),
            )
            Text(
                if (count > 1) "العدد المأثور: $count" else "مرّة واحدة",
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline,
            )
            CounterBadge(left = left, total = count, done = done)
            Text(
                if (done) "اكتمل ✓" else "انقر البطاقة للعدّ",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
internal fun CounterBadge(left: Int, total: Int, done: Boolean) {
    Box(
        Modifier.size(56.dp).clip(CircleShape)
            .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(Icons.Filled.Check, contentDescription = "اكتمل", tint = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text(
                "$left",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
