package app.habitmaker.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import app.habitmaker.domain.niceStep
import kotlin.math.ceil
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import app.habitmaker.R
import app.habitmaker.domain.DayState
import app.habitmaker.domain.Habit
import app.habitmaker.domain.PeriodKind
import app.habitmaker.domain.RewardEngine

val CardShape = RoundedCornerShape(14.dp)

/** Minimum height and padding of a habit / reward row. */
val RowMinHeight = 58.dp
val RowPaddingH = 12.1.dp
val RowPaddingV = 4.84.dp

private val IconSize = 32.dp

/** The gap between two reward bars, and between the icon and the first one. */
private val BarGap = 1.8.dp

/** Leaves [BarGap] of the slack under the icon, which is centered in a row of [RowMinHeight]. */
private val BarsPullUp = (RowMinHeight - IconSize) / 2 - BarGap

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
            CircleIcon(icon, tint, size = IconSize)
            RowText(name, note, Modifier.weight(1f), dimmed)
            trailing()
        }
        if (bars.isNotEmpty()) {
            // Pulled up into the row's bottom slack, as far under the icon as the bars are from each other.
            Column(
                Modifier.pullUp(BarsPullUp).padding(start = RowPaddingH, end = RowPaddingH, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(BarGap),
            ) {
                bars.forEach { ProgressLine(it, tint) }
            }
        }
    }
}

/**
 * One reward's progress in a habit row: [done] of the [needed] days toward [name], and the state of
 * each day of the period (clipped to the habit's dates) as seen from today.
 */
data class RewardBar(val name: String, val kind: PeriodKind, val done: Int, val needed: Int, val days: List<DayState>)

/**
 * The habit's reward bars for the periods around [day]; [rewardNames] by reward id. Day colors are
 * relative to [today], whichever day is shown.
 */
fun Habit.rewardBars(doneDays: Set<Long>, day: Long, today: Long, rewardNames: Map<Long, String>): List<RewardBar> =
    RewardEngine.progress(this, doneDays, day).mapNotNull { p ->
        rewardNames[p.rewardId]?.let {
            RewardBar(it, p.kind, p.done, p.needed, RewardEngine.dayStates(p.from, p.to, doneDays, today))
        }
    }

private val DayDone = Color(0xFF43A047)
private val DayToday = Color(0xFFFBC02D)
private val DayMissed = Color(0xFFE53935)

private val CaretSize = 14.dp
private val CaretGap = 6.dp
private val PlotHeight = 80.dp
private val LabelGap = 3.dp
private const val MAX_X_LABELS = 7

/** The label, the day bar with a caret beside it and, once tapped open, the chart under them. */
@Composable
private fun ProgressLine(bar: RewardBar, lineColor: Color) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    // 1.2x the rows' former 0.8 label size.
    val labelStyle = MaterialTheme.typography.labelSmall.scaled(0.96f)
    val kind = stringResource(
        when (bar.kind) {
            PeriodKind.WEEK -> R.string.bar_weekly
            PeriodKind.MONTH -> R.string.bar_monthly
            PeriodKind.FINAL -> R.string.bar_final
        },
    )
    val future = MaterialTheme.colorScheme.surfaceVariant
    Column(
        Modifier.clickable(
            interactionSource = null,
            indication = null,
            onClickLabel = stringResource(R.string.bar_show_chart),
        ) { expanded = !expanded },
    ) {
        // The caret is centered on the label and the bar together.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CaretGap)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${bar.name} - $kind",
                        style = labelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${bar.done}/${bar.needed}", style = labelStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // One section per day of the period, edge to edge.
                Canvas(
                    Modifier
                        .padding(top = 2.dp)
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                ) {
                    val n = bar.days.size
                    if (n == 0) return@Canvas
                    val w = size.width / n
                    bar.days.forEachIndexed { i, state ->
                        drawRect(
                            color = when (state) {
                                DayState.FUTURE -> future
                                DayState.DONE -> DayDone
                                DayState.TODAY_PENDING -> DayToday
                                DayState.MISSED -> DayMissed
                            },
                            topLeft = Offset(i * w, 0f),
                            // Overlap by a pixel so no seams show between sections.
                            size = Size(w + 1f, size.height),
                        )
                    }
                }
            }
            Icon(
                painterResource(R.drawable.ph_caret_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(CaretSize).rotate(if (expanded) 180f else 0f),
            )
        }
        AnimatedVisibility(expanded) { RewardChart(bar, lineColor) }
    }
}

