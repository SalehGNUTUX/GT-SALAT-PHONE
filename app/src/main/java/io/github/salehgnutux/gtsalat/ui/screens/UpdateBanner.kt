package io.github.salehgnutux.gtsalat.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.salehgnutux.gtsalat.data.UpdateDownload
import io.github.salehgnutux.gtsalat.data.UpdateInfo
import io.github.salehgnutux.gtsalat.data.UpdateInstaller
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val installer: UpdateInstaller,
) : ViewModel() {
    val download = installer.state
    fun downloadAndInstall(url: String) = viewModelScope.launch { installer.downloadAndInstall(url) }
    fun install(file: File) = installer.install(file)
    fun reset() = installer.reset()
}

/**
 * شريط «تحديثٌ متوفّر» أعلى الرئيسيّة. النقر يفتح نافذة خيارين:
 * «صفحة الإصدارات» (المتصفّح) أو «تنزيل وتثبيت» (داخل التطبيق بشريط تقدّم ثمّ مثبّت النظام).
 */
@Composable
fun UpdateBanner(vm: UpdateViewModel = hiltViewModel()) {
    val update by UpdateInfo.available.collectAsStateWithLifecycle()
    val u = update ?: return
    val dl by vm.download.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    fun openReleases() {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.url))) }
    }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        val onColor = MaterialTheme.colorScheme.onTertiaryContainer
        when (val d = dl) {
            is UpdateDownload.Downloading -> Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("جارٍ تنزيل التحديث… ${d.percent}%", fontWeight = FontWeight.Bold, color = onColor)
                LinearProgressIndicator(progress = { d.percent / 100f }, modifier = Modifier.fillMaxWidth())
            }
            is UpdateDownload.NeedsPermission -> Row(onColor) {
                Text("امنح إذن «تثبيت التطبيقات المجهولة» ثمّ اضغط للتثبيت", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = onColor)
                TextButton(onClick = { vm.install(d.file) }) { Text("تثبيت") }
            }
            is UpdateDownload.Ready -> Row(onColor) {
                Text("التحديث جاهزٌ للتثبيت", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = onColor)
                TextButton(onClick = { vm.install(d.file) }) { Text("تثبيت") }
            }
            is UpdateDownload.Failed -> Row(onColor) {
                Text(d.message, Modifier.weight(1f), color = onColor)
                TextButton(onClick = { vm.reset(); showDialog = true }) { Text("إعادة") }
            }
            else -> Row(onColor, modifier = Modifier.clickable { showDialog = true }) {
                Icon(Icons.Outlined.Download, null, tint = onColor)
                Text("تحديثٌ متوفّر: v${u.version} — اضغط للتحديث", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = onColor)
                IconButton(onClick = { UpdateInfo.dismiss() }) { Icon(Icons.Outlined.Close, "إخفاء", tint = onColor) }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("تحديثٌ متوفّر — v${u.version}") },
            text = { Text(if (u.apkUrl != null) "اختر طريقة التحديث:" else "افتح صفحة الإصدارات لتنزيل الحزمة يدويّاً.") },
            confirmButton = {
                if (u.apkUrl != null) {
                    TextButton(onClick = { showDialog = false; vm.downloadAndInstall(u.apkUrl!!) }) { Text("تنزيل وتثبيت") }
                } else {
                    TextButton(onClick = { showDialog = false; openReleases() }) { Text("صفحة الإصدارات") }
                }
            },
            dismissButton = {
                if (u.apkUrl != null) {
                    TextButton(onClick = { showDialog = false; openReleases() }) { Text("صفحة الإصدارات") }
                } else {
                    TextButton(onClick = { showDialog = false }) { Text("إلغاء") }
                }
            },
        )
    }
}

/** صفٌّ مضغوطٌ موحّدٌ لعناصر الشريط. */
@Composable
private fun Row(onColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
