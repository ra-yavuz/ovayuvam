package tr.ovayuva.ovayuvam.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import tr.ovayuva.ovayuvam.domain.VisitedCell
import tr.ovayuva.ovayuvam.domain.WorldCell
import tr.ovayuva.ovayuvam.domain.WorldSummary

class VisitRepository(context: Context) {
    private val db = VisitDatabase(context.applicationContext)

    fun record(cell: WorldCell, seenMs: Long = System.currentTimeMillis()) {
        db.writableDatabase.transaction {
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

    fun exportJson(): String {
        val rows = cells(Int.MAX_VALUE)
        val payload = JSONObject()
            .put("schema", Schema)
            .put("exportedAtMs", System.currentTimeMillis())
            .put("cellCount", rows.size)
        val items = JSONArray()
        rows.forEach { cell ->
            items.put(
                JSONObject()
                    .put("x", cell.x)
                    .put("y", cell.y)
                    .put("firstSeenMs", cell.firstSeenMs)
                    .put("lastSeenMs", cell.lastSeenMs)
                    .put("samples", cell.samples),
            )
        }
        payload.put("cells", items)
        return payload.toString(2)
    }

    fun replaceFromJson(json: String): Int {
        val payload = JSONObject(json)
        require(payload.getString("schema") == Schema) { "Unsupported export schema" }
        val items = payload.getJSONArray("cells")
        val parsed = buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val firstSeen = item.getLong("firstSeenMs")
                val lastSeen = item.getLong("lastSeenMs")
                val samples = item.getInt("samples")
                require(samples > 0) { "Samples must be positive" }
                require(lastSeen >= firstSeen) { "Last seen must not be before first seen" }
                add(
                    VisitedCell(
                        x = item.getInt("x"),
                        y = item.getInt("y"),
                        firstSeenMs = firstSeen,
                        lastSeenMs = lastSeen,
                        samples = samples,
                    ),
                )
            }
        }
        db.writableDatabase.transaction {
            delete("visited_cells", null, null)
            parsed.forEach { cell ->
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
        }
        return parsed.size
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

    companion object {
        const val Schema = "ovayuvam.visited_cells.v1"
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
