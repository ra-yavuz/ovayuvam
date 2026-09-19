package tr.ovayuva.ovayuvam

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    var summary by remember { mutableStateOf(repository.summary()) }
    var cells by remember { mutableStateOf(repository.recentCells()) }
    var tracking by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val hasLocation = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasLocationPermission(context)
        if (hasLocation) {
            LocationTrailService.start(context)
            tracking = true
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            summary = repository.summary()
            cells = repository.recentCells()
            delay(2_000L)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7F4EC),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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

            WorldCanvas(cells = cells, modifier = Modifier.weight(1f).fillMaxWidth())

            SummaryRow(summary)

            Text(
                text = "Tracking uses a visible notification. Visited cells stay on this device.",
                color = Color(0xFF4B5651),
                style = MaterialTheme.typography.bodyMedium,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (hasLocationPermission(context)) {
                            LocationTrailService.start(context)
                            tracking = true
                        } else {
                            permissionLauncher.launch(requiredPermissions())
                        }
                    },
                ) {
                    Text(if (tracking) "Tracking on" else "Start tracking")
                }
                Button(
                    onClick = {
                        LocationTrailService.stop(context)
                        tracking = false
                    },
                ) {
                    Text("Stop")
                }
            }
        }
    }
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
    Column {
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

private fun hasLocationPermission(context: android.content.Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

