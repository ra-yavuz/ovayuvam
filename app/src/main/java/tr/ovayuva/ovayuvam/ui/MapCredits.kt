package tr.ovayuva.ovayuvam.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import kotlinx.coroutines.delay
import tr.ovayuva.ovayuvam.R
import tr.ovayuva.ovayuvam.ui.theme.Ink
import tr.ovayuva.ovayuvam.ui.theme.sketchSurface

@Composable
fun MapCredits(modifier: Modifier = Modifier, forceExpanded: Boolean = false) {
    var expanded by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(expanded, interaction, forceExpanded) {
        if (expanded && !forceExpanded) {
            delay(accessibility?.calculateRecommendedTimeoutMillis(8_000, containsText = true, containsControls = true) ?: 8_000)
            expanded = false
        }
    }
    Surface(color = Color.Transparent, modifier = modifier.sketchSurface(Ink.copy(alpha = 0.88f), Color.White.copy(alpha = 0.42f), seed = 302)) {
        if (expanded || forceExpanded) Text(buildAnnotatedString {
            listOf("OpenFreeMap" to "https://openfreemap.org/", "© OpenMapTiles" to "https://openmaptiles.org/",
                "© OpenStreetMap" to "https://www.openstreetmap.org/copyright").forEachIndexed { index, (label, link) ->
                if (index > 0) append(" / ")
                withLink(LinkAnnotation.Url(link, TextLinkStyles(style = SpanStyle(color = Color.White)))) {
                    append(label)
                }
            }
        }, Modifier.padding(horizontal = 10.dp, vertical = 8.dp), color = Color.White, style = MaterialTheme.typography.bodySmall)
        else IconButton(onClick = { expanded = true; interaction++ }) {
            Icon(Icons.Outlined.Map, stringResource(R.string.map_credits), tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}
