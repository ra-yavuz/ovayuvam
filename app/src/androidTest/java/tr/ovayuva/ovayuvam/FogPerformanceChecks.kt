package tr.ovayuva.ovayuvam

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun Instrumentation.checkFogPerformance(label: String) {
    check(android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("sdk"))
    val output = File(targetContext.filesDir,"fog-performance").apply { mkdirs() }
    val activity = startActivitySync(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun find(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
        return null
    }
    var map: MapLibreMap? = null
    val ready = CountDownLatch(1)
    runOnMainSync { checkNotNull(find(activity.window.decorView)).getMapAsync { map=it; ready.countDown() } }
    check(ready.await(20,TimeUnit.SECONDS))
    Thread.sleep(12000)
    val results = JSONArray()
    for (zoom in listOf(15.6,12.0,8.0,2.0,0.0,15.6)) {
        val frames = mutableListOf<Double>()
        var last = 0L
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(time: Long) {
                if (last != 0L) frames += (time-last)/1_000_000.0
                last = time
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        val started = System.nanoTime()
        val finished = CountDownLatch(1)
        runOnMainSync {
            Choreographer.getInstance().postFrameCallback(callback)
            map!!.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                .target(LatLng(48.863,2.331)).zoom(zoom).build()),2500,
                object : MapLibreMap.CancelableCallback {
                    override fun onFinish() { finished.countDown() }
                    override fun onCancel() { finished.countDown() }
                })
        }
        check(finished.await(120,TimeUnit.SECONDS)) { "Camera animation stalled at $zoom" }
        runOnMainSync { Choreographer.getInstance().removeFrameCallback(callback) }
        val elapsed = (System.nanoTime()-started)/1_000_000.0
        val sorted = frames.sorted()
        check(sorted.isNotEmpty())
        val row = JSONObject().put("zoom",zoom).put("elapsedMs",elapsed).put("frames",sorted.size)
            .put("p50Ms",sorted[sorted.size/2]).put("p95Ms",sorted[(sorted.size*0.95).toInt().coerceAtMost(sorted.lastIndex)])
            .put("maxMs",sorted.last()).put("over100Ms",sorted.count { it > 100 })
        results.put(row)
        Thread.sleep(4000)
        check(activity.hasWindowFocus()) { "App covered during benchmark" }
        val shot = checkNotNull(uiAutomation.takeScreenshot())
        File(output,"$label-$zoom.png").outputStream().use { shot.compress(Bitmap.CompressFormat.PNG,100,it) }
        shot.recycle()
    }
    File(output,"$label.json").writeText(JSONObject().put("label",label).put("camera",results).toString(2))
    runOnMainSync { activity.finish() }
}
