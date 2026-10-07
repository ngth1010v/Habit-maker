package app.habitmaker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.habitmaker.R
import app.habitmaker.ui.LocalAppContainer
import app.habitmaker.ui.component.DonutChart
import app.habitmaker.ui.component.DoneToggle
import app.habitmaker.ui.component.EmptyLine
import app.habitmaker.ui.component.HabitRow
import app.habitmaker.ui.component.SectionHeader
import app.habitmaker.ui.component.slideItem
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.launch
import kotlin.math.abs
import app.habitmaker.util.DateFormat
import java.time.LocalDate

private val PanelWidth = 73.dp
private val NowPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
private val NowHeight = 28.dp
private val DAY_SWIPE_DISTANCE = 40.dp
private const val SETTLE_MS = 200

@Composable
private fun NowLabel() = Text(
    stringResource(R.string.home_now),
    style = MaterialTheme.typography.labelMedium,
    maxLines = 1,
    softWrap = false,
)

/**
 * The day's habits on the left ("In-process" — "Missed" on settled past days, "To-do" on future
 * ones — and "Done"), the day panel on the right. Swiping up /
 * down on the panel steps to the next / previous day like a vertical pager; sideways swipes anywhere
 * go to the tab level.
 */
@Composable
fun HomeScreen() {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HomeViewModel(container.habitRepository, container.rewardRepository, container.today) } },
    )
    val data by viewModel.data.collectAsStateWithLifecycle()
    val day by viewModel.day.collectAsStateWithLifecycle()
    var height by remember { mutableIntStateOf(0) }
    val offset = remember { Animatable(0f) }
    val moving by remember { derivedStateOf { offset.value != 0f } }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val panelPx = with(density) { PanelWidth.toPx() }
    val minDistance = with(density) { DAY_SWIPE_DISTANCE.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { height = it.height }
            // On the parent, not the (moving) panel: the finger is tracked in a fixed frame, and the
            // panel's own button still gets its taps.
            .pointerInput(panelPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.x < size.width - panelPx) return@awaitEachGesture
                    var dy = 0f
                    val start = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
                        change.consume()
                        dy = overSlop
                    } ?: return@awaitEachGesture
                    scope.launch { offset.snapTo(dy) }
                    val finished = verticalDrag(start.id) { change ->
                        dy += change.positionChange().y
                        change.consume()
                        scope.launch { offset.snapTo(dy) }
                    }
                    val moved = dy
                    scope.launch {
                        if (finished && abs(moved) > minDistance) {
                            // Up = next day (it comes in from below), down = previous day.
                            viewModel.stepDay(if (moved < 0) 1 else -1)
                            offset.snapTo(moved + if (moved < 0) height else -height)
                        }
                        offset.animateTo(0f, tween(SETTLE_MS))
                    }
                }
            },
    ) {
        for (page in -1..1) {
            if (page != 0 && !moving) continue
            key(day + page) {
                DayPage(
                    day = day + page,
                    data = data,
                    onToggle = { id, done -> viewModel.setDone(id, day + page, done) },
                    onNow = viewModel::toToday,
                    modifier = Modifier.fillMaxSize().graphicsLayer { translationY = offset.value + page * height },
                )
            }
        }
    }
}

@Composable
private fun DayPage(
    day: Long,
    data: HomeData,
    onToggle: (habitId: Long, done: Boolean) -> Unit,
    onNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habits = remember(data, day) { data.dayOf(day) }
    val editable = isEditableDay(day, data.today)
    val openHeader = when {
        editable -> R.string.section_in_process
        day < data.today -> R.string.section_missed
        else -> R.string.section_todo
    }
    // The panel runs edge to edge (behind the status bar, down to the bottom bar); the list keeps clear of the status bar.
    Row(modifier.background(MaterialTheme.colorScheme.background).padding(start = 12.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight().statusBarsPadding(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "h_in") { SectionHeader(stringResource(openHeader), slideItem(), habits.inProcess.size) }
            if (data.loaded && habits.inProcess.isEmpty()) {
                item(key = "e_in") { EmptyLine(stringResource(R.string.home_none_remain), slideItem()) }
            }
            items(habits.inProcess, key = { it.id }) { habit ->
                HabitRow(habit.icon, habit.color, habit.name, habit.note, slideItem(), bars = data.barsOf(habit, day)) {
                    DoneToggle(false, editable, stringResource(R.string.action_done)) { onToggle(habit.id, true) }
                }
            }
            item(key = "h_done") {
                SectionHeader(stringResource(R.string.section_done), slideItem().padding(top = 8.dp), habits.done.size)
            }
            if (data.loaded && habits.done.isEmpty()) {
                item(key = "e_done") { EmptyLine(stringResource(R.string.home_none_done), slideItem()) }
            }
            items(habits.done, key = { it.id }) { habit ->
                HabitRow(habit.icon, habit.color, habit.name, habit.note, slideItem(), dimmed = true, bars = data.barsOf(habit, day)) {
                    DoneToggle(true, editable, stringResource(R.string.action_undone)) { onToggle(habit.id, false) }
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        DayPanel(day, data.today, habits, onNow, Modifier.width(PanelWidth).fillMaxHeight())
    }
}

@Composable
private fun DayPanel(day: Long, today: Long, habits: DayHabits, onNow: () -> Unit, modifier: Modifier) {
    val date = LocalDate.ofEpochDay(day)
    val isToday = day == today
    val accent = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .statusBarsPadding()
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DonutChart(done = habits.done.size, total = habits.total, size = 52.dp, stroke = 6.dp)
        Spacer(Modifier.height(14.dp))
        Text(
            DateFormat.dayOfWeek(day),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            DateFormat.dayMonth(day),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            date.year.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(
                when {
                    isToday -> R.string.home_today
                    day == today - 1 -> R.string.home_yesterday
                    day == today + 1 -> R.string.home_tomorrow
                    day < today -> R.string.home_past
                    else -> R.string.home_future
                },
            ),
            style = MaterialTheme.typography.labelMedium,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.weight(1f))
        // Filled only when it would move: away from today.
        if (isToday) {
            FilledTonalButton(onClick = onNow, modifier = Modifier.fillMaxWidth().height(NowHeight), contentPadding = NowPadding) { NowLabel() }
        } else {
            Button(onClick = onNow, modifier = Modifier.fillMaxWidth().height(NowHeight), contentPadding = NowPadding) { NowLabel() }
        }
    }
}
