package app.habitmaker.ui.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.habitmaker.R
import app.habitmaker.domain.Reward
import app.habitmaker.ui.LocalAppContainer
import app.habitmaker.ui.component.CircleIcon
import app.habitmaker.ui.component.ColorGrid
import app.habitmaker.ui.component.ConfirmDialog
import app.habitmaker.ui.component.IconPickerSheet
import app.habitmaker.ui.component.NeutralCircleIcon
import app.habitmaker.util.DateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val DAY_MS = 86_400_000L
private val FieldShape = RoundedCornerShape(12.dp)

/** Create (habitId 0) or edit a habit: a full sub-screen over the tabs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditScreen(habitId: Long, onClose: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: HabitEditViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HabitEditViewModel(habitId, container.habitRepository, container.rewardRepository, container.today) }
        },
    )
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val rewards by viewModel.rewards.collectAsStateWithLifecycle()
    var pickingIcon by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (habitId == 0L) R.string.habit_new else R.string.habit_edit)) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(painterResource(R.drawable.ph_arrow_left), stringResource(R.string.common_back)) }
                },
                actions = {
                    if (habitId != 0L) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(painterResource(R.drawable.ph_trash), stringResource(R.string.common_delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp).imePadding()) {
                Button(
                    onClick = {
                        showErrors = true
                        viewModel.save(onClose)
                    },
                    enabled = ready,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text(stringResource(R.string.common_save)) }
            }
        },
    ) { padding ->
        if (!ready) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Icon + name
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIcon(
                    draft.icon,
                    Color(draft.color),
                    size = 56.dp,
                    modifier = Modifier.clip(CircleShape).clickable { pickingIcon = true },
                )
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { v -> viewModel.edit { it.copy(name = v.take(60)) } },
                    label = { Text(stringResource(R.string.habit_name)) },
                    singleLine = true,
                    isError = showErrors && draft.name.isBlank(),
                    modifier = Modifier.weight(1f),
                )
            }
            TextButton(onClick = { pickingIcon = true }) { Text(stringResource(R.string.habit_change_icon)) }
            OutlinedTextField(
                value = draft.note,
                onValueChange = { v -> viewModel.edit { it.copy(note = v.take(200)) } },
                label = { Text(stringResource(R.string.habit_note)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            Label(stringResource(R.string.habit_color))
            ColorGrid(selected = draft.color, onSelect = { c -> viewModel.edit { it.copy(color = c) } })

            Label(stringResource(R.string.habit_time))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DateField(
                    label = stringResource(R.string.habit_start),
                    day = draft.startDay,
                    onPick = { d -> viewModel.edit { it.copy(startDay = d) } },
                    modifier = Modifier.weight(1f),
                )
                DateField(
                    label = stringResource(R.string.habit_end),
                    day = draft.endDay,
                    onPick = { d -> viewModel.edit { it.copy(endDay = d) } },
                    onClear = { viewModel.edit { it.copy(endDay = null) } },
                    initial = draft.startDay,
                    isError = !draft.datesValid,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!draft.datesValid) {
                Text(stringResource(R.string.habit_end_before_start), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Label(stringResource(R.string.habit_rewards))
            if (rewards.isEmpty()) {
                Text(stringResource(R.string.habit_no_rewards), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            RuleEditor(stringResource(R.string.reward_weekly), draft.weekly, rewards, maxTolerance = 6) { r -> viewModel.edit { it.copy(weekly = r) } }
            RuleEditor(stringResource(R.string.reward_monthly), draft.monthly, rewards, maxTolerance = 30) { r -> viewModel.edit { it.copy(monthly = r) } }
            RuleEditor(
                stringResource(R.string.reward_final),
                draft.final,
                rewards,
                maxTolerance = 999,
                disabledHint = if (draft.endDay == null) stringResource(R.string.reward_final_needs_end) else null,
            ) { r -> viewModel.edit { it.copy(final = r) } }

            Label(stringResource(R.string.habit_exceptions))
            Text(stringResource(R.string.habit_exceptions_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            SubLabel(stringResource(R.string.exception_days_of_week))
            DaysOfWeek(draft.exceptDow) { mask -> viewModel.edit { it.copy(exceptDow = mask) } }
            SubLabel(stringResource(R.string.exception_days_of_month))
            DaysOfMonth(draft.exceptDom) { mask -> viewModel.edit { it.copy(exceptDom = mask) } }
            SubLabel(stringResource(R.string.exception_fixed_days))
            FixedDates(draft.exceptDates) { list -> viewModel.edit { it.copy(exceptDates = list) } }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickingIcon) {
        IconPickerSheet(
            selected = draft.icon,
            tint = Color(draft.color),
            onSelect = { name ->
                viewModel.edit { it.copy(icon = name) }
                pickingIcon = false
            },
            onDismiss = { pickingIcon = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.habit_delete_title),
            message = stringResource(R.string.habit_delete_message, draft.name),
            onConfirm = {
                confirmDelete = false
                viewModel.delete(onClose)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun SubLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    day: Long?,
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    initial: Long? = null,
    isError: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier
            .clip(FieldShape)
            .border(1.dp, if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline, FieldShape)
            .clickable { open = true }
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                day?.let(DateFormat::full) ?: stringResource(R.string.habit_end_none),
                style = MaterialTheme.typography.bodyLarge,
                color = if (day == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (onClear != null && day != null) {
            IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                Icon(painterResource(R.drawable.ph_x_bold), stringResource(R.string.common_clear), modifier = Modifier.size(16.dp))
            }
        } else {
            Icon(
                painterResource(R.drawable.ph_calendar_blank),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp).size(20.dp),
            )
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (day ?: initial)?.times(DAY_MS))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(Math.floorDiv(it, DAY_MS)) }
                    open = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) { DatePicker(state = state, showModeToggle = false) }
    }
}

@Composable
private fun RuleEditor(
    title: String,
    rule: RuleDraft,
    rewards: List<Reward>,
    maxTolerance: Int,
    disabledHint: String? = null,
    onChange: (RuleDraft) -> Unit,
) {
    val enabled = disabledHint == null && rewards.isNotEmpty()
    val reward = rewards.firstOrNull { it.id == rule.rewardId }
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, FieldShape)
            .padding(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        if (disabledHint != null) {
            Text(disabledHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            return@Column
        }
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(FieldShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, FieldShape)
                    .clickable(enabled = enabled) { menu = true }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (reward != null) {
                    NeutralCircleIcon(reward.icon, size = 28.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(reward.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                } else {
                    Text(
                        stringResource(R.string.reward_none),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f),
                    )
                }
                Icon(painterResource(R.drawable.ph_caret_down), contentDescription = null, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reward_none)) },
                    onClick = {
                        onChange(rule.copy(rewardId = null))
                        menu = false
                    },
                )
                rewards.forEach { r ->
                    DropdownMenuItem(
                        leadingIcon = { NeutralCircleIcon(r.icon, size = 28.dp) },
                        text = { Text(r.name) },
                        onClick = {
                            onChange(rule.copy(rewardId = r.id))
                            menu = false
                        },
                    )
                }
            }
        }
        if (reward != null) {
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.reward_tolerance), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { onChange(rule.copy(tolerance = (rule.tolerance - 1).coerceAtLeast(0))) }, enabled = rule.tolerance > 0) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text("${rule.tolerance}", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
                IconButton(
                    onClick = { onChange(rule.copy(tolerance = (rule.tolerance + 1).coerceAtMost(maxTolerance))) },
                    enabled = rule.tolerance < maxTolerance,
                ) { Text("+", style = MaterialTheme.typography.titleLarge) }
            }
            Text(
                stringResource(R.string.reward_tolerance_hint, rule.tolerance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DaysOfWeek(mask: Int, onChange: (Int) -> Unit) {
    val locale = Locale.getDefault()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DayOfWeek.entries.forEachIndexed { i, dow ->
            val on = (mask shr i) and 1 == 1
            FilterChip(
                selected = on,
                onClick = { onChange(mask xor (1 shl i)) },
                label = { Text(dow.getDisplayName(TextStyle.SHORT, locale)) },
            )
        }
    }
}

@Composable
private fun DaysOfMonth(mask: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..31).chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { d ->
                    val on = (mask shr (d - 1)) and 1 == 1
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1.3f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (on) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest)
                            .border(1.dp, if (on) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable { onChange(mask xor (1 shl (d - 1))) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$d", style = MaterialTheme.typography.bodyMedium, color = if (on) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FixedDates(dates: List<Int>, onChange: (List<Int>) -> Unit) {
    var open by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        dates.sorted().forEach { md ->
            InputChip(
                selected = false,
                onClick = { onChange(dates - md) },
                label = { Text(DateFormat.monthDay(md)) },
                trailingIcon = { Icon(painterResource(R.drawable.ph_x_bold), stringResource(R.string.common_delete), modifier = Modifier.size(14.dp)) },
            )
        }
        AssistChip(
            onClick = { open = true },
            label = { Text(stringResource(R.string.exception_add_day)) },
            leadingIcon = { Icon(painterResource(R.drawable.ph_plus), contentDescription = null, modifier = Modifier.size(16.dp)) },
        )
    }
    if (open) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val d = LocalDate.ofEpochDay(Math.floorDiv(it, DAY_MS))
                        val md = d.monthValue * 100 + d.dayOfMonth
                        if (md !in dates) onChange(dates + md)
                    }
                    open = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) {
            Column {
                Text(
                    stringResource(R.string.exception_add_day_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
                )
                DatePicker(state = state, showModeToggle = false, title = null)
            }
        }
    }
    HorizontalDivider(Modifier.padding(top = 8.dp), color = Color.Transparent)
}
