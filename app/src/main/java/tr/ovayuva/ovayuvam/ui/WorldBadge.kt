package tr.ovayuva.ovayuvam.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import tr.ovayuva.ovayuvam.R
import tr.ovayuva.ovayuvam.ui.theme.sketchSurface
import java.text.NumberFormat

object BadgeTiming {
    const val IntervalMs = 180_000L
    const val PreviewMs = 8_000L
}

@Composable
fun WorldBadge(total: Double, today: Double, pinned: Boolean, onPinned: (Boolean) -> Unit,
    previewsEnabled: Boolean, modifier: Modifier = Modifier) {
    var preview by remember { mutableStateOf(false) }
    var interaction by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(pinned, previewsEnabled, lifecycle, interaction) {
        preview = false
        if (pinned || !previewsEnabled) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {
                while (true) {
                    delay(BadgeTiming.IntervalMs)
                    preview = true
                    delay(BadgeTiming.PreviewMs)
                    preview = false
                }
            } finally { preview = false }
        }
    }
    val showArea = pinned || preview
    val rotation by animateFloatAsState(if (showArea) 180f else 0f, tween(420), label = "world badge flip")
    val density = LocalDensity.current
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val totalText = stringResource(R.string.area_total, format.format(total.toLong()))
    val todayText = stringResource(R.string.area_today, format.format(today.toLong()))
    val action = stringResource(if (showArea) R.string.show_logo else R.string.show_area)
    val areaVisible = rotation > 90
    fun face(visible: Boolean) = Modifier.graphicsLayer { alpha = if (visible) 1f else 0f }
        .then(if (visible) Modifier else Modifier.clearAndSetSemantics { })
    // Measure both faces so flipping stays stable, while large text can wrap and grow.
    Surface(onClick = { val next = !showArea; preview = false; interaction++; onPinned(next) }, color = Color.Transparent,
        modifier = modifier.widthIn(min = 168.dp, max = 220.dp).width(IntrinsicSize.Max)
            .heightIn(min = 48.dp)
            .semantics { contentDescription = action }
            .graphicsLayer { rotationY = rotation; cameraDistance = 16 * density.density }
            .sketchSurface(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), MaterialTheme.colorScheme.outline, seed = 17)) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            .graphicsLayer { rotationY = if (areaVisible) 180f else 0f }, contentAlignment = Alignment.Center) {
            Column(face(areaVisible), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(totalText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(todayText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(face(!areaVisible), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.launcher_map_foreground), null, Modifier.size(26.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
                Spacer(Modifier.width(6.dp))
                Text("ovayuvam", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
