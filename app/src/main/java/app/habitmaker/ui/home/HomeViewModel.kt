package app.habitmaker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.habitmaker.data.repo.HabitRepository
import app.habitmaker.data.repo.RewardRepository
import app.habitmaker.domain.Habit
import app.habitmaker.domain.isRequiredOn
import app.habitmaker.ui.component.rewardBars
import app.habitmaker.util.Today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeData(
    val habits: List<Habit> = emptyList(),
    val records: Map<Long, Set<Long>> = emptyMap(),
    /** Reward names by id, for the rows' progress bars. */
    val rewardNames: Map<Long, String> = emptyMap(),
    val today: Long,
    val loaded: Boolean = false,
) {
    /** The habits due on [day], split by whether they are done that day. Cheap: a filter over the habits. */
    fun dayOf(day: Long): DayHabits {
        val due = habits.filter { it.isRequiredOn(day) }
        val (done, todo) = due.partition { day in records[it.id].orEmpty() }
        return DayHabits(todo, done)
    }

    fun barsOf(habit: Habit, day: Long) = habit.rewardBars(records[habit.id].orEmpty(), day, rewardNames)
}

data class DayHabits(val inProcess: List<Habit>, val done: List<Habit>) {
    val total get() = inProcess.size + done.size
}

class HomeViewModel(
    private val repository: HabitRepository,
    rewardRepository: RewardRepository,
    private val today: Today,
) : ViewModel() {

    val data: StateFlow<HomeData> = combine(repository.habits, repository.records, rewardRepository.rewards, today.flow) { habits, records, rewards, t ->
        HomeData(habits, records, rewards.associate { it.id to it.name }, t, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeData(today = today.value))

    private val _day = MutableStateFlow(today.value)
    val day: StateFlow<Long> = _day

    /** The day the Home screen was showing as "today", to follow the date over midnight. */
    private var lastToday = today.value

    init {
        viewModelScope.launch {
            today.flow.collect { t ->
                if (_day.value == lastToday) _day.value = t
                lastToday = t
            }
        }
    }

    fun setDay(day: Long) {
        _day.value = day
    }

    fun stepDay(delta: Int) {
        _day.value += delta
    }

    fun toToday() {
        _day.value = today.value
    }

    fun setDone(habitId: Long, day: Long, done: Boolean) {
        // Future days are read-only.
        if (day > today.value) return
        viewModelScope.launch { repository.setDone(habitId, day, done) }
    }
}
