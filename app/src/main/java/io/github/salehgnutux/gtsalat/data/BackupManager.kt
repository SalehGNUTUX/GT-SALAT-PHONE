package io.github.salehgnutux.gtsalat.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.data.local.TimetableDao
import io.github.salehgnutux.gtsalat.data.local.TimetableEntity
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** ما يمكن تضمينه/استيراده — يختاره المستخدم (المُصدِّر والمستورِد كلاهما). */
data class BackupOptions(
    val settings: Boolean = true,
    val prayers: Boolean = false,
    val audio: Boolean = false,
    val mushaf: Boolean = false,
) {
    val any: Boolean get() = settings || prayers || audio || mushaf
}

/** ما هو متاحٌ على الجهاز للتصدير (أحجامٌ تقديريّة). */
data class BackupSizes(val prayersCount: Int = 0, val audioBytes: Long = 0, val mushafBytes: Long = 0)

/** ما تحتويه حزمةٌ عند فحصها قبل الاستيراد. */
data class BackupContents(
    val hasSettings: Boolean = false,
    val prayersCount: Int = 0,
    val audioFiles: Int = 0,
    val audioBytes: Long = 0,
    val mushafFiles: Int = 0,
    val mushafBytes: Long = 0,
) {
    val hasPrayers get() = prayersCount > 0
    val hasAudio get() = audioFiles > 0
    val hasMushaf get() = mushafFiles > 0
    val anything get() = hasSettings || hasPrayers || hasAudio || hasMushaf
}

/** نتيجة الاستيراد: ما استُعيد فعلاً. */
data class BackupImport(val ok: Boolean, val settings: Boolean = false, val prayers: Int = 0, val files: Int = 0)

