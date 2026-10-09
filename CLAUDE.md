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
- `habit_record` has one row per habit per done day; every day in a habit's dates is required (no
  exceptions since DB v3), and a past day without a row is a miss.
  Only today and yesterday can be marked done or undone (`isEditableDay` in `HomeViewModel.kt`);
  on Home the open section is headed "In-process" on those days, "Missed" on older ones and
  "To-do" on future ones.
- Reward bars (`ui/component/HabitRow.kt`): one section per day of the period, colored from today
  (future gray, done green, today open yellow, past missed red); the label shows done/needed.
  Tapping a bar opens a chart under it (`RewardChart`): cumulative done days in the habit color
  against a pale red area under the (undrawn) line from (days - needed, 0) to (days, needed), the fewest
  done days that still keep the reward reachable, and a pale green area from needed up (reward
  earned; the y axis always tops out above needed so it shows). Axis labels sit on 1/2/5 x 10^k steps
  (`domain/NiceStep.kt`, as in Outgo), at most 7 along x.
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
- Backup (Settings > Data): `data/backup/BackupManager.kt` exports every table as one SQL script
  and imports one, replacing all rows in a transaction. `domain/SqlBackup.kt` writes and parses the
  script; the parser accepts only what the writer emits and the rows are inserted with bound
  arguments, so an imported file is never executed as SQL. A new table must be added to
  `BackupManager.Tables`; a backup with a schema newer than the database is refused.
- Cold start: nothing blocks the first frame; `HabitApp.onCreate` opens the DB and reads the icon
  asset on background threads. Language is a SharedPreferences value (`util/LocalePrefs.kt`).
