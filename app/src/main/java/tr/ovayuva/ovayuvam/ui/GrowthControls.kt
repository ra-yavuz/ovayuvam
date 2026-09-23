package tr.ovayuva.ovayuvam.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import tr.ovayuva.ovayuvam.map.GrowthPeriod
import tr.ovayuva.ovayuvam.ui.theme.sketchSurface
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthControls(
    period: GrowthPeriod,
    onPeriodChange: (GrowthPeriod) -> Unit,
    fraction: Float,
    onSeek: (Float) -> Unit,
    dateMs: Long,
    playing: Boolean,
    hasDiscoveries: Boolean,
    onPlayPause: () -> Unit,
    onRestart: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val compact = LocalWindowInfo.current.containerSize.height / density.density < 480
    Column(modifier.sketchSurface(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        MaterialTheme.colorScheme.outline, seed = 711).padding(horizontal = 10.dp, vertical = 6.dp)) {
        if (!compact) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (hasDiscoveries) "Your world growing" else "No new discoveries",
                Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            ReplayAction(Icons.Default.Close, "Close replay", onClose)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = if (maxWidth < 420.dp && density.fontScale > 1.2f) 2 else 4
            Column {
                GrowthPeriod.entries.chunked(columns).forEach { options ->
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        options.forEachIndexed { index, option ->
                            SegmentedButton(selected = period == option, onClick = { onPeriodChange(option) },
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                shape = SegmentedButtonDefaults.itemShape(index, options.size), icon = {}) {
                                Text(option.label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
        Slider(value = fraction, onValueChange = onSeek,
            colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Replay date" })
        Row(verticalAlignment = Alignment.CenterVertically) {
            ReplayAction(Icons.Default.Replay, "Restart replay", onRestart, hasDiscoveries)
            ReplayAction(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (playing) "Pause replay" else "Play replay", onPlayPause, hasDiscoveries)
            Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(dateMs)),
                Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
            if (compact) ReplayAction(Icons.Default.Close, "Close replay", onClose)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReplayAction(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    TooltipBox(positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } }, state = rememberTooltipState()) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
            Icon(icon, contentDescription = label)
        }
    }
}
