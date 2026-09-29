package app.habitmaker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit ORDER BY sort_order, id")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit WHERE id = :id")
    suspend fun find(id: Long): HabitEntity?

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM habit")
    suspend fun nextSortOrder(): Int

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("DELETE FROM habit WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE habit SET sort_order = :order WHERE id = :id")
    suspend fun setSortOrder(id: Long, order: Int)

    @Transaction
    suspend fun reorder(ids: List<Long>) {
        ids.forEachIndexed { index, id -> setSortOrder(id, index) }
    }
}

@Dao
interface RecordDao {
    @Query("SELECT * FROM habit_record")
    fun observeAll(): Flow<List<HabitRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: HabitRecordEntity)

    @Query("DELETE FROM habit_record WHERE habit_id = :habitId AND day = :day")
    suspend fun delete(habitId: Long, day: Long)
}

@Dao
interface RewardDao {
    @Query("SELECT * FROM reward ORDER BY sort_order, id")
    fun observeAll(): Flow<List<RewardEntity>>

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM reward")
    suspend fun nextSortOrder(): Int

    @Query("SELECT * FROM reward WHERE id = :id")
    suspend fun find(id: Long): RewardEntity?

    @Insert
    suspend fun insert(reward: RewardEntity): Long

    @Update
    suspend fun update(reward: RewardEntity)

    @Query("DELETE FROM reward WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM reward_claim")
    fun observeClaims(): Flow<List<RewardClaimEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertClaim(claim: RewardClaimEntity)
}
