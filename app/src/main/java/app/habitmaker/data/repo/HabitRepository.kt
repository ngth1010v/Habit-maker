package app.habitmaker.data.repo

import androidx.room.withTransaction
import app.habitmaker.data.db.HabitDatabase
import app.habitmaker.data.db.HabitEntity
import app.habitmaker.data.db.HabitRecordEntity
import app.habitmaker.domain.Habit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class HabitRepository(private val db: HabitDatabase) {
    private val habitDao = db.habitDao()
    private val recordDao = db.recordDao()

    val habits: Flow<List<Habit>> = habitDao.observeAll()
        .map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.Default)

    /** Done days per habit id. */
    val records: Flow<Map<Long, Set<Long>>> = recordDao.observeAll()
        .map { list ->
            val out = HashMap<Long, MutableSet<Long>>()
            for (r in list) out.getOrPut(r.habitId) { HashSet() }.add(r.day)
            out as Map<Long, Set<Long>>
        }
        .flowOn(Dispatchers.Default)

    suspend fun find(id: Long): Habit? = habitDao.find(id)?.toDomain()

    /** Inserts when `habit.id == 0` (appended at the end of the list), updates otherwise. */
    suspend fun save(habit: Habit): Long = db.withTransaction {
        if (habit.id == 0L) {
            val order = habitDao.nextSortOrder()
            habitDao.insert(HabitEntity.from(habit.copy(sortOrder = order), System.currentTimeMillis()))
        } else {
            val old = habitDao.find(habit.id) ?: return@withTransaction habit.id
            habitDao.update(HabitEntity.from(habit.copy(sortOrder = old.sortOrder), old.createdAt))
            habit.id
        }
    }

    suspend fun delete(id: Long) = habitDao.delete(id)

    suspend fun reorder(ids: List<Long>) = habitDao.reorder(ids)

    suspend fun setDone(habitId: Long, day: Long, done: Boolean) {
        if (done) recordDao.insert(HabitRecordEntity(habitId, day, System.currentTimeMillis()))
        else recordDao.delete(habitId, day)
    }
}
