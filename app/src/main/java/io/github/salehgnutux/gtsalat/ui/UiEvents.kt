package io.github.salehgnutux.gtsalat.ui

import kotlinx.coroutines.flow.MutableSharedFlow

/** ناقل أحداثٍ خفيف: إعادة النقر على «الرئيسيّة» تطلب التمرير لرأس الصفحة. */
object UiEvents {
    val scrollHomeToTop = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    fun requestHomeTop() { scrollHomeToTop.tryEmit(Unit) }
}
