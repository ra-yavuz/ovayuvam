package tr.ovayuva.ovayuvam.location

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyProgress(
    val dayKey: String,
    val distanceMeters: Float,
    val lastProgressMs: Long?,
)

class DailyProgressStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("daily-progress", Context.MODE_PRIVATE)

    fun addProgress(nowMs: Long, distanceMeters: Float) {
        val day = dayKey(nowMs)
        val current = snapshot(nowMs)
        val nextDistance = if (current.dayKey == day) {
            current.distanceMeters + distanceMeters.coerceAtLeast(0f)
        } else {
            distanceMeters.coerceAtLeast(0f)
        }
        preferences.edit()
            .putString(KEY_DAY, day)
            .putFloat(KEY_DISTANCE, nextDistance)
            .putLong(KEY_LAST_PROGRESS, nowMs)
            .apply()
    }

    fun snapshot(nowMs: Long): DailyProgress {
        val today = dayKey(nowMs)
        val storedDay = preferences.getString(KEY_DAY, today) ?: today
        val sameDay = storedDay == today
        val lastProgress = preferences.getLong(KEY_LAST_PROGRESS, 0L).takeIf { it > 0L }
        return DailyProgress(
            dayKey = today,
            distanceMeters = if (sameDay) preferences.getFloat(KEY_DISTANCE, 0f) else 0f,
            lastProgressMs = lastProgress,
        )
    }

    private fun dayKey(nowMs: Long): String =
        LocalDate.ofInstant(Instant.ofEpochMilli(nowMs), ZoneId.systemDefault()).toString()

    private companion object {
        const val KEY_DAY = "day"
        const val KEY_DISTANCE = "distanceMeters"
        const val KEY_LAST_PROGRESS = "lastProgressMs"
    }
}
