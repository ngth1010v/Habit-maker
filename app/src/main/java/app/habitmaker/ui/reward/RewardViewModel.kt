package app.habitmaker.ui.reward

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.habitmaker.data.repo.HabitRepository
import app.habitmaker.data.repo.RewardRepository
import app.habitmaker.domain.EarnedReward
import app.habitmaker.domain.Reward
import app.habitmaker.domain.RewardEngine
import app.habitmaker.util.Today
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RewardRow(val reward: Reward, val earnedCount: Int)

/** An earned, not yet claimed reward, with what the list needs to show it. */
data class Unclaimed(val earned: EarnedReward, val reward: Reward, val habitName: String)

data class RewardUiState(
    val rewards: List<RewardRow> = emptyList(),
    val unclaimed: List<Unclaimed> = emptyList(),
    val loaded: Boolean = false,
)

class RewardViewModel(
    private val rewardRepository: RewardRepository,
    habitRepository: HabitRepository,
    today: Today,
) : ViewModel() {

    val state: StateFlow<RewardUiState> = combine(
        rewardRepository.rewards,
        rewardRepository.claims,
        habitRepository.habits,
        habitRepository.records,
        today.flow,
    ) { rewards, claims, habits, records, t ->
        val byId = rewards.associateBy { it.id }
        val claimed = claims.mapTo(HashSet()) { Triple(it.habitId, it.kind, it.periodStart) }
        val unclaimed = habits.flatMap { habit ->
            RewardEngine.earned(habit, records[habit.id].orEmpty(), t)
                .filter { Triple(it.habitId, it.kind.code, it.periodStart) !in claimed }
                .mapNotNull { e -> byId[e.rewardId]?.let { Unclaimed(e, it, habit.name) } }
        }.sortedByDescending { it.earned.periodEnd }
        val claimCounts = claims.groupingBy { it.rewardId }.eachCount()
        val unclaimedCounts = unclaimed.groupingBy { it.reward.id }.eachCount()
        RewardUiState(
            rewards = rewards.map { RewardRow(it, (claimCounts[it.id] ?: 0) + (unclaimedCounts[it.id] ?: 0)) },
            unclaimed = unclaimed,
            loaded = true,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RewardUiState())

    fun save(id: Long, name: String, icon: String, color: Int, note: String) {
        viewModelScope.launch { rewardRepository.save(id, name.trim(), icon, color, note.trim()) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { rewardRepository.delete(id) }
    }

    fun claim(earned: EarnedReward) {
        viewModelScope.launch { rewardRepository.claim(earned) }
    }
}
