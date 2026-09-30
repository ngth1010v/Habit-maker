package app.habitmaker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.habitmaker.domain.Habit
import app.habitmaker.domain.HabitColors
import app.habitmaker.domain.Reward
import app.habitmaker.domain.RewardRule

@Entity(tableName = "reward")
data class RewardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    /** ARGB; rows from schema 1 get [HabitColors.DEFAULT] (teal 600). */
    @ColumnInfo(defaultValue = "-16742021") val color: Int,
    val note: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
) {
    fun toDomain() = Reward(id, name, icon, color, note, sortOrder)
}

@Entity(
    tableName = "habit",
    foreignKeys = [
        ForeignKey(RewardEntity::class, ["id"], ["weekly_reward_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(RewardEntity::class, ["id"], ["monthly_reward_id"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(RewardEntity::class, ["id"], ["final_reward_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("weekly_reward_id"), Index("monthly_reward_id"), Index("final_reward_id")],
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Int,
    val note: String,
    /** LocalDate.toEpochDay(). */
    @ColumnInfo(name = "start_day") val startDay: Long,
    @ColumnInfo(name = "end_day") val endDay: Long?,
    @ColumnInfo(name = "weekly_reward_id") val weeklyRewardId: Long?,
    @ColumnInfo(name = "weekly_tolerance") val weeklyTolerance: Int,
    @ColumnInfo(name = "monthly_reward_id") val monthlyRewardId: Long?,
    @ColumnInfo(name = "monthly_tolerance") val monthlyTolerance: Int,
    @ColumnInfo(name = "final_reward_id") val finalRewardId: Long?,
    @ColumnInfo(name = "final_tolerance") val finalTolerance: Int,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
) {
    fun toDomain() = Habit(
        id = id,
        name = name,
        icon = icon,
        color = color,
        note = note,
        startDay = startDay,
        endDay = endDay,
        weekly = weeklyRewardId?.let { RewardRule(it, weeklyTolerance) },
        monthly = monthlyRewardId?.let { RewardRule(it, monthlyTolerance) },
        final = if (endDay != null) finalRewardId?.let { RewardRule(it, finalTolerance) } else null,
        sortOrder = sortOrder,
    )

    companion object {
        fun from(habit: Habit, createdAt: Long) = HabitEntity(
            id = habit.id,
            name = habit.name,
            icon = habit.icon,
            color = habit.color,
            note = habit.note,
            startDay = habit.startDay,
            endDay = habit.endDay,
            weeklyRewardId = habit.weekly?.rewardId,
            weeklyTolerance = habit.weekly?.tolerance ?: 0,
            monthlyRewardId = habit.monthly?.rewardId,
            monthlyTolerance = habit.monthly?.tolerance ?: 0,
            finalRewardId = habit.final?.rewardId?.takeIf { habit.endDay != null },
            finalTolerance = habit.final?.tolerance ?: 0,
            sortOrder = habit.sortOrder,
            createdAt = createdAt,
        )
    }
}

/** One row per habit per day it was done; a required past day without a row is a miss. */
@Entity(
    tableName = "habit_record",
    primaryKeys = ["habit_id", "day"],
    foreignKeys = [ForeignKey(HabitEntity::class, ["id"], ["habit_id"], onDelete = ForeignKey.CASCADE)],
)
data class HabitRecordEntity(
    @ColumnInfo(name = "habit_id") val habitId: Long,
    val day: Long,
    @ColumnInfo(name = "done_at") val doneAt: Long,
)

/**
 * An earned reward the user claimed. Keyed by the habit's period, so a period is claimed at most
 * once. No foreign key to the habit: deleting a habit keeps the reward's claimed count.
 */
@Entity(
    tableName = "reward_claim",
    primaryKeys = ["habit_id", "kind", "period_start"],
    foreignKeys = [ForeignKey(RewardEntity::class, ["id"], ["reward_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("reward_id")],
)
data class RewardClaimEntity(
    @ColumnInfo(name = "habit_id") val habitId: Long,
    /** [app.habitmaker.domain.PeriodKind.code]. */
    val kind: Int,
    @ColumnInfo(name = "period_start") val periodStart: Long,
    @ColumnInfo(name = "reward_id") val rewardId: Long,
    @ColumnInfo(name = "claimed_at") val claimedAt: Long,
)
