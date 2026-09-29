package app.habitmaker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import app.habitmaker.ui.LocalAppContainer
import app.habitmaker.ui.nav.HabitRoot
import app.habitmaker.ui.theme.HabitTheme
import app.habitmaker.util.LocalePrefs

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocalePrefs.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Leaves the plain-color starting theme (see themes.xml) before the first Compose frame.
        setTheme(R.style.Theme_HabitMaker)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as HabitApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                HabitTheme { HabitRoot() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The day may have changed while the app sat in the background.
        (application as HabitApp).container.today.refresh()
    }
}
