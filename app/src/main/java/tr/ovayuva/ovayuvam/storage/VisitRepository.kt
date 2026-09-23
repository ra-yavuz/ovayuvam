package tr.ovayuva.ovayuvam.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import tr.ovayuva.ovayuvam.domain.RevealCell
import tr.ovayuva.ovayuvam.domain.VisitedCell
import tr.ovayuva.ovayuvam.domain.WorldCell
import tr.ovayuva.ovayuvam.domain.WorldSummary
import tr.ovayuva.ovayuvam.domain.GeoPosition
import tr.ovayuva.ovayuvam.location.StayZone
import tr.ovayuva.ovayuvam.location.VisitCount
import tr.ovayuva.ovayuvam.location.VisitDetector
import tr.ovayuva.ovayuvam.location.VisitFix
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.ceil

data class MapWindow(val south: Double, val west: Double, val north: Double, val east: Double, val wholeWorld: Boolean = false)
data class WorldMapData(val cells: List<VisitedCell>, val reveal: List<RevealCell>, val visits: List<VisitCount>)

class VisitRepository(context: Context) {
    private val db = VisitDatabase(context.applicationContext)
    private var lastVisitFix: VisitFix? = null

    fun close() = db.close()

    @Synchronized
    fun interruptPresence() {
        db.writableDatabase.execSQL("UPDATE visit_zones SET away_since=0,candidate_since=0,candidate_fixes=0")
    }

    @Synchronized
    fun recordPresence(fix: VisitFix, radius: Double) {
        val previous = lastVisitFix
        if (previous != null && previous.bootId == fix.bootId) {
            val elapsed = fix.elapsedMs - previous.elapsedMs
            if (elapsed < 2_000) return
            if (elapsed <= VisitDetector.MaxGapMs && VisitDetector.groundDistance(previous.position, fix.position) >
                elapsed / 1000.0 * 55 + previous.accuracyMeters + fix.accuracyMeters) return
        }
        if (!VisitDetector.usable(fix)) {
            interruptPresence()
            return
        }
        lastVisitFix = fix
        db.writableDatabase.transaction {
            val cell = WorldCell.fromLocation(fix.position.latitude, fix.position.longitude)
            val assigned = rawQuery("SELECT zone_id FROM visit_counts WHERE cell_x=? AND cell_y=?",
                arrayOf(cell.x.toString(), cell.y.toString())).use { if (it.moveToFirst()) it.getLong(0) else null }
            val zones = mutableListOf<StayZone>()
            val latReach = (radius + 200) / 110_000
            val lonReach = latReach / cos(Math.toRadians(fix.position.latitude)).coerceAtLeast(0.08)
            // Far, already departed stays need no updates until approached again.
            rawQuery("""SELECT * FROM visit_zones WHERE armed=0 OR id=? OR
                (latitude BETWEEN ? AND ? AND
                MIN(ABS(longitude-?),360-ABS(longitude-?)) <= ?)""",
                arrayOf((assigned ?: -1).toString(), (fix.position.latitude-latReach).toString(),
                    (fix.position.latitude+latReach).toString(), fix.position.longitude.toString(),
                    fix.position.longitude.toString(), lonReach.toString())).use { c ->
                while (c.moveToNext()) zones += StayZone(
                    c.getLong(0), GeoPosition(c.getDouble(1), c.getDouble(2)), c.getDouble(3),
                    c.getLong(4), c.getInt(5) != 0, c.getLong(6), c.getLong(7), c.getInt(8), c.getLong(9), c.getInt(10),
                )
            }
            var selected = zones.firstOrNull { it.id == assigned }
                ?: zones.filter { VisitDetector.groundDistance(it.anchor, fix.position) + fix.accuracyMeters < radius }
                    .minByOrNull { VisitDetector.groundDistance(it.anchor, fix.position) }
            if (selected == null) {
                val id = insertOrThrow("visit_zones", null, ContentValues().apply {
                    put("latitude", fix.position.latitude); put("longitude", fix.position.longitude); put("radius", radius)
                })
                selected = StayZone(id, fix.position, radius)
                zones += selected
            }
            val selectedId = selected.id
            zones.forEach { old ->
                val next = VisitDetector.advance(old, fix, radius)
                update("visit_zones", ContentValues().apply {
                    put("radius", next.radiusMeters); put("epoch", next.epoch); put("armed", if (next.armed) 1 else 0)
                    put("away_since", next.awaySince); put("candidate_since", next.candidateSince)
                    put("candidate_fixes", next.candidateFixes); put("last_elapsed", next.lastElapsed); put("boot_id", next.bootId)
                }, "id=?", arrayOf(next.id.toString()))
                if (next.id == selectedId && next.epoch > 0 && !next.armed &&
                    VisitDetector.groundDistance(next.anchor, fix.position) + fix.accuracyMeters <= radius) {
                    execSQL("""INSERT OR IGNORE INTO visit_counts
                        (cell_x,cell_y,zone_id,visits,first_visit_ms,last_visit_ms,counted_epoch)
                        VALUES (?,?,?,0,?,?,0)""", arrayOf<Any>(cell.x, cell.y, next.id, fix.timeMs, fix.timeMs))
                    execSQL("""UPDATE visit_counts SET visits=visits+1,last_visit_ms=?,counted_epoch=?
                        WHERE cell_x=? AND cell_y=? AND counted_epoch<>?""",
                        arrayOf<Any>(fix.timeMs, next.epoch, cell.x, cell.y, next.epoch))
                }
            }
        }
    }

