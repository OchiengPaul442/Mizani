package com.mizani.money.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mizani.money.core.model.CategoryType
import com.mizani.money.data.local.CategoryEntity
import com.mizani.money.ui.AppUiState
import com.mizani.money.ui.AppViewModel
import com.mizani.money.ui.components.CategoryAvatar
import com.mizani.money.ui.components.MizaniCard
import com.mizani.money.ui.components.MizaniIcons
import com.mizani.money.ui.components.MizaniTopBar
import com.mizani.money.ui.components.SmallPill
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.StatusBanner
import com.mizani.money.ui.components.parseHexColor
import com.mizani.money.ui.theme.BrandPalette
import kotlinx.coroutines.launch

@Composable
fun CategoriesScreen(
    vm: AppViewModel,
    state: AppUiState,
    onBack: () -> Unit,
) {
    val snapshot = state.snapshot
    val scope = rememberCoroutineScope()
    var type by rememberSaveable { mutableStateOf(CategoryType.EXPENSE) }
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val categories = snapshot.categories.filter { it.type == type && !it.archived && it.deletedAt == null }
    val archived = snapshot.categories.filter { it.type == type && it.archived && it.deletedAt == null }

    if (showDialog) {
        CategoryDialog(
            initial = editing,
            fixedType = type,
            onDismiss = { showDialog = false; editing = null },
            onSave = { name, icon, color ->
                scope.launch {
                    runCatching {
                        if (editing == null) {
                            vm.finance.addCategory(name, type, icon, color)
                        } else {
                            vm.finance.updateCategory(editing!!.id, name, icon, color)
                        }
                    }.onSuccess {
                        showDialog = false
                        editing = null
                    }.onFailure { error = it.message ?: "Could not save this category. Try again." }
                }
            },
            onArchive = { category, archivedFlag ->
                scope.launch {
                    runCatching { vm.finance.setCategoryArchived(category.id, archivedFlag) }
                        .onSuccess { showDialog = false; editing = null }
                        .onFailure { error = it.message ?: "Could not archive this category. Try again." }
                }
            },
            onMove = { category, direction ->
                scope.launch {
                    runCatching { vm.finance.moveCategory(category.id, direction) }
                        .onSuccess { error = null }
                        .onFailure { error = it.message ?: "Could not change the category order. Try again." }
                }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        MizaniTopBar(
            title = "Categories",
            subtitle = "${categories.size} ${type.label.lowercase()} categories",
            onBack = onBack,
            actions = {
                IconButton(onClick = { editing = null; showDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add category")
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
            if (error != null) {
                StatusBanner(
                    text = error!!,
                    isError = true,
                    onDismiss = { error = null },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                CategoryType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(option.label + " categories") },
                    )
                }
            }
            Text(
                "Default categories can be renamed, reordered and archived. Use Move up / Move down in the editor " +
                    "to change the order. Renaming a category keeps your past reports accurate.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            categories.forEach { category ->
                MizaniCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
                    onClick = { editing = category; showDialog = true },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryAvatar(category.icon, category.colorHex, size = 40)
                        Spacer(Modifier.width(Spacing.md))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!category.isDefault) {
                                    Spacer(Modifier.width(Spacing.sm))
                                    SmallPill("Custom")
                                }
                            }
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (archived.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                Text("Archived", style = MaterialTheme.typography.titleSmall)
                archived.forEach { category ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { editing = category; showDialog = true },
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                            CategoryAvatar(category.icon, category.colorHex, size = 32)
                            Spacer(Modifier.width(Spacing.md))
                            Text(
                                category.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text("Archived", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryDialog(
    initial: CategoryEntity?,
    fixedType: CategoryType,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onArchive: (CategoryEntity, Boolean) -> Unit,
    onMove: (CategoryEntity, Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var icon by rememberSaveable { mutableStateOf(initial?.icon ?: "category") }
    var colorHex by rememberSaveable { mutableStateOf(initial?.colorHex ?: "#087F72") }
    var error by remember { mutableStateOf<String?>(null) }
    val icons = listOf(
        "home", "food", "transport", "fuel", "phone", "fees", "bolt", "school",
        "health", "family", "debt", "savings", "giving", "business", "personal",
        "entertainment", "income", "freelance", "category",
    )
    val colors = BrandPalette.swatches

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(if (initial == null) "Add ${fixedType.label.lowercase()} category" else "Edit category") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category name") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Icon", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (icon == key) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    MizaniIcons.category(key),
                                    contentDescription = "$key icon",
                                    modifier = Modifier.size(19.dp),
                                    tint = if (icon == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Text("Colour", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    colors.forEach { hex ->
                        Box(
                            // 48dp touch target; the visible swatch keeps its original size.
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .selectable(
                                    selected = colorHex == hex,
                                    onClick = { colorHex = hex },
                                    role = Role.Button,
                                )
                                // TalkBack needs a name and selection state; colour alone is not accessible.
                                .semantics { contentDescription = "Colour option ${colors.indexOf(hex) + 1}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (colorHex == hex) {
                                    Box(Modifier.size(11.dp).clip(CircleShape).background(Color.White))
                                }
                            }
                        }
                    }
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (initial != null && !initial.archived) {
                    Text("Order", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedButton(onClick = { onMove(initial, -1) }) {
                            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Move up")
                        }
                        OutlinedButton(onClick = { onMove(initial, 1) }) {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Move down")
                        }
                    }
                }
                if (initial != null) {
                    TextButton(
                        onClick = { onArchive(initial, !initial.archived) },
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Icon(
                            if (initial.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (initial.archived) "Restore category" else "Archive category")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) error = "Give this category a name" else onSave(name, icon, colorHex)
            }) { Text(if (initial == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
