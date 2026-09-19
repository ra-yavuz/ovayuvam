package tr.ovayuva.ovayuvam

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import tr.ovayuva.ovayuvam.domain.VisitedCell
import tr.ovayuva.ovayuvam.domain.WorldCell
import tr.ovayuva.ovayuvam.location.LocationTrailService
import tr.ovayuva.ovayuvam.location.TrackingState
import tr.ovayuva.ovayuvam.storage.VisitRepository
import tr.ovayuva.ovayuvam.ui.theme.Fog
import tr.ovayuva.ovayuvam.ui.theme.Forest
import tr.ovayuva.ovayuvam.ui.theme.Ink
import tr.ovayuva.ovayuvam.ui.theme.OvayuvamTheme
import tr.ovayuva.ovayuvam.ui.theme.Paper
import tr.ovayuva.ovayuvam.ui.theme.PaperDeep
import tr.ovayuva.ovayuvam.ui.theme.Revealed
import tr.ovayuva.ovayuvam.ui.theme.sketchSurface
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val repository by lazy { VisitRepository(this) }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        setContent {
            OvayuvamTheme {
                OvayuvamScreen(repository)
            }
        }
    }
}

@Composable
private fun OvayuvamScreen(repository: VisitRepository) {
    val context = LocalContext.current
    val trackingState = remember { TrackingState(context) }
    var cells by remember { mutableStateOf(repository.recentCells(4_000)) }
    var currentCell by remember { mutableStateOf(trackingState.currentCell()) }
    var tracking by remember { mutableStateOf(trackingState.isTracking()) }
    var status by remember { mutableStateOf("Revealing your world") }
    var infoOpen by remember { mutableStateOf(false) }
    var clearOpen by remember { mutableStateOf(false) }
    var recenterRequest by remember { mutableIntStateOf(0) }
    var following by remember { mutableStateOf(true) }

    fun refresh() {
        cells = repository.recentCells(4_000)
        currentCell = trackingState.currentCell()
        tracking = trackingState.isTracking()
    }

    fun startTracking() {
        when {
            !hasLocationPermission(context) -> status = "Location permission is needed to reveal the map."
            !isDeviceLocationEnabled(context) -> status = "Turn on Android Location to reveal the map."
            else -> {
                LocationTrailService.start(context)
                tracking = true
                status = "Revealing your world"
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val hasLocation = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasLocationPermission(context)
        if (hasLocation) startTracking() else status = "Location permission is needed to reveal the map."
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            startTracking()
        } else {
            permissionLauncher.launch(requiredPermissions())
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(1_500L)
        }
    }

    if (infoOpen) {
        InfoDialog(
            tracking = tracking,
            onDismiss = { infoOpen = false },
            onClear = {
                infoOpen = false
                clearOpen = true
            },
        )
    }

    if (clearOpen) {
        AlertDialog(
            onDismissRequest = { clearOpen = false },
            title = { Text("Clear local world?") },
            text = { Text("This removes every revealed cell stored by ovayuvam on this phone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.clearAll()
                        trackingState.clearCurrentCell()
                        clearOpen = false
                        refresh()
                        status = "Local world cleared"
                    },
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { clearOpen = false }) { Text("Cancel") }
            },
        )
    }

    Box(Modifier.fillMaxSize().background(Paper)) {
        FogWorldMap(
            cells = cells,
            currentCell = currentCell,
            following = following,
            recenterRequest = recenterRequest,
            onUserMovedMap = { following = false },
            modifier = Modifier.fillMaxSize(),
        )

        BrandPill(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 12.dp),
        )

        MapIconButton(
            icon = R.drawable.art_map,
            label = "Info",
            onClick = { infoOpen = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 12.dp, top = 12.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            MapIconButton(
                icon = R.drawable.art_recenter,
                label = "Center map on me",
                enabled = currentCell != null,
                onClick = {
                    following = true
                    recenterRequest += 1
                },
            )
        }

        if (!tracking || currentCell == null || status != "Revealing your world") {
            StatusPill(
                text = if (!tracking) status else "Finding your place",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, bottom = 12.dp, end = 84.dp),
            )
        }
    }
}

@Composable
private fun BrandPill(modifier: Modifier = Modifier) {
    Surface(
        color = Color.Transparent,
        modifier = modifier.sketchSurface(
            fill = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = MaterialTheme.colorScheme.outline,
            seed = 17,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.art_map),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "ovayuvam",
                style = MaterialTheme.typography.titleLarge,
                color = Forest,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StatusPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.Transparent,
        modifier = modifier.sketchSurface(
            fill = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            border = MaterialTheme.colorScheme.outline,
            seed = text.hashCode(),
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Ink,
        )
    }
}

