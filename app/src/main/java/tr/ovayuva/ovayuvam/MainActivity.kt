package tr.ovayuva.ovayuvam

import android.Manifest
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import tr.ovayuva.ovayuvam.domain.GeoPosition
import tr.ovayuva.ovayuvam.domain.VisitedCell
import tr.ovayuva.ovayuvam.domain.WorldCell
import tr.ovayuva.ovayuvam.location.GoalPin
import tr.ovayuva.ovayuvam.location.GoalState
import tr.ovayuva.ovayuvam.location.LocationTrailService
import tr.ovayuva.ovayuvam.location.TrackingState
import tr.ovayuva.ovayuvam.map.BasemapStyle
import tr.ovayuva.ovayuvam.storage.VisitRepository
import tr.ovayuva.ovayuvam.ui.theme.Fog
import tr.ovayuva.ovayuvam.ui.theme.Forest
import tr.ovayuva.ovayuvam.ui.theme.Ink
import tr.ovayuva.ovayuvam.ui.theme.OvayuvamTheme
import tr.ovayuva.ovayuvam.ui.theme.Paper
import tr.ovayuva.ovayuvam.ui.theme.sketchSurface
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

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

private const val InitialZoom = 15.6
private const val RevealRadiusCells = 0.75
private const val RevealRadiusMeters = WorldCell.DefaultCellSizeMeters * RevealRadiusCells
private const val EarthRadiusMeters = 6_378_137.0
private const val GoalArrowMinZoom = 11.0
private const val FamiliarityIslandMaxZoom = 14.35
private const val MaxRenderedVisitedCells = 8_000

private data class RevealMark(
    val cell: WorldCell,
    val point: Offset,
    val radiusPx: Float,
    val samples: Int,
)

private data class RevealCluster(
    val seedX: Int,
    val seedY: Int,
    val point: Offset,
    val radiusPx: Float,
    val samples: Int,
    val count: Int,
)

