package tr.ovayuva.ovayuvam.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
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

    fun recentCells(limit: Int = 800): List<VisitedCell> = cells(limit)

    fun clearAll() {
        db.writableDatabase.transaction {
            delete("visited_cells", null, null)
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

    private fun SQLiteDatabase.rawSamples(cell: WorldCell): Int {
        rawQuery(
            "SELECT samples FROM visited_cells WHERE cell_x = ? AND cell_y = ?",
            arrayOf(cell.x.toString(), cell.y.toString()),
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }
}

private class VisitDatabase(context: Context) : SQLiteOpenHelper(
    context,
    "ovayuvam-world.db",
    null,
    1,
) {
    override fun onCreate(db: SQLiteDatabase) {
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

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
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
