package tr.ovayuva.ovayuvam

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import tr.ovayuva.ovayuvam.domain.RevealCell
import tr.ovayuva.ovayuvam.domain.WorldCell
import tr.ovayuva.ovayuvam.location.TrackingState
import tr.ovayuva.ovayuvam.storage.VisitRepository
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun Instrumentation.checkGrowthCamera() {
    check(targetContext.packageName.endsWith(".verification"))
    check(android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("sdk"))
    targetContext.deleteDatabase("ovayuvam-world.db")
    val repository = VisitRepository(targetContext)
    val now = System.currentTimeMillis()
    for ((days, longitude) in listOf(3 to 2.331, 2 to 2.34, 1 to 2.42)) {
        val center = WorldCell.fromLocation(48.863,longitude,WorldCell.RevealCellSizeMeters)
        repository.recordRevealCells((-1..1).flatMap { x -> (-1..1).map { y -> WorldCell(center.x+x,center.y+y) } },
            RevealCell.Kind.Core, now-days*86_400_000L)
    }
    TrackingState(targetContext).setCurrentLocation(48.863,2.331,WorldCell.fromLocation(48.863,2.331))
    val activity = startActivitySync(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun findMap(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findMap(view.getChildAt(i))?.let { return it }
        return null
    }
    var map: MapLibreMap? = null
    val ready = CountDownLatch(1)
    runOnMainSync { checkNotNull(findMap(activity.window.decorView)).getMapAsync { map=it;ready.countDown() } }
    check(ready.await(20,TimeUnit.SECONDS))
    Thread.sleep(10000)
    val saved = repository.allRevealCells()
    fun findNode(node: AccessibilityNodeInfo?, label: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.text?.toString() == label || node.contentDescription?.toString() == label) return node
        for (i in 0 until node.childCount) findNode(node.getChild(i),label)?.let { return it }
        return null
    }
    fun node(label: String): AccessibilityNodeInfo {
        repeat(30) {
            findNode(uiAutomation.rootInActiveWindow,label)?.let { return it }
            Thread.sleep(200)
        }
        error("Missing UI control: $label")
    }
    fun click(label: String) {
        var target = node(label)
        while (!target.isClickable) target = checkNotNull(target.parent)
        check(target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        Thread.sleep(400)
    }
    click("Info")
    click("Watch your world grow")
    val output = File(targetContext.filesDir,"growth-camera").apply { mkdirs() }
    val frames = JSONArray()
    val zooms = mutableListOf<Double>()
    fun rangeNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.rangeInfo != null) return node
        for (i in 0 until node.childCount) node.getChild(i)?.let { child -> rangeNode(child)?.let { return it } }
        return null
    }
    for ((name,fraction) in listOf("start" to .001f,"middle" to .5f,"end" to 1f,"rewind" to 0f)) {
        var group = node("Replay date")
        while (rangeNode(group) == null) group = checkNotNull(group.parent)
        val slider = checkNotNull(rangeNode(group))
        // Accessibility can cache the old value; always send the seek and verify the actual camera below.
        slider.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,
            Bundle().apply { putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,fraction) })
        Thread.sleep(2200)
        var zoom = 0.0
        runOnMainSync { zoom = map!!.cameraPosition.zoom }
        zooms += zoom
        check(activity.hasWindowFocus())
        val screenshot = checkNotNull(uiAutomation.takeScreenshot())
        File(output,"$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG,100,it) }
        screenshot.recycle()
        frames.put(JSONObject().put("frame",name).put("zoom",zoom))
    }
    check(zooms[0] > zooms[1]+2) { "First discovery was not framed closely: $zooms" }
    check(zooms[1] > zooms[2]+2) { "Camera did not expand with distant discoveries: $zooms" }
    check(kotlin.math.abs(zooms[0]-zooms[3]) < .1) { "Rewind did not restore close framing: $zooms" }
    check(repository.allRevealCells() == saved) { "Replay modified saved history" }
    click("Close replay")
    node("Center map on me")
    File(output,"results.json").writeText(JSONObject().put("frames",frames).put("preservedHistory",true).toString(2))
    repository.close()
    runOnMainSync { activity.finish() }
}
