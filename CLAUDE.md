# CLAUDE.md

Habit maker — a fast-starting Android habit tracker (Kotlin + Jetpack Compose, Material 3, light
theme only). Spec: `idea.md`. UI, tab swipe and reorder code are modeled on
https://github.com/ngth1010v/Outgo (`ui/component/SwipeStep.kt` and `Reorder.kt` are copied from it).

## Build

Toolchain: Gradle 9.8, AGP 9.4.1 (built-in Kotlin 2.2.10, no kotlin-android plugin), KSP, Compose
BOM 2026.09.00, compileSdk 37. Runs on the JDK 25 shipped with Android Studio.

```bash
gradlew.bat assembleDebug            # app\build\outputs\apk\debug\app-debug.apk (app.habitmaker.debug)
gradlew.bat assembleRelease          # R8-minified, signed with the debug key (replace before publishing)
gradlew.bat testDebugUnitTest        # JVM tests for the schedule/reward rules
gradlew.bat :app:generateBaselineProfile   # needs an API 33+ emulator, English UI
```

Regenerate the baseline profile (`app/src/main/generated/baselineProfiles/`) after adding screens
or transitions; the journey in `baselineprofile/.../BaselineProfileGenerator.kt` finds views by
their English text / content descriptions.

## Architecture

UI (one package per tab under `ui/`) -> ViewModel (StateFlow) -> repository (`data/repo`) -> Room
(`data/db`, one `habitmaker.sqlite`). No DI framework: `di/AppContainer.kt` is `by lazy` properties.
`domain/` is pure Kotlin (no Android) and unit-tested.

- Days are `LocalDate.toEpochDay()` longs everywhere.
- `habit_record` has one row per habit per done day; a required past day without a row is a miss.
- Exceptions: day-of-week bitmask (bit 0 = Monday), day-of-month bitmask (bit 0 = day 1), yearly
  dates as `month * 100 + day`.
- Rewards (`domain/RewardEngine.kt`): calendar weeks (Mon–Sun) and months; a period cut by the
  habit's dates counts only its days inside. Earned once done days >= max(1, required - tolerance),
  possibly mid-period; the final reward from the end date on. Earned rewards are computed, not
  stored; only claims are stored (`reward_claim`, no FK to habit so counts survive deletion).
- Habit/reward icons are Phosphor "fill" icons by name, from `assets/phosphor_fill.txt`, parsed on
  demand into ImageVectors (`data/icon/PhosphorIcons.kt`). Regenerate that asset and the UI
  `res/drawable/ph_*.xml` with `node tools/gen_phosphor.mjs <extracted @phosphor-icons/core package>`.
- Tabs live outside the NavHost (as in Outgo) and stay composed; the NavHost only holds the habit
  editor (`habit/edit/{id}`, 0 = new). LazyColumn keys must be Bundle-able (Long/String).
- Home: a vertical swipe that starts on the right-hand day panel changes the day (up = next day),
  handled by a pointerInput on the Home root that ignores touches left of the panel; sideways swipes
  anywhere switch tabs.
- Cold start: nothing blocks the first frame; `HabitApp.onCreate` opens the DB and reads the icon
  asset on background threads. Language is a SharedPreferences value (`util/LocalePrefs.kt`).
