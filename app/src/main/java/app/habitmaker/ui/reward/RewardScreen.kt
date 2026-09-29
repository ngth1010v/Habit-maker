package app.habitmaker.ui.reward

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.habitmaker.R
import app.habitmaker.data.icon.PhosphorIcons
import app.habitmaker.domain.PeriodKind
import app.habitmaker.domain.Reward
import app.habitmaker.ui.LocalAppContainer
import app.habitmaker.ui.component.ConfirmDialog
import app.habitmaker.ui.component.EmptyLine
import app.habitmaker.ui.component.IconPickerSheet
import app.habitmaker.ui.component.NeutralCircleIcon
import app.habitmaker.ui.component.CardShape
import app.habitmaker.ui.component.SectionHeader
import app.habitmaker.ui.component.slideItem
import app.habitmaker.util.DateFormat

/** Marker for "create a new reward" in the sheet state. */
private val NewReward = Reward(0, "", PhosphorIcons.DEFAULT_REWARD, "", 0)

/**
 * Earned rewards waiting to be claimed on top, then every reward (one row each, with how many
 * times it has been earned). Tapping a reward opens a small edit sheet from the bottom.
 */
@Composable
fun RewardScreen() {
    val container = LocalAppContainer.current
    val viewModel: RewardViewModel = viewModel(
        factory = viewModelFactory { initializer { RewardViewModel(container.rewardRepository, container.habitRepository, container.today) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Reward?>(null) }

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.unclaimed.isNotEmpty()) {
                item(key = "h_earned") { SectionHeader(stringResource(R.string.reward_earned), slideItem(), state.unclaimed.size) }
                items(state.unclaimed, key = { "e_${it.earned.habitId}_${it.earned.kind}_${it.earned.periodStart}" }) { u ->
                    Row(
                        slideItem()
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), CardShape)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NeutralCircleIcon(u.reward.icon, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.reward.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                u.habitName + " · " + periodLabel(u.earned.kind, u.earned.periodStart, u.earned.periodEnd),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        FilledTonalButton(onClick = { viewModel.claim(u.earned) }) { Text(stringResource(R.string.reward_claim)) }
                    }
                }
                item(key = "gap") { Spacer(Modifier.height(8.dp)) }
            }
            item(key = "h_all") { SectionHeader(stringResource(R.string.nav_reward), slideItem(), state.rewards.size) }
            if (state.loaded && state.rewards.isEmpty()) {
                item(key = "empty") { EmptyLine(stringResource(R.string.reward_empty), slideItem()) }
            }
            items(state.rewards, key = { it.reward.id }) { row ->
                Row(
                    slideItem()
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest, CardShape)
                        .clickable { editing = row.reward }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeutralCircleIcon(row.reward.icon, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.reward.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (row.reward.note.isNotBlank()) {
                            Text(
                                row.reward.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (row.earnedCount > 0) {
                        Row(
                            Modifier
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(R.drawable.ph_trophy_fill), contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                            Text(" ×${row.earnedCount}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { editing = NewReward },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Icon(painterResource(R.drawable.ph_plus), contentDescription = stringResource(R.string.reward_new))
        }
    }

    editing?.let { reward ->
        RewardSheet(
            reward = reward,
            onSave = { name, icon, note ->
                viewModel.save(reward.id, name, icon, note)
                editing = null
            },
            onDelete = {
                viewModel.delete(reward.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
fun periodLabel(kind: PeriodKind, start: Long, end: Long): String = when (kind) {
    PeriodKind.WEEK -> stringResource(R.string.period_week, DateFormat.dayMonth(start), DateFormat.dayMonth(end))
    PeriodKind.MONTH -> stringResource(R.string.period_month, DateFormat.monthYear(start))
    PeriodKind.FINAL -> stringResource(R.string.period_final)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RewardSheet(
    reward: Reward,
    onSave: (name: String, icon: String, note: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(reward.name) }
    var note by remember { mutableStateOf(reward.note) }
    var icon by remember { mutableStateOf(reward.icon) }
    var picking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val isNew = reward.id == 0L

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(if (isNew) R.string.reward_new else R.string.reward_edit), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeutralCircleIcon(icon, size = 52.dp, modifier = Modifier.clip(CircleShape).clickable { picking = true })
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text(stringResource(R.string.reward_name)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(200) },
                label = { Text(stringResource(R.string.habit_note)) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isNew) {
                    OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Button(
                    onClick = { onSave(name, icon, note) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.common_save)) }
            }
        }
    }
    if (picking) {
        IconPickerSheet(
            selected = icon,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            onSelect = {
                icon = it
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.reward_delete_title),
            message = stringResource(R.string.reward_delete_message, reward.name),
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
