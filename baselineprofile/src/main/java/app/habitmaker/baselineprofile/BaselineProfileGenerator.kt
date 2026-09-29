package app.habitmaker.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the baseline + startup profile:
 * `gradlew :app:generateBaselineProfile` with an API 33+ emulator connected, English UI.
 * Covers cold start, every tab, the habit editor and its icon picker.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Now")), TIMEOUT)

        for (tab in listOf("Habits", "Rewards", "Analysis", "Settings", "Home")) tap(By.desc(tab))
        tap(By.desc("Habits"))
        tap(By.desc("New habit"))
        tap(By.text("Change icon"))
        device.pressBack()
        Thread.sleep(700)
        device.pressBack()
        Thread.sleep(700)
        tap(By.desc("Home"))
    }
}

private fun MacrobenchmarkScope.tap(selector: BySelector) {
    // A node found mid-animation can be recycled before the click lands; look it up again.
    for (attempt in 1..3) {
        try {
            device.wait(Until.findObject(selector), TIMEOUT)?.click()
            break
        } catch (_: StaleObjectException) {
            device.waitForIdle()
        }
    }
    device.waitForIdle()
    Thread.sleep(700)
}

internal const val PACKAGE = "app.habitmaker"
internal const val TIMEOUT = 5_000L
