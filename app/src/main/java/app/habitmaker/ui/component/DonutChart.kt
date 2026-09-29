package app.habitmaker.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

/** Donut of [done] / [total], with the two numbers stacked in the middle, split by a line. */
@Composable
fun DonutChart(done: Int, total: Int, modifier: Modifier = Modifier, size: Dp = 96.dp, stroke: Dp = 10.dp) {
    val target = if (total == 0) 0f else done.toFloat() / total
    val fraction by animateFloatAsState(target, tween(350), label = "donut")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val fill = MaterialTheme.colorScheme.primary
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val arcSize = Size(this.size.width - w, this.size.height - w)
            val topLeft = Offset(w / 2, w / 2)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(w))
            if (fraction > 0f) {
                drawArc(fill, -90f, 360f * fraction, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(w, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$done", style = MaterialTheme.typography.titleLarge.scaled(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            // Tops the smaller numbers' gaps back up to 0.9 of the full-size ones.
            Box(Modifier.padding(top = 0.4.dp, bottom = 0.8.dp).width(size * 0.32f).height(1.5.dp).background(MaterialTheme.colorScheme.outline))
            Text("$total", style = MaterialTheme.typography.titleMedium.scaled(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The numbers at 0.8 of the theme size; the line height (when the theme sets one) shrinks with them. */
private fun TextStyle.scaled() = copy(
    fontSize = if (fontSize.isSpecified) fontSize * 0.8f else fontSize,
    lineHeight = if (lineHeight.isSpecified) lineHeight * 0.8f else lineHeight,
)
