package tr.ovayuva.ovayuvam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tr.ovayuva.ovayuvam.R

val Paper = Color(0xFFF7F9F7)
val PaperDeep = Color(0xFFE3ECE4)
val Ink = Color(0xFF242A28)
val Forest = Color(0xFF285B45)
val Mint = Color(0xFFD8EBDD)
val Fog = Color(0x9917211D)
val Revealed = Color(0xFFEAF4EC)

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = Color(0xFF173A28),
    secondary = Color(0xFF176D75),
    onSecondary = Color.White,
    tertiary = Color(0xFF9E3956),
    background = Paper,
    onBackground = Ink,
    surface = Color(0xFFFCFDFC),
    onSurface = Ink,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = Color(0xFF424C46),
    outline = Color(0xFF69786F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA4D8B6),
    onPrimary = Color(0xFF123525),
    primaryContainer = Color(0xFF284D39),
    onPrimaryContainer = Mint,
    background = Color(0xFF171C1B),
    onBackground = Color(0xFFE6ECE8),
    surface = Color(0xFF202725),
    onSurface = Color(0xFFE6ECE8),
    surfaceVariant = Color(0xFF303B36),
    onSurfaceVariant = Color(0xFFC7D2CB),
    outline = Color(0xFF93A39A),
)

private val HandBody = FontFamily(Font(R.font.patrick_hand))
private val HandDisplay = FontFamily(Font(R.font.caveat_variable))

private fun typography(): Typography {
    val base = Typography()
    fun TextStyle.hand(size: Int? = null) = copy(
        fontFamily = HandBody,
        fontSize = size?.sp ?: fontSize,
        lineHeight = size?.let { (it + 6).sp } ?: lineHeight,
        letterSpacing = 0.sp,
    )
    fun TextStyle.display(size: Int? = null) = copy(
        fontFamily = HandDisplay,
        fontSize = size?.sp ?: fontSize,
        lineHeight = size?.let { (it + 6).sp } ?: lineHeight,
        letterSpacing = 0.sp,
    )
    return Typography(
        headlineLarge = base.headlineLarge.display(38),
        headlineMedium = base.headlineMedium.display(34),
        headlineSmall = base.headlineSmall.display(30),
        titleLarge = base.titleLarge.display(28),
        titleMedium = base.titleMedium.hand(19),
        titleSmall = base.titleSmall.hand(17),
        bodyLarge = base.bodyLarge.copy(letterSpacing = 0.sp),
        bodyMedium = base.bodyMedium.copy(letterSpacing = 0.sp),
        bodySmall = base.bodySmall.copy(letterSpacing = 0.sp),
        labelLarge = base.labelLarge.copy(letterSpacing = 0.sp),
        labelMedium = base.labelMedium.copy(letterSpacing = 0.sp),
        labelSmall = base.labelSmall.copy(letterSpacing = 0.sp),
    )
}

@Composable
fun OvayuvamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = typography(),
        shapes = Shapes(),
        content = content,
    )
}
