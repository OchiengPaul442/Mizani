package com.mizani.money.feature.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mizani.money.R
import com.mizani.money.core.model.WalletType
import com.mizani.money.core.money.MoneyFormat
import com.mizani.money.core.time.DateText
import com.mizani.money.feature.plan.ContributeGoalDialog
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.CategoryAvatar
import com.mizani.money.ui.components.DateField
import com.mizani.money.ui.components.EmptyState
import com.mizani.money.ui.components.MizaniAmountField
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniConfirmDialog
import com.mizani.money.ui.components.MizaniCelebration
import com.mizani.money.ui.components.MizaniIcons
import com.mizani.money.ui.components.MizaniPicker
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.PickerOption
import com.mizani.money.ui.components.ProgressBar
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.StatusBanner
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalScreen(
    vm: AppViewModel,
    state: AppUiState,
    goalId: String?,
    onBack: () -> Unit,
) {
    val snapshot = state.snapshot
    val currency = state.currency
    val today = vm.today
    val scope = rememberCoroutineScope()
    val existing = remember(goalId, snapshot.goals) {
        goalId?.let { id -> snapshot.goals.firstOrNull { it.id == id } }
    }
    val contributions = remember(existing, snapshot.contributions) {
        existing?.let { goal ->
            snapshot.contributions.filter { it.goalId == goal.id }.sortedByDescending { it.localDateEpochDay }
        } ?: emptyList()
    }
    val savingsWallets = snapshot.activeWallets.filter { it.type == WalletType.SAVINGS || it.type == WalletType.BANK }

    var name by rememberSaveable(goalId) { mutableStateOf(existing?.name ?: "") }
    var targetText by rememberSaveable(goalId) {
        mutableStateOf(existing?.let { MoneyFormat.toInput(it.targetMinor, currency) } ?: "")
    }
    var hasTargetDate by rememberSaveable(goalId) { mutableStateOf(existing?.targetEpochDay != null) }
    var targetDate by rememberSaveable(goalId) {
        mutableStateOf(existing?.targetEpochDay?.let { LocalDate.ofEpochDay(it) } ?: today.plusMonths(6))
    }
    var linkedWalletId by rememberSaveable(goalId) { mutableStateOf(existing?.linkedWalletId) }
    var priority by rememberSaveable(goalId) { mutableStateOf(existing?.priority ?: 2) }
    var icon by rememberSaveable(goalId) { mutableStateOf(existing?.icon ?: "savings") }
    var note by rememberSaveable(goalId) { mutableStateOf(existing?.note ?: "") }
    var showContribute by remember { mutableStateOf(false) }
    var showArchive by remember { mutableStateOf(false) }
    var showCelebration by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val target = MoneyFormat.parse(targetText, currency) ?: 0L
    val progress = existing?.let {
        if (it.targetMinor <= 0) 0.0 else (it.allocatedMinor.toDouble() / it.targetMinor * 100.0).coerceIn(0.0, 100.0)
    } ?: 0.0

    if (showContribute && existing != null) {
        ContributeGoalDialog(
            goalName = existing.name,
            targetMinor = existing.targetMinor,
            allocatedMinor = existing.allocatedMinor,
            wallets = snapshot.activeWallets,
            currency = currency,
            onDismiss = { showContribute = false },
            onContribute = { amount, walletId ->
                scope.launch {
                    runCatching { vm.finance.contributeToGoal(existing.id, amount, walletId, today) }
                        .onSuccess {
                            showContribute = false
                            showCelebration = true
                        }
                        .onFailure { error = it.message ?: "Could not add this contribution. Try again." }
                }
            },
        )
    }

    if (showArchive && existing != null) {
        MizaniConfirmDialog(
            title = "Archive this goal?",
            body = "It will be hidden from active goals. Contributions and history are retained.",
            confirmLabel = "Archive",
            dismissLabel = "Cancel",
            onConfirm = {
                scope.launch {
                    runCatching { vm.finance.setGoalArchived(existing.id, true) }
                        .onSuccess { onBack() }
                        .onFailure { error = it.message ?: "Could not archive this goal. Try again." }
                }
            },
            onDismiss = { showArchive = false },
        )
    }

    fun save() {
        error = null
        if (name.isBlank()) {
            error = "Give this goal a name"
            return
        }
        if (target <= 0) {
            error = "Enter a target greater than zero"
            return
        }
        saving = true
        scope.launch {
            runCatching {
                vm.finance.saveGoal(
                    goalId = existing?.id,
                    name = name,
                    targetMinor = target,
                    targetDate = if (hasTargetDate) targetDate else null,
                    linkedWalletId = linkedWalletId,
                    priority = priority,
                    note = note,
                    icon = icon,
                )
            }.onSuccess {
                saving = false
                if (existing == null) onBack() else message = "Goal updated."
            }.onFailure {
                saving = false
                error = it.message ?: "Could not save this goal. Try again."
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(
            title = if (existing == null) "New goal" else existing.name,
            subtitle = if (existing == null) "Set money aside on purpose" else "Savings goal",
            onBack = onBack,
            actions = {
                if (existing != null) {
                    TextButton(onClick = { showArchive = true }) {
                        Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Archive")
                    }
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.bottom),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            if (goalId != null && existing == null) {
                EmptyState(
                    title = "Goal not found",
                    body = "It may have been deleted. Go back and choose another goal.",
                    iconRes = R.drawable.ic_ph_shield_check,
                    actionLabel = "Go back",
                    onAction = onBack,
                )
                return@Column
            }
            if (error != null) {
                StatusBanner(
                    text = error!!,
                    isError = true,
                    onDismiss = { error = null },
                )
            }
            if (message != null) {
                StatusBanner(
                    text = message!!,
                    isError = false,
                    onDismiss = { message = null },
                )
            }

            if (existing != null) {
                MizaniCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                MizaniIcons.goal(existing.icon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Spacer(Modifier.width(Spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                MoneyFormat.format(existing.allocatedMinor, currency),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                "of ${MoneyFormat.format(existing.targetMinor, currency)} set aside",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                            )
                        }
                        Text(
                            "${progress.toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    ProgressBar(
                        fraction = (progress / 100.0).toFloat(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            existing.targetEpochDay?.let { "Target ${DateText.medium(LocalDate.ofEpochDay(it))}" } ?: "No target date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = { showContribute = true }) { Text("Add money") }
                    }
                }
            } else {
                MizaniCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryAvatar(icon, "#087F72", size = 40)
                        Spacer(Modifier.width(Spacing.md))
                        Text(
                            "Goals hold money you have deliberately set aside. A contribution is not ordinary spending " +
                                "when it moves between your own wallets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Goal name") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
            MizaniAmountField(
                value = targetText,
                onValueChange = { input -> targetText = input },
                label = "Target amount",
                currency = currency,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Target date", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = hasTargetDate, onCheckedChange = { hasTargetDate = it })
            }
            if (hasTargetDate) {
                DateField(label = "Target", date = targetDate, onDateSelected = { targetDate = it })
            }

            Text("Linked savings wallet (optional)", style = MaterialTheme.typography.labelLarge)
            MizaniPicker(
                selectedText = linkedWalletId?.let { id -> snapshot.wallets.firstOrNull { it.id == id }?.name } ?: "No linked wallet",
                options = savingsWallets.map { PickerOption(it.id, "${it.name} · ${it.type.label}") },
                onSelect = { linkedWalletId = it },
                noneLabel = "No linked wallet",
                onSelectNone = { linkedWalletId = null },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "When linked, contributions can move money from another wallet into savings. That transfer is not " +
                    "counted as expenditure.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Icon", style = MaterialTheme.typography.labelLarge)
            val icons = listOf("savings", "school", "home", "phone", "business", "health", "travel", "gift")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                icons.forEach { key ->
                    Box(
                        // 48dp touch target; the visible circle keeps its original size.
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .selectable(
                                selected = icon == key,
                                onClick = { icon = key },
                                role = Role.Button,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (icon == key) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                MizaniIcons.goal(key),
                                contentDescription = "$key icon",
                                modifier = Modifier.size(20.dp),
                                tint = if (icon == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Text("Priority", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf(1 to "High", 2 to "Medium", 3 to "Low").forEach { (value, label) ->
                    FilterChip(selected = priority == value, onClick = { priority = value }, label = { Text(label) })
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                minLines = 2,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = ::save,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(if (saving) "Saving…" else if (existing == null) "Create goal" else "Save changes")
            }

            if (contributions.isNotEmpty()) {
                Text("Contributions", style = MaterialTheme.typography.titleMedium)
                contributions.forEach { contribution ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                contribution.note.ifBlank { "Goal contribution" },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                DateText.medium(LocalDate.ofEpochDay(contribution.localDateEpochDay)) +
                                    (contribution.walletId?.let { id -> " · from ${snapshot.walletName(id)}" } ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            "+${MoneyFormat.format(contribution.amountMinor, currency)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Text(
                "Savings contributions are recorded separately. They do not reduce income and are not counted as " +
                    "ordinary expenses when the money stays in a wallet you own.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (showCelebration) {
        MizaniCelebration(onFinished = { showCelebration = false })
    }
    }
}
