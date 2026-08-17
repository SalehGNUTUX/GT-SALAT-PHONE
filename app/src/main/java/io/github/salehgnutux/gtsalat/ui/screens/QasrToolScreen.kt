package io.github.salehgnutux.gtsalat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.salehgnutux.gtsalat.domain.QasrTool

@Composable
fun QasrToolScreen(onBack: () -> Unit) {
    var q1 by remember { mutableStateOf<Boolean?>(null) }   // المسافة
    var q2 by remember { mutableStateOf<Boolean?>(null) }   // مفارقة العمران
    var q3 by remember { mutableStateOf<Boolean?>(null) }   // مباح
    var q4 by remember { mutableStateOf<Boolean?>(null) }   // نيّة الإقامة

    val result = remember(q1, q2, q3, q4) {
        if (q1 != null && q2 != null && q3 != null && q4 != null)
            QasrTool.evaluate(QasrTool.Input(q1!!, q2!!, q3!!, q4!!)) else null
    }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("هل يجوز لي القصر الآن؟", onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item("d") { InfoBanner("أداةٌ إرشاديّةٌ على المشهور المالكيّ، لا فتوى. راجِع أهل العلم في حالتك الخاصّة.") }
            item("q1") { YesNoQuestion("هل تبلغ وجهتك حدّ مسافة القصر (نحو ${QasrTool.QASR_DISTANCE_KM} كلم) فأكثر؟", q1) { q1 = it } }
            item("q2") { YesNoQuestion("هل فارقتَ عمران بلدك؟", q2) { q2 = it } }
            item("q3") { YesNoQuestion("هل سفرك مباحٌ (ليس لمعصية)؟", q3) { q3 = it } }
            item("q4") { YesNoQuestion("هل نويتَ الإقامة أربعة أيّامٍ فأكثر في وجهتك؟", q4) { q4 = it } }
            result?.let { r ->
                item("res") { ResultCard(r) }
            }
        }
    }
}

@Composable
private fun YesNoQuestion(text: String, value: Boolean?, onAnswer: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(value == true, { onAnswer(true) }, { Text("نعم") })
                FilterChip(value == false, { onAnswer(false) }, { Text("لا") })
            }
        }
    }
}

@Composable
private fun ResultCard(r: QasrTool.Result) {
    val positive = r.hukm == QasrTool.Hukm.QASR_SUNNAH
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (positive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(r.title, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = if (positive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
            Field("الصلوات:", r.shortenedPrayers)
            Field("الجمع:", r.joinText)
            Field("السبب:", r.reason)
            Field("المصدر:", r.source)
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, color = MaterialTheme.colorScheme.onSurface)
    }
}
