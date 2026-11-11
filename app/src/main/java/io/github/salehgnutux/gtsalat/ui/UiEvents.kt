package io.github.salehgnutux.gtsalat.ui

import kotlinx.coroutines.flow.MutableSharedFlow

/** ناقل أحداثٍ خفيف: إعادة النقر على «الرئيسيّة» تطلب التمرير لرأس الصفحة. */
object UiEvents {
    val scrollHomeToTop = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    fun requestHomeTop() { scrollHomeToTop.tryEmit(Unit) }

    /**
     * قسم إعداداتٍ يُفتَح عند الوصول إلى الإعدادات (من «ما الجديد» أو رسالة الأذونات). StateFlow لا SharedFlow:
     * قد يُطلَب قبل أن تُركَّب شاشة الإعدادات، فيبقى حتى تستهلكه. في شكل المطويّات يكفي `settingsOpenSection`،
     * وفي شكل البطاقات تفتح الشاشةُ صفحةَ القسم مباشرةً بدل الشبكة.
     */
    val openSettingsPage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    fun requestSettingsPage(section: String) { openSettingsPage.value = section }
}
