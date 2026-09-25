package tr.ovayuva.ovayuvam.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tr.ovayuva.ovayuvam.backup.BackupFiles
import tr.ovayuva.ovayuvam.backup.WorldBackup
import tr.ovayuva.ovayuvam.backup.WorldBackupCodec
import tr.ovayuva.ovayuvam.location.GoalState
import tr.ovayuva.ovayuvam.storage.VisitRepository

@Composable
fun BackupDocumentDialog(
    uri: Uri,
    importing: Boolean,
    repository: VisitRepository,
    onImported: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    // Secrets stay in memory. A recreated activity asks again after restoring the selected URI.
    var passphrase by remember(uri) { mutableStateOf("") }
    var confirmation by remember(uri) { mutableStateOf("") }
    var showPassphrase by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember(uri) { mutableStateOf<String?>(null) }
    var result by remember(uri) { mutableStateOf<String?>(null) }
    val valid = passphrase.length >= WorldBackupCodec.MinPassphraseLength && (importing || passphrase == confirmation)
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
        title = { Text(if (importing) "Import world" else "Export world") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (result != null) {
                    Text(result!!)
                } else {
                    Text(if (importing) "Enter the passphrase used when this backup was exported. Your existing discoveries will be kept."
                        else "Choose a passphrase for this backup. Keep it somewhere safe; it cannot be recovered.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it; error = null },
                        enabled = !busy,
                        label = { Text("Backup passphrase") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                        supportingText = { Text("At least ${WorldBackupCodec.MinPassphraseLength} characters") },
                        visualTransformation = if (showPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                    )
                    if (!importing) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmation,
                            onValueChange = { confirmation = it },
                            enabled = !busy,
                            label = { Text("Confirm passphrase") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                            visualTransformation = if (showPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                        )
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = showPassphrase, onCheckedChange = { showPassphrase = it }, enabled = !busy)
                        Text("Show passphrase")
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (busy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text(if (importing) "Importing your world..." else "Saving your encrypted world...")
                    }
                }
            }
        },
        confirmButton = {
            if (result != null) TextButton(onClick = onDismiss) { Text("Done") }
            else TextButton(enabled = valid && !busy, onClick = {
                busy = true
                error = null
                val secret = passphrase
                scope.launch {
                    try {
                        val message = withContext(Dispatchers.IO) {
                            val resolver = context.contentResolver
                            val goals = GoalState(context)
                            if (importing) {
                                val backup = resolver.openInputStream(uri)?.use {
                                    WorldBackupCodec.decrypt(BackupFiles.read(it), secret)
                                } ?: throw java.io.FileNotFoundException()
                                repository.importCells(backup.visitedCells, backup.revealCells, backup.visitCounts)
                                backup.goal?.let { goals.setGoal(it.position, it.createdMs) }
                                "World imported: ${backup.visitedCells.size} visited cells, ${backup.revealCells.size} reveal cells, and ${backup.visitCounts.size} visit records. Existing discoveries were kept."
                            } else {
                                val backup = WorldBackup(repository.allVisitedCells(), repository.allRevealCells(),
                                    goals.goal(), visitCounts = repository.allVisitCounts())
                                val bytes = WorldBackupCodec.encrypt(backup, secret)
                                resolver.openOutputStream(uri, "wt")?.use { it.write(bytes); it.flush() }
                                    ?: throw java.io.FileNotFoundException()
                                "Encrypted world saved. Keep this file and its passphrase before reinstalling."
                            }
                        }
                        if (importing) onImported()
                        passphrase = ""
                        confirmation = ""
                        result = message
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        error = BackupFiles.errorMessage(failure)
                    } finally {
                        busy = false
                    }
                }
            }) { Text(if (importing) "Import" else "Save backup") }
        },
        dismissButton = {
            if (result == null) TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") }
        },
    )
}