/**
 * نسخٌ احتياطيّ/تصديرٌ انتقائيّ إلى حزمة ZIP واحدة: الإعدادات + المواقيت المخزّنة
 * + القرآن الصوتيّ والمصحف المُنزَّلان. المُصدِّر يختار ما يضمّن، والمستورِد يختار ما يستعيد.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository,
    private val dao: TimetableDao,
) {
    private val files = context.filesDir

    private fun audioDirs() = listOf(File(files, "audio"), File(files, "audio_ayat"))
    private fun mushafDirs() = files.listFiles()?.filter { it.isDirectory && (it.name == "mushaf" || it.name.startsWith("mushaf_")) }.orEmpty()

    /** أحجامٌ تقديريّةٌ لما هو متاحٌ على الجهاز (لحوار التصدير). */
    suspend fun sizes(): BackupSizes = withContext(Dispatchers.IO) {
        BackupSizes(
            prayersCount = runCatching { dao.count() }.getOrDefault(0),
            audioBytes = audioDirs().sumOf { dirSize(it) },
            mushafBytes = mushafDirs().sumOf { dirSize(it) },
        )
    }

    private fun dirSize(dir: File): Long =
        if (!dir.exists()) 0L else dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    // ---------- التصدير ----------

    /** يصدّر إلى [uri] (SAF) وفق [opts]. */
    suspend fun export(uri: Uri, opts: BackupOptions): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { writeZip(it, opts) } != null
        }.getOrDefault(false)
    }

    /** يصدّر إلى ملفٍّ في ذاكرة التخبئة لأجل المشاركة عبر ورقة النظام. يعيد الملفّ أو null. */
    suspend fun exportToCache(opts: BackupOptions): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "backups").apply { mkdirs() }
            val out = File(dir, "GT-SALAT-backup.zip")
            out.outputStream().use { writeZip(it, opts) }
            out
        }.getOrNull()
    }

    private suspend fun writeZip(os: OutputStream, opts: BackupOptions) {
        ZipOutputStream(os.buffered()).use { zip ->
            if (opts.settings) {
                zip.putNextEntry(ZipEntry("settings.json"))
                zip.write(settingsRepo.exportJson().toByteArray())
                zip.closeEntry()
            }
            if (opts.prayers) {
                zip.putNextEntry(ZipEntry("prayers.json"))
                zip.write(prayersToJson(dao.allBlockingSafe()).toByteArray())
                zip.closeEntry()
            }
            if (opts.audio) audioDirs().forEach { zipDir(zip, it) }
            if (opts.mushaf) mushafDirs().forEach { zipDir(zip, it) }
        }
    }

    private fun zipDir(zip: ZipOutputStream, dir: File) {
        if (!dir.exists()) return
        dir.walkTopDown().filter { it.isFile && it.length() > 0 }.forEach { f ->
            val rel = "files/" + f.relativeTo(files).path.replace(File.separatorChar, '/')
            zip.putNextEntry(ZipEntry(rel))
            f.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }

    // ---------- الفحص قبل الاستيراد ----------

    /** يفحص حزمةً ويبيّن ما تحتويه (ليختار المستورِد ما يستعيد). */
    suspend fun inspect(uri: Uri): BackupContents = withContext(Dispatchers.IO) {
        runCatching {
            var hasSettings = false; var prayers = 0
            var audioFiles = 0; var audioBytes = 0L; var mushafFiles = 0; var mushafBytes = 0L
            context.contentResolver.openInputStream(uri)?.use { ins ->
                ZipInputStream(ins.buffered()).use { zip ->
                    var e: ZipEntry? = zip.nextEntry
                    while (e != null) {
                        val name = e.name
                        when {
                            name == "settings.json" -> hasSettings = true
                            name == "prayers.json" -> prayers = runCatching { JSONObject(zip.readBytes().decodeToString()).getJSONArray("prayers").length() }.getOrDefault(0)
                            name.startsWith("files/") && !e.isDirectory -> {
                                val len = readAndCount(zip)
                                when (groupOf(name)) {
                                    "audio" -> { audioFiles++; audioBytes += len }
                                    "mushaf" -> { mushafFiles++; mushafBytes += len }
                                }
                            }
                        }
                        zip.closeEntry()
                        e = zip.nextEntry
                    }
                }
            }
            BackupContents(hasSettings, prayers, audioFiles, audioBytes, mushafFiles, mushafBytes)
        }.getOrDefault(BackupContents())
    }

    /** يستهلك محتوى المدخل ويعيد طوله (لحساب الأحجام أثناء الفحص). */
    private fun readAndCount(zip: ZipInputStream): Long {
        val buf = ByteArray(64 * 1024); var total = 0L; var n: Int
        while (zip.read(buf).also { n = it } != -1) total += n
        return total
    }

    private fun groupOf(name: String): String? = when {
        name.startsWith("files/audio/") || name.startsWith("files/audio_ayat/") -> "audio"
        name.startsWith("files/mushaf") -> "mushaf"
        else -> null
    }

    // ---------- الاستيراد الانتقائيّ ----------

    /** يستورد من [uri] ما اختاره المستورِد في [opts] فقط. */
    suspend fun import(uri: Uri, opts: BackupOptions): BackupImport = withContext(Dispatchers.IO) {
        runCatching {
            var didSettings = false; var prayers = 0; var fileCount = 0
            context.contentResolver.openInputStream(uri)?.use { ins ->
                ZipInputStream(ins.buffered()).use { zip ->
                    var e: ZipEntry? = zip.nextEntry
                    while (e != null) {
                        val name = e.name
                        when {
                            name == "settings.json" && opts.settings -> {
                                settingsRepo.importJson(zip.readBytes().decodeToString()); didSettings = true
                            }
                            name == "prayers.json" && opts.prayers -> {
                                prayers = restorePrayers(zip.readBytes().decodeToString())
                            }
                            name.startsWith("files/") && !e.isDirectory -> {
                                val grp = groupOf(name)
                                val wanted = (grp == "audio" && opts.audio) || (grp == "mushaf" && opts.mushaf)
                                val rel = name.removePrefix("files/")
                                if (wanted && isSafeRelPath(rel)) {
                                    val out = File(files, rel)
                                    out.parentFile?.mkdirs()
                                    out.outputStream().use { zip.copyTo(it) }
                                    fileCount++
                                }
                            }
                        }
                        zip.closeEntry()
                        e = zip.nextEntry
                    }
                }
            }
            BackupImport(ok = true, settings = didSettings, prayers = prayers, files = fileCount)
        }.getOrDefault(BackupImport(ok = false))
    }

    /** يمنع الهروب من filesDir عبر مسارٍ خبيث (../). */
    private fun isSafeRelPath(rel: String): Boolean =
        rel.isNotBlank() && !rel.startsWith("/") && ".." !in rel.split('/')

    private fun prayersToJson(rows: List<TimetableEntity>): String {
        val arr = JSONArray()
        rows.forEach { r ->
            arr.put(
                JSONObject()
                    .put("dateIso", r.dateIso).put("methodId", r.methodId).put("locKey", r.locKey)
                    .put("hijri", r.hijri ?: JSONObject.NULL)
                    .put("fajr", r.fajr).put("sunrise", r.sunrise).put("dhuhr", r.dhuhr)
                    .put("asr", r.asr).put("maghrib", r.maghrib).put("isha", r.isha)
                    .put("source", r.source).put("savedAt", r.savedAt),
            )
        }
        return JSONObject().put("prayers", arr).toString()
    }

    private suspend fun restorePrayers(json: String): Int {
        val arr = JSONObject(json).getJSONArray("prayers")
        val rows = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TimetableEntity(
                dateIso = o.getString("dateIso"), methodId = o.getInt("methodId"), locKey = o.getString("locKey"),
                hijri = if (o.isNull("hijri")) null else o.getString("hijri"),
                fajr = o.getLong("fajr"), sunrise = o.getLong("sunrise"), dhuhr = o.getLong("dhuhr"),
                asr = o.getLong("asr"), maghrib = o.getLong("maghrib"), isha = o.getLong("isha"),
                source = o.optString("source", "api"), savedAt = o.optLong("savedAt", 0L),
            )
        }
        if (rows.isNotEmpty()) dao.upsertAll(rows)
        return rows.size
    }

    private suspend fun TimetableDao.allBlockingSafe(): List<TimetableEntity> = runCatching { all() }.getOrDefault(emptyList())
}