@Composable
private fun OvayuvamScreen(repository: VisitRepository) {
    val context = LocalContext.current
    val trackingState = remember { TrackingState(context) }
    val goalState = remember { GoalState(context) }
    var cells by remember { mutableStateOf(repository.recentCells(MaxRenderedVisitedCells)) }
    var currentCell by remember { mutableStateOf(trackingState.currentCell()) }
    var currentPosition by remember { mutableStateOf(trackingState.currentPosition()) }
    var goal by remember { mutableStateOf(goalState.goal()) }
    var tracking by remember { mutableStateOf(trackingState.isTracking()) }
    var status by remember { mutableStateOf("Revealing your world") }
    var infoOpen by remember { mutableStateOf(false) }
    var recenterRequest by remember { mutableIntStateOf(0) }
    var following by remember { mutableStateOf(true) }

    fun refresh() {
        cells = repository.recentCells(MaxRenderedVisitedCells)
        currentCell = trackingState.currentCell()
        currentPosition = trackingState.currentPosition()
        goal = goalState.goal()
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
            delay(750L)
        }
    }

    if (infoOpen) {
        InfoDialog(
            tracking = tracking,
            goal = goal,
            onClearGoal = {
                goalState.clearGoal()
                goal = null
            },
            onDismiss = { infoOpen = false },
        )
    }

    Box(Modifier.fillMaxSize().background(Paper)) {
        FogWorldMap(
            cells = cells,
            currentCell = currentCell,
            currentPosition = currentPosition,
            goal = goal,
            following = following,
            recenterRequest = recenterRequest,
            onUserMovedMap = { following = false },
            onGoalSelected = { position ->
                goal = goalState.setGoal(position)
                status = "Goal set"
            },
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

        MapIconButton(
            icon = R.drawable.art_recenter,
            label = "Center map on me",
            enabled = currentPosition != null || currentCell != null,
            onClick = {
                following = true
                recenterRequest += 1
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = 12.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 12.dp, bottom = 12.dp, end = 84.dp),
        ) {
            if (!tracking || currentCell == null || status != "Revealing your world") {
                StatusPill(text = if (!tracking) status else "Finding your place")
                Spacer(Modifier.height(8.dp))
            }
            AttributionPill()
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
                painter = painterResource(R.drawable.launcher_map_foreground),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
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
private fun AttributionPill(modifier: Modifier = Modifier) {
    Surface(
        color = Color.Transparent,
        modifier = modifier.sketchSurface(
            fill = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
            border = MaterialTheme.colorScheme.outline.copy(alpha = 0.52f),
            seed = 302,
        ),
    ) {
        Text(
            text = BasemapStyle.Attribution,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
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
private fun InfoDialog(
    tracking: Boolean,
    goal: GoalPin?,
    onClearGoal: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ovayuvam") },
        text = {
            Column {
                Text("Your own world starts immediately after location permission is granted.")
                Spacer(Modifier.height(10.dp))
                Text("Long-press the map to place a private goal pin. If the pin is off screen, the arrow points toward it.")
                Spacer(Modifier.height(10.dp))
                Text("The visible map uses OpenFreeMap tiles. Your revealed world is stored as local cells on this phone. There is no ovayuva account, no ads, and no ovayuva backend.")
                Spacer(Modifier.height(10.dp))
                Text(if (tracking) "Tracking is on and shown by a notification." else "Tracking is waiting for location access.")
                Spacer(Modifier.height(10.dp))
                Text("Privacy policy: https://ovayuva.tr/yuvam/privacy/")
                Spacer(Modifier.height(10.dp))
                Text("Map data: ${BasemapStyle.Attribution}. Impressum: Tangelo Bilisim Ltd. Contact: contact@tangelo.com.tr. No warranty is provided.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = if (goal != null) {
            {
                TextButton(
                    onClick = {
                        onClearGoal()
                        onDismiss()
                    },
                ) { Text("Clear goal") }
            }
        } else {
            null
        },
    )
}

@Composable
private fun FogWorldMap(
    cells: List<VisitedCell>,
    currentCell: WorldCell?,
    currentPosition: GeoPosition?,
    goal: GoalPin?,
    following: Boolean,
    recenterRequest: Int,
    onUserMovedMap: () -> Unit,
    onGoalSelected: (GeoPosition) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val initialPosition = remember(cells, currentCell, currentPosition) {
        currentPosition ?: currentCell?.centerPosition() ?: fallbackPosition(cells)
    }
    var activeMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var cameraTick by remember { mutableIntStateOf(0) }
    val latestGoalSelected by rememberUpdatedState(onGoalSelected)

    LaunchedEffect(activeMap, following, currentPosition, currentCell, recenterRequest) {
        val map = activeMap ?: return@LaunchedEffect
        val target = currentPosition ?: currentCell?.centerPosition() ?: return@LaunchedEffect
        if (following) {
            val zoom = max(map.cameraPosition.zoom, InitialZoom)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(target.toLatLng(), zoom), 650)
        }
    }

    Box(modifier) {
        val mapView = remember {
            MapLibre.getInstance(context)
            MapView(
                context,
                MapLibreMapOptions.createFromAttributes(context)
                    .textureMode(true)
                    .foregroundLoadColor(android.graphics.Color.rgb(243, 244, 237)),
            ).apply {
                setBackgroundColor(android.graphics.Color.rgb(243, 244, 237))
                onCreate(null)
                setMaximumFps(30)
                getMapAsync { map ->
                    map.uiSettings.apply {
                        isScrollGesturesEnabled = true
                        isZoomGesturesEnabled = true
                        isDoubleTapGesturesEnabled = true
                        isRotateGesturesEnabled = false
                        isTiltGesturesEnabled = false
                        isCompassEnabled = false
                        isLogoEnabled = false
                        isAttributionEnabled = false
                    }
                    map.setMinZoomPreference(0.0)
                    map.setMaxZoomPreference(22.0)
                    map.cameraPosition = CameraPosition.Builder()
                        .target(initialPosition.toLatLng())
                        .zoom(InitialZoom)
                        .build()
                    map.addOnCameraMoveStartedListener { reason ->
                        if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                            onUserMovedMap()
                        }
                    }
                    map.addOnCameraMoveListener { cameraTick += 1 }
                    map.addOnCameraIdleListener { cameraTick += 1 }
                    map.addOnMapLongClickListener { latLng ->
                        latestGoalSelected(GeoPosition(latitude = latLng.latitude, longitude = latLng.longitude))
                        true
                    }
                    map.setStyle(Style.Builder().fromJson(BasemapStyle.json())) {
                        cameraTick += 1
                    }
                    activeMap = map
                }
            }
        }

        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        FogRevealOverlay(
            cells = cells,
            currentCell = currentCell,
            currentPosition = currentPosition,
            goal = goal,
            map = activeMap,
            cameraTick = cameraTick,
            modifier = Modifier.fillMaxSize(),
        )

        DisposableEffect(mapView, lifecycle) {
            var started = false
            var resumed = false
            fun release() {
                if (!mapView.isDestroyed) {
                    if (resumed) mapView.onPause()
                    if (started) mapView.onStop()
                    resumed = false
                    started = false
                    activeMap = null
                    mapView.onDestroy()
                }
            }
            val observer = LifecycleEventObserver { _, event ->
                if (!mapView.isDestroyed) {
                    when (event) {
                        Lifecycle.Event.ON_START -> {
                            mapView.onStart()
                            started = true
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            mapView.onResume()
                            resumed = true
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            mapView.onPause()
                            resumed = false
                        }
                        Lifecycle.Event.ON_STOP -> {
                            mapView.onStop()
                            started = false
                        }
                        Lifecycle.Event.ON_DESTROY -> release()
                        else -> Unit
                    }
                }
            }
            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            val memory = object : ComponentCallbacks2 {
                override fun onConfigurationChanged(newConfig: Configuration) = Unit
                override fun onLowMemory() {
                    if (!mapView.isDestroyed) mapView.onLowMemory()
                }
                override fun onTrimMemory(level: Int) {
                    if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) onLowMemory()
                }
            }
            context.applicationContext.registerComponentCallbacks(memory)
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                context.applicationContext.unregisterComponentCallbacks(memory)
                release()
            }
        }
    }
}

@Composable
private fun FogRevealOverlay(
    cells: List<VisitedCell>,
    currentCell: WorldCell?,
    currentPosition: GeoPosition?,
    goal: GoalPin?,
    map: MapLibreMap?,
    cameraTick: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        cameraTick
        drawRect(Fog)
        if (map != null) {
            drawRevealedPlaces(map, cells, currentCell)
            currentPosition?.let { drawCurrentDot(map, it) }
            goal?.let { drawGoal(map, it) }
        }
    }
}

private fun DrawScope.drawRevealedPlaces(
    map: MapLibreMap,
    cells: List<VisitedCell>,
    currentCell: WorldCell?,
) {
    val revealed = linkedMapOf<Pair<Int, Int>, Int>()
    cells.forEach { revealed[it.x to it.y] = it.samples }
    currentCell?.let { revealed[it.x to it.y] = max(revealed[it.x to it.y] ?: 1, 3) }
    val canvasWidth = size.width
    val canvasHeight = size.height
    val visible = buildList {
        revealed.forEach { entry ->
            val cell = WorldCell(entry.key.first, entry.key.second)
            val position = cell.centerPosition()
            val point = map.projection.toScreenLocation(position.toLatLng())
            val center = Offset(point.x, point.y)
            val radius = revealRadiusPixels(map, position)
            if (
                center.x + radius >= 0f &&
                center.y + radius >= 0f &&
                center.x - radius <= canvasWidth &&
                center.y - radius <= canvasHeight
            ) {
                add(RevealMark(cell, center, radius, entry.value))
            }
        }
    }
    if (map.cameraPosition.zoom < FamiliarityIslandMaxZoom) {
        drawFamiliarityIslands(visible, map.cameraPosition.zoom)
    } else {
        visible.forEach(::drawRevealBrush)
    }
}

private fun DrawScope.drawFamiliarityIslands(
    marks: List<RevealMark>,
    zoom: Double,
) {
    if (marks.isEmpty()) return
    val bucketSize = familiarityBucketSize(zoom)
    val clusters = marks
        .groupBy { mark -> floorDiv(mark.cell.x, bucketSize) to floorDiv(mark.cell.y, bucketSize) }
        .values
        .map { group ->
            var weightedX = 0f
            var weightedY = 0f
            var totalWeight = 0f
            var samples = 0
            var maxRadius = 0f
            group.forEach { mark ->
                val weight = mark.samples.coerceAtLeast(1).toFloat()
                weightedX += mark.point.x * weight
                weightedY += mark.point.y * weight
                totalWeight += weight
                samples += mark.samples
                maxRadius = max(maxRadius, mark.radiusPx)
            }
            val count = group.size
            val zoomSpread = (FamiliarityIslandMaxZoom - zoom).toFloat().coerceIn(0f, 5f)
            val densityBoost = sqrt(count.toFloat()).coerceAtMost(12f)
            val radius = (maxRadius * (bucketSize * 0.7f + densityBoost * 1.15f) * (1f + zoomSpread * 0.18f))
                .coerceAtLeast((24f + zoomSpread * 6f).dp.toPx())
                .coerceAtMost(max(size.width, size.height) * 0.34f)
            RevealCluster(
                seedX = group.first().cell.x,
                seedY = group.first().cell.y,
                point = Offset(weightedX / totalWeight, weightedY / totalWeight),
                radiusPx = radius,
                samples = samples,
                count = count,
            )
        }
        .sortedBy { it.samples }

    clusters.forEach(::drawFamiliarityIsland)
}

private fun DrawScope.drawFamiliarityIsland(cluster: RevealCluster) {
    val radius = cluster.radiusPx
    val sampleBoost = sqrt(cluster.samples.coerceAtLeast(1).toFloat())
    val strength = (0.52f + sampleBoost * 0.035f + cluster.count * 0.012f).coerceIn(0.58f, 0.9f)
    val offsets = listOf(
        Offset(-0.26f, -0.10f),
        Offset(0.24f, -0.20f),
        Offset(-0.16f, 0.26f),
        Offset(0.28f, 0.16f),
        Offset(0.02f, -0.30f),
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.24f),
        radius = radius * 1.55f,
        center = cluster.point,
        blendMode = BlendMode.DstOut,
    )
    offsets.forEachIndexed { index, offset ->
        val roughRadius = radius * (0.66f + noise(cluster.seedX + index * 17, cluster.seedY - index * 13) * 0.28f)
        drawCircle(
            color = Color.Black.copy(alpha = 0.28f),
            radius = roughRadius,
            center = cluster.point + Offset(offset.x * radius, offset.y * radius),
            blendMode = BlendMode.DstOut,
        )
    }
    drawCircle(
        color = Color.Black.copy(alpha = strength),
        radius = radius * 0.92f,
        center = cluster.point,
        blendMode = BlendMode.DstOut,
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.76f),
        radius = radius * 0.52f,
        center = cluster.point,
        blendMode = BlendMode.DstOut,
    )
}

