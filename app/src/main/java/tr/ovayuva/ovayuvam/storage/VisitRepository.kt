package tr.ovayuva.ovayuvam.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import tr.ovayuva.ovayuvam.domain.RevealCell
import tr.ovayuva.ovayuvam.domain.VisitedCell
import tr.ovayuva.ovayuvam.domain.WorldCell
import tr.ovayuva.ovayuvam.domain.WorldSummary

class VisitRepository(context: Context) {
    private val db = VisitDatabase(context.applicationContext)

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
            delete("visited_cells", null, null)
            delete("reveal_cells", null, null)
        }
    }

    private fun cells(limit: Int): List<VisitedCell> {
        db.readableDatabase.rawQuery(
            """
            SELECT cell_x, cell_y, first_seen_ms, last_seen_ms, samples
            FROM visited_cells
            ORDER BY last_seen_ms DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(limit.coerceAtLeast(1).toString()),
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

    private fun revealCells(limit: Int): List<RevealCell> {
        db.readableDatabase.rawQuery(
            """
            SELECT cell_x, cell_y, kind, first_seen_ms, last_seen_ms, samples
            FROM reveal_cells
            ORDER BY last_seen_ms DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(limit.coerceAtLeast(1).toString()),
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
    2,
) {
    override fun onCreate(db: SQLiteDatabase) {
        createVisitedCells(db)
        createRevealCells(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createRevealCells(db)
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