/**
 * Done days adding up over the period, in [lineColor], against the red "latest possible" line: the
 * fewest done days each day must already have for the reward to stay reachable. The done line stops
 * at the last settled day. Days run along x (at most [MAX_X_LABELS] labels), done days up y to the
 * next labelled step above the highest line; labels sit on 1/2/5 x 10^k steps.
 */
@Composable
private fun RewardChart(bar: RewardBar, lineColor: Color) {
    val axis = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.scaled(0.8f).copy(color = MaterialTheme.colorScheme.outline)
    val measurer = rememberTextMeasurer()
    val labelHeight = with(LocalDensity.current) { measurer.measure("0", labelStyle).size.height.toDp() }
    // As wide as the bar and its caret. Room for half a label above the plot and the day labels under it.
    Canvas(
        Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .height(PlotHeight + labelHeight * 1.5f + LabelGap),
    ) {
        val n = bar.days.size
        if (n == 0) return@Canvas
        val needed = bar.needed.coerceAtMost(n)
        // Done days so far after each settled day, from 0 before the first.
        val totals = arrayListOf(0)
        for (state in bar.days) {
            if (state == DayState.DONE) totals.add(totals.last() + 1) else if (state == DayState.MISSED) totals.add(totals.last()) else break
        }

        val stroke = 1.5.dp.toPx()
        val gap = LabelGap.toPx()
        val top = labelHeight.toPx() / 2
        val h = PlotHeight.toPx()
        val yMax = maxOf(needed, totals.last(), 1)
        val yStep = niceStep(yMax / (h / (labelHeight.toPx() * 1.6f)).toInt().coerceAtLeast(1).toFloat())
        val yHigh = ceil(yMax / yStep.toFloat()).toInt() * yStep
        val xStep = niceStep(n / MAX_X_LABELS.toFloat())
        val yLabels = (0..yHigh step yStep).map { it to measurer.measure(it.toString(), labelStyle) }
        // The plot starts at the bar's left edge (clear of it so the strokes are not cut); its labels sit on the right.
        val left = stroke / 2
        val w = size.width - left - gap - yLabels.maxOf { it.second.size.width }
        fun at(x: Int, y: Int) = Offset(left + w * x / n, top + h * (yHigh - y) / yHigh)

        for ((value, text) in yLabels) {
            val p = at(n, value)
            drawLine(axis, at(0, value), p, 1.dp.toPx())
            drawText(text, topLeft = Offset(p.x + gap, p.y - text.size.height / 2f))
        }
        for (day in xStep..n step xStep) {
            val p = at(day, 0)
            val text = measurer.measure(day.toString(), labelStyle)
            drawLine(axis, p, p.copy(y = p.y - 3.dp.toPx()), 1.dp.toPx())
            drawText(text, topLeft = Offset(p.x - text.size.width / 2f, p.y + gap))
        }

        val limit = Path().apply {
            at(n - needed, 0).let { moveTo(it.x, it.y) }
            at(n, needed).let { lineTo(it.x, it.y) }
            at(n, 0).let { lineTo(it.x, it.y) }
            close()
        }
        drawPath(limit, DayMissed.copy(alpha = 0.3f))
        drawLine(DayMissed, at(n - needed, 0), at(n, needed), stroke, StrokeCap.Round)

        val done = Path().apply {
            totals.forEachIndexed { day, total -> at(day, total).let { if (day == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } }
        }
        drawPath(done, lineColor, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(lineColor, 2.5.dp.toPx(), at(totals.lastIndex, totals.last()))
    }
}

/**
 * The done / undone toggle: a tick on green for a habit still to do, an x on red to undo a done
 * one. Gray and disabled on days that cannot be changed.
 */
@Composable
fun DoneToggle(done: Boolean, enabled: Boolean, contentDescription: String, onClick: () -> Unit) {
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
                        done -> DayMissed.copy(alpha = 0.8f)
                        else -> DayDone.copy(alpha = 0.8f)
                    },
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(if (done) R.drawable.ph_x_bold else R.drawable.ph_check_bold),
                contentDescription = contentDescription,
                tint = if (enabled) Color.White else MaterialTheme.colorScheme.outline,
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