private fun DrawScope.drawRevealBrush(mark: RevealMark) {
    val x = mark.cell.x
    val y = mark.cell.y
    val radius = mark.radiusPx
    val strength = (0.64f + mark.samples.coerceAtMost(8) * 0.035f).coerceAtMost(0.92f)
    val offsets = listOf(
        Offset(-0.32f, -0.08f),
        Offset(0.28f, -0.18f),
        Offset(-0.18f, 0.28f),
        Offset(0.34f, 0.18f),
        Offset(0.04f, -0.36f),
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.32f),
        radius = radius * 1.2f,
        center = mark.point,
        blendMode = BlendMode.DstOut,
    )
    offsets.forEachIndexed { index, offset ->
        val roughRadius = radius * (0.58f + noise(x + index * 11, y - index * 7) * 0.28f)
        drawCircle(
            color = Color.Black.copy(alpha = 0.34f),
            radius = roughRadius,
            center = mark.point + Offset(offset.x * radius, offset.y * radius),
            blendMode = BlendMode.DstOut,
        )
    }
    drawCircle(
        color = Color.Black.copy(alpha = strength),
        radius = radius * 0.86f,
        center = mark.point,
        blendMode = BlendMode.DstOut,
    )
    drawCircle(
        color = Color.Black,
        radius = radius * 0.56f,
        center = mark.point,
        blendMode = BlendMode.Clear,
    )
}

