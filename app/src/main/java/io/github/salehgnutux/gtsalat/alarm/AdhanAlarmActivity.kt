package io.github.salehgnutux.gtsalat.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.salehgnutux.gtsalat.audio.AdhanService
import io.github.salehgnutux.gtsalat.ui.theme.GtSalatTheme

/**
 * نافذة أذانٍ ملء الشاشة تظهر فوق شاشة القفل مع وميضٍ متدرّج، اسم البرنامج،
 * اسم الصلاة/الذكر ووقتها، وزرّ إيقافٍ يوقف صوت الأذان.
 * تُطلَق عبر full-screen intent من إشعار الأذان (المحدّد بالإعداد `fullScreenAdhan`).
 */
class AdhanAlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "GT-SALAT"
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE).orEmpty()
        val isDhikr = intent.getBooleanExtra(EXTRA_IS_DHIKR, false)

        setContent {
            GtSalatTheme {
                AdhanAlarmScreen(title = title, subtitle = subtitle, isDhikr = isDhikr) {
                    // إيقاف الصوت ثمّ إغلاق النافذة.
                    runCatching {
                        startService(Intent(this, AdhanService::class.java).setAction(AdhanService.ACTION_STOP))
                    }
                    finish()
                }
            }
        }
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
    // وميضٌ لطيف: تدرّجٌ يتنفّس بين لونين من ألوان السِمة.
    val transition = rememberInfiniteTransition(label = "flash")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "t",
    )
    val c1 = MaterialTheme.colorScheme.primary
    val c2 = MaterialTheme.colorScheme.tertiary
    val top by animateColorAsState(lerp(c1, c2, t), label = "top")
    val bottom by animateColorAsState(lerp(c2, c1, t), label = "bottom")

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
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            if (isDhikr) "🕌 أذكارٌ بعد الصلاة" else "🕌 حان الآن وقت الصلاة",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            modifier = Modifier.padding(top = 20.dp),
            textAlign = TextAlign.Center,
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 24.sp,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center,
            )
        }
        Button(
            onClick = onStop,
            modifier = Modifier.padding(top = 44.dp).size(width = 200.dp, height = 60.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = c1),
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null)
            Text("إيقاف", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp))
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
