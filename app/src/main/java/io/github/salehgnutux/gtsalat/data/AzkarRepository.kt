package io.github.salehgnutux.gtsalat.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * أذكار وأدعية عامّة من ملفّ الأصول `azkar.txt` (مفصولة بسطرٍ فيه `%`).
 * تُقرأ مرّةً وتُخزَّن في الذاكرة.
 */
@Singleton
class AzkarRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var cache: List<String>? = null

    suspend fun all(): List<String> = cache ?: withContext(Dispatchers.IO) {
        val text = runCatching {
            context.assets.open("azkar.txt").bufferedReader().use { it.readText() }
        }.getOrDefault("")
        val list = text.split("\n")
            .fold(mutableListOf(StringBuilder())) { acc, line ->
                if (line.trim() == "%") acc.add(StringBuilder()) else {
                    val sb = acc.last()
                    if (sb.isNotEmpty()) sb.append('\n')
                    sb.append(line.trim())
                }
                acc
            }
            .map { it.toString().trim() }
            .filter { it.isNotBlank() }
        cache = list
        list
    }
}
