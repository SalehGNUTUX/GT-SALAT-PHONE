package io.github.salehgnutux.gtsalat.audio

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * الكاتم التلقائيّ: يكتم رنين المكالمات والإشعارات أثناء الصلاة ثمّ يستعيد الوضع السابق.
 * لا يمسّ مجرى المنبّه، فالأذان (USAGE_ALARM) يظلّ يُسمع. تغيير وضع الرنين إلى الصمت
 * يتطلّب إذن الوصول لسياسة الإشعارات على أندرويد 6+.
 */
@Singleton
class RingerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository,
) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /** هل يملك التطبيق إذن تغيير وضع الرنين إلى الصمت؟ */
    fun canSilence(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || nm.isNotificationPolicyAccessGranted

    /** يحفظ وضع الرنين الحاليّ ثمّ يكتمه (يبقى المنبّه/الأذان مسموعاً). */
    suspend fun silence() {
        if (!canSilence()) return
        settingsRepo.setSavedRingerMode(audio.ringerMode)
        runCatching { audio.ringerMode = AudioManager.RINGER_MODE_SILENT }
    }

    /** يستعيد وضع الرنين المحفوظ (أو العاديّ إن لم يُحفَظ). */
    suspend fun restore() {
        if (!canSilence()) return
        val saved = settingsRepo.savedRingerMode() ?: AudioManager.RINGER_MODE_NORMAL
        runCatching { audio.ringerMode = saved }
    }
}
