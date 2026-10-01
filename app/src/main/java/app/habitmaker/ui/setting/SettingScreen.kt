package app.habitmaker.ui.setting

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import app.habitmaker.R
import app.habitmaker.data.backup.BackupTooNewException
import app.habitmaker.ui.LocalAppContainer
import app.habitmaker.util.LocalePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private val languages = listOf("", "en", "vi")

@Composable
fun SettingScreen() {
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    val current = remember { LocalePrefs.get(context) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    val backup = LocalAppContainer.current.backup
    val scope = rememberCoroutineScope()
    var confirmingImport by remember { mutableStateOf(false) }
    fun toast(message: Int) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/sql")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val saved = runCatching {
                val sql = backup.export()
                withContext(Dispatchers.IO) {
                    checkNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(sql.toByteArray()) }
                }
            }.isSuccess
            toast(if (saved) R.string.setting_export_done else R.string.setting_export_failed)
        }
    }
    // File managers disagree on the type of a .sql file, so any file can be picked; the import checks it.
    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val sql = withContext(Dispatchers.IO) {
                    checkNotNull(context.contentResolver.openInputStream(uri)).use { it.readBytes().decodeToString() }
                }
                backup.import(sql)
            }
            toast(
                when (result.exceptionOrNull()) {
                    null -> R.string.setting_import_done
                    is BackupTooNewException -> R.string.setting_import_too_new
                    else -> R.string.setting_import_invalid
                },
            )
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
        Text(
            stringResource(R.string.nav_setting),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )
        SectionLabel(stringResource(R.string.setting_general))
        SettingRow(R.drawable.ph_translate, stringResource(R.string.setting_language), languageLabel(current)) { picking = true }
        SectionLabel(stringResource(R.string.setting_data))
        SettingRow(R.drawable.ph_upload_simple, stringResource(R.string.setting_export), stringResource(R.string.setting_export_body)) {
            exportFile.launch("habit-maker-backup-${LocalDate.now()}.sql")
        }
        SettingRow(R.drawable.ph_download_simple, stringResource(R.string.setting_import), stringResource(R.string.setting_import_body)) {
            confirmingImport = true
        }
        SectionLabel(stringResource(R.string.setting_about))
        SettingRow(R.drawable.ph_info, stringResource(R.string.app_name), stringResource(R.string.setting_version, version), null)
        SettingRow(R.drawable.ph_info, stringResource(R.string.setting_licenses), stringResource(R.string.setting_licenses_body), null)
    }

    if (confirmingImport) {
        AlertDialog(
            onDismissRequest = { confirmingImport = false },
            title = { Text(stringResource(R.string.setting_import)) },
            text = { Text(stringResource(R.string.setting_import_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingImport = false
                        importFile.launch(arrayOf("*/*"))
                    },
                ) { Text(stringResource(R.string.setting_import_choose)) }
            },
            dismissButton = { TextButton(onClick = { confirmingImport = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    if (picking) {
        LanguageSheet(
            selected = current,
            onSelect = { tag ->
                picking = false
                if (tag != current) {
                    LocalePrefs.set(context, tag)
                    (context as? Activity)?.recreate()
                }
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun languageLabel(tag: String): String = when (tag) {
    "en" -> "English"
    "vi" -> "Tiếng Việt"
    else -> stringResource(R.string.setting_language_system)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSheet(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.setting_language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            languages.forEach { tag ->
                Row(
                    Modifier.fillMaxWidth().clickable { onSelect(tag) }.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = tag == selected, onClick = { onSelect(tag) })
                    Text(languageLabel(tag), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        // Nudged down onto the row's top padding: half the old label-to-row gap.
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp).offset(y = 4.dp),
    )
}

@Composable
private fun SettingRow(icon: Int, title: String, subtitle: String?, onClick: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