    fun allVisitCounts(): List<VisitCount> = visitCounts()

    fun visitsStartedMs(): Long? = db.readableDatabase.rawQuery("SELECT MIN(first_visit_ms) FROM visit_counts", null).use {
        it.moveToFirst(); if (it.isNull(0)) null else it.getLong(0)
    }

    fun importVisitCounts(counts: List<VisitCount>) {
        db.writableDatabase.transaction {
            counts.forEach { count ->
                require(count.visits >= 0 && count.firstVisitMs >= 0 && count.lastVisitMs >= count.firstVisitMs)
                val args = arrayOf(count.x.toString(), count.y.toString())
                val existing = rawQuery("SELECT visits,first_visit_ms,last_visit_ms FROM visit_counts WHERE cell_x=? AND cell_y=?", args).use {
                    if (it.moveToFirst()) VisitCount(count.x, count.y, it.getInt(0), it.getLong(1), it.getLong(2)) else null
                }
                if (existing == null) {
                    // A restored count starts occupied. A later observed departure is needed to count again.
                    val anchor = WorldCell(count.x, count.y).centerPosition()
                    val zone = insertOrThrow("visit_zones", null, ContentValues().apply {
                        put("latitude", anchor.latitude); put("longitude", anchor.longitude)
                        put("radius", VisitDetector.DefaultRadius); put("epoch", 1)
                    })
                    insertOrThrow("visit_counts", null, ContentValues().apply {
                        put("cell_x", count.x); put("cell_y", count.y); put("zone_id", zone)
                        put("visits", count.visits); put("first_visit_ms", count.firstVisitMs)
                        put("last_visit_ms", count.lastVisitMs); put("counted_epoch", 1)
                    })
                } else update("visit_counts", ContentValues().apply {
                    put("visits", maxOf(existing.visits, count.visits))
                    put("first_visit_ms", minOf(existing.firstVisitMs, count.firstVisitMs))
                    put("last_visit_ms", maxOf(existing.lastVisitMs, count.lastVisitMs))
                }, "cell_x=? AND cell_y=?", args)
            }
            execSQL("UPDATE visit_zones SET armed=0,away_since=0,candidate_since=0,candidate_fixes=0,last_elapsed=0,boot_id=-1")
            execSQL("UPDATE visit_counts SET counted_epoch=(SELECT epoch FROM visit_zones WHERE id=visit_counts.zone_id)")
        }
    }

    fun mapData(window: MapWindow): WorldMapData = WorldMapData(cells(Int.MAX_VALUE, window), revealCells(Int.MAX_VALUE, window), visitCounts(window))

    private fun visitCounts(window: MapWindow? = null): List<VisitCount> {
        val (where, args) = windowClause(window, WorldCell.DefaultCellSizeMeters)
        return db.readableDatabase.rawQuery("SELECT cell_x,cell_y,visits,first_visit_ms,last_visit_ms FROM visit_counts $where", args).use { c ->
            buildList { while (c.moveToNext()) add(VisitCount(c.getInt(0),c.getInt(1),c.getInt(2),c.getLong(3),c.getLong(4))) }
        }
    }