private fun DrawScope.drawCurrentDot(map: MapLibreMap, position: GeoPosition) {
    val screen = map.projection.toScreenLocation(position.toLatLng())
    val point = Offset(screen.x, screen.y)
    drawCircle(Color(0x552E6FF2), 22.dp.toPx(), point)
    drawCircle(Color.White, 10.dp.toPx(), point)
    drawCircle(Color(0xFF2E6FF2), 6.dp.toPx(), point)
    drawCircle(Color(0xFF3A2E1E), 10.dp.toPx(), point, style = Stroke(1.4.dp.toPx()))
}

private fun DrawScope.drawGoal(map: MapLibreMap, goal: GoalPin) {
    val screen = map.projection.toScreenLocation(goal.position.toLatLng())
    val point = Offset(screen.x, screen.y)
    val visible = point.x in 0f..size.width && point.y in 0f..size.height
    if (visible) {
        drawGoalPin(point)
    } else if (map.cameraPosition.zoom >= GoalArrowMinZoom) {
        drawGoalArrow(point)
    }
}

private fun DrawScope.drawGoalPin(point: Offset) {
    val stemTop = point + Offset(0f, (-28).dp.toPx())
    val stemBottom = point + Offset(0f, 18.dp.toPx())
    drawLine(
        color = Color(0xFF422418),
        start = stemTop,
        end = stemBottom,
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawCircle(Color(0x55F8D85A), 30.dp.toPx(), stemTop)
    drawCircle(Color(0xFFE84A5F), 15.dp.toPx(), stemTop)
    drawCircle(Color(0xFFFDF5DB), 7.dp.toPx(), stemTop)
    drawCircle(Color(0xFF422418), 16.dp.toPx(), stemTop, style = Stroke(2.dp.toPx()))
}

private fun DrawScope.drawGoalArrow(offscreenPoint: Offset) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val vector = offscreenPoint - center
    val length = hypot(vector.x.toDouble(), vector.y.toDouble()).toFloat().coerceAtLeast(1f)
    val unit = Offset(vector.x / length, vector.y / length)
    val margin = 76.dp.toPx()
    val limitX = (size.width / 2f - margin).coerceAtLeast(1f)
    val limitY = (size.height / 2f - margin).coerceAtLeast(1f)
    val scaleX = if (abs(unit.x) > 0.001f) limitX / abs(unit.x) else Float.POSITIVE_INFINITY
    val scaleY = if (abs(unit.y) > 0.001f) limitY / abs(unit.y) else Float.POSITIVE_INFINITY
    val tip = center + unit * min(scaleX, scaleY)
    val base = tip - unit * 38.dp.toPx()
    val side = Offset(-unit.y, unit.x) * 16.dp.toPx()
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo((base + side).x, (base + side).y)
        lineTo((base - side).x, (base - side).y)
        close()
    }
    drawCircle(Color(0xCCFDF5DB), 34.dp.toPx(), tip - unit * 18.dp.toPx())
    drawPath(path, Color(0xFFE84A5F))
    drawPath(path, Color(0xFF422418), style = Stroke(2.dp.toPx()))
}

