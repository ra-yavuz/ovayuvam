package tr.ovayuva.ovayuvam

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import tr.ovayuva.ovayuvam.domain.VisitedCell
import tr.ovayuva.ovayuvam.domain.WorldSummary
import tr.ovayuva.ovayuvam.location.LocationTrailService
import tr.ovayuva.ovayuvam.location.TrackingState
import tr.ovayuva.ovayuvam.storage.VisitRepository

class MainActivity : ComponentActivity() {
    private val repository by lazy { VisitRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                OvayuvamScreen(repository)
            }
        }
    }
}

@Composable
private fun OvayuvamScreen(repository: VisitRepository) {
    val context = LocalContext.current
    val trackingState = remember { TrackingState(context) }
    var summary by remember { mutableStateOf(repository.summary()) }
    var cells by remember { mutableStateOf(repository.recentCells()) }
    var tracking by remember { mutableStateOf(trackingState.isTracking()) }
    var message by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    fun refresh() {
        summary = repository.summary()
        cells = repository.recentCells()
        tracking = trackingState.isTracking()
    }

    fun startTracking() {
        if (!isDeviceLocationEnabled(context)) {
            message = "Turn on Android Location before starting ovayuvam."
        } else if (hasLocationPermission(context)) {
            LocationTrailService.start(context)
            tracking = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val hasLocation = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasLocationPermission(context)
        if (hasLocation) {
            startTracking()
        } else {
            message = "Location permission is needed before ovayuvam can reveal visited cells."
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            runCatching { context.writeText(uri, repository.exportJson()) }
                .onSuccess { message = "Private world exported." }
                .onFailure { message = "Export failed: ${it.message ?: "unknown error"}" }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val count = repository.replaceFromJson(context.readText(uri))
                refresh()
                count
            }
                .onSuccess { message = "Imported $it visited cells." }
                .onFailure { message = "Import failed: ${it.message ?: "unknown error"}" }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(2_000L)
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Delete local world?") },
            text = { Text("This removes every visited cell stored by ovayuvam on this device. Export first if you want a private copy.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        LocationTrailService.stop(context)
                        repository.clearAll()
                        showClearDialog = false
                        refresh()
                        message = "Local world deleted."
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7F4EC),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header()
            DisclosureCard()
            WorldCanvas(
                cells = cells,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
            )
            SummaryRow(summary)
            Controls(
                tracking = tracking,
                onStart = {
                    if (hasLocationPermission(context)) {
                        startTracking()
                    } else {
                        permissionLauncher.launch(requiredPermissions())
                    }
                },
                onStop = {
                    LocationTrailService.stop(context)
                    tracking = false
                },
            )
            DataControls(
                onExport = { exportLauncher.launch("ovayuvam-world.json") },
                onImport = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                onClear = { showClearDialog = true },
            )
            PrivacyPanel()
            message?.let {
                Text(
                    text = it,
                    color = Color(0xFF285B45),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Header() {
    Column {
        Text(
            text = "ovayuvam",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF18392E),
        )
        Text(
            text = "my own world",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF48695D),
        )
    }
}

@Composable
private fun DisclosureCard() {
    Panel {
        Text(
            text = "Before you start",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF18392E),
        )
        Text(
            text = "ovayuvam records location-derived grid cells only after you start tracking. Tracking uses a visible notification. The app has no internet permission, no account, and no backend.",
            color = Color(0xFF4B5651),
        )
    }
}

@Composable
private fun Controls(tracking: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    Panel {
        Text(
            text = if (tracking) "Tracking is on" else "Tracking is off",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF18392E),
        )
        Text(
            text = if (tracking) {
                "Keep the notification visible while ovayuvam reveals your world."
            } else {
                "Start tracking when you want new places to be revealed."
            },
            color = Color(0xFF4B5651),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onStart, enabled = !tracking) {
                Text("Start")
            }
            OutlinedButton(onClick = onStop, enabled = tracking) {
                Text("Stop")
            }
        }
    }
}

@Composable
private fun DataControls(onExport: () -> Unit, onImport: () -> Unit, onClear: () -> Unit) {
    Panel {
        Text(
            text = "Your data",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF18392E),
        )
        Text(
            text = "Export and import are manual JSON files. Treat them as private location history.",
            color = Color(0xFF4B5651),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Export") }
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Import") }
            OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Delete") }
        }
    }
}

@Composable
private fun PrivacyPanel() {
    Panel {
        Text(
            text = "Privacy",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF18392E),
        )
        Text(
            text = "No route leaves this app in version 1. Android automatic backup is disabled. Future Google Drive backup and friend groups must be explicit opt-ins.",
            color = Color(0xFF4B5651),
        )
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun SummaryRow(summary: WorldSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFFFF), RoundedCornerShape(8.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Stat(label = "Cells", value = summary.cells.toString())
        Stat(label = "Samples", value = summary.samples.toString())
        Stat(label = "Last", value = summary.lastSeenMs?.let { "saved" } ?: "none")
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(modifier = Modifier.widthIn(min = 72.dp)) {
        Text(text = label, color = Color(0xFF6A746F), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(2.dp))
        Text(text = value, color = Color(0xFF18392E), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WorldCanvas(cells: List<VisitedCell>, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .background(Color(0xFF18211E), RoundedCornerShape(8.dp))
            .padding(2.dp),
    ) {
        drawRect(Color(0xFF18211E))
        if (cells.isEmpty()) {
            val step = size.minDimension / 10f
            repeat(11) { index ->
                val p = index * step
                drawLine(Color(0xFF26302C), Offset(p, 0f), Offset(p, size.height), 1f)
                drawLine(Color(0xFF26302C), Offset(0f, p), Offset(size.width, p), 1f)
            }
            return@Canvas
        }

        val minX = cells.minOf { it.x }
        val maxX = cells.maxOf { it.x }
        val minY = cells.minOf { it.y }
        val maxY = cells.maxOf { it.y }
        val spanX = (maxX - minX + 1).coerceAtLeast(1)
        val spanY = (maxY - minY + 1).coerceAtLeast(1)
        val cellSize = minOf(size.width / spanX, size.height / spanY).coerceAtMost(18f)
        val offsetX = (size.width - spanX * cellSize) / 2f
        val offsetY = (size.height - spanY * cellSize) / 2f

        cells.forEach { cell ->
            val left = offsetX + (cell.x - minX) * cellSize
            val top = offsetY + (maxY - cell.y) * cellSize
            val intensity = (0.34f + cell.samples.coerceAtMost(8) * 0.06f).coerceAtMost(0.82f)
            drawRect(
                color = Color(0xFF7FD7A7).copy(alpha = intensity),
                topLeft = Offset(left, top),
                size = Size(cellSize - 1f, cellSize - 1f),
            )
        }
    }
}

private fun requiredPermissions(): Array<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

private fun isDeviceLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(LocationManager::class.java)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        locationManager.isLocationEnabled
    } else {
        runCatching { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false) ||
            runCatching { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
    }
}

private fun Context.writeText(uri: Uri, text: String) {
    contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(text.toByteArray(Charsets.UTF_8))
    } ?: error("Could not open export file")
}

private fun Context.readText(uri: Uri): String {
    return contentResolver.openInputStream(uri)?.use { stream ->
        stream.reader(Charsets.UTF_8).readText()
    } ?: error("Could not open import file")
}
