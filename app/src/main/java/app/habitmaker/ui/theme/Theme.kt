package app.habitmaker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// One light theme only (see idea.md). A fixed scheme rather than dynamic color, so the app looks
// the same on every device and the donut/done colors stay predictable.
private val LightColors = lightColorScheme(
    primary = Color(0xFF00796B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00251F),
    secondary = Color(0xFF4A635E),
    secondaryContainer = Color(0xFFCCE8E2),
    onSecondaryContainer = Color(0xFF06201B),
    tertiary = Color(0xFFE08A00),
    background = Color(0xFFFAF8F5),
    onBackground = Color(0xFF1B1C1A),
    surface = Color(0xFFFAF8F5),
    onSurface = Color(0xFF1B1C1A),
    surfaceVariant = Color(0xFFE7E3DC),
    onSurfaceVariant = Color(0xFF5B5F5C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F2EE),
    surfaceContainer = Color(0xFFF0EDE8),
    surfaceContainerHigh = Color(0xFFEAE7E2),
    surfaceContainerHighest = Color(0xFFE4E1DC),
    outline = Color(0xFF8A8F8B),
    outlineVariant = Color(0xFFD5D2CC),
    error = Color(0xFFBA1A1A),
)

// Platform default font only: no bundled font means no font-loading cost on the first frame.
private val AppTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
)

@Composable
fun HabitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, typography = AppTypography, content = content)
}