private fun familiarityBucketSize(zoom: Double): Int = when {
    zoom < 10.5 -> 18
    zoom < 12.0 -> 12
    zoom < 13.25 -> 7
    else -> 4
}

private fun floorDiv(value: Int, divisor: Int): Int = Math.floorDiv(value, divisor)

private fun revealRadiusPixels(map: MapLibreMap, position: GeoPosition): Float {
    val center = map.projection.toScreenLocation(position.toLatLng())
    val edge = map.projection.toScreenLocation(position.offsetEast(RevealRadiusMeters).toLatLng())
    return hypot((edge.x - center.x).toDouble(), (edge.y - center.y).toDouble())
        .toFloat()
        .coerceAtLeast(1f)
}

private fun fallbackPosition(cells: List<VisitedCell>): GeoPosition {
    if (cells.isEmpty()) return GeoPosition(latitude = 41.0082, longitude = 28.9784)
    return WorldCell(
        x = ((cells.minOf { it.x } + cells.maxOf { it.x }) / 2f).roundToInt(),
        y = ((cells.minOf { it.y } + cells.maxOf { it.y }) / 2f).roundToInt(),
    ).centerPosition()
}

private fun GeoPosition.toLatLng(): LatLng = LatLng(latitude, longitude)

private fun GeoPosition.offsetEast(meters: Double): GeoPosition {
    val metersPerDegree = max(1.0, (PI / 180.0) * EarthRadiusMeters * cos(latitude.toRadians()))
    return copy(longitude = (longitude + meters / metersPerDegree).coerceIn(-180.0, 180.0))
}

private fun noise(x: Int, y: Int): Float {
    val mixed = (x * 73856093) xor (y * 19349663)
    return (mixed and 0xFFFF) / 65535f
}

private fun Double.toRadians(): Double = this * PI / 180.0

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
