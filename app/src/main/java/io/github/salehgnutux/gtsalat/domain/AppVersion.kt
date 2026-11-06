package io.github.salehgnutux.gtsalat.domain

/**
 * مقارنة إصدارات التطبيق بمراحلها: 2.0-alpha < 2.0-alpha2 < 2.0-beta < 2.0-rc < 2.0 < 2.1-alpha.
 * تُهمَل لاحقة النكهة (foss/full). فلا يفوت من ثبّت ألفا إشعارُ صدور المستقرّة من الرقم نفسه.
 */
object AppVersion {
    private const val STABLE = 3
    private val stagePattern = Regex("^(alpha|beta|rc)[._]?(\\d*)$")

    private class Parsed(val numbers: List<Int>, val stage: Int, val stageNumber: Int)

    private fun parse(raw: String): Parsed {
        val parts = raw.trim().removePrefix("v").removePrefix("V").lowercase().split("-")
        val numbers = parts.first().split(".").mapNotNull { it.toIntOrNull() }
        val match = parts.drop(1).firstNotNullOfOrNull { stagePattern.matchEntire(it) }
        val stage = when (match?.groupValues?.get(1)) {
            "alpha" -> 0
            "beta" -> 1
            "rc" -> 2
            else -> STABLE
        }
        return Parsed(numbers, stage, match?.groupValues?.get(2)?.toIntOrNull() ?: 0)
    }

    /** هل [a] أحدث من [b]؟ */
    fun isNewer(a: String, b: String): Boolean {
        val x = parse(a)
        val y = parse(b)
        for (i in 0 until maxOf(x.numbers.size, y.numbers.size)) {
            val p = x.numbers.getOrElse(i) { 0 }
            val q = y.numbers.getOrElse(i) { 0 }
            if (p != q) return p > q
        }
        if (x.stage != y.stage) return x.stage > y.stage
        return x.stageNumber > y.stageNumber
    }

    fun isPrerelease(version: String): Boolean = parse(version).stage != STABLE
}
