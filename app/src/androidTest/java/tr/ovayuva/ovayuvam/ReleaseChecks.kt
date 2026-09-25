package tr.ovayuva.ovayuvam

import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import tr.ovayuva.ovayuvam.domain.*
import tr.ovayuva.ovayuvam.location.*
import tr.ovayuva.ovayuvam.storage.*
import java.io.File
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Uses a separate verification package and isolated database, never the user's world. */
class ReleaseChecks : Instrumentation() {
    private var visual = false
    private var notifications = false
    private var performance: String? = null
    private var raster = false
    private var tiles = false
    override fun onCreate(arguments: Bundle?) {
        visual = arguments?.getString("visual") == "true"
        notifications = arguments?.getString("notifications") == "true"
        performance = arguments?.getString("performance")
        raster = arguments?.getString("raster") == "true"
        tiles = arguments?.getString("tiles") == "true"
        super.onCreate(arguments)
        start()
    }
    override fun onStart() {
        val result = Bundle()
        try {
            if (tiles) {
                checkFogTiles()
                result.putString("stream", "PASS: tile reuse, local invalidation, overview reuse, pixels, seams, replay, memory, cancellation\n")
                finish(-1, result)
                return
            }
            performance?.let {
                checkFogPerformance(it)
                result.putString("stream", "PASS: fog camera benchmark $it\n")
                finish(-1,result)
                return
            }
            if (raster) {
                checkFogRaster()
                result.putString("stream", "PASS: fog raster pixels, heat, legacy, date line, cancellation\n")
                finish(-1,result)
                return
            }
            if (notifications) {
                checkQuietNotifications(targetContext)
                result.putString("stream", "PASS: silent ongoing notification replacement\n")
                finish(-1, result)
                return
            }
            if (visual) {
                captureHeat()
                result.putString("stream", "PASS: heat screenshots\n")
                finish(-1, result)
                return
            }
            val testContext = object : ContextWrapper(targetContext) {
                override fun getApplicationContext(): Context = this
                override fun getDatabasePath(name: String): File = super.getDatabasePath("release-check-$name")
                override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
                    SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).apply { parentFile?.mkdirs() }, factory)
                override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?, errorHandler: android.database.DatabaseErrorHandler?): SQLiteDatabase =
                    SQLiteDatabase.openDatabase(getDatabasePath(name).apply { parentFile?.mkdirs() }.path, factory,
                        SQLiteDatabase.CREATE_IF_NECESSARY, errorHandler)
            }
            val path = testContext.getDatabasePath("ovayuvam-world.db")
            SQLiteDatabase.deleteDatabase(path)
            var repo = VisitRepository(testContext)
            val position = GeoPosition(41.0, 29.0)
            var seconds = 0L
            fun fix(meters: Double = 0.0, accuracy: Double = 5.0) {
                repo.recordPresence(VisitFix(GeoPosition(position.latitude + meters / 111195, position.longitude),
                    accuracy, 1_700_000_000_000 + seconds * 1000, 1000 + seconds * 1000, 1), 150.0)
                seconds += 5
            }
            repeat(4) { fix() }
            check(repo.allVisitCounts().single().visits == 1)
            repeat(150) { fix(if (it % 2 == 0) 90.0 else -80.0) }
            fix()
            check(repo.allVisitCounts().all { it.visits == 1 }) { "House movement counted as returns" }
            repo.close()
            repo = VisitRepository(testContext)
            repeat(4) { fix() }
            check(repo.allVisitCounts().all { it.visits == 1 }) { "Restart counted a visit" }
            repeat(125) { fix(600.0) }
            repeat(8) { fix() }
            val home = WorldCell.fromLocation(position.latitude, position.longitude)
            check(repo.allVisitCounts().single { it.x == home.x && it.y == home.y }.visits == 2)
            val snapshot = repo.allVisitCounts()
            repeat(2) { repo.importVisitCounts(snapshot) }
            repeat(4) { fix() }
            check(repo.allVisitCounts() == snapshot) { "Backup import duplicated counts" }
            repo.recordRevealCells((0..8100).map { WorldCell(it, 0) }, RevealCell.Kind.Core, 100L)
            check(repo.mapData(MapWindow(-85.0,-180.0,85.0,180.0,true)).reveal.size == 8101)
            check(repo.mapData(MapWindow(40.0,28.0,42.0,30.0)).reveal.isEmpty())
            repo.recordRevealCells(listOf(WorldCell.fromLocation(0.0,179.999,20.0),WorldCell.fromLocation(0.0,-179.999,20.0)), RevealCell.Kind.Core)
            check(repo.mapData(MapWindow(-1.0,179.0,1.0,-179.0)).reveal.size == 2)
            repo.recordRevealCells(listOf(WorldCell.fromLocation(80.00015,0.0005,20.0)), RevealCell.Kind.Core)
            check(repo.mapData(MapWindow(79.999,0.0,80.0,0.001)).reveal.size == 1) { "Polar viewport clipped a reveal brush" }
            repo.close()
            SQLiteDatabase.deleteDatabase(path)
            result.putString("stream", "PASS: stay movement, restart, leave-return, backup merge, 8101-cell history, spatial queries, date line\n")
            finish(-1, result)
        } catch (error: Throwable) {
            result.putString("stream", android.util.Log.getStackTraceString(error))
            finish(1, result)
        }
    }

    private fun captureHeat() {
        check(android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("sdk")) { "Emulator only" }
        val repository = VisitRepository(targetContext)
        val baseline = repository.allVisitedCells()
        val center = WorldCell.fromLocation(48.863, 2.331)
        repository.importVisitCounts(baseline.map {
            val visits = when {
                it.x < center.x - 10 -> 1
                it.x < center.x -> 10
                it.x < center.x + 10 -> 100
                else -> 365
            }
            VisitCount(it.x,it.y,visits,1000L,2000L)
        })
        repository.close()
        val preferences = VisitPreferences(targetContext)
        val output = File(targetContext.filesDir, "heat-check").apply { mkdirs() }
        fun find(view: View): MapView? {
            if (view is MapView) return view
            if (view is ViewGroup) for (i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
            return null
        }
        for (enabled in listOf(false, true)) {
            preferences.showHeat = enabled
            val activity = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(3000)
            val ready = CountDownLatch(1)
            var map: MapLibreMap? = null
            runOnMainSync { checkNotNull(find(activity.window.decorView)).getMapAsync { map = it; ready.countDown() } }
            check(ready.await(20, TimeUnit.SECONDS))
            for (zoom in listOf(12.0, 13.0, 14.0, 14.6, 15.6)) {
                runOnMainSync { map!!.cameraPosition = CameraPosition.Builder().target(LatLng(48.863,2.331)).zoom(zoom).build() }
                Thread.sleep(12000)
                var focused = false
                runOnMainSync { focused = activity.hasWindowFocus() }
                check(focused) {
                    "Map is covered by another window at zoom $zoom"
                }
                val screenshot = checkNotNull(uiAutomation.takeScreenshot())
                File(output, "heat-$enabled-$zoom.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG,100,it) }
                screenshot.recycle()
            }
            runOnMainSync { activity.finish() }
            Thread.sleep(1000)
        }
        preferences.showHeat = true
    }
}
