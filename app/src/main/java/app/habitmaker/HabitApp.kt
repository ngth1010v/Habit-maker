package app.habitmaker

import android.app.Application
import android.content.Context
import app.habitmaker.di.AppContainer
import app.habitmaker.util.LocalePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HabitApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocalePrefs.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Open the database and read the icon asset in parallel with the first frame, never
        // blocking it: the Home screen draws its static parts first and fills in the rows.
        appScope.launch(Dispatchers.IO) { container.icons.load() }
        appScope.launch(Dispatchers.IO) {
            container.database.openHelper.writableDatabase
            val used = container.habitRepository.habits.first().map { it.icon } +
                container.rewardRepository.rewards.first().map { it.icon }
            container.icons.warm(used.toSet())
        }
    }
}
