package tr.ovayuva.ovayuvam

import android.graphics.Color
import kotlinx.coroutines.runBlocking
import tr.ovayuva.ovayuvam.domain.*
import tr.ovayuva.ovayuvam.map.*
import kotlin.math.abs

fun checkDiscoveries() = runBlocking {
    val day = DiscoveryDay(1_000, 100_000)
    val cell = WorldCell.fromLocation(41.0136, 28.981, 20.0)
    val current = RevealCell(cell.x, cell.y, RevealCell.Kind.Core, 2_000, 2_000, 8)
    val center = MercatorPoint((cell.x + 0.5) * 20, (cell.y + 0.5) * 20)
    val view = FogViewport(center, 0.5, 300, 300)
    fun alpha(frame: FogTileFrame): Int {
        val entry = frame.images.entries.first { (key, _) -> center.x >= key.left && center.x < key.left + key.span && center.y <= key.top && center.y > key.top - key.span }
        val bitmap = entry.value.fresh ?: return 0
        return Color.alpha(bitmap.getPixel(((center.x - entry.key.left) / entry.key.span * 256).toInt().coerceIn(0,255),
            ((entry.key.top - center.y) / entry.key.span * 256).toInt().coerceIn(0,255)))
    }
    val cache = FogTileCache()
    cache.update(emptyList(), listOf(current), emptyList(), day)
    val fresh = cache.frame(view)
    check(alpha(fresh) > 240 && fresh.day == day)
    val renders = cache.renderedTiles
    cache.frame(view)
    check(cache.renderedTiles == renders)
    // A revisit today still has yesterday's first discovery date.
    cache.update(emptyList(), listOf(current.copy(firstSeenMs = 900)), emptyList(), day)
    check(alpha(cache.frame(view)) == 0)
    cache.update(emptyList(), listOf(current, current.copy(kind = RevealCell.Kind.Road, firstSeenMs = 900)), emptyList(), day)
    check(alpha(cache.frame(view)) < 10)
    cache.update(emptyList(), listOf(current), emptyList(), DiscoveryDay(day.endMs, 200_000))
    check(alpha(cache.frame(view)) == 0)
    cache.update(emptyList(), listOf(current), emptyList())
    check(cache.frame(view).images.values.all { it.fresh == null })

    val area = RevealedArea()
    check(area.update(emptyList(), emptyList()) == 0.0)
    val first = area.update(emptyList(), listOf(current))
    check(first in 1_400.0..2_200.0) { "Unexpected brush area $first" }
    val areaRenders = area.renderedTiles
    check(area.update(emptyList(), listOf(current.copy(samples = 100, lastSeenMs = 90_000))) == first)
    check(area.renderedTiles == areaRenders)
    val duplicate = area.update(emptyList(), listOf(current, current, current.copy(kind = RevealCell.Kind.Road)))
    check(abs(duplicate - first) < 5) { "Overlapping core/road counted twice: $first vs $duplicate" }
    val distant = current.copy(x = current.x + 30)
    val twice = area.update(emptyList(), listOf(current, distant))
    check(twice / first in 1.98..2.02)
    val overlap = area.update(emptyList(), listOf(current, current.copy(x = current.x + 1)))
    check(overlap > first && overlap < twice)
    check(abs(area.update(emptyList(), listOf(current)) - first) < 0.01)
    val high = WorldCell.fromLocation(70.0, 28.981, 20.0)
    val highArea = area.update(emptyList(), listOf(current.copy(x = high.x, y = high.y)))
    check(highArea / first in 0.97..1.03) { "Ground area changed with latitude: $first vs $highArea" }
    val legacy = VisitedCell(cell.x, cell.y, 500, 800, 1)
    check(area.update(listOf(legacy), emptyList()) > 0)
    check(area.update(emptyList(), emptyList()) < 0.01)
    check(cache.bytes <= 32 * 1024 * 1024)
    val totalCounter = RevealedArea()
    val oldCounter = RevealedArea()
    val old = current.copy(x = current.x + 1, firstSeenMs = 900)
    val world = listOf(old, current)
    val total = totalCounter.update(emptyList(), world)
    val oldOnly = oldCounter.update(emptyList(), world, day)
    check(total - oldOnly in 1.0..first) { "Today counted overlapping old ground" }
    val beforeRenders = oldCounter.renderedTiles
    check(oldCounter.update(emptyList(), world.map { it.copy(samples = 800, lastSeenMs = 99_999) }, day) == oldOnly)
    check(oldCounter.renderedTiles == beforeRenders)
    check(abs(oldCounter.update(emptyList(), world, DiscoveryDay(100_000, 200_000)) - total) < 0.01)
    check(oldCounter.update(emptyList(), listOf(current.copy(firstSeenMs = 0)), day) > 0) { "Unknown dates counted as today" }
    check(oldCounter.update(listOf(legacy.copy(firstSeenMs = 2_000)), emptyList(), day) == 0.0)
}
