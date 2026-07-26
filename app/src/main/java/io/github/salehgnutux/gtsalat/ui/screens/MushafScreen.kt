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
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    // صفحة 1 = فهرس الصفحة 0. التصفّح لأعلى المصحف من اليمين (RTL افتراضيّ).
    val pager = rememberPagerState(pageCount = { Quran.TOTAL_PAGES })
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    var jumpOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("المصحف المصوَّر", onBack)
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 1.dp) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    TextButton(onClick = { jumpOpen = true }) {
                        Icon(Icons.Outlined.MenuBook, null)
                        Text("  الذهاب إلى سورة", fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = jumpOpen, onDismissRequest = { jumpOpen = false }) {
                        surahs.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.n}. ${s.ar}") },
                                onClick = { jumpOpen = false; scrollToPage(pager, s.page) },
                            )
                        }
                    }
                }
                Text(
                    "صفحة ${pager.currentPage + 1} / ${Quran.TOTAL_PAGES}",
                    Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().background(if (dark) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.White)) { index ->
            MushafPage(page = index + 1, invert = dark)
        }
    }
}

private fun scrollToPage(
    pager: androidx.compose.foundation.pager.PagerState,
    page: Int,
) { pager.requestScrollToPage((page - 1).coerceIn(0, Quran.TOTAL_PAGES - 1)) }

@Composable
private fun MushafPage(page: Int, invert: Boolean) {
    val ctx = LocalContext.current
    val urls = remember(page) { listOf(Quran.pageImageUrl(page)) + Quran.pageImageFallbacks(page) }
    var idx by remember(page) { mutableIntStateOf(0) }
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(ctx).data(urls[idx]).crossfade(true).build(),
    )
    val state = painter.state
    // عند فشل مصدرٍ، جرّب التالي.
    LaunchedEffect(state) {
        if (state is AsyncImagePainter.State.Error && idx < urls.lastIndex) idx++
    }
    val filter = remember(invert) {
        if (invert) ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
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
