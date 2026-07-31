package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import io.github.salehgnutux.gtsalat.domain.Quran
import io.github.salehgnutux.gtsalat.ui.theme.AmiriQuran

/**
 * المصحف المصوَّر (مصحف المدينة، 604 صفحة). الصور تُنزَّل عند التصفّح وتُخزَّن في كاش القرص (Coil)،
 * مع مصادرَ بديلةٍ تُجرَّب عند الفشل، وقلبِ ألوانٍ للوضع الليليّ. للقراءة فقط.
 */
@Composable
fun MushafScreen(onBack: () -> Unit, vm: QuranMetaViewModel = hiltViewModel()) {
    val surahs by vm.surahs.collectAsStateWithLifecycle()
    val lastPage by vm.lastMushafPage.collectAsStateWithLifecycle()
    // صفحة 1 = فهرس الصفحة 0. التصفّح لأعلى المصحف من اليمين (RTL افتراضيّ).
    val pager = rememberPagerState(pageCount = { Quran.TOTAL_PAGES })
    // يتبع سِمة التطبيق الفعليّة (لا وضع النظام) — وإلّا قُلبت صور المصحف خطأً عند فرض وضعٍ مخالفٍ للنظام.
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    var jumpOpen by remember { mutableStateOf(false) }
    var riwaya by remember { mutableStateOf("hafs") }   // حفص (Quran-PNG) · ورش (مجمّع الملك فهد)

    // استئنافٌ من آخر صفحةٍ محفوظة (مرّةً واحدةً عند التحميل).
    var restored by remember { mutableStateOf(false) }
    LaunchedEffect(lastPage) {
        if (!restored && lastPage in 1..604) {
            restored = true
            if (lastPage > 1) pager.scrollToPage(lastPage - 1)
        }
    }
    // حفظ الصفحة الجاريّة عند الاستقرار.
    LaunchedEffect(pager.currentPage, restored) {
        if (restored) vm.saveMushafPage(pager.currentPage + 1)
    }

    // اسم السورة الحاليّة (أكبر سورةٍ تبدأ صفحتُها ≤ الصفحة الجاريّة).
    val currentPage = pager.currentPage + 1
    val currentSurah = remember(surahs, currentPage) {
        surahs.lastOrNull { it.page <= currentPage }
    }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("المصحف المصوَّر", onBack)
        // عارضةٌ مضغوطةٌ في صفٍّ واحد: زرّ سورة + مبدّل رواية + اسم السورة/الصفحة — لتكبر مساحة المصحف.
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 1.dp) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box {
                    IconButton(onClick = { jumpOpen = true }) { Icon(Icons.Outlined.MenuBook, "الذهاب إلى سورة", tint = MaterialTheme.colorScheme.primary) }
                    DropdownMenu(expanded = jumpOpen, onDismissRequest = { jumpOpen = false }) {
                        surahs.forEach { s ->
                            DropdownMenuItem(text = { Text("${s.n}. ${s.ar}") }, onClick = { jumpOpen = false; scrollToPage(pager, s.page) })
                        }
                    }
                }
                androidx.compose.material3.FilterChip(riwaya == "hafs", { riwaya = "hafs" }, { Text("حفص") })
                androidx.compose.material3.FilterChip(riwaya == "warsh", { riwaya = "warsh" }, { Text("ورش") })
                // تنزيل صور الرواية الحاليّة كاملةً للعمل دون إنترنت (بتقدّمٍ)، أو حذفها إن اكتملت.
                val dl by vm.mushafDownload.collectAsStateWithLifecycle()
                val downloadingThis = dl.running && dl.riwaya == riwaya
                val count = remember(riwaya, dl) { vm.mushafCount(riwaya) }
                when {
                    downloadingThis -> Text("${dl.done * 100 / dl.total}٪", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    count >= Quran.TOTAL_PAGES -> IconButton(onClick = { vm.deleteMushaf(riwaya) }) { Icon(Icons.Filled.DownloadDone, "المصحف مُنزَّل (اضغط للحذف)", tint = MaterialTheme.colorScheme.primary) }
                    else -> IconButton(onClick = { vm.downloadMushaf(riwaya) }, enabled = !dl.running) { Icon(Icons.Filled.Download, "تنزيل صور الرواية للعمل دون إنترنت", tint = MaterialTheme.colorScheme.outline) }
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    currentSurah?.let {
                        Text("سورة ${it.ar}", fontFamily = AmiriQuran, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                    }
                    Text("$currentPage / ${Quran.TOTAL_PAGES}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        // خلفيّةٌ رماديّةٌ داكنةٌ ثابتةٌ تُطابق لون صفحة المصحف المقلوبة (≈0.12) فتندمج الحواشي.
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().background(if (dark) androidx.compose.ui.graphics.Color(0xFF1E1E1E) else androidx.compose.ui.graphics.Color.White)) { index ->
            // يُفضّل الملفّ المحلّيّ إن نُزِّل لهذه الرواية، وإلّا يُبثّ (Coil يخزّنه).
            MushafPage(page = index + 1, invert = dark, riwaya = riwaya, local = vm.localPage(index + 1, riwaya))
        }
    }
}

private fun scrollToPage(
    pager: androidx.compose.foundation.pager.PagerState,
    page: Int,
) { pager.requestScrollToPage((page - 1).coerceIn(0, Quran.TOTAL_PAGES - 1)) }

@Composable
private fun MushafPage(page: Int, invert: Boolean, riwaya: String = "hafs", local: java.io.File? = null) {
    val ctx = LocalContext.current
    // إن نُزِّلت الصفحة محليّاً استعملها مباشرةً، وإلّا الشبكة مع مصادرَ بديلة (حسب الرواية).
    val urls = remember(page, riwaya) { listOf(Quran.pageImageUrl(page, riwaya)) + Quran.pageImageFallbacks(page, riwaya) }
    var idx by remember(page, riwaya) { mutableIntStateOf(0) }
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(ctx).data(local ?: urls[idx]).crossfade(true).build(),
    )
    val state = painter.state
    // عند فشل مصدرٍ، جرّب التالي.
    LaunchedEffect(state) {
        if (state is AsyncImagePainter.State.Error && idx < urls.lastIndex) idx++
    }
    // قلبٌ ليليٌّ ناعم: خلفيّةٌ رماديّةٌ داكنة (لا أسود قاسٍ) ونصٌّ أبيضُ مائلٌ للدفء (لا أبيض ناصع)
    // لتقليل إجهاد العين — الميل -0.73 والإزاحة 216 يحوّلان الأبيض 255→30 والأسود 0→216.
    val filter = remember(invert) {
        if (invert) ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
            -0.73f, 0f, 0f, 0f, 216f,
            0f, -0.73f, 0f, 0f, 216f,
            0f, 0f, -0.73f, 0f, 216f,
            0f, 0f, 0f, 1f, 0f,
        ))) else null
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painter,
            contentDescription = "صفحة $page",
            modifier = Modifier.fillMaxSize().padding(8.dp),
            contentScale = ContentScale.Fit,
            colorFilter = filter,
        )
        when (state) {
            is AsyncImagePainter.State.Loading -> CircularProgressIndicator()
            is AsyncImagePainter.State.Error ->
                if (idx >= urls.lastIndex) Text("تعذّر تحميل الصفحة $page", color = MaterialTheme.colorScheme.error)
            else -> Unit
        }
    }
}
