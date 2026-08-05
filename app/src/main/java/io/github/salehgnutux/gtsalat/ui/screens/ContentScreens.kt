package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.domain.DuaCategory
import io.github.salehgnutux.gtsalat.domain.HadithCollection
import io.github.salehgnutux.gtsalat.domain.HikamCategory
import io.github.salehgnutux.gtsalat.domain.Quran
import io.github.salehgnutux.gtsalat.ui.theme.AmiriQuran

/** عنصرُ محتوًى موحّدٌ للعرض البطاقيّ (حديث/دعاء/حكمة). */
data class ContentItem(
    val number: Int, val title: String, val subtitle: String,
    val body: String, val chips: List<String>, val shareCaption: String?,
)

/** عرضٌ بطاقيٌّ (سلايد) لعناصر المحتوى — بطاقةٌ لكلّ عنصرٍ بنسخٍ ومشاركة. */
@Composable
fun ContentPager(items: List<ContentItem>) {
    if (items.isEmpty()) return
    val pager = rememberPagerState(pageCount = { items.size })
    Column(Modifier.fillMaxSize()) {
    HorizontalPager(
        state = pager,
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentPadding = PaddingValues(horizontal = 24.dp),
        pageSpacing = 12.dp,
    ) { page ->
        val it = items[page]
        val clipboard = LocalClipboardManager.current
        val ctx = LocalContext.current
        val caption = listOfNotNull(it.subtitle.ifBlank { null }, it.shareCaption?.ifBlank { null }).joinToString(" — ").ifBlank { null }
        Card(Modifier.fillMaxSize().padding(vertical = 12.dp)) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        if (it.title.isNotBlank()) Text("${it.number}. ${it.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (it.subtitle.isNotBlank()) Text(it.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Row {
                        IconButton(onClick = { io.github.salehgnutux.gtsalat.util.Share.send(ctx, it.body, caption) }) { Icon(Icons.Filled.Share, "مشاركة", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = { clipboard.setText(AnnotatedString(io.github.salehgnutux.gtsalat.util.Share.decorate(it.body, caption))) }) { Icon(Icons.Filled.ContentCopy, "نسخ", tint = MaterialTheme.colorScheme.primary) }
                    }
                }
                Text(
                    it.body, fontFamily = AmiriQuran,
                    fontSize = if (it.body.length > 400) 20.sp else 23.sp,
                    lineHeight = if (it.body.length > 400) 38.sp else 44.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                )
                if (it.chips.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    it.chips.forEach { c -> AssistChip(onClick = {}, label = { Text(c, style = MaterialTheme.typography.labelSmall) }) }
                }
            }
        }
    }
    Text(
        "${pager.currentPage + 1} / ${items.size}",
        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    )
    }
}

/* ============================ الأحاديث ============================ */

@Composable
fun HadithScreen(onBack: () -> Unit, vm: HadithViewModel = hiltViewModel()) {
    val collections by vm.collections.collectAsStateWithLifecycle()
    val cards by hiltViewModel<ThemeToggleViewModel>().adhkarCardView.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var q by remember { mutableStateOf("") }

    // كلّ الأحاديث موحّدةً (للبحث والعرض البطاقيّ عبر المجموعات).
    val allItems = remember(collections) {
        collections.flatMap { col -> col.hadiths.map { h ->
            ContentItem(h.n, h.chapter, h.narrator, h.text, listOfNotNull(h.source.ifBlank { null }, h.grade.ifBlank { null }), h.source)
        } }
    }
    val filtered = remember(allItems, q) {
        val nq = Quran.normalize(q)
        if (nq.isBlank()) allItems else allItems.filter { Quran.normalize("${it.title} ${it.subtitle} ${it.body}").contains(nq) }
    }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("الأربعون والأحاديث", onBack, actions = {
            IconButton(onClick = { showSearch = !showSearch; if (!showSearch) q = "" }) {
                Icon(if (showSearch) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "بحث")
            }
            AdhkarViewToggleButton()
        })
        if (showSearch) {
            OutlinedTextField(
                value = q, onValueChange = { q = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("ابحث في الأحاديث بكلمة…") }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Filled.Close, "مسح") } },
            )
        }

        // بطاقات عند تفعيل الوضع أو أثناء البحث؛ وإلّا القائمة المبوّبة.
        if (cards || q.isNotBlank()) {
            if (filtered.isEmpty()) Text("لا نتائج لـ«$q»", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outline)
            else ContentPager(filtered)
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            collections.forEach { col: HadithCollection ->
                item(key = "h_" + col.id) {
                    SectionTitle(col.name, "${col.hadiths.size} حديثاً · ${col.author}")
                }
                items(col.hadiths, key = { "${col.id}_${it.n}" }) { h ->
                    ContentCard(
                        number = h.n,
                        title = h.chapter,
                        subtitle = h.narrator,
                        body = h.text,
                        chips = listOfNotNull(h.source.ifBlank { null }, h.grade.ifBlank { null }),
                        shareCaption = h.source,
                    )
                }
            }
        }
    }
}

