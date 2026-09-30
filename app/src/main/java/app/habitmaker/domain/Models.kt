package app.habitmaker.domain

/**
 * Pure Kotlin models — no Android dependencies, so the schedule and reward rules are unit-testable
 * on the JVM. Days are `LocalDate.toEpochDay()` values everywhere.
 */
data class Habit(
    val id: Long,
    val name: String,
    /** Phosphor icon name, e.g. "barbell" (see `assets/phosphor_fill.txt`). */
    val icon: String,
    /** ARGB, one of [app.habitmaker.domain.HabitColors]. */
    val color: Int,
    val note: String,
    val startDay: Long,
    val endDay: Long?,
    val weekly: RewardRule?,
    val monthly: RewardRule?,
    /** Only meaningful when [endDay] is set. */
    val final: RewardRule?,
    val sortOrder: Int,
)

/** A reward granted when a period is done with at most [tolerance] missed days. */
data class RewardRule(val rewardId: Long, val tolerance: Int)

data class Reward(
    val id: Long,
    val name: String,
    val icon: String,
    /** ARGB, one of [app.habitmaker.domain.HabitColors]. */
    val color: Int,
    val note: String,
    val sortOrder: Int,
)

enum class HabitPhase { IN_PROCESS, PLANNED, DONE }

enum class PeriodKind(val code: Int) {
    WEEK(0), MONTH(1), FINAL(2);

    companion object {
        fun of(code: Int) = entries.first { it.code == code }
    }
}

/**
 * How far a habit is toward one reward in a period: [done] of the [needed] days. [from]..[to] is the
 * period clipped to the habit's dates.
 */
data class RewardProgress(
    val kind: PeriodKind,
    val rewardId: Long,
    val done: Int,
    val needed: Int,
    val from: Long,
    val to: Long,
)

/** One day of a period, as seen from today. */
enum class DayState { FUTURE, DONE, TODAY_PENDING, MISSED }

/** A reward a habit has earned for one period; [periodStart] identifies the period. */
data class EarnedReward(
    val habitId: Long,
    val rewardId: Long,
    val kind: PeriodKind,
    val periodStart: Long,
    val periodEnd: Long,
)
