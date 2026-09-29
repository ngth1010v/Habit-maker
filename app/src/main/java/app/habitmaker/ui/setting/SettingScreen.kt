package app.habitmaker.ui.setting

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import app.habitmaker.R
import app.habitmaker.util.LocalePrefs

private val languages = listOf("", "en", "vi")

@Composable
fun SettingScreen() {
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    val current = remember { LocalePrefs.get(context) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
        Text(
            stringResource(R.string.nav_setting),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        SectionLabel(stringResource(R.string.setting_general))
        SettingRow(R.drawable.ph_translate, stringResource(R.string.setting_language), languageLabel(current)) { picking = true }
        SectionLabel(stringResource(R.string.setting_about))
        SettingRow(R.drawable.ph_info, stringResource(R.string.app_name), stringResource(R.string.setting_version, version), null)
        SettingRow(R.drawable.ph_info, stringResource(R.string.setting_licenses), stringResource(R.string.setting_licenses_body), null)
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
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
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
