package io.github.salehgnutux.gtsalat.ui.update

/**
 * حاجزٌ (seam) بين نكهتَي التوزيع. في نكهة foss (تنزيلٌ مباشر + F-Droid) المحدِّثُ
 * الذاتيّ متوفّر، فتظهر خيارات التحديث في الإعدادات. أمّا في full (بلاي) فالقيمةُ
 * false فتُخفى تلك الخيارات، إذ التحديث في بلاي عبر المتجر وحده.
 */
const val SELF_UPDATE_AVAILABLE = true