@Composable
private fun MapIconButton(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
        modifier = modifier
            .size(52.dp)
            .semantics { contentDescription = label }
            .sketchSurface(
                fill = MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 0.95f else 0.62f),
                border = MaterialTheme.colorScheme.outline,
                seed = label.hashCode(),
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(icon),
                contentDescription = label,
                modifier = Modifier.size(34.dp),
                alpha = if (enabled) 1f else 0.42f,
            )
        }
    }
}

@Composable
private fun InfoDialog(tracking: Boolean, onDismiss: () -> Unit, onClear: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ovayuvam") },
        text = {
            Column {
                Text("Your own world starts immediately after location permission is granted.")
                Spacer(Modifier.height(10.dp))
                Text("Visited places are stored as local map cells on this phone. The app has no internet permission, no account, no ads, and no backend.")
                Spacer(Modifier.height(10.dp))
                Text(if (tracking) "Tracking is on and shown by a notification." else "Tracking is waiting for location access.")
                Spacer(Modifier.height(10.dp))
                Text("Impressum: Tangelo Bilisim Ltd. Contact: contact@tangelo.com.tr. No warranty is provided.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            TextButton(onClick = onClear) { Text("Clear local world") }
        },
    )
}

@Composable
private fun FogWorldMap(
    cells: List<VisitedCell>,
    currentCell: WorldCell?,
    following: Boolean,
    recenterRequest: Int,
    onUserMovedMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var zoom by remember { mutableStateOf(1.1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val fallbackCenter = remember(cells) {
        if (cells.isEmpty()) {
            WorldCell(0, 0)
        } else {
            WorldCell(
                x = ((cells.minOf { it.x } + cells.maxOf { it.x }) / 2f).roundToInt(),
                y = ((cells.minOf { it.y } + cells.maxOf { it.y }) / 2f).roundToInt(),
            )
        }
    }
    val center = if (following) currentCell ?: fallbackCenter else fallbackCenter

    LaunchedEffect(recenterRequest, currentCell) {
        if (following) {
            pan = Offset.Zero
            zoom = 1.1f
        }
    }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTransformGestures { _, panChange, zoomChange, _ ->
                if (panChange != Offset.Zero || zoomChange != 1f) onUserMovedMap()
                pan += panChange
                zoom = (zoom * zoomChange).coerceIn(0.65f, 4.0f)
            }
        },
    ) {
        drawPaperMap(center, pan, zoom)
        drawRect(Fog)
        drawFogTexture(center, pan, zoom)
        drawRevealedCells(cells, currentCell, center, pan, zoom)
        currentCell?.let { drawCurrentDot(it, center, pan, zoom) }
    }
}

private fun DrawScope.drawPaperMap(center: WorldCell, pan: Offset, zoom: Float) {
    drawRect(Paper)
    val cellPx = cellPixels(zoom)
    val minX = floor((-pan.x - size.width * 0.65f) / cellPx + center.x).toInt()
    val maxX = ceil((-pan.x + size.width * 0.65f) / cellPx + center.x).toInt()
    val minY = floor((pan.y - size.height * 0.65f) / cellPx + center.y).toInt()
    val maxY = ceil((pan.y + size.height * 0.65f) / cellPx + center.y).toInt()

    for (x in minX..maxX step 7) {
        val p1 = cellToScreen(x, minY, center, pan, cellPx)
        val p2 = cellToScreen(x + wobble(x, minY, 2), maxY, center, pan, cellPx)
        drawLine(Color(0xFFBAC6BD).copy(alpha = 0.38f), p1, p2, 2.2f)
    }
    for (y in minY..maxY step 6) {
        val p1 = cellToScreen(minX, y + wobble(minX, y, 3), center, pan, cellPx)
        val p2 = cellToScreen(maxX, y, center, pan, cellPx)
        drawLine(Color(0xFF97B0A1).copy(alpha = 0.28f), p1, p2, 1.7f)
    }
    for (x in minX..maxX step 11) {
        for (y in minY..maxY step 9) {
            if (noise(x, y) > 0.58f) {
                val topLeft = cellToScreen(x, y, center, pan, cellPx)
                drawOval(
                    color = PaperDeep.copy(alpha = 0.32f),
                    topLeft = topLeft,
                    size = Size(cellPx * (2.5f + noise(y, x)), cellPx * (1.5f + noise(x + 4, y))),
                )
            }
        }
    }
}

private fun DrawScope.drawFogTexture(center: WorldCell, pan: Offset, zoom: Float) {
    val cellPx = cellPixels(zoom) * 1.8f
    val startX = floor((-pan.x - size.width) / cellPx).toInt()
    val endX = ceil((-pan.x + size.width) / cellPx).toInt()
    val startY = floor((-pan.y - size.height) / cellPx).toInt()
    val endY = ceil((-pan.y + size.height) / cellPx).toInt()
    for (x in startX..endX) {
        for (y in startY..endY) {
            val left = x * cellPx + pan.x + size.width / 2f + center.x % 5
            val top = y * cellPx + pan.y + size.height / 2f + center.y % 5
            val alpha = 0.10f + noise(x + center.x, y + center.y) * 0.18f
            drawOval(
                color = Color(0xFF0E1714).copy(alpha = alpha),
                topLeft = Offset(left, top),
                size = Size(cellPx * 1.45f, cellPx * 1.1f),
            )
        }
    }
}

private fun DrawScope.drawRevealedCells(
    cells: List<VisitedCell>,
    currentCell: WorldCell?,
    center: WorldCell,
    pan: Offset,
    zoom: Float,
) {
    val cellPx = cellPixels(zoom)
    val revealed = linkedMapOf<Pair<Int, Int>, Int>()
    cells.forEach { revealed[it.x to it.y] = it.samples }
    currentCell?.let { revealed[it.x to it.y] = max(revealed[it.x to it.y] ?: 1, 3) }

    revealed.forEach { entry ->
        val x = entry.key.first
        val y = entry.key.second
        val topLeft = cellToScreen(x, y, center, pan, cellPx) - Offset(cellPx / 2f, cellPx / 2f)
        if (topLeft.x > size.width || topLeft.y > size.height || topLeft.x + cellPx < 0f || topLeft.y + cellPx < 0f) return@forEach
        val strength = (0.78f + entry.value.coerceAtMost(6) * 0.03f).coerceAtMost(0.96f)
        drawCircle(
            color = Revealed.copy(alpha = 0.18f),
            radius = cellPx * 1.2f,
            center = topLeft + Offset(cellPx / 2f, cellPx / 2f),
        )
        drawRoundRect(
            color = Revealed.copy(alpha = strength),
            topLeft = topLeft,
            size = Size(cellPx + 1f, cellPx + 1f),
            cornerRadius = CornerRadius(cellPx * 0.18f, cellPx * 0.18f),
        )
        drawCellInk(topLeft, cellPx, x, y)
    }
}

private fun DrawScope.drawCellInk(topLeft: Offset, cellPx: Float, x: Int, y: Int) {
    val road = Color(0xFF5E7468).copy(alpha = 0.62f)
    val water = Color(0xFF9AD5DA).copy(alpha = 0.34f)
    val path = Path().apply {
        moveTo(topLeft.x, topLeft.y + cellPx * (0.35f + noise(x, y) * 0.3f))
        cubicTo(
            topLeft.x + cellPx * 0.25f,
            topLeft.y + cellPx * noise(y, x),
            topLeft.x + cellPx * 0.72f,
            topLeft.y + cellPx * (0.45f + noise(x + 2, y) * 0.2f),
            topLeft.x + cellPx,
            topLeft.y + cellPx * (0.3f + noise(x, y + 2) * 0.42f),
        )
    }
    drawPath(path, road, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.7f))
    if (noise(x + 9, y - 3) > 0.72f) {
        drawOval(
            color = water,
            topLeft = topLeft + Offset(cellPx * 0.18f, cellPx * 0.18f),
            size = Size(cellPx * 0.56f, cellPx * 0.22f),
        )
    }
}

