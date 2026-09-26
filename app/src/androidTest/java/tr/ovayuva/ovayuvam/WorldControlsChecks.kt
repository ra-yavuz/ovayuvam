package tr.ovayuva.ovayuvam

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.runBlocking
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import tr.ovayuva.ovayuvam.domain.*
import tr.ovayuva.ovayuvam.location.*
import tr.ovayuva.ovayuvam.storage.*
import tr.ovayuva.ovayuvam.ui.ShareWorld
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun Instrumentation.checkWorldControls() {
    val context = targetContext
    check(context.packageName.endsWith(".verification"))
    val isolated = object : ContextWrapper(context) {
        override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences("controls-$name", mode)
    }
    isolated.getSharedPreferences("map-appearance", 0).edit().clear().commit()
    check(!MapPreferences(isolated).showArea)
    MapPreferences(isolated).showArea = true
    check(MapPreferences(isolated).showArea)
    MapPreferences(isolated).showArea = false
    check(!MapPreferences(isolated).showArea)

    val translations = mutableSetOf<String>()
    for (code in listOf("en", "de", "tr", "ru", "es", "fr")) {
        val local = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(code)) })
        check(translations.add(local.getString(R.string.help_growth)))
        for (id in listOf(R.string.help_share, R.string.help_tracking, R.string.help_growth, R.string.help_fog,
            R.string.help_visits, R.string.help_weekly, R.string.help_world, R.string.help_export,
            R.string.help_import, R.string.help_language, R.string.help_privacy)) check(local.getString(id).length > 30)
        check(local.getString(R.string.area_today, "123").contains("123"))
        val notification = TrackingNotification.create(local, local.resources.getStringArray(R.array.notification_everyday)[0])
        check(notification.actions.single().title.toString() == local.getString(R.string.pause_tracking))
        check(notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE).toString() == local.getString(R.string.tracking_notification_title))
        TrackingNotification.ensureChannel(local)
        check(local.getSystemService(android.app.NotificationManager::class.java)
            .getNotificationChannel(TrackingNotification.ChannelId).name.toString() == local.getString(R.string.tracking_channel_name))
    }
    val pen = BitmapFactory.decodeResource(context.resources, R.drawable.art_pen)
    check(pen.hasAlpha())
    check((pen.getPixel(0, 0) ushr 24) == 0)
    val transparent = (0 until pen.height step 4).sumOf { y ->
        (0 until pen.width step 4).count { x -> pen.getPixel(x, y) ushr 24 == 0 }
    }
    check(transparent > pen.width * pen.height / 16 * 0.45) { "Pen still has a paper background" }
    pen.recycle()

    val home = GeoPosition(41.0136, 28.981)
    val cell = WorldCell.fromLocation(home.latitude, home.longitude, 20.0)
    val now = System.currentTimeMillis()
    val repository = VisitRepository(context)
    repository.clearPlannedPaths()
    repository.recordRevealCells((-10..0).map { WorldCell(cell.x + it, cell.y) }, RevealCell.Kind.Core, now - 2 * 86400_000)
    repository.recordRevealCells((1..10).map { WorldCell(cell.x + it, cell.y) }, RevealCell.Kind.Core, now)
    val state = TrackingState(context)
    state.acceptDisclosure(); state.setEnabled(false)
    state.setCurrentLocation(home.latitude, home.longitude, WorldCell.fromLocation(home.latitude, home.longitude))
    ExploreState(context).enabled = false
    MapPreferences(context).showArea = false

    fun launch(): Activity = startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    var activity = launch()
    fun nodes(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        if (node == null) return emptyList()
        return listOf(node) + (0 until node.childCount).flatMap { nodes(node.getChild(it)) }
    }
    fun has(label: String): Boolean {
        repeat(40) {
            if (nodes(uiAutomation.rootInActiveWindow).any { it.text?.toString() == label || it.contentDescription?.toString() == label }) return true
            Thread.sleep(100)
        }
        return false
    }
    fun tap(label: String) {
        var node: AccessibilityNodeInfo? = null
        repeat(30) {
            if (node == null) {
                node = nodes(uiAutomation.rootInActiveWindow).firstOrNull { it.text?.toString() == label || it.contentDescription?.toString() == label }
                if (node == null) Thread.sleep(200)
            }
        }
        val rect = Rect(); checkNotNull(node) { "Missing control $label" }.getBoundsInScreen(rect)
        val down = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, rect.exactCenterX(), rect.exactCenterY(), 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN; sendPointerSync(event); event.recycle(); Thread.sleep(60)
        }
        Thread.sleep(700)
    }
    fun findMap(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findMap(view.getChildAt(i))?.let { return it }
        return null
    }
    var map: MapLibreMap? = null
    fun loadMap() {
        val ready = CountDownLatch(1)
        runOnMainSync { checkNotNull(findMap(activity.window.decorView)).getMapAsync { map = it; ready.countDown() } }
        check(ready.await(30, TimeUnit.SECONDS))
        Thread.sleep(20_000)
    }
    loadMap()
    // A throttled emulator can leave its launcher ANR dialog above the target app.
    // Never dismiss a target-app ANR or treat it as a successful check.
    repeat(3) { if (nodes(uiAutomation.rootInActiveWindow).any {
        it.text?.toString() in listOf("Pixel Launcher isn't responding", "System UI isn't responding")
    }) {
        checkNotNull(nodes(uiAutomation.rootInActiveWindow).firstOrNull { it.text?.toString() == "Close app" })
            .performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Thread.sleep(2000)
    } }
    check(has(context.getString(R.string.show_area))) {
        "Badge missing: " + nodes(uiAutomation.rootInActiveWindow).joinToString { "${it.packageName}:${it.text}/${it.contentDescription}" }
    }
    tap(context.getString(R.string.show_area))
    check(MapPreferences(context).showArea && has(context.getString(R.string.show_logo)))
    runOnMainSync { activity.finish() }; Thread.sleep(1000)
    activity = launch(); loadMap()
    check(MapPreferences(context).showArea && has(context.getString(R.string.show_logo)))
    tap(context.getString(R.string.show_logo))
    check(!MapPreferences(context).showArea)
    check(has(context.getString(R.string.map_credits)))
    tap(context.getString(R.string.map_credits))
    check(nodes(uiAutomation.rootInActiveWindow).any { it.text?.contains("OpenStreetMap") == true })
    Thread.sleep(9_000)
    check(has(context.getString(R.string.map_credits)))

    tap(context.getString(R.string.plan_path))
    var width = 0; var height = 0
    runOnMainSync { width = activity.window.decorView.width; height = activity.window.decorView.height }
    fun gesture(two: Boolean, spread: Float = 1f, pan: Float = 0f) {
        val down = SystemClock.uptimeMillis()
        val props = Array(2) { id -> MotionEvent.PointerProperties().apply { this.id = id; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        fun send(action: Int, count: Int, fraction: Float) {
            val coords = Array(count) { id -> MotionEvent.PointerCoords().apply {
                val offset = width * 0.15f * (1 + (spread - 1) * fraction)
                x = width * 0.5f + (if (id == 0) -offset else offset) + pan * fraction
                y = height * 0.5f + (if (two) 0f else 100f * fraction)
                pressure = 1f; size = 1f
            } }
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, count, props, coords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
            sendPointerSync(event); event.recycle(); Thread.sleep(40)
        }
        send(MotionEvent.ACTION_DOWN, 1, 0f)
        if (two) send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, 0f)
        for (step in 1..16) send(MotionEvent.ACTION_MOVE, if (two) 2 else 1, step / 16f)
        if (two) send(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 2, 1f)
        send(MotionEvent.ACTION_UP, 1, 1f)
        Thread.sleep(1500)
    }
    var before: CameraPosition? = null
    runOnMainSync { before = map!!.cameraPosition }
    gesture(false)
    check(repository.plannedPaths().size == 1) { "One finger did not paint" }
    runOnMainSync { check(map!!.cameraPosition == before) { "One finger moved the map" } }
    gesture(true, spread = 1.6f, pan = 60f)
    runOnMainSync {
        check(map!!.cameraPosition.zoom > before!!.zoom + 0.4) { "Pinch did not zoom in" }
        check(map!!.cameraPosition.target != before!!.target) { "Two fingers did not pan" }
        before = map!!.cameraPosition
    }
    check(repository.plannedPaths().size == 1) { "Two fingers saved an accidental stroke" }
    gesture(true, spread = 0.65f)
    runOnMainSync { check(map!!.cameraPosition.zoom < before!!.zoom - 0.4) }
    check(repository.plannedPaths().size == 1)
    runOnMainSync { map!!.cameraPosition = CameraPosition.Builder().target(LatLng(home.latitude, home.longitude)).zoom(13.0).build() }
    gesture(true, spread = 1.6f)
    runOnMainSync { check(map!!.cameraPosition.zoom > 13.4) { "Navigation below painting zoom was blocked" } }
    check(repository.plannedPaths().size == 1)
    tap(context.getString(R.string.done))
    runOnMainSync { map!!.cameraPosition = CameraPosition.Builder().target(LatLng(home.latitude, home.longitude)).zoom(15.6).build() }
    Thread.sleep(5000)
    tap(context.getString(R.string.settings))
    tap(context.getString(R.string.about_setting, context.getString(R.string.watch_growth)))
    check(has(context.getString(R.string.help_growth)))
    tap(context.getString(R.string.close))
    tap(context.getString(R.string.share_world))
    Thread.sleep(3000)
    val image = File(context.cacheDir, "world-shares").listFiles()?.maxByOrNull { it.lastModified() }
    checkNotNull(image) { "Share did not capture a map" }
    val bitmap = BitmapFactory.decodeFile(image.path)
    check(bitmap.width == width && bitmap.height in (height * 0.8).toInt()..height)
    val colors = mutableSetOf<Int>()
    for (y in bitmap.height / 3 until bitmap.height * 2 / 3 step 4) for (x in 0 until bitmap.width step 4) colors.add(bitmap.getPixel(x, y))
    check(colors.size > 150) { "Shared map is blank" }
    val output = File(context.filesDir, "controls-check").apply { mkdirs() }
    image.copyTo(File(output, "shared-world.png"), overwrite = true)
    bitmap.recycle()
    val sample = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
    val intent = runBlocking { ShareWorld.intent(context, sample) }
    check(intent.type == "image/png" && intent.getStringExtra(Intent.EXTRA_TEXT)!!.contains(ShareWorld.Website))
    check(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0 && intent.clipData != null)
    check(intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.scheme == "content")
    uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
    repository.close()
    runOnMainSync { activity.finish() }
}
