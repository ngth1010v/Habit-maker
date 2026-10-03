package app.habitmaker.data.repo

import app.habitmaker.data.db.HabitDatabase
import app.habitmaker.data.db.RewardClaimEntity
import app.habitmaker.data.db.RewardEntity
import app.habitmaker.domain.EarnedReward
import app.habitmaker.domain.Reward
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RewardRepository(db: HabitDatabase) {
    private val dao = db.rewardDao()

    val rewards: Flow<List<Reward>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    val claims: Flow<List<RewardClaimEntity>> = dao.observeClaims()

    suspend fun save(id: Long, name: String, icon: String, color: Int, note: String) {
        if (id == 0L) {
            dao.insert(
                RewardEntity(name = name, icon = icon, color = color, note = note, sortOrder = dao.nextSortOrder(), createdAt = System.currentTimeMillis()),
            )
        } else {
            val old = dao.find(id) ?: return
            dao.update(old.copy(name = name, icon = icon, color = color, note = note))
        }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun reorder(ids: List<Long>) = dao.reorder(ids)

    suspend fun claim(earned: EarnedReward) = dao.insertClaim(
        RewardClaimEntity(earned.habitId, earned.kind.code, earned.periodStart, earned.rewardId, System.currentTimeMillis()),
    )
}
