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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import io.github.salehgnutux.gtsalat.domain.TaharaTool

@Composable
fun TaharaToolScreen(onBack: () -> Unit) {
    var purity by remember { mutableStateOf<TaharaTool.Purity?>(null) }
    val g = remember(purity) { purity?.let { TaharaTool.guide(it) } }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("طهرت الآن، ماذا أصلّي؟", onBack)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item("d") { InfoBanner("أداةٌ إرشاديّةٌ تعليميّةٌ على المشهور المالكيّ، لا فتوى ولا حكمٌ مُلزِم. للمسائل الخاصّة (اضطراب العادة، الشكّ، إدراك الوقت) راجِعي أهل العلم.") }
            item("q") {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("هل رأيتِ علامة الطهر (القصّة البيضاء أو الجفوف)؟", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(purity == TaharaTool.Purity.CONFIRMED, { purity = TaharaTool.Purity.CONFIRMED }, { Text("نعم، تحقّق الطهر") })
                            FilterChip(purity == TaharaTool.Purity.NOT_YET, { purity = TaharaTool.Purity.NOT_YET }, { Text("لا") })
                        }
                        FilterChip(purity == TaharaTool.Purity.UNSURE, { purity = TaharaTool.Purity.UNSURE }, { Text("لستُ متأكّدة") })
                    }
                }
            }
            g?.let { gd ->
                item("res") {
                    val positive = purity == TaharaTool.Purity.CONFIRMED
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (positive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(gd.title, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                color = if (positive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            Field("الغسل:", gd.ghusl)
                            Field("الصلاة الحاضرة:", gd.currentPrayer)
                            Field("الصلاة السابقة:", gd.previousPrayer)
                            Field("الصلوات الفائتة:", gd.missed)
                            Field("السبب:", gd.reason)
                            Field("المصدر:", gd.source)
                            Field("تنبيه:", gd.consult)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    if (value.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, color = MaterialTheme.colorScheme.onSurface)
    }
}
