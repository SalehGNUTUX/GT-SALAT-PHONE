package io.github.salehgnutux.gtsalat.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.salehgnutux.gtsalat.MainActivity
import io.github.salehgnutux.gtsalat.R

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
                if (snap.remainingMillis > 0L) {
                    // عدّادٌ تنازليٌّ حيّ (يتناقص كلّ ثانيةٍ دون إيقاظ التطبيق).
                    AndroidRemoteViews(remoteViews = chronoRemoteViews(context, snap.remainingMillis))
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

/* ============ ودجت 3: الساعة وتقدّم الصلاة ============ */

class PrayerProgressWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snap = loadWidgetSnapshot(context)
        provideContent {
            if (!snap.hasLocation) {
                Column(
                    GlanceModifier.fillMaxWidth().background(Scrim).cornerRadius(20.dp)
                        .padding(16.dp).clickable(openApp(context)),
                ) { Text("افتح التطبيق لتحديد موقعك", style = TextStyle(color = ColorProvider(White))) }
            } else {
                // عناصرٌ أصليّة (RemoteViews) تتحدّث ذاتيّاً: الساعة والعدّاد حيّان دون إيقاظ التطبيق.
                // fillMaxSize ضروريّ وإلّا انكمش المحتوى (match_parent يُقاس ضدّ حاوية Glance الملتفّة).
                AndroidRemoteViews(remoteViews = progressRemoteViews(context, snap), modifier = GlanceModifier.fillMaxSize())
            }
        }
    }
}

/** يبني RemoteViews لودجت التقدّم: ساعة TextClock حيّة + عدّاد Chronometer تنازليّ حيّ + شريط تقدّم. */
private fun progressRemoteViews(context: Context, snap: WidgetSnapshot): RemoteViews {
    val layout = if (snap.progressStyle == "center") R.layout.widget_progress_center else R.layout.widget_progress_classic
    val rv = RemoteViews(context.packageName, layout)
    // نظام الساعة: نفرض نمط التطبيق (24/12) بضبط الصيغتين معاً بغضّ النظر عن إعداد النظام.
    val pattern = if (snap.use24) "HH:mm" else "hh:mm a"
    rv.setCharSequence(R.id.widget_clock, "setFormat24Hour", pattern)
    rv.setCharSequence(R.id.widget_clock, "setFormat12Hour", pattern)

    rv.setTextViewText(R.id.widget_hijri, snap.hijri)
    rv.setViewVisibility(R.id.widget_hijri, if (snap.hijri.isBlank()) View.GONE else View.VISIBLE)
    rv.setTextViewText(R.id.widget_gregorian, snap.gregorian)
    rv.setViewVisibility(R.id.widget_gregorian, if (snap.gregorian.isBlank()) View.GONE else View.VISIBLE)

    rv.setTextViewText(R.id.widget_prev, snap.prevName)
    rv.setTextViewText(R.id.widget_next, "${snap.nextName} ${snap.nextTime}")
    rv.setProgressBar(R.id.widget_progress, 1000, (snap.progress * 1000).toInt().coerceIn(0, 1000), false)

    // العدّاد التنازليّ الحيّ: أساسه لحظةُ حلول الصلاة القادمة.
    val hasRemaining = snap.remainingMillis > 0L
    rv.setViewVisibility(R.id.widget_remaining, if (hasRemaining) View.VISIBLE else View.GONE)
    if (hasRemaining) {
        rv.setChronometerCountDown(R.id.widget_remaining, true)
        rv.setChronometer(R.id.widget_remaining, SystemClock.elapsedRealtime() + snap.remainingMillis, "المتبقّي %s", true)
    }
    rv.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
    return rv
}

/** RemoteViews لعدّادٍ تنازليٍّ حيٍّ («المتبقّي MM:SS») — يُستعمَل في ودجت الصلاة القادمة. */
private fun chronoRemoteViews(context: Context, remainingMillis: Long): RemoteViews {
    val rv = RemoteViews(context.packageName, R.layout.widget_chrono)
    rv.setChronometerCountDown(R.id.widget_chrono, true)
    rv.setChronometer(R.id.widget_chrono, SystemClock.elapsedRealtime() + remainingMillis, "المتبقّي %s", true)
    return rv
}

/** PendingIntent يفتح التطبيق (لنقر RemoteViews الأصليّة). */
private fun openAppIntent(context: Context): PendingIntent =
    PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

/** يحدّث كلّ ودجتات الصلاة (يُستدعى من مسارات الخلفيّة). */
suspend fun updateAllPrayerWidgets(context: Context) {
    NextPrayerWidget().updateAll(context)
    TodayTimesWidget().updateAll(context)
    PrayerProgressWidget().updateAll(context)
}
