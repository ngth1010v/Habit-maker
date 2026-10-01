package app.habitmaker.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.habitmaker.R
import app.habitmaker.ui.analysis.AnalysisScreen
import app.habitmaker.ui.component.LocalSwipeParent
import app.habitmaker.ui.component.rememberSwipeLevel
import app.habitmaker.ui.component.swipeShift
import app.habitmaker.ui.component.swipeStep
import app.habitmaker.ui.habit.HabitEditScreen
import app.habitmaker.ui.habit.HabitListScreen
import app.habitmaker.ui.home.HomeScreen
import app.habitmaker.ui.reward.RewardScreen
import app.habitmaker.ui.setting.SettingScreen
import kotlinx.coroutines.delay

/** Plain string routes — the route set is small and fixed. */
object Routes {
    const val TABS = "tabs"
    const val HOME = "home"
    const val HABIT = "habit"
    const val REWARD = "reward"
    const val ANALYSIS = "analysis"
    const val SETTING = "setting"

    const val HABIT_EDIT_PATTERN = "habit/edit/{habitId}"
    /** [habitId] 0 creates a new habit. */
    fun habitEdit(habitId: Long) = "habit/edit/$habitId"
}

private data class BottomItem(val route: String, val labelRes: Int, val icon: Int, val iconSelected: Int)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, R.string.nav_home, R.drawable.ph_house, R.drawable.ph_house_fill),
    BottomItem(Routes.HABIT, R.string.nav_habit, R.drawable.ph_list_checks, R.drawable.ph_list_checks_fill),
    BottomItem(Routes.REWARD, R.string.nav_reward, R.drawable.ph_gift, R.drawable.ph_gift_fill),
    BottomItem(Routes.ANALYSIS, R.string.nav_analysis, R.drawable.ph_chart_pie_slice, R.drawable.ph_chart_pie_slice_fill),
    BottomItem(Routes.SETTING, R.string.nav_setting, R.drawable.ph_gear_six, R.drawable.ph_gear_six_fill),
)

/** Wait after launch before composing the not-yet-visited tabs, so cold start stays untouched. */
private const val PREWARM_DELAY_MS = 800L

/**
 * The five tabs live outside the NavHost (as in Outgo): each stays composed once visited and a
 * switch only changes which one is placed, so no screen is rebuilt on a tap or swipe. Swiping
 * sideways anywhere a screen doesn't use the swipe itself steps to the neighbor tab. The NavHost
 * only holds the habit editor, pushed on top.
 */
@Composable
fun HabitRoot() {
    val navController = rememberNavController()
    var tab by rememberSaveable { mutableStateOf(Routes.HOME) }
    val composedTabs = remember { mutableStateListOf(tab) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val onTabs = backStackEntry?.destination?.route.let { it == null || it == Routes.TABS }
    val focusManager = LocalFocusManager.current
    fun selectTab(route: String) {
        focusManager.clearFocus()
        if (route !in composedTabs) composedTabs += route
        tab = route
        if (!onTabs) navController.popBackStack(Routes.TABS, inclusive = false)
    }

    BackHandler(enabled = onTabs && tab != Routes.HOME) { selectTab(Routes.HOME) }

    val tabIndex = bottomItems.indexOfFirst { it.route == tab }
    // Wraps around like Outgo: Home's left neighbor is Setting and the reverse.
    fun tabAt(page: Int) = bottomItems[(tabIndex + page).mod(bottomItems.size)].route
    val tabSwipe = rememberSwipeLevel { next, _ -> tabAt(if (next) 1 else -1).let { route -> { selectTab(route) } } }
    val moving = tabSwipe.moving
    LaunchedEffect(moving) {
        if (moving) listOf(tabAt(-1), tabAt(1)).forEach { if (it !in composedTabs) composedTabs += it }
    }
    LaunchedEffect(Unit) {
        delay(PREWARM_DELAY_MS)
        for (item in bottomItems) {
            if (item.route !in composedTabs) {
                composedTabs += item.route
                delay(100)
            }
        }
    }

    Scaffold(
        bottomBar = { BottomBar(selected = if (onTabs) tab else null, onSelect = ::selectTab) },
        // Each screen pads for the status bar itself, so Home's day panel can run up behind it.
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
            composedTabs.forEach { route ->
                key(route) {
                    val shown = onTabs && route == tab
                    val page = when (route) {
                        tab -> 0
                        tabAt(-1) -> -1
                        tabAt(1) -> 1
                        else -> null
                    }?.takeIf { it == 0 || moving }
                    Box(
                        Modifier.fillMaxSize().placedIf(onTabs && page != null).swipeStep(tabSwipe)
                            .swipeShift(tabSwipe, page ?: 0),
                    ) {
                        CompositionLocalProvider(LocalSwipeParent provides tabSwipe) {
                            TabContent(route, shown, navController)
                        }
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = Routes.TABS,
                enterTransition = { slideInHorizontally(tween(220)) { it } },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { slideOutHorizontally(tween(220)) { it } },
            ) {
                composable(Routes.TABS) {}
                composable(
                    route = Routes.HABIT_EDIT_PATTERN,
                    arguments = listOf(navArgument("habitId") { type = NavType.LongType }),
                ) { entry ->
                    HabitEditScreen(
                        habitId = entry.arguments?.getLong("habitId") ?: 0L,
                        onClose = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabContent(route: String, shown: Boolean, navController: NavHostController) {
    when (route) {
        Routes.HOME -> HomeScreen()
        Routes.HABIT -> HabitListScreen(onOpen = { id -> navController.navigate(Routes.habitEdit(id)) })
        Routes.REWARD -> RewardScreen()
        Routes.ANALYSIS -> AnalysisScreen()
        Routes.SETTING -> SettingScreen()
    }
}

/** Icon-only bottom bar: small icons (17dp) in a compact bar. */
@Composable
private fun BottomBar(selected: String?, onSelect: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(42.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            bottomItems.forEach { item ->
                val isSelected = item.route == selected
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelect(item.route) },
                            role = Role.Tab,
                            interactionSource = null,
                            indication = ripple(bounded = false, radius = 24.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(width = 40.dp, height = 24.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(if (isSelected) item.iconSelected else item.icon),
                            contentDescription = stringResource(item.labelRes),
                            tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Measures the content either way but places it only when [shown]: unplaced content is not drawn and gets no touches. */
internal fun Modifier.placedIf(shown: Boolean) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) { if (shown) placeable.place(0, 0) }
}
