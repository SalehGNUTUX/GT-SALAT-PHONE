package io.github.salehgnutux.gtsalat.data

import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.notification.NotificationHelper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * جسدٌ فارغٌ لنكهة بلاي (full): سياسةُ بلاي تمنع أن يحدّث التطبيقُ نفسَه بغير بلاي
 * («إساءة استخدام الجهاز والشبكة»)، فلا محدِّثَ ذاتيّ في هذه النكهة. نفس تواقيع
 * النسخة الحرّة (foss) ليبقى حقنُ Hilt في RootViewModel سليماً بلا شرطٍ في المستدعي.
 */
@Singleton
class UpdateRepository @Inject constructor(
    @Suppress("UNUSED_PARAMETER") settingsRepo: SettingsRepository,
    @Suppress("UNUSED_PARAMETER") notifications: NotificationHelper,
) {
    suspend fun checkForUpdate() { /* لا شيء في نكهة بلاي */ }
}
