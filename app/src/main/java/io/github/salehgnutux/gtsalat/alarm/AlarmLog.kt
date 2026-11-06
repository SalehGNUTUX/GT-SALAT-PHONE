package io.github.salehgnutux.gtsalat.alarm

import android.content.Context
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * سجلٌّ محلّيٌّ قصيرٌ لإطلاق التنبيهات (لا يغادر الجهاز): موعد كلّ تنبيهٍ ووقت وصوله الفعليّ وما جرى له.
 * يفرّق بين تأخيرٍ من النظام أو إدارة بطاريّة المُصنّع، وخللٍ داخل التطبيق.
 */
object AlarmLog {
    private const val FILE = "alarm_log.txt"
    private const val MAX_LINES = 80
    private val fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss", Locale.US)

    private fun stamp(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(fmt)

    @Synchronized
    fun add(context: Context, what: String, scheduledAt: Long = 0L, detail: String = "") {
        runCatching {
            val now = System.currentTimeMillis()
            val parts = buildList {
                add(stamp(now))
                add(what)
                if (scheduledAt > 0L) {
                    add("موعده ${stamp(scheduledAt)}")
                    add("تأخّر ${(now - scheduledAt) / 1000}ث")
                }
                if (detail.isNotBlank()) add(detail)
            }
            val file = File(context.filesDir, FILE)
            val old = if (file.exists()) file.readLines() else emptyList()
            file.writeText((old + parts.joinToString(" · ")).takeLast(MAX_LINES).joinToString("\n") + "\n")
        }
    }

    /** الأحدث أوّلاً. */
    @Synchronized
    fun read(context: Context): String = runCatching {
        File(context.filesDir, FILE).takeIf { it.exists() }?.readLines()?.asReversed()?.joinToString("\n").orEmpty()
    }.getOrDefault("")

    @Synchronized
    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE).delete() }
    }
}
