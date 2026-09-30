package app.habitmaker.ui.habit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.habitmaker.R
import app.habitmaker.data.repo.HabitRepository
import app.habitmaker.data.repo.RewardRepository
import app.habitmaker.domain.Habit
import app.habitmaker.domain.HabitPhase
import app.habitmaker.domain.phase
import app.habitmaker.ui.LocalAppContainer
import app.habitmaker.ui.component.EmptyLine
import app.habitmaker.ui.component.HabitRow
import app.habitmaker.ui.component.RewardBar
import app.habitmaker.ui.component.rewardBars
import app.habitmaker.ui.component.ScreenTitle
import app.habitmaker.ui.component.SectionHeader
import app.habitmaker.ui.component.rememberReorderState
import app.habitmaker.ui.component.reorderableItem
import app.habitmaker.ui.component.slideItem
import app.habitmaker.util.DateFormat
import app.habitmaker.util.Today
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitSections(
    val sections: Map<HabitPhase, List<Habit>> = emptyMap(),
    /** Each habit's reward progress bars for the current periods. */
    val bars: Map<Long, List<RewardBar>> = emptyMap(),
    val loaded: Boolean = false,
)

class HabitListViewModel(private val repository: HabitRepository, rewardRepository: RewardRepository, today: Today) : ViewModel() {
    val state: StateFlow<HabitSections> = combine(repository.habits, repository.records, rewardRepository.rewards, today.flow) { habits, records, rewards, t ->
        val names = rewards.associate { it.id to it.name }
        HabitSections(
            sections = habits.groupBy { it.phase(t) },
            bars = habits.associate { it.id to it.rewardBars(records[it.id].orEmpty(), t, t, names) },
            loaded = true,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitSections())

    fun reorder(ids: List<Long>) {
        viewModelScope.launch { repository.reorder(ids) }
    }
}

// List keys must be storable in a Bundle: habit rows are keyed by their id (Long), a section's
// header and "No habits remain" line by these strings.
private fun headerKey(phase: HabitPhase) = "h:" + phase.name
private fun emptyKey(phase: HabitPhase) = "e:" + phase.name
private fun phaseOf(key: String) = HabitPhase.valueOf(key.substring(2))

/**
 * All habits in three sections (In-process, Planned, Done), each always shown, with "No habits
 * remain" when empty. Tap a row to edit it; long-press and drag to reorder within its section.
 */
@Composable
fun HabitListScreen(onOpen: (habitId: Long) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: HabitListViewModel = viewModel(
        factory = viewModelFactory { initializer { HabitListViewModel(container.habitRepository, container.rewardRepository, container.today) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val byId = remember(state) { state.sections.values.flatten().associateBy { it.id } }

    // The keys a drag works on, in display order; the database's next emission replaces them.
    var order by remember(state) {
        mutableStateOf(
            HabitPhase.entries.flatMap { phase ->
                val list = state.sections[phase].orEmpty()
                listOf<Any>(headerKey(phase)) + (if (list.isEmpty()) listOf(emptyKey(phase)) else list.map { it.id })
            },
        )
    }
    fun sectionOf(key: Any?): HabitPhase? {
        if (key == null) return null
        var current: HabitPhase? = null
        for (k in order) {
            if (k is String && k.startsWith("h:")) current = phaseOf(k)
            if (k == key) return current
        }
        return null
    }
    val listState = rememberLazyListState()
    val reorder = rememberReorderState(listState)
    reorder.update(
        keys = order,
        canDrag = { it is Long },
        // The lifted row may only sit right after its own section's header or one of its rows.
        isSlot = { before, _ ->
            val dragged = reorder.draggingKey
            before != null && dragged != null && sectionOf(before) == sectionOf(dragged)
        },
        onMove = { key, to ->
            order = order.filter { it != key }.toMutableList().apply { add(to, key) }
        },
        onDrop = { viewModel.reorder(order.filterIsInstance<Long>()) },
    )

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        Column {
        ScreenTitle(stringResource(R.string.nav_habit))
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(order, key = { it }) { key ->
                when (key) {
                    is String -> if (key.startsWith("h:")) {
                        val phase = phaseOf(key)
                        SectionHeader(
                        stringResource(
                            when (phase) {
                                HabitPhase.IN_PROCESS -> R.string.section_in_process
                                HabitPhase.PLANNED -> R.string.section_planned
                                HabitPhase.DONE -> R.string.section_done
                            },
                        ),
                        // The gap between sections; no background or border.
                        slideItem().padding(top = if (phase == HabitPhase.IN_PROCESS) 0.dp else 16.dp),
                        state.sections[phase]?.size ?: 0,
                    )
                    } else if (state.loaded) {
                        EmptyLine(stringResource(R.string.habits_none_remain), slideItem())
                    }
                    is Long -> {
                        val habit = byId[key] ?: return@items
                        HabitRow(
                            icon = habit.icon,
                            color = habit.color,
                            name = habit.name,
                            note = habit.note,
                            modifier = reorderableItem(reorder, key),
                            dimmed = sectionOf(key) == HabitPhase.DONE,
                            bars = state.bars[key].orEmpty(),
                            // A lifted row's release also ends a tap on it: that must not open the editor.
                            onClick = { if (reorder.draggingKey == null) onOpen(habit.id) },
                        ) {
                            Text(
                                DateFormat.dayMonth(habit.startDay) + (habit.endDay?.let { " – " + DateFormat.dayMonth(it) } ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(end = 10.dp),
                            )
                        }
                    }
                }
            }
        }
        }
        FloatingActionButton(
            onClick = { onOpen(0L) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Icon(painterResource(R.drawable.ph_plus), contentDescription = stringResource(R.string.habit_new))
        }
    }
}
