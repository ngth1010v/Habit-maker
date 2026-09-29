package app.habitmaker.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.habitmaker.data.icon.PhosphorIcons
import app.habitmaker.data.repo.HabitRepository
import app.habitmaker.data.repo.RewardRepository
import app.habitmaker.domain.Exceptions
import app.habitmaker.domain.Habit
import app.habitmaker.domain.HabitColors
import app.habitmaker.domain.Reward
import app.habitmaker.domain.RewardRule
import app.habitmaker.util.Today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A reward slot being edited: no reward picked means the slot is off. */
data class RuleDraft(val rewardId: Long? = null, val tolerance: Int = 0) {
    fun toRule() = rewardId?.let { RewardRule(it, tolerance) }
}

data class HabitDraft(
    val id: Long = 0,
    val name: String = "",
    val icon: String = PhosphorIcons.DEFAULT_HABIT,
    val color: Int = HabitColors.DEFAULT,
    val note: String = "",
    val startDay: Long,
    val endDay: Long? = null,
    val weekly: RuleDraft = RuleDraft(),
    val monthly: RuleDraft = RuleDraft(),
    val final: RuleDraft = RuleDraft(),
    val exceptDow: Int = 0,
    val exceptDom: Int = 0,
    val exceptDates: List<Int> = emptyList(),
    val sortOrder: Int = 0,
) {
    val datesValid get() = endDay == null || endDay >= startDay
    val valid get() = name.isNotBlank() && datesValid

    fun toHabit() = Habit(
        id = id,
        name = name.trim(),
        icon = icon,
        color = color,
        note = note.trim(),
        startDay = startDay,
        endDay = endDay,
        weekly = weekly.toRule(),
        monthly = monthly.toRule(),
        final = if (endDay != null) final.toRule() else null,
        exceptions = Exceptions(exceptDow, exceptDom, exceptDates.distinct().sorted()),
        sortOrder = sortOrder,
    )

    companion object {
        fun of(h: Habit) = HabitDraft(
            id = h.id,
            name = h.name,
            icon = h.icon,
            color = h.color,
            note = h.note,
            startDay = h.startDay,
            endDay = h.endDay,
            weekly = RuleDraft(h.weekly?.rewardId, h.weekly?.tolerance ?: 0),
            monthly = RuleDraft(h.monthly?.rewardId, h.monthly?.tolerance ?: 0),
            final = RuleDraft(h.final?.rewardId, h.final?.tolerance ?: 0),
            exceptDow = h.exceptions.daysOfWeek,
            exceptDom = h.exceptions.daysOfMonth,
            exceptDates = h.exceptions.dates,
            sortOrder = h.sortOrder,
        )
    }
}

class HabitEditViewModel(
    private val habitId: Long,
    private val habits: HabitRepository,
    rewards: RewardRepository,
    today: Today,
) : ViewModel() {

    private val _draft = MutableStateFlow(HabitDraft(startDay = today.value))
    val draft: StateFlow<HabitDraft> = _draft

    /** False until an existing habit has been read, so the form never flashes empty fields. */
    private val _ready = MutableStateFlow(habitId == 0L)
    val ready: StateFlow<Boolean> = _ready

    val rewards: StateFlow<List<Reward>> = rewards.rewards.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        if (habitId != 0L) {
            viewModelScope.launch {
                habits.find(habitId)?.let { _draft.value = HabitDraft.of(it) }
                _ready.value = true
            }
        }
    }

    fun edit(change: (HabitDraft) -> HabitDraft) = _draft.update(change)

    fun save(onDone: () -> Unit) {
        val d = _draft.value
        if (!d.valid) return
        viewModelScope.launch {
            habits.save(d.toHabit())
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            habits.delete(habitId)
            onDone()
        }
    }
}
