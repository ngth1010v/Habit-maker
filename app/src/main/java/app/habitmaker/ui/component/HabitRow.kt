package app.habitmaker.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import app.habitmaker.R
import app.habitmaker.domain.Habit
import app.habitmaker.domain.RewardEngine

val CardShape = RoundedCornerShape(14.dp)

/** Minimum height and padding of a habit / reward row. */
val RowMinHeight = 58.dp
val RowPaddingH = 12.1.dp
val RowPaddingV = 4.84.dp

private val BarsPullUp = 11.75.dp

/** How far the note is pulled up under the name: halves their gap. */
private val NotePullUp = 4.15.dp

/** The name, with the note (when there is one) under it; text at 0.9 of the theme size. */
@Composable
fun RowText(name: String, note: String, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    Column(modifier) {
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge.scaled(0.9f),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dimmed) 0.6f else 1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (note.isNotBlank()) {
            Text(
                note,
                style = MaterialTheme.typography.bodySmall.scaled(0.9f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.pullUp(NotePullUp),
            )
        }
    }
}

/** [this] style at [factor] of its size; the line height (when set) scales with it. */
fun TextStyle.scaled(factor: Float) = copy(
    fontSize = if (fontSize.isSpecified) fontSize * factor else fontSize,
    lineHeight = if (lineHeight.isSpecified) lineHeight * factor else lineHeight,
)

/** Moves the content up by [by] and drops that much from its measured height. */
private fun Modifier.pullUp(by: Dp) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val cut = by.roundToPx()
    layout(placeable.width, (placeable.height - cut).coerceAtLeast(0)) { placeable.place(0, -cut) }
}

/**
 * The habit row shared by Home and the Habit list: icon on the left, the name beside it, the note
 * under the name (when there is one), and [trailing] on the right.
 */
@Composable
fun HabitRow(
    icon: String,
    color: Int,
    name: String,
    note: String,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    bars: List<RewardBar> = emptyList(),
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val tint = Color(color).let { if (dimmed) it.copy(alpha = 0.55f) else it }
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, CardShape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        Row(
            Modifier
                .heightIn(min = RowMinHeight)
                .padding(start = RowPaddingH, end = 4.84.dp, top = RowPaddingV, bottom = RowPaddingV),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleIcon(icon, tint, size = 32.dp)
            RowText(name, note, Modifier.weight(1f), dimmed)
            trailing()
        }
        if (bars.isNotEmpty()) {
            // Pulled up into the row's bottom slack, leaving a small gap under the text.
            Column(
                Modifier.pullUp(BarsPullUp).padding(start = RowPaddingH, end = RowPaddingH, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(1.8.dp),
            ) {
                bars.forEach { ProgressLine(it, tint) }
            }
        }
    }
}

/** One reward's progress in a habit row: [done] of the [needed] days toward [name]. */
data class RewardBar(val name: String, val done: Int, val needed: Int)

/** The habit's reward bars for the periods around [day]; [rewardNames] by reward id. */
fun Habit.rewardBars(doneDays: Set<Long>, day: Long, rewardNames: Map<Long, String>): List<RewardBar> =
    RewardEngine.progress(this, doneDays, day).mapNotNull { p ->
        rewardNames[p.rewardId]?.let { RewardBar(it, p.done, p.needed) }
    }

@Composable
private fun ProgressLine(bar: RewardBar, color: Color) {
    val labelStyle = MaterialTheme.typography.labelSmall.scaled(0.8f)
    val fraction by animateFloatAsState((bar.done.toFloat() / bar.needed).coerceIn(0f, 1f), tween(300), label = "bar")
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                bar.name,
                style = labelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text("${bar.done}/${bar.needed}", style = labelStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            Modifier
                .padding(top = 2.dp)
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(color, CircleShape),
            )
        }
    }
}

/**
 * The done / undone toggle: a tick for a habit still to do, an x to undo a done one.
 * Disabled on future days.
 */
@Composable
fun DoneToggle(done: Boolean, enabled: Boolean, color: Int, contentDescription: String, onClick: () -> Unit) {
    val tint = Color(color)
    Box(
        Modifier.size(36.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    when {
                        !enabled -> MaterialTheme.colorScheme.surfaceVariant
                        done -> MaterialTheme.colorScheme.surfaceVariant
                        else -> tint
                    },
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(if (done) R.drawable.ph_x_bold else R.drawable.ph_check_bold),
                contentDescription = contentDescription,
                tint = when {
                    !enabled -> MaterialTheme.colorScheme.outline
                    done -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> Color.White
                },
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, count: Int? = null) {
    Row(modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (count != null) {
            Text(
                "  $count",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** A tab's title, centered at the top. */
@Composable
fun ScreenTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
    )
}

@Composable
fun EmptyLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier.padding(start = 4.dp, top = 2.dp, bottom = 8.dp),
    )
}
