package io.github.salehgnutux.gtsalat.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.salehgnutux.gtsalat.MainActivity

// ألوان الودجت: خلفيّةٌ شبه شفّافة (زجاجيّة) تُقرأ فوق أيّ خلفيّة، مع نصٍّ أبيض وذهبيّ.
private val Scrim = Color(0x66000000)
private val White = Color(0xFFFFFFFF)
private val Gold = Color(0xFFF0CE7A)
private val Faint = Color(0xFFE0E0E0)

private fun openApp(context: Context) = actionStartActivity(Intent(context, MainActivity::class.java))

/* ============ ودجت 1: الصلاة القادمة (مدمج) ============ */

class NextPrayerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snap = loadWidgetSnapshot(context)
        val open = openApp(context)
        provideContent {
            Column(
                GlanceModifier.fillMaxWidth().background(Scrim).cornerRadius(20.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp).clickable(open),
            ) {
                if (!snap.hasLocation) {
                    Text("افتح التطبيق لتحديد موقعك", style = TextStyle(color = ColorProvider(White)))
                    return@Column
                }
                Text("الصلاة القادمة", style = TextStyle(color = ColorProvider(Faint), fontSize = 12.sp))
                Row(GlanceModifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(snap.nextName, style = TextStyle(color = ColorProvider(Gold), fontSize = 22.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.width(10.dp))
                    Text(snap.nextTime, style = TextStyle(color = ColorProvider(White), fontSize = 20.sp, fontWeight = FontWeight.Bold))
                }
                if (snap.remaining.isNotBlank()) {
                    Text("المتبقّي ${snap.remaining}", style = TextStyle(color = ColorProvider(Faint), fontSize = 13.sp))
                }
            }
        }
    }
}

/* ============ ودجت 2: مواقيت اليوم (متجاوب) ============ */

class TodayTimesWidget : GlanceAppWidget() {
    // حجمان: مدمج (صفّ المواقيت فقط) وكامل (ترويسة + المواقيت) — فلا يُقصّ المحتوى ولا يختفي.
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(200.dp, 52.dp), DpSize(250.dp, 110.dp)),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snap = loadWidgetSnapshot(context)
        val open = openApp(context)
        provideContent {
            val tall = LocalSize.current.height >= 90.dp
            Column(
                GlanceModifier.fillMaxWidth().background(Scrim).cornerRadius(20.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp).clickable(open),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!snap.hasLocation) {
                    Text("افتح التطبيق لتحديد موقعك", style = TextStyle(color = ColorProvider(White)))
                    return@Column
                }
                if (tall) {
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            snap.city.ifBlank { "المواقيت" },
                            style = TextStyle(color = ColorProvider(White), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        if (snap.hijri.isNotBlank()) {
                            Text(snap.hijri, style = TextStyle(color = ColorProvider(Faint), fontSize = 11.sp))
                        }
                    }
                    Spacer(GlanceModifier.height(8.dp))
                }
                TimesRow(snap)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun TimesRow(snap: WidgetSnapshot) {
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        snap.prayers.forEach { (name, time, pid) ->
            val isNext = pid == snap.nextId
            Column(
                GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    name,
                    style = TextStyle(color = ColorProvider(if (isNext) Gold else Faint), fontSize = 12.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal),
                )
                Text(
                    time,
                    style = TextStyle(color = ColorProvider(if (isNext) Gold else White), fontSize = 15.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal),
                )
            }
        }
    }
}

/** يحدّث كلّ ودجتات الصلاة (يُستدعى من مسارات الخلفيّة). */
suspend fun updateAllPrayerWidgets(context: Context) {
    NextPrayerWidget().updateAll(context)
    TodayTimesWidget().updateAll(context)
}