    private fun windowClause(window: MapWindow?, size: Double): Pair<String, Array<String>> {
        if (window == null) return "" to emptyArray()
        val sw = WorldCell.fromLocation(window.south, window.west, size)
        val ne = WorldCell.fromLocation(window.north, window.east, size)
        // Geographic brush radii occupy more Mercator cells at high latitudes.
        val latitude = maxOf(abs(window.south), abs(window.north)).coerceAtMost(85.05112878)
        val padding = ceil(25.0 / (size * cos(Math.toRadians(latitude)))).toInt() + 2
        val y = arrayOf((sw.y - padding).toString(), (ne.y + padding).toString())
        if (window.wholeWorld) return "WHERE cell_y BETWEEN ? AND ?" to y
        val x = arrayOf((sw.x - padding).toString(), (ne.x + padding).toString())
        val longitude = if (window.west > window.east) "(cell_x >= ? OR cell_x <= ?)" else "cell_x BETWEEN ? AND ?"
        return "WHERE cell_y BETWEEN ? AND ? AND $longitude" to (y + x)
    }

    fun record(cell: WorldCell, seenMs: Long = System.currentTimeMillis()) {
        db.writableDatabase.transaction {
            recordCell(cell, seenMs)
        }
    }

    fun recordVisitArea(center: WorldCell, seenMs: Long = System.currentTimeMillis(), radiusCells: Int = 2) {
        db.writableDatabase.transaction {
            for (dx in -radiusCells..radiusCells) {
                for (dy in -radiusCells..radiusCells) {
                    if (dx * dx + dy * dy <= radiusCells * radiusCells) {
                        recordCell(WorldCell(center.x + dx, center.y + dy), seenMs)
                    }
                }
            }
        }
    }

    fun recordRevealArea(
        center: WorldCell,
        kind: RevealCell.Kind,
        seenMs: Long = System.currentTimeMillis(),
        radiusCells: Int = 1,
    ) {
        db.writableDatabase.transaction {
            for (dx in -radiusCells..radiusCells) {
                for (dy in -radiusCells..radiusCells) {
                    if (dx * dx + dy * dy <= radiusCells * radiusCells) {
                        recordRevealCell(WorldCell(center.x + dx, center.y + dy), kind, seenMs)
                    }
                }
            }
        }
    }

    fun recordRevealCells(
        cells: Collection<WorldCell>,
        kind: RevealCell.Kind,
        seenMs: Long = System.currentTimeMillis(),
    ) {
        if (cells.isEmpty()) return
        db.writableDatabase.transaction {
            cells.forEach { cell -> recordRevealCell(cell, kind, seenMs) }
        }
    }

    private fun SQLiteDatabase.recordCell(cell: WorldCell, seenMs: Long) {
        val updated = update(
            "visited_cells",
            ContentValues().apply {
                put("last_seen_ms", seenMs)
                put("samples", rawSamples(cell) + 1)
            },
            "cell_x = ? AND cell_y = ?",
            arrayOf(cell.x.toString(), cell.y.toString()),
        )
        if (updated == 0) {
            insert(
                "visited_cells",
                null,
                ContentValues().apply {
                    put("cell_x", cell.x)
                    put("cell_y", cell.y)
                    put("first_seen_ms", seenMs)
                    put("last_seen_ms", seenMs)
                    put("samples", 1)
                },
            )
        }
    }

    private fun SQLiteDatabase.recordRevealCell(cell: WorldCell, kind: RevealCell.Kind, seenMs: Long) {
        val updated = update(
            "reveal_cells",
            ContentValues().apply {
                put("last_seen_ms", seenMs)
                put("samples", rawRevealSamples(cell, kind) + 1)
            },
            "cell_x = ? AND cell_y = ? AND kind = ?",
            arrayOf(cell.x.toString(), cell.y.toString(), kind.id.toString()),
        )
        if (updated == 0) {
            insert(
                "reveal_cells",
                null,
                ContentValues().apply {
                    put("cell_x", cell.x)
                    put("cell_y", cell.y)
                    put("kind", kind.id)
                    put("first_seen_ms", seenMs)
                    put("last_seen_ms", seenMs)
                    put("samples", 1)
                },
            )
        }
    }

