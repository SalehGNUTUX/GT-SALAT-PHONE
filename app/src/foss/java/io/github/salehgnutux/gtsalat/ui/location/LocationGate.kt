package io.github.salehgnutux.gtsalat.ui.location

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.salehgnutux.gtsalat.util.Connectivity

/**
 * نكهة foss: يعمل الإجراء فوراً إن كانت خدمة الموقع مفعَّلة، وإلّا تظهر نافذةٌ تنقل إلى صفحة الموقع
 * في النظام، ثمّ يُتابَع الإجراء تلقائيّاً بعد العودة مفعَّلةً. (نافذة التفعيل المباشر تتطلّب خدمات Google.)
 */
@Composable
fun rememberLocationGate(): ((() -> Unit) -> Unit) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPrompt by remember { mutableStateOf(false) }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val action = pending
        pending = null
        if (Connectivity.hasGps(context)) action?.invoke()
    }
    if (showPrompt) {
        AlertDialog(
            onDismissRequest = { showPrompt = false; pending = null },
            title = { Text("خدمة الموقع مطفأة") },
            text = { Text("فعّل خدمة الموقع (GPS) لتحديد موقعك بدقّة، ثمّ عُد إلى التطبيق ليُتابَع التحديث.") },
            confirmButton = {
                Button(onClick = {
                    showPrompt = false
                    runCatching { settings.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                        .onFailure { pending = null }
                }) { Text("تفعيل خدمة الموقع") }
            },
            dismissButton = { TextButton(onClick = { showPrompt = false; pending = null }) { Text("إغلاق") } },
        )
    }
    return remember {
        { action ->
            if (Connectivity.hasGps(context)) action()
            else {
                pending = action
                showPrompt = true
            }
        }
    }
}