/* ============================ الأدعية ============================ */

@Composable
fun DuasScreen(onBack: () -> Unit, vm: DuasViewModel = hiltViewModel()) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val cards by hiltViewModel<ThemeToggleViewModel>().adhkarCardView.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("الأدعية المأثورة", onBack, actions = { AdhkarViewToggleButton() })
        if (cards) {
            val items = remember(categories) {
                categories.flatMap { cat -> cat.items.map { d -> ContentItem(d.n, cat.name, d.context, d.text, listOfNotNull(d.source.ifBlank { null }), d.source) } }
            }
            ContentPager(items)
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            categories.forEach { cat: DuaCategory ->
                item(key = "d_${cat.id}") {
                    SectionTitle("${cat.icon} ${cat.name}".trim(), "${cat.items.size} دعاءً")
                }
                items(cat.items, key = { "${cat.id}_${it.n}" }) { d ->
                    ContentCard(
                        number = d.n,
                        title = "",
                        subtitle = d.context,
                        body = d.text,
                        chips = listOfNotNull(d.source.ifBlank { null }),
                        shareCaption = d.source,
                    )
                }
            }
        }
    }
}

/* ============================ الحِكَم ============================ */

@Composable
fun HikamScreen(onBack: () -> Unit, vm: HikamViewModel = hiltViewModel()) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val cards by hiltViewModel<ThemeToggleViewModel>().adhkarCardView.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("حِكَم ومواعظ السلف", onBack, actions = { AdhkarViewToggleButton() })
        if (cards) {
            val items = remember(categories) {
                categories.flatMap { cat -> cat.items.map { w -> ContentItem(w.n, cat.name, w.sayer, w.text, listOfNotNull(w.source.ifBlank { null }), w.source) } }
            }
            ContentPager(items)
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            categories.forEach { cat: HikamCategory ->
                item(key = "k_${cat.id}") {
                    SectionTitle(cat.name, "${cat.items.size} حكمة")
                }
                items(cat.items, key = { "${cat.id}_${it.n}" }) { w ->
                    ContentCard(
                        number = w.n,
                        title = "",
                        subtitle = w.sayer,
                        body = w.text,
                        chips = listOfNotNull(w.source.ifBlank { null }),
                        shareCaption = w.source,
                    )
                }
            }
        }
    }
}

/* ============================ عناصر مشتركة ============================ */

@Composable
private fun SectionTitle(title: String, meta: String) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun ContentCard(
    number: Int,
    title: String,
    subtitle: String,
    body: String,
    chips: List<String>,
    shareCaption: String? = null,
) {
    val clipboard = LocalClipboardManager.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // المصدر المرفق بالنسخ/المشاركة (المتحدّث + المرجع إن وُجدا).
    val caption = listOfNotNull(subtitle.ifBlank { null }, shareCaption?.ifBlank { null }).joinToString(" — ").ifBlank { null }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    if (title.isNotBlank()) {
                        Text("$number. $title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
                Row {
                    IconButton(onClick = { io.github.salehgnutux.gtsalat.util.Share.send(ctx, body, caption) }) {
                        Icon(Icons.Filled.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { clipboard.setText(AnnotatedString(io.github.salehgnutux.gtsalat.util.Share.decorate(body, caption))) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Text(
                body,
                fontFamily = AmiriQuran,
                fontSize = 21.sp,
                lineHeight = 38.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (chips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    chips.forEach { c ->
                        AssistChip(
                            onClick = {},
                            label = { Text(c, style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(),
                        )
                    }
                }
            }
        }
    }
}
