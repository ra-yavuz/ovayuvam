package tr.ovayuva.ovayuvam

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlinx.coroutines.runBlocking
import tr.ovayuva.ovayuvam.domain.*
import tr.ovayuva.ovayuvam.location.*
import tr.ovayuva.ovayuvam.map.*
import tr.ovayuva.ovayuvam.storage.*
import tr.ovayuva.ovayuvam.backup.*
import java.io.File
import java.util.Locale

fun checkReviewCandidate(target: Context) {
    check(target.packageName.endsWith(".verification"))
    val context = object : ContextWrapper(target) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences("review-$name", mode)
        override fun getDatabasePath(name: String): File = super.getDatabasePath("review-$name")
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).apply { parentFile?.mkdirs() }, factory)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?, handler: android.database.DatabaseErrorHandler?): SQLiteDatabase =
            SQLiteDatabase.openDatabase(getDatabasePath(name).apply { parentFile?.mkdirs() }.path, factory, SQLiteDatabase.CREATE_IF_NECESSARY, handler)
    }
    context.getSharedPreferences("tracking", 0).edit().clear().commit()
    val tracking = TrackingState(context)
    check(!tracking.consented && !tracking.enabled)
    tracking.setEnabled(true)
    check(!tracking.enabled)
    tracking.acceptDisclosure()
    check(TrackingState(context).consented && TrackingState(context).enabled)
    tracking.setEnabled(false)
    check(!TrackingState(context).enabled && TrackingState(context).consented)
    context.getSharedPreferences("explore", 0).edit().clear().commit()
    check(ExploreState(context).enabled)
    ExploreState(context).enabled = false
    check(!ExploreState(context).enabled)
    context.getSharedPreferences("visit-settings", 0).edit().putInt("stay-radius", 1000).commit()
    check(VisitPreferences(context).stayRadius == 150)

    SQLiteDatabase.deleteDatabase(context.getDatabasePath("ovayuvam-world.db"))
    var repo = VisitRepository(context)
    val cell = WorldCell.fromLocation(41.013, 28.982, 20.0)
    val path = PlannedPath("native-plan", 1000, listOf(cell, WorldCell(cell.x + 1, cell.y)))
    repo.savePlannedPaths(listOf(path))
    repo.savePlannedPaths(listOf(path))
    check(repo.plannedPaths() == listOf(path))
    check(repo.allVisitedCells().isEmpty() && repo.allRevealCells().isEmpty() && repo.allVisitCounts().isEmpty())
    repo.close()
    repo = VisitRepository(context)
    check(repo.plannedPaths() == listOf(path))
    val backup = WorldBackup(emptyList(), emptyList(), null, plannedPaths = repo.plannedPaths())
    val restored = WorldBackupCodec.decrypt(WorldBackupCodec.encrypt(backup, "native-plan-password"), "native-plan-password")
    repo.clearPlannedPaths()
    repo.importCells(restored.visitedCells, restored.revealCells, restored.visitCounts, restored.plannedPaths)
    check(repo.plannedPaths() == listOf(path))
    repo.recordRevealCells(path.cells, RevealCell.Kind.Core, 2000)
    val discoveries = repo.allRevealCells()
    repo.undoPlannedPath()
    check(repo.plannedPaths().isEmpty() && repo.allRevealCells() == discoveries)
    repo.close()
    SQLiteDatabase.deleteDatabase(context.getDatabasePath("ovayuvam-world.db"))

    runBlocking {
        val cache = FogTileCache(8 * 1024 * 1024)
        cache.update(emptyList(), listOf(RevealCell(cell.x,cell.y,RevealCell.Kind.Core,0,0,8)), emptyList())
        val view = FogViewport(MercatorPoint.from(cell.centerPosition(20.0)), 1.0, 512,512)
        val frame = cache.frame(view)
        val painted = cache.paintedMarks
        repeat(10) { cache.frame(view) }
        check(cache.paintedMarks == painted) { "Static plans were repainted" }
        val center = MercatorPoint.from(cell.centerPosition(20.0))
        val image = frame.images.entries.first { (key, _) ->
            center.x >= key.left && center.x < key.left + key.span && center.y <= key.top && center.y > key.top - key.span
        }.value.reveal!!
        val output = Bitmap.createBitmap(image.width,image.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.argb((FogAppearance.MinimumOpacity*255).toInt(),22,33,29))
        val erase = Paint().apply {
            alpha = (FogAppearance.PlanErasure*255).toInt()
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
        canvas.drawBitmap(image,0f,0f,erase)
        for (y in 0 until output.height) for (x in 0 until output.width) {
            check(Color.alpha(output.getPixel(x,y)) >= 82) { "Plan fully revealed a pixel" }
        }
        erase.alpha = 255
        canvas.drawBitmap(image,0f,0f,erase)
        check((0 until output.height).any { y -> (0 until output.width).any { x -> Color.alpha(output.getPixel(x,y)) == 0 } }) {
            "The visited brush center did not fully clear planned fog"
        }
    }
    val strings = listOf(R.string.welcome,R.string.tracking_disclosure,R.string.settings,R.string.backup_import_help,
        R.string.plan_path,R.string.privacy_local,R.string.pause_tracking)
    val seen = mutableSetOf<String>()
    for (language in listOf("en","de","tr","ru","es","fr")) {
        val localized = target.createConfigurationContext(Configuration(target.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) })
        strings.forEach { check(localized.getString(it).isNotBlank()) }
        check(seen.add(localized.getString(R.string.welcome)))
        for (array in listOf(R.array.notification_everyday,R.array.notification_progress,R.array.notification_quiet)) {
            check(localized.resources.getStringArray(array).size >= 10)
        }
        check(!localized.getString(R.string.percent, 82).contains("%1"))
        check(TrackingNotification.create(localized, "test").actions.single().title.isNotBlank())
    }
}
