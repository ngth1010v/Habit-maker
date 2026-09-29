package app.habitmaker.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.habitmaker.ui.LocalAppContainer

/** A Phosphor icon by name; empty until the icon asset has loaded (a few ms after launch). */
@Composable
fun PhIcon(name: String, tint: Color, modifier: Modifier = Modifier) {
    val icons = LocalAppContainer.current.icons
    // Reading `all` subscribes to the asset load, so this redraws once it lands.
    if (icons.all.isEmpty()) {
        Box(modifier)
        return
    }
    val vector = icons.vector(name) ?: return Box(modifier)
    Icon(vector, contentDescription = null, tint = tint, modifier = modifier)
}

/** The icon in a circle: glyph in [color], background a light tint of it. */
@Composable
fun CircleIcon(name: String, color: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier.size(size).background(color.light(), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        PhIcon(name, tint = color, modifier = Modifier.padding(size * 0.22f).size(size * 0.56f))
    }
}

/** Neutral variant, for rewards (which have no color). */
@Composable
fun NeutralCircleIcon(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier.size(size).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        PhIcon(name, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(size * 0.56f))
    }
}

fun Color.light(fraction: Float = 0.82f): Color = lerp(this, Color.White, fraction)
