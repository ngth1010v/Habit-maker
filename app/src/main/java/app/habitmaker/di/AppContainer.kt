package app.habitmaker.di

import android.content.Context
import app.habitmaker.data.backup.BackupManager
import app.habitmaker.data.db.HabitDatabase
import app.habitmaker.data.icon.PhosphorIcons
import app.habitmaker.data.repo.HabitRepository
import app.habitmaker.data.repo.RewardRepository
import app.habitmaker.util.Today

/**
 * Hand-rolled dependency graph — no Hilt/Koin, which cost startup time. Everything is `by lazy`,
 * so building the container in [app.habitmaker.HabitApp.onCreate] is nearly free; the database is
 * first touched on a background thread (see HabitApp).
 */
class AppContainer(private val context: Context) {
    val database: HabitDatabase by lazy { HabitDatabase.build(context) }
    val icons: PhosphorIcons by lazy { PhosphorIcons(context) }
    val habitRepository: HabitRepository by lazy { HabitRepository(database) }
    val rewardRepository: RewardRepository by lazy { RewardRepository(database) }
    val backup: BackupManager by lazy { BackupManager(database) }
    val today = Today()
}
