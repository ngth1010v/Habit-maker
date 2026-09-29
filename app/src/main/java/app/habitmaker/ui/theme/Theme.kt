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
    // White page; cards, rows, the day panel and editor fields sit on it in light gray
    // (surfaceContainerLowest is what those components use).
    background = Color.White,
    onBackground = Color(0xFF1B1C1A),
    surface = Color.White,
    onSurface = Color(0xFF1B1C1A),
    surfaceVariant = Color(0xFFE4E4E4),
    onSurfaceVariant = Color(0xFF5B5F5C),
    surfaceContainerLowest = Color(0xFFF3F3F3),
    surfaceContainerLow = Color(0xFFF5F5F5),
    surfaceContainer = Color(0xFFEFEFEF),
    surfaceContainerHigh = Color(0xFFE9E9E9),
    surfaceContainerHighest = Color(0xFFE3E3E3),
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
