package io.github.salehgnutux.gtsalat.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.salehgnutux.gtsalat.MainActivity

// ألوان الودجت: خلفيّةٌ شفّافةٌ خفيفة (زجاجيّة) تُقرأ فوق أيّ خلفيّة، مع نصٍّ أبيض وذهبيّ.
private val Scrim = Color(0x59000000)          // أسود ~35% (شبه شفّاف)
private val White = Color(0xFFFFFFFF)
private val Gold = Color(0xFFE8C766)
private val Faint = Color(0xFFD9D9D9)

/* ============ ودجت 1: الصلاة القادمة (مدمج) ============ */

class NextPrayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        val snap = loadWidgetSnapshot(context)
        val open = actionStartActivity(Intent(context, MainActivity::class.java))
        provideContent {
            Column(
                GlanceModifier.fillMaxWidth().background(Scrim).cornerRadius(20.dp)
                    .padding(14.dp).clickable(open),
            ) {
                if (!snap.hasLocation) {
                    Text("حدّد موقعك في التطبيق", style = TextStyle(color = ColorProvider(White)))
                    return@Column
                }
                Text(
                    "الصلاة القادمة",
                    style = TextStyle(color = ColorProvider(Faint), fontSize = 12.sp),
                )
                Row(GlanceModifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(snap.nextName, style = TextStyle(color = ColorProvider(Gold), fontSize = 22.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.width(10.dp))
                    Text(snap.nextTime, style = TextStyle(color = ColorProvider(White), fontSize = 20.sp, fontWeight = FontWeight.Bold))
                }
                if (snap.remaining.isNotBlank()) {
                    Text(
                        "المتبقّي ${snap.remaining}",
                        style = TextStyle(color = ColorProvider(Faint), fontSize = 13.sp),
                        modifier = GlanceModifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

/* ============ ودجت 2: مواقيت اليوم (كامل) ============ */

class TodayTimesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        val snap = loadWidgetSnapshot(context)
        val open = actionStartActivity(Intent(context, MainActivity::class.java))
        provideContent {
            Column(
                GlanceModifier.fillMaxWidth().background(Scrim).cornerRadius(20.dp)
                    .padding(14.dp).clickable(open),
            ) {
                if (!snap.hasLocation) {
                    Text("حدّد موقعك في التطبيق", style = TextStyle(color = ColorProvider(White)))
                    return@Column
                }
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(snap.city.ifBlank { "المواقيت" }, style = TextStyle(color = ColorProvider(White), fontSize = 15.sp, fontWeight = FontWeight.Bold), modifier = GlanceModifier.defaultWeight())
                    if (snap.hijri.isNotBlank()) {
                        Text(snap.hijri, style = TextStyle(color = ColorProvider(Faint), fontSize = 11.sp))
                    }
                }
                Spacer(GlanceModifier.width(6.dp))
                Row(GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
                    snap.prayers.forEach { (name, time, pid) ->
                        val isNext = pid == snap.nextId
                        Column(
                            GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(name, style = TextStyle(color = ColorProvider(if (isNext) Gold else Faint), fontSize = 12.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal))
                            Text(time, style = TextStyle(color = ColorProvider(if (isNext) Gold else White), fontSize = 15.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal))
                        }
                    }
                }
            }
        }
    }
}

/** يحدّث كلّ ودجتات الصلاة (يُستدعى من مسارات الخلفيّة). */
suspend fun updateAllPrayerWidgets(context: Context) {
    NextPrayerWidget().updateAll(context)
    TodayTimesWidget().updateAll(context)
}
