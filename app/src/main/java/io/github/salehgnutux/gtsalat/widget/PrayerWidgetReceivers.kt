package io.github.salehgnutux.gtsalat.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** مُستقبِل ودجت «الصلاة القادمة» (مدمج). */
class NextPrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextPrayerWidget()
}

/** مُستقبِل ودجت «مواقيت اليوم» (كامل). */
class TodayTimesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayTimesWidget()
}