private fun DrawScope.drawCurrentDot(cell: WorldCell, center: WorldCell, pan: Offset, zoom: Float) {
    val point = cellToScreen(cell.x, cell.y, center, pan, cellPixels(zoom))
    drawCircle(Color(0x552E6FF2), 22.dp.toPx(), point)
    drawCircle(Color.White, 10.dp.toPx(), point)
    drawCircle(Color(0xFF2E6FF2), 6.dp.toPx(), point)
    drawCircle(Color(0xFF3A2E1E), 10.dp.toPx(), point, style = androidx.compose.ui.graphics.drawscope.Stroke(1.4.dp.toPx()))
}

private fun cellPixels(zoom: Float): Float = 22f * zoom

private fun cellToScreen(
    x: Int,
    y: Int,
    center: WorldCell,
    pan: Offset,
    cellPx: Float,
): Offset = Offset(
    x = (x - center.x) * cellPx + pan.x,
    y = (center.y - y) * cellPx + pan.y,
)

private operator fun Offset.plus(size: Size): Offset = Offset(x + size.width, y + size.height)

private fun DrawScope.cellToScreen(
    x: Int,
    y: Int,
    center: WorldCell,
    pan: Offset,
    cellPx: Float,
): Offset = Offset(
    x = size.width / 2f + (x - center.x) * cellPx + pan.x,
    y = size.height / 2f + (center.y - y) * cellPx + pan.y,
)

private fun noise(x: Int, y: Int): Float {
    val mixed = (x * 73856093) xor (y * 19349663)
    return (mixed and 0xFFFF) / 65535f
}

private fun wobble(x: Int, y: Int, salt: Int): Int = ((noise(x + salt, y - salt) - 0.5f) * 4f).roundToInt()

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
