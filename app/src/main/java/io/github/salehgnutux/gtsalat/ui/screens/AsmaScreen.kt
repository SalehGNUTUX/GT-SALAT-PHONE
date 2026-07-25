package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.salehgnutux.gtsalat.domain.AsmaName
import io.github.salehgnutux.gtsalat.ui.theme.AmiriQuran

@Composable
fun AsmaScreen(onBack: () -> Unit, vm: AsmaViewModel = hiltViewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("أسماء الله الحسنى", onBack)
        if (items.isEmpty()) return@Column

        val pager = rememberPagerState(pageCount = { items.size })

        // بطاقاتٌ تُمرَّر يميناً ويساراً (سلايد)
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            pageSpacing = 12.dp,
        ) { page ->
            AsmaCard(items[page])
        }

        // مؤشّر الموضع «الاسم n من 99» + نقاط مصغّرة
        Text(
            "${pager.currentPage + 1} / ${items.size}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun AsmaCard(name: AsmaName) {
    Card(Modifier.fillMaxSize().padding(vertical = 16.dp)) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text("${name.index}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
                name.arabic,
                fontFamily = AmiriQuran,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                name.meaning,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
            if (name.ref.isNotBlank()) {
                Text(
                    name.ref,
                    fontFamily = AmiriQuran,
                    fontSize = 22.sp,
                    lineHeight = 40.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
    }
}
