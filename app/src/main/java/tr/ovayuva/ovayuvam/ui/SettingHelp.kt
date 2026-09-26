package tr.ovayuva.ovayuvam.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tr.ovayuva.ovayuvam.R

@Composable
fun SettingHelp(title: Int, explanation: Int) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.Outlined.Info, stringResource(R.string.about_setting, stringResource(title)))
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(explanation), Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.close)) } })
}