    fun summary(): WorldSummary {
        db.readableDatabase.rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(samples), 0), MAX(last_seen_ms) FROM visited_cells",
            emptyArray(),
        ).use { cursor ->
            cursor.moveToFirst()
            val lastSeen = if (cursor.isNull(2)) null else cursor.getLong(2)
            return WorldSummary(
                cells = cursor.getInt(0),
                samples = cursor.getInt(1),
                lastSeenMs = lastSeen,
            )
        }
    }

    fun revealedCellCountSince(sinceMs: Long): Int {
        db.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM reveal_cells WHERE first_seen_ms >= ?",
            arrayOf(sinceMs.toString()),
        ).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    fun coreRevealedCellCountSince(sinceMs: Long): Int {
        db.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM reveal_cells WHERE kind = ? AND first_seen_ms >= ?",
            arrayOf(RevealCell.Kind.Core.id.toString(), sinceMs.toString()),
        ).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    fun lastRevealSeenMs(): Long? {
        db.readableDatabase.rawQuery(
            "SELECT MAX(last_seen_ms) FROM reveal_cells",
            emptyArray(),
        ).use { cursor ->
            cursor.moveToFirst()
            return if (cursor.isNull(0)) null else cursor.getLong(0)
        }
    }

    fun recentCells(limit: Int = 800): List<VisitedCell> = cells(limit)

    fun recentRevealCells(limit: Int = 4_000): List<RevealCell> = revealCells(limit)

    fun allVisitedCells(): List<VisitedCell> = cells(Int.MAX_VALUE)

    fun allRevealCells(): List<RevealCell> = revealCells(Int.MAX_VALUE)

    fun importCells(visitedCells: List<VisitedCell>, revealCells: List<RevealCell>) {
        db.writableDatabase.transaction {
            visitedCells.forEach { cell -> importCell(cell) }
            revealCells.forEach { cell -> importRevealCell(cell) }
        }
    }

    fun clearAll() {
        db.writableDatabase.transaction {
            delete("visit_counts", null, null)
            delete("visit_zones", null, null)
            delete("visited_cells", null, null)
            delete("reveal_cells", null, null)
        }
    }

    private fun cells(limit: Int, window: MapWindow? = null): List<VisitedCell> {
        val (where, args) = windowClause(window, WorldCell.DefaultCellSizeMeters)
        db.readableDatabase.rawQuery(
            """
            SELECT cell_x, cell_y, first_seen_ms, last_seen_ms, samples
            FROM visited_cells
            $where
            ORDER BY last_seen_ms DESC
            LIMIT ?
            """.trimIndent(),
            args + limit.coerceAtLeast(1).toString(),
        ).use { cursor ->
            val cells = mutableListOf<VisitedCell>()
            while (cursor.moveToNext()) {
                cells += VisitedCell(
                    x = cursor.getInt(0),
                    y = cursor.getInt(1),
                    firstSeenMs = cursor.getLong(2),
                    lastSeenMs = cursor.getLong(3),
                    samples = cursor.getInt(4),
                )
            }
            return cells
        }
    }

    private fun revealCells(limit: Int, window: MapWindow? = null): List<RevealCell> {
        val (where, args) = windowClause(window, WorldCell.RevealCellSizeMeters)
        db.readableDatabase.rawQuery(
            """
            SELECT cell_x, cell_y, kind, first_seen_ms, last_seen_ms, samples
            FROM reveal_cells
            $where
            ORDER BY last_seen_ms DESC
            LIMIT ?
            """.trimIndent(),
            args + limit.coerceAtLeast(1).toString(),
        ).use { cursor ->
            val cells = mutableListOf<RevealCell>()
            while (cursor.moveToNext()) {
                cells += RevealCell(
                    x = cursor.getInt(0),
                    y = cursor.getInt(1),
                    kind = RevealCell.Kind.fromId(cursor.getInt(2)),
                    firstSeenMs = cursor.getLong(3),
                    lastSeenMs = cursor.getLong(4),
                    samples = cursor.getInt(5),
                )
            }
            return cells
        }
    }

    private fun SQLiteDatabase.rawSamples(cell: WorldCell): Int {
        rawQuery(
            "SELECT samples FROM visited_cells WHERE cell_x = ? AND cell_y = ?",
            arrayOf(cell.x.toString(), cell.y.toString()),
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun SQLiteDatabase.rawRevealSamples(cell: WorldCell, kind: RevealCell.Kind): Int {
        rawQuery(
            "SELECT samples FROM reveal_cells WHERE cell_x = ? AND cell_y = ? AND kind = ?",
            arrayOf(cell.x.toString(), cell.y.toString(), kind.id.toString()),
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun SQLiteDatabase.importCell(cell: VisitedCell) {
        rawQuery(
            "SELECT first_seen_ms, last_seen_ms, samples FROM visited_cells WHERE cell_x = ? AND cell_y = ?",
            arrayOf(cell.x.toString(), cell.y.toString()),
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                update(
                    "visited_cells",
                    ContentValues().apply {
                        put("first_seen_ms", minOf(cursor.getLong(0), cell.firstSeenMs))
                        put("last_seen_ms", maxOf(cursor.getLong(1), cell.lastSeenMs))
                        put("samples", maxOf(cursor.getInt(2), cell.samples))
                    },
                    "cell_x = ? AND cell_y = ?",
                    arrayOf(cell.x.toString(), cell.y.toString()),
                )
                return
            }
        }
        insert(
            "visited_cells",
            null,
            ContentValues().apply {
                put("cell_x", cell.x)
                put("cell_y", cell.y)
                put("first_seen_ms", cell.firstSeenMs)
                put("last_seen_ms", cell.lastSeenMs)
                put("samples", cell.samples)
            },
        )
    }

    private fun SQLiteDatabase.importRevealCell(cell: RevealCell) {
        rawQuery(
            "SELECT first_seen_ms, last_seen_ms, samples FROM reveal_cells WHERE cell_x = ? AND cell_y = ? AND kind = ?",
            arrayOf(cell.x.toString(), cell.y.toString(), cell.kind.id.toString()),
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                update(
                    "reveal_cells",
                    ContentValues().apply {
                        put("first_seen_ms", minOf(cursor.getLong(0), cell.firstSeenMs))
                        put("last_seen_ms", maxOf(cursor.getLong(1), cell.lastSeenMs))
                        put("samples", maxOf(cursor.getInt(2), cell.samples))
                    },
                    "cell_x = ? AND cell_y = ? AND kind = ?",
                    arrayOf(cell.x.toString(), cell.y.toString(), cell.kind.id.toString()),
                )
                return
            }
        }
        insert(
            "reveal_cells",
            null,
            ContentValues().apply {
                put("cell_x", cell.x)
                put("cell_y", cell.y)
                put("kind", cell.kind.id)
                put("first_seen_ms", cell.firstSeenMs)
                put("last_seen_ms", cell.lastSeenMs)
                put("samples", cell.samples)
            },
        )
    }
}

private class VisitDatabase(context: Context) : SQLiteOpenHelper(
    context,
    "ovayuvam-world.db",
    null,
    3,
) {
    override fun onCreate(db: SQLiteDatabase) {
        createVisitedCells(db)
        createRevealCells(db)
        createVisitCounts(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createRevealCells(db)
        if (oldVersion < 3) createVisitCounts(db)
    }

    private fun createVisitCounts(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE visit_zones (
            id INTEGER PRIMARY KEY, latitude REAL NOT NULL, longitude REAL NOT NULL, radius REAL NOT NULL,
            epoch INTEGER NOT NULL DEFAULT 0, armed INTEGER NOT NULL DEFAULT 0,
            away_since INTEGER NOT NULL DEFAULT 0, candidate_since INTEGER NOT NULL DEFAULT 0,
            candidate_fixes INTEGER NOT NULL DEFAULT 0, last_elapsed INTEGER NOT NULL DEFAULT 0,
            boot_id INTEGER NOT NULL DEFAULT -1)""")
        db.execSQL("""CREATE TABLE visit_counts (
            cell_x INTEGER NOT NULL,cell_y INTEGER NOT NULL,zone_id INTEGER NOT NULL,
            visits INTEGER NOT NULL,first_visit_ms INTEGER NOT NULL,last_visit_ms INTEGER NOT NULL,
            counted_epoch INTEGER NOT NULL,PRIMARY KEY(cell_x,cell_y))""")
        db.execSQL("CREATE INDEX visit_counts_zone ON visit_counts(zone_id)")
    }

    private fun createVisitedCells(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE visited_cells (
                cell_x INTEGER NOT NULL,
                cell_y INTEGER NOT NULL,
                first_seen_ms INTEGER NOT NULL,
                last_seen_ms INTEGER NOT NULL,
                samples INTEGER NOT NULL,
                PRIMARY KEY (cell_x, cell_y)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX visited_cells_last_seen ON visited_cells(last_seen_ms DESC)")
    }

    private fun createRevealCells(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS reveal_cells (
                cell_x INTEGER NOT NULL,
                cell_y INTEGER NOT NULL,
                kind INTEGER NOT NULL,
                first_seen_ms INTEGER NOT NULL,
                last_seen_ms INTEGER NOT NULL,
                samples INTEGER NOT NULL,
                PRIMARY KEY (cell_x, cell_y, kind)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS reveal_cells_last_seen ON reveal_cells(last_seen_ms DESC)")
    }
}

private inline fun SQLiteDatabase.transaction(block: SQLiteDatabase.() -> Unit) {
    beginTransaction()
    try {
        block()
        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}
