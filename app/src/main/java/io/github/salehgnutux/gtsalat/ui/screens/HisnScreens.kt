package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import io.github.salehgnutux.gtsalat.ui.theme.AmiriQuran

/* ============ قائمة أبواب حصن المسلم (132) ============ */

@Composable
fun HisnScreen(onOpen: (Int) -> Unit, onBack: () -> Unit, vm: HisnViewModel = hiltViewModel()) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val q = query.trim()

    // نتائج البحث: أذكارٌ يحوي نصّها الكلمات المفتاحيّة (عبر كلّ الأبواب).
    val results = remember(q, categories) {
        if (q.isBlank()) emptyList()
        else categories.flatMap { cat -> cat.items.map { cat to it } }
            .filter { (cat, d) -> d.text.contains(q) || cat.name.contains(q) }
    }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("حصن المسلم", onBack)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("ابحث عن ذكرٍ بكلمةٍ مفتاحيّة…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (q.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = "مسح") }
                }
            },
            singleLine = true,
        )

        if (q.isNotBlank()) {
            if (results.isEmpty()) {
                Text("لا نتائج لـ«$q»", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outline)
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text("${results.size} نتيجة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    items(results, key = { (c, d) -> "${c.id}_${d.n}" }) { (cat, d) ->
                        Card(Modifier.fillMaxWidth().clickable { onOpen(cat.id) }) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${cat.icon} ${cat.name}".trim(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    d.text,
                                    fontFamily = AmiriQuran,
                                    fontSize = 18.sp,
                                    lineHeight = 32.sp,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(categories, key = { it.id }) { cat ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(cat.id) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(cat.icon.ifBlank { "📿" }, fontSize = 26.sp)
                        Column(Modifier.weight(1f)) {
                            Text(cat.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${cat.count} ذكراً", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

/* ============ جلسة باب واحد بعدٍّ تنازليّ ============ */

@Composable
fun HisnCategoryScreen(onBack: () -> Unit, vm: HisnCategoryViewModel = hiltViewModel()) {
    val name by vm.name.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val remaining by vm.remaining.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val done = remaining.count { it == 0 }
    val total = items.size

    val cards by androidx.hilt.navigation.compose.hiltViewModel<ThemeToggleViewModel>().adhkarCardView.collectAsStateWithLifecycle()
    val pager = androidx.compose.foundation.pager.rememberPagerState(pageCount = { items.size })
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader(name.ifBlank { "حصن المسلم" }, onBack, actions = {
            // أذكار النوم (الباب 2) لها تسجيلٌ صوتيّ.
            if (vm.categoryId == 2) {
                AdhkarListenAction(sectionKey = "sleep", title = "أذكار النوم", route = "hisn/2", rawResId = io.github.salehgnutux.gtsalat.R.raw.adhkar_sleep)
            }
            TextButton(onClick = { vm.reset(); scope.launch { if (items.isNotEmpty()) pager.scrollToPage(0) } }) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text("تصفير")
            }
            AdhkarViewToggleButton()
        })
        if (total > 0) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("أكملتَ $done من $total", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else done.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }
        if (cards && items.isNotEmpty()) {
            // عرضٌ بطاقيّ (سلايد) بأسلوب أسماء الله الحسنى.
            androidx.compose.foundation.pager.HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 28.dp),
                pageSpacing = 12.dp,
            ) { page ->
                val dhikr = items[page]
                val left = remaining.getOrElse(page) { dhikr.count }
                AdhkarCard(text = dhikr.text, count = dhikr.count, left = left, done = left == 0, onTap = {
                    vm.tap(page)
                    if (left <= 1 && page < items.lastIndex) {
                        scope.launch { kotlinx.coroutines.delay(350); pager.animateScrollToPage(page + 1) }
                    }
                })
            }
            Text(
                "${pager.currentPage + 1} / ${items.size}",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            )
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(items, key = { _, it -> it.n }) { i, dhikr ->
                val left = remaining.getOrElse(i) { dhikr.count }
                val isDone = left == 0
                Card(
                    Modifier.fillMaxWidth().clickable(enabled = !isDone) { vm.tap(i) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            dhikr.text,
                            fontFamily = AmiriQuran,
                            fontSize = 22.sp,
                            lineHeight = 40.sp,
                            color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            val ctx = androidx.compose.ui.platform.LocalContext.current
                            Row {
                                IconButton(onClick = { io.github.salehgnutux.gtsalat.util.Share.send(ctx, dhikr.text) }) {
                                    Icon(Icons.Filled.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { clipboard.setText(AnnotatedString(io.github.salehgnutux.gtsalat.util.Share.decorate(dhikr.text))) }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            CounterBadge(left = left, total = dhikr.count, done = isDone)
                        }
                    }
                }
            }
        }
    }
}
