package io.github.salehgnutux.gtsalat.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import io.github.salehgnutux.gtsalat.audio.AdhanPlayback
import io.github.salehgnutux.gtsalat.audio.AdhanService
import io.github.salehgnutux.gtsalat.data.settings.SettingsRepository
import io.github.salehgnutux.gtsalat.ui.theme.AmiriQuran
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import io.github.salehgnutux.gtsalat.ui.theme.GtSalatTheme

/**
 * نافذة أذانٍ ملء الشاشة تظهر فوق شاشة القفل مع وميضٍ متدرّج، اسم البرنامج،
 * اسم الصلاة/الذكر ووقتها، وزرّ إيقافٍ يوقف صوت الأذان.
 * تُطلَق عبر full-screen intent من إشعار الأذان (المحدّد بالإعداد `fullScreenAdhan`).
 */
@AndroidEntryPoint
class AdhanAlarmActivity : ComponentActivity() {

    @Inject lateinit var settingsRepo: SettingsRepository
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        forceWakeScreen()

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "GT-SALAT"
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE).orEmpty()
        val isDhikr = intent.getBooleanExtra(EXTRA_IS_DHIKR, false)

        setContent {
            GtSalatTheme {
                AdhanAlarmScreen(title = title, subtitle = subtitle, isDhikr = isDhikr, onStop = { closeWindow() })
            }
        }

        // عند انتهاء الصوت: إن لم يطلب المستخدم الإبقاء نُغلق النافذة؛ وإن طلب الإبقاء نُبقيها ظاهرةً
        // لكن **ندع الشاشة تنطفئ طبيعيّاً** (نحرّر قفل الاستيقاظ ونزيل علم إبقاء الشاشة) فلا تُستنزَف الطاقة.
        lifecycleScope.launch {
            val keep = runCatching { settingsRepo.settings.first().keepAdhanScreen }.getOrDefault(false)
            var wasActive = false
            AdhanPlayback.active.collect { active ->
                if (active) wasActive = true
                else if (wasActive) { if (keep) allowScreenOff() else finish() }
            }
        }
        // شبكة أمان: بعد خمس دقائق دع الشاشة تنطفئ حتى لو لم يُشِر الصوت لنهايته (نمط الرنّة/الصامت).
        lifecycleScope.launch {
            kotlinx.coroutines.delay(5 * 60_000L)
            allowScreenOff()
        }
    }

    /** يُبقي النافذة ظاهرةً لكن يسمح للشاشة بالانطفاء (تحرير القفل + إزالة علم إبقاء الشاشة). */
    private fun allowScreenOff() {
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        runCatching { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    private fun closeWindow() {
        runCatching { startService(Intent(this, AdhanService::class.java).setAction(AdhanService.ACTION_STOP)) }
        finish()
    }

    /** يوقظ الشاشة فعليّاً (لبعض الأجهزة لا يكفي setTurnScreenOn). */
    private fun forceWakeScreen() {
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "gtsalat:adhanscreen",
            ).apply { acquire(5 * 60_000L) }
        }
    }

    override fun onDestroy() {
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        super.onDestroy()
    }

    /** يُظهر النافذة فوق القفل ويوقظ الشاشة (يومضها) — بديل الأعلام القديمة على أندرويد 8.1+. */
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_SUBTITLE = "subtitle"
        const val EXTRA_IS_DHIKR = "is_dhikr"

        /** يبني نيّةً تُطلق هذه النافذة (تُستعمَل كـ full-screen intent في الإشعار). */
        fun intent(context: Context, title: String, subtitle: String, isDhikr: Boolean): Intent =
            Intent(context, AdhanAlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_SUBTITLE, subtitle)
                .putExtra(EXTRA_IS_DHIKR, isDhikr)
    }
}

@Composable
private fun AdhanAlarmScreen(title: String, subtitle: String, isDhikr: Boolean, onStop: () -> Unit) {
    // تنفّسٌ لطيفٌ وبطيء: تدرّجٌ يتحوّل بين لونين من ألوان السِمة (لا وميضٌ عنيف).
    val transition = rememberInfiniteTransition(label = "breathe")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "t",
    )
    // نبضةٌ خفيفةٌ للرمز.
    val pulse by transition.animateFloat(
        initialValue = 0.94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val c1 = MaterialTheme.colorScheme.primary
    val c2 = MaterialTheme.colorScheme.tertiary
    val top by animateColorAsState(lerp(c1, c2, t), label = "top")
    val bottom by animateColorAsState(lerp(c2, c1, t * 0.6f), label = "bottom")

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "GT-SALAT",
            color = Color.White.copy(alpha = 0.92f),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
        )
        // رمز الكعبة بنبضةٍ لطيفة.
        Text(
            if (isDhikr) "📿" else "🕋",
            fontSize = 72.sp,
            modifier = Modifier
                .padding(top = 20.dp)
                .graphicsLayer(scaleX = pulse, scaleY = pulse),
            textAlign = TextAlign.Center,
        )
        Text(
            if (isDhikr) "أذكارٌ بعد الصلاة" else "حان الآن وقت الصلاة",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 17.sp,
            modifier = Modifier.padding(top = 18.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            title,
            color = Color.White,
            fontFamily = AmiriQuran,
            fontWeight = FontWeight.Bold,
            fontSize = 46.sp,
            modifier = Modifier.padding(top = 10.dp),
            textAlign = TextAlign.Center,
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp),
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = onStop,
            modifier = Modifier.padding(top = 48.dp).size(width = 210.dp, height = 62.dp),
            shape = RoundedCornerShape(31.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = c1),
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null)
            Text("إيقاف", fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** مزجٌ خطّيٌّ بسيط بين لونين (بلا تبعيّاتٍ إضافيّة). */
private fun lerp(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)
