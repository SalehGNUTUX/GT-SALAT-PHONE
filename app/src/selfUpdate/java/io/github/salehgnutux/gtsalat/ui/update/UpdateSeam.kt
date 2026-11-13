package io.github.salehgnutux.gtsalat.ui.update

/**
 * حاجزٌ (seam) بين التوزيع المباشر ومتجر بلاي. هنا (foss وfull ما دام التطبيق خارج المتجر)
 * المحدِّث الذاتيّ متوفّر، فتظهر خيارات التحديث في الإعدادات. وبناء المتجر (-PplayStore=true)
 * يستعمل src/playStore حيث القيمة false، إذ التحديث في بلاي عبر المتجر وحده.
 */
const val SELF_UPDATE_AVAILABLE = true
