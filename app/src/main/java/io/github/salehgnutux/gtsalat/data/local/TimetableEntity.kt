package io.github.salehgnutux.gtsalat.data.local

import androidx.room.Entity
import androidx.room.Index

/**
 * يومٌ واحد من المواقيت مخزَّن محلّيّاً. المفتاح المركّب (التاريخ + الطريقة + الموقع)
 * يجعل الكاش صالحاً لأشهرٍ طويلة ويُبطَل تلقائيّاً عند تغيّر الطريقة أو الموقع.
 */
@Entity(
    tableName = "timetable",
    primaryKeys = ["dateIso", "methodId", "locKey"],
    indices = [Index(value = ["methodId", "locKey"])],
)
data class TimetableEntity(
    val dateIso: String,
    val methodId: Int,
    val locKey: String,
    val hijri: String?,
    val fajr: Long,
    val sunrise: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long,
    val source: String,        // "api" أو "local"
    val savedAt: Long,
)
