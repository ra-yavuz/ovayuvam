package tr.ovayuva.ovayuvam

import android.Manifest
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.RectF
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
import org.maplibre.geojson.Geometry
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiLineString
import org.maplibre.geojson.Point
import tr.ovayuva.ovayuvam.domain.GeoPosition
import tr.ovayuva.ovayuvam.domain.RevealCell
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
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
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

private const val InitialZoom = 15.6
private const val CoreRevealRadiusMeters = 20.0
private const val RoadRevealRadiusMeters = 11.0
private const val LegacyRevealRadiusMeters = 20.0
private const val RoadRevealReachMeters = 32.0
private const val RoadRevealMinMovementMeters = 6.0
private const val RoadQueryRadiusMeters = 38.0
private const val RoadSampleStepMeters = 8.0
private const val MaxRoadRevealCellsPerQuery = 180
private const val EarthRadiusMeters = 6_378_137.0
private const val GoalArrowMinZoom = 11.0
private val RoadLayerIds = arrayOf("street-paper", "paths")

private data class RevealMark(
    val seedX: Int,
    val seedY: Int,
    val point: Offset,
    val radiusPx: Float,
    val samples: Int,
    val kind: RevealCell.Kind,
)

@Composable
private fun OvayuvamScreen(repository: VisitRepository) {
    val context = LocalContext.current
    val trackingState = remember { TrackingState(context) }
    val goalState = remember { GoalState(context) }
    var cells by remember { mutableStateOf(repository.recentCells(4_000)) }
    var revealCells by remember { mutableStateOf(repository.recentRevealCells(8_000)) }
    var currentCell by remember { mutableStateOf(trackingState.currentCell()) }
    var currentPosition by remember { mutableStateOf(trackingState.currentPosition()) }
    var goal by remember { mutableStateOf(goalState.goal()) }
    var tracking by remember { mutableStateOf(trackingState.isTracking()) }
    var status by remember { mutableStateOf("Revealing your world") }
    var infoOpen by remember { mutableStateOf(false) }
    var recenterRequest by remember { mutableIntStateOf(0) }
    var following by remember { mutableStateOf(true) }

    fun refresh() {
        cells = repository.recentCells(4_000)
        revealCells = repository.recentRevealCells(8_000)
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
            revealCells = revealCells,
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
            onRoadCellsObserved = { cells ->
                repository.recordRevealCells(cells, RevealCell.Kind.Road)
                revealCells = repository.recentRevealCells(8_000)
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
            fill = Ink.copy(alpha = 0.82f),
            border = Color.White.copy(alpha = 0.42f),
            seed = 302,
        ),
    ) {
        Text(
            text = BasemapStyle.Attribution,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
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
    revealCells: List<RevealCell>,
    currentCell: WorldCell?,
    currentPosition: GeoPosition?,
    goal: GoalPin?,
    following: Boolean,
    recenterRequest: Int,
    onUserMovedMap: () -> Unit,
    onGoalSelected: (GeoPosition) -> Unit,
    onRoadCellsObserved: (Set<WorldCell>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val initialPosition = remember(cells, currentCell, currentPosition) {
        currentPosition ?: currentCell?.centerPosition() ?: fallbackPosition(cells)
    }
    var activeMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var cameraTick by remember { mutableIntStateOf(0) }
    var mapResumed by remember {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var lastRoadRevealPosition by remember { mutableStateOf<GeoPosition?>(null) }
    val latestGoalSelected by rememberUpdatedState(onGoalSelected)

    LaunchedEffect(activeMap, following, currentPosition, currentCell, recenterRequest) {
        val map = activeMap ?: return@LaunchedEffect
        val target = currentPosition ?: currentCell?.centerPosition() ?: return@LaunchedEffect
        if (following) {
            val zoom = max(map.cameraPosition.zoom, InitialZoom)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(target.toLatLng(), zoom), 650)
        }
    }

    LaunchedEffect(activeMap, currentPosition, mapResumed) {
        val map = activeMap ?: return@LaunchedEffect
        val position = currentPosition ?: return@LaunchedEffect
        if (!mapResumed) {
            lastRoadRevealPosition = position
            return@LaunchedEffect
        }
        val previous = lastRoadRevealPosition
        if (previous == null) {
            lastRoadRevealPosition = position
            return@LaunchedEffect
        }
        if (previous.distanceMetersTo(position) < RoadRevealMinMovementMeters) {
            return@LaunchedEffect
        }
        val nearbyRoadCells = map.roadRevealCellsNear(position)
        lastRoadRevealPosition = position
        if (nearbyRoadCells.isNotEmpty()) {
            onRoadCellsObserved(nearbyRoadCells)
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
            revealCells = revealCells,
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
                            mapResumed = true
                            resumed = true
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            mapResumed = false
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
    revealCells: List<RevealCell>,
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
            drawRevealedPlaces(map, cells, revealCells, currentCell, currentPosition)
            currentPosition?.let { drawCurrentDot(map, it) }
            goal?.let { drawGoal(map, it) }
        }
    }
}

private fun DrawScope.drawRevealedPlaces(
    map: MapLibreMap,
    cells: List<VisitedCell>,
    revealCells: List<RevealCell>,
    currentCell: WorldCell?,
    currentPosition: GeoPosition?,
) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val visible = buildList {
        revealCells.forEach { cell ->
            val position = WorldCell(cell.x, cell.y).centerPosition(WorldCell.RevealCellSizeMeters)
            val point = map.projection.toScreenLocation(position.toLatLng())
            val center = Offset(point.x, point.y)
            val meters = if (cell.kind == RevealCell.Kind.Road) RoadRevealRadiusMeters else CoreRevealRadiusMeters
            val radius = radiusPixels(map, position, meters)
            if (isVisible(center, radius, canvasWidth, canvasHeight)) {
                add(RevealMark(cell.x, cell.y, center, radius, cell.samples, cell.kind))
            }
        }
        if (revealCells.isEmpty()) {
            cells.forEach { cell ->
                val position = WorldCell(cell.x, cell.y).centerPosition()
                val point = map.projection.toScreenLocation(position.toLatLng())
                val center = Offset(point.x, point.y)
                val radius = radiusPixels(map, position, LegacyRevealRadiusMeters)
                if (isVisible(center, radius, canvasWidth, canvasHeight)) {
                    add(RevealMark(cell.x, cell.y, center, radius, cell.samples, RevealCell.Kind.Core))
                }
            }
        }
        currentPosition?.let { position ->
            val point = map.projection.toScreenLocation(position.toLatLng())
            val center = Offset(point.x, point.y)
            val radius = radiusPixels(map, position, CoreRevealRadiusMeters)
            if (isVisible(center, radius, canvasWidth, canvasHeight)) {
                add(RevealMark(0, 0, center, radius, 8, RevealCell.Kind.Core))
            }
        } ?: currentCell?.let { cell ->
            val position = cell.centerPosition()
            val point = map.projection.toScreenLocation(position.toLatLng())
            val center = Offset(point.x, point.y)
            val radius = radiusPixels(map, position, LegacyRevealRadiusMeters)
            if (isVisible(center, radius, canvasWidth, canvasHeight)) {
                add(RevealMark(cell.x, cell.y, center, radius, 3, RevealCell.Kind.Core))
            }
        }
    }
    visible.forEach(::drawRevealBrush)
}

private fun DrawScope.drawRevealBrush(mark: RevealMark) {
    val x = mark.seedX
    val y = mark.seedY
    val radius = mark.radiusPx
    val road = mark.kind == RevealCell.Kind.Road
    val strength = if (road) {
        (0.48f + mark.samples.coerceAtMost(6) * 0.035f).coerceAtMost(0.72f)
    } else {
        (0.7f + mark.samples.coerceAtMost(8) * 0.035f).coerceAtMost(0.95f)
    }
    val offsets = listOf(
        Offset(-0.32f, -0.08f),
        Offset(0.28f, -0.18f),
        Offset(-0.18f, 0.28f),
        Offset(0.34f, 0.18f),
        Offset(0.04f, -0.36f),
    )
    drawCircle(
        color = Color.Black.copy(alpha = if (road) 0.18f else 0.3f),
        radius = radius * if (road) 1.05f else 1.18f,
        center = mark.point,
        blendMode = BlendMode.DstOut,
    )
    offsets.forEachIndexed { index, offset ->
        val roughRadius = radius * (if (road) 0.36f else 0.58f + noise(x + index * 11, y - index * 7) * 0.28f)
        drawCircle(
            color = Color.Black.copy(alpha = if (road) 0.14f else 0.34f),
            radius = roughRadius,
            center = mark.point + Offset(offset.x * radius, offset.y * radius),
            blendMode = BlendMode.DstOut,
        )
    }
    drawCircle(
        color = Color.Black.copy(alpha = strength),
        radius = radius * if (road) 0.82f else 0.86f,
        center = mark.point,
        blendMode = BlendMode.DstOut,
    )
    drawCircle(
        color = Color.Black,
        radius = radius * if (road) 0.28f else 0.56f,
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

private fun isVisible(center: Offset, radius: Float, canvasWidth: Float, canvasHeight: Float): Boolean =
    center.x + radius >= 0f &&
        center.y + radius >= 0f &&
        center.x - radius <= canvasWidth &&
        center.y - radius <= canvasHeight

private fun radiusPixels(map: MapLibreMap, position: GeoPosition, meters: Double): Float {
    val center = map.projection.toScreenLocation(position.toLatLng())
    val edge = map.projection.toScreenLocation(position.offsetEast(meters).toLatLng())
    return hypot((edge.x - center.x).toDouble(), (edge.y - center.y).toDouble())
        .toFloat()
        .coerceAtLeast(1f)
}

private fun MapLibreMap.roadRevealCellsNear(position: GeoPosition): Set<WorldCell> {
    val screen = projection.toScreenLocation(position.toLatLng())
    val queryRadius = radiusPixelsForQuery(this, position, RoadQueryRadiusMeters).coerceAtLeast(28f)
    val queryBox = RectF(
        screen.x - queryRadius,
        screen.y - queryRadius,
        screen.x + queryRadius,
        screen.y + queryRadius,
    )
    val features = runCatching { queryRenderedFeatures(queryBox, *RoadLayerIds) }.getOrDefault(emptyList())
    val cells = LinkedHashSet<WorldCell>()
    for (feature in features) {
        for (line in feature.geometry().linePointLists()) {
            collectRoadRevealCells(line, position, cells)
            if (cells.size >= MaxRoadRevealCellsPerQuery) return cells
        }
    }
    return cells
}

private fun radiusPixelsForQuery(map: MapLibreMap, position: GeoPosition, meters: Double): Float {
    val center = map.projection.toScreenLocation(position.toLatLng())
    val edge = map.projection.toScreenLocation(position.offsetEast(meters).toLatLng())
    return hypot((edge.x - center.x).toDouble(), (edge.y - center.y).toDouble()).toFloat()
}

private fun Geometry?.linePointLists(): List<List<Point>> = when (this) {
    is LineString -> listOf(coordinates())
    is MultiLineString -> coordinates()
    else -> emptyList()
}

private fun collectRoadRevealCells(
    points: List<Point>,
    center: GeoPosition,
    cells: MutableSet<WorldCell>,
) {
    if (points.size < 2) return
    for (index in 0 until points.lastIndex) {
        val start = points[index].toGeoPosition()
        val end = points[index + 1].toGeoPosition()
        val segmentMeters = start.distanceMetersTo(end)
        val steps = ceil(segmentMeters / RoadSampleStepMeters).toInt().coerceIn(1, 32)
        for (step in 0..steps) {
            val fraction = step.toDouble() / steps
            val sample = start.interpolate(end, fraction)
            if (sample.distanceMetersTo(center) <= RoadRevealReachMeters) {
                cells += WorldCell.fromLocation(
                    sample.latitude,
                    sample.longitude,
                    WorldCell.RevealCellSizeMeters,
                )
                if (cells.size >= MaxRoadRevealCellsPerQuery) return
            }
        }
    }
}

private fun Point.toGeoPosition(): GeoPosition = GeoPosition(latitude = latitude(), longitude = longitude())

private fun GeoPosition.interpolate(other: GeoPosition, fraction: Double): GeoPosition =
    GeoPosition(
        latitude = latitude + (other.latitude - latitude) * fraction,
        longitude = longitude + (other.longitude - longitude) * fraction,
    )

private fun GeoPosition.distanceMetersTo(other: GeoPosition): Double {
    val averageLat = ((latitude + other.latitude) / 2.0).toRadians()
    val metersPerDegreeLat = PI * EarthRadiusMeters / 180.0
    val metersPerDegreeLon = metersPerDegreeLat * cos(averageLat)
    val dx = (other.longitude - longitude) * metersPerDegreeLon
    val dy = (other.latitude - latitude) * metersPerDegreeLat
    return hypot(dx, dy)
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
