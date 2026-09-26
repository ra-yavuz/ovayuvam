package tr.ovayuva.ovayuvam.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tr.ovayuva.ovayuvam.BuildConfig
import tr.ovayuva.ovayuvam.R
import tr.ovayuva.ovayuvam.map.FogAppearance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldSettings(tracking: Boolean, trackingEnabled: Boolean, onTrackingChange: (Boolean) -> Unit,
    showHeat: Boolean, onHeatChange: (Boolean) -> Unit, weekly: Boolean, onWeeklyChange: (Boolean) -> Unit,
    fog: Float, onFogChange: (Float) -> Unit, onReplay: () -> Unit, onExport: () -> Unit,
    onImport: () -> Unit, onShare: () -> Unit, onDismiss: () -> Unit) {
    var privacy by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf(false) }
    val context = LocalContext.current
    if (privacy) PrivacyDialog { privacy = false }
    if (language) LanguageDialog { language = false }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.close)) }
            }
            SettingsAction(Icons.Default.Share, stringResource(R.string.share_world), onShare,
                R.string.share_world, R.string.help_share)
            SettingSwitch(R.string.tracking, R.string.help_tracking, trackingEnabled, onTrackingChange)
            Text(stringResource(if (tracking) R.string.tracking_active else if (trackingEnabled) R.string.tracking_waiting else R.string.tracking_paused),
                style = MaterialTheme.typography.bodySmall)
            if (trackingEnabled && !tracking) SettingsAction(Icons.Default.MyLocation, stringResource(R.string.location_settings), {
                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:" + context.packageName)))
            }, R.string.location_settings, R.string.help_tracking)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = onReplay, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
                    Icon(Icons.Default.Replay, null, Modifier.size(24.dp))
                    Text(stringResource(R.string.watch_growth), Modifier.padding(start = 12.dp))
                }
                SettingHelp(R.string.watch_growth, R.string.help_growth)
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(stringResource(R.string.map_appearance), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.fog_opacity), Modifier.weight(1f))
                Text(stringResource(R.string.percent, (fog * 100).toInt()))
                SettingHelp(R.string.fog_opacity, R.string.help_fog)
            }
            val fogLabel = stringResource(R.string.fog_opacity)
            Slider(value = fog, onValueChange = onFogChange, modifier = Modifier.semantics { contentDescription = fogLabel },
                colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant),
                valueRange = FogAppearance.MinimumOpacity..FogAppearance.MaximumOpacity)
            SettingSwitch(R.string.visit_colors, R.string.help_visits, showHeat, onHeatChange)
            SettingSwitch(R.string.weekly_exploration, R.string.help_weekly, weekly, onWeeklyChange)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.your_world), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                SettingHelp(R.string.your_world, R.string.help_world)
            }
            Text(stringResource(R.string.backup_details), style = MaterialTheme.typography.bodySmall)
            SettingsAction(Icons.Default.FileUpload, stringResource(R.string.export_world), onExport, R.string.export_world, R.string.help_export)
            SettingsAction(Icons.Default.FileDownload, stringResource(R.string.import_world), onImport, R.string.import_world, R.string.help_import)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            val selected = AppLanguage.choices[AppLanguage.selected(context)] ?: stringResource(R.string.system_language)
            SettingsAction(Icons.Default.Language, stringResource(R.string.language) + ": " + selected,
                { language = true }, R.string.language, R.string.help_language)
            SettingsAction(Icons.Default.PrivacyTip, stringResource(R.string.privacy), { privacy = true }, R.string.privacy, R.string.help_privacy)
            Text(stringResource(R.string.version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingSwitch(label: Int, help: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    val text = stringResource(label)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f))
        SettingHelp(label, help)
        Switch(checked, onChange, modifier = Modifier.semantics { contentDescription = text },
            colors = SwitchDefaults.colors(uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
private fun SettingsAction(icon: ImageVector, label: String, action: () -> Unit, helpTitle: Int? = null, help: Int? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
    TextButton(onClick = action, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
        Icon(icon, null, Modifier.size(24.dp))
        Text(label, Modifier.weight(1f).padding(start = 16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
    }
    if (helpTitle != null && help != null) SettingHelp(helpTitle, help)
    }
}

@Composable
fun LanguageDialog(onDismiss: () -> Unit) {
    val activity = LocalContext.current as? Activity
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.language)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            (linkedMapOf("" to stringResource(R.string.system_language)) + AppLanguage.choices).forEach { (code, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = activity?.let { AppLanguage.selected(it) } == code,
                        onClick = { onDismiss(); activity?.let { AppLanguage.choose(it, code) } })
                    TextButton(onClick = { onDismiss(); activity?.let { AppLanguage.choose(it, code) } }) { Text(label) }
                }
            }
        } }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } })
}

@Composable
fun TrackingWelcome(onContinue: () -> Unit, onNotNow: () -> Unit) {
    var privacy by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf(false) }
    if (privacy) PrivacyDialog { privacy = false }
    if (language) LanguageDialog { language = false }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.launcher_map_foreground), null, Modifier.size(76.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary))
                IconButton(onClick = { language = true }) { Icon(Icons.Default.Language, stringResource(R.string.language)) }
            }
            Text(stringResource(R.string.welcome), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.tracking_disclosure), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.disclosure_privacy), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { privacy = true }) { Text(stringResource(R.string.privacy)) }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.continue_label)) }
            TextButton(onClick = onNotNow, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.not_now)) }
        }
    }
}

@Composable
fun PrivacyDialog(onDismiss: () -> Unit) {
    val uri = LocalUriHandler.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.privacy), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.close)) }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(stringResource(R.string.privacy_local))
                    Text(stringResource(R.string.privacy_map))
                    Text(stringResource(R.string.privacy_backup))
                    Text(stringResource(R.string.privacy_control))
                    Text(stringResource(R.string.safety))
                    Text(stringResource(R.string.operator))
                    TextButton(onClick = { uri.openUri("https://ovayuva.tr/yuvam/privacy/") }) { Text(stringResource(R.string.full_privacy)) }
                }
            }
        }
    }
}
