package app.habitmaker.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** The current local day as an epoch day, refreshed whenever the app comes to the foreground. */
class Today {
    private val state = MutableStateFlow(now())
    val flow: StateFlow<Long> = state
    val value: Long get() = state.value

    fun refresh() {
        state.value = now()
    }

    private fun now() = LocalDate.now().toEpochDay()
}
