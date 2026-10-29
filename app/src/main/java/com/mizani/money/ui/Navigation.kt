package com.mizani.money.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mizani.money.feature.categories.CategoriesScreen
import com.mizani.money.feature.commitments.CommitmentsScreen
import com.mizani.money.feature.home.HomeScreen
import com.mizani.money.feature.insights.InsightsScreen
import com.mizani.money.feature.more.MoreScreen
import com.mizani.money.feature.settings.AboutScreen
import com.mizani.money.feature.settings.SettingsScreen
import com.mizani.money.feature.transactions.AddTransactionScreen
import com.mizani.money.feature.transactions.MessageImportScreen
import com.mizani.money.feature.transactions.TransactionsScreen
import com.mizani.money.feature.wallets.WalletsScreen
import com.mizani.money.core.model.TransactionType
import com.mizani.money.ui.components.Spacing
import com.mizani.money.ui.components.mizaniSurfaceBorderColor
import com.mizani.money.ui.theme.LocalMizaniThemeTokens
import kotlinx.coroutines.launch

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    Tab("home", "Home", Icons.Rounded.Home),
    Tab("transactions", "Activity", Icons.AutoMirrored.Rounded.ReceiptLong),
    Tab("insights", "Insights", Icons.Rounded.BarChart),
    Tab("commitments", "Bills", Icons.Rounded.CalendarMonth),
    Tab("more", "More", Icons.Rounded.MoreHoriz),
)

private val topLevelRoutes = tabs.map { it.route }.toSet()

private fun navEnter(
    scope: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>,
): EnterTransition = EnterTransition.None

private fun navExit(
    scope: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>,
): ExitTransition = ExitTransition.None

private fun navPopEnter(
    scope: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>,
): EnterTransition = EnterTransition.None

private fun navPopExit(
    scope: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>,
): ExitTransition = ExitTransition.None

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MizaniNavHost(vm: AppViewModel, state: AppUiState) {
    val navController = rememberNavController()
    val pendingImport by vm.pendingImport.collectAsState()
    val sharedImportReady by vm.sharedImportReady.collectAsState()
    val sharedImportNotice by vm.sharedImportNotice.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = currentRoute == null || currentRoute in topLevelRoutes
    val hasTransactions = state.snapshot.transactions.any { it.deletedAt == null }
    val showGlobalAdd = isTopLevel && currentRoute != "commitments" &&
        (hasTransactions || (currentRoute != "home" && currentRoute != "transactions"))
    var showAddSheet by remember { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState()
    val sheetScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(currentRoute) {
        vm.hideWalletBalances()
        // Keep an import candidate alive until its destination has actually been popped. Clearing
        // it from the save/back callback can replace the destination content while Navigation
        // Compose is still moving its lifecycle entry, which is especially fragile during IME
        // dismissal and Activity teardown.
        if (currentRoute != "import-review" && pendingImport != null) {
            vm.clearPendingImport()
        }
    }

    LaunchedEffect(sharedImportReady) {
        if (sharedImportReady && currentRoute != "import-review") {
            navController.navigate("import-review") { launchSingleTop = true }
        }
    }

    LaunchedEffect(sharedImportNotice) {
        if (sharedImportNotice != null && currentRoute != "import-message") {
            navController.navigate("import-message") { launchSingleTop = true }
        }
    }

    fun openAdd(type: TransactionType) {
        showAddSheet = false
        navController.navigate("add/${type.name}") {
            launchSingleTop = true
        }
    }

    fun openDetail(route: String) {
        showAddSheet = false
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun openFromAddSheet(destination: () -> Unit) {
        sheetScope.launch {
            addSheetState.hide()
            showAddSheet = false
            destination()
        }
    }

    fun openTopLevel(route: String) {
        showAddSheet = false
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun finishImportedTransaction() {
        showAddSheet = false
        if (navController.popBackStack("transactions", inclusive = false)) {
            return
        }
        navController.navigate("transactions") {
            // Do not save the transient paste/review destinations. Saving that back stack can
            // retain an INITIALIZED import-review entry and make Activity teardown unsafe.
            popUpTo(navController.graph.findStartDestination().id) { saveState = false }
            launchSingleTop = true
        }
    }

    fun navigateBack() {
        showAddSheet = false
        navController.popBackStack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (showGlobalAdd) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Add") },
                )
            }
        },
        bottomBar = {
            if (isTopLevel) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                ) {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .border(
                                width = 1.dp,
                                color = mizaniSurfaceBorderColor(),
                                shape = MaterialTheme.shapes.large,
                            ),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp,
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { openTopLevel(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        ) {
            composable(
                "home",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                HomeScreen(
                    vm = vm,
                    state = state,
                    onOpenTransactions = { openTopLevel("transactions") },
                    onEditTransaction = { id -> openDetail("edit/$id") },
                    onOpenInsights = { openTopLevel("insights") },
                    onOpenWallets = { openDetail("wallets") },
                    onSetupPin = { openDetail("settings") },
                    onAdd = { showAddSheet = true },
                    onAddIncome = { openAdd(TransactionType.INCOME) },
                    onAddExpense = { openAdd(TransactionType.EXPENSE) },
                )
            }
            composable(
                "transactions",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                TransactionsScreen(
                    vm = vm,
                    state = state,
                    onEdit = { id -> openDetail("edit/$id") },
                    onAdd = { showAddSheet = true },
                )
            }
            composable(
                "insights",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                InsightsScreen(
                    vm = vm,
                    state = state,
                    onExportData = { openDetail("export") },
                    onSetupPin = { openDetail("settings") },
                )
            }
            composable(
                "more",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                MoreScreen(
                    onNavigate = ::openDetail,
                )
            }
            composable(
                "commitments",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                CommitmentsScreen(
                    vm = vm,
                    state = state,
                    onRecordExpense = { openDetail("add/EXPENSE") },
                )
            }
            composable(
                "add/{type}",
                // The add form can immediately open the import flow. Keep this hand-off
                // synchronous so the review entry always reaches CREATED before it is popped.
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) { entry ->
                val initialType = entry.arguments?.getString("type")
                    ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
                    ?.takeIf { it == TransactionType.EXPENSE || it == TransactionType.INCOME || it == TransactionType.TRANSFER }
                    ?: TransactionType.EXPENSE
                AddTransactionScreen(
                    vm = vm,
                    state = state,
                    initialTransactionId = null,
                    initialType = initialType,
                    onDone = ::navigateBack,
                    onBack = ::navigateBack,
                )
            }
            composable(
                "import-message",
                // Import review is a short, form-like flow. Keeping these two destinations
                // synchronous avoids a lifecycle race when a user reviews and saves quickly
                // while the IME or the previous destination is still settling.
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                MessageImportScreen(
                    vm = vm,
                    initialNotice = sharedImportNotice,
                    onReview = { navController.navigate("import-review") { launchSingleTop = true } },
                    onBack = {
                        vm.clearPendingImport()
                        navigateBack()
                    },
                )
            }
            composable(
                "import-review",
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                val candidate = pendingImport
                if (candidate == null) {
                    MessageImportScreen(
                        vm = vm,
                        initialNotice = "Paste a transaction message to begin.",
                        onReview = { navController.navigate("import-review") { launchSingleTop = true } },
                        onBack = ::navigateBack,
                    )
                } else {
                    AddTransactionScreen(
                        vm = vm,
                        state = state,
                        initialTransactionId = null,
                        initialType = candidate.type,
                        importCandidate = candidate,
                        onDone = {
                            // Saving an import completes the whole flow and returns to Activity,
                            // rather than leaving the user on the paste screen. Replace the
                            // detail stack first, then clear the candidate so the review entry is
                            // removed without a transient re-composition.
                            finishImportedTransaction()
                        },
                        onBack = {
                            navigateBack()
                        },
                    )
                }
            }
            composable(
                "edit/{id}",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) { entry ->
                AddTransactionScreen(
                    vm = vm,
                    state = state,
                    initialTransactionId = entry.arguments?.getString("id"),
                    onDone = ::navigateBack,
                    onBack = ::navigateBack,
                )
            }
            composable(
                "wallets",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                WalletsScreen(
                    vm = vm,
                    state = state,
                    onBack = ::navigateBack,
                    onSetupPin = { openDetail("settings") },
                )
            }
            composable(
                "categories",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                CategoriesScreen(vm = vm, state = state, onBack = ::navigateBack)
            }
            composable(
                "settings",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                SettingsScreen(
                    vm = vm,
                    state = state,
                    onBack = ::navigateBack,
                )
            }
            composable(
                "about",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                AboutScreen(state = state, onBack = ::navigateBack)
            }
            composable(
                "export",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                SettingsScreen(
                    vm = vm,
                    state = state,
                    onBack = ::navigateBack,
                    dataToolsOnly = true,
                )
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = addSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("What would you like to record?", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Choose how money moved.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val tokens = LocalMizaniThemeTokens.current
                AddActionButton(
                    icon = Icons.Rounded.ArrowUpward,
                    iconTint = tokens.expense,
                    title = "Record expense",
                    supportingText = "Money spent from a wallet",
                    onClick = { openFromAddSheet { openAdd(TransactionType.EXPENSE) } },
                )
                AddActionButton(
                    icon = Icons.Rounded.ArrowDownward,
                    iconTint = tokens.income,
                    title = "Record income",
                    supportingText = "Money received into a wallet",
                    onClick = { openFromAddSheet { openAdd(TransactionType.INCOME) } },
                )
                AddActionButton(
                    icon = Icons.Rounded.SwapHoriz,
                    iconTint = tokens.transfer,
                    title = "Move between wallets",
                    supportingText = "Does not affect income or spending",
                    onClick = { openFromAddSheet { openAdd(TransactionType.TRANSFER) } },
                )
                AddActionButton(
                    icon = Icons.Rounded.ContentPaste,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Import a message",
                    supportingText = "Review an MTN or Airtel confirmation",
                    onClick = {
                        vm.clearPendingImport()
                        openFromAddSheet { openDetail("import-message") }
                    },
                )
            }
        }
    }
}

@Composable
private fun AddActionButton(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    supportingText: String,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, mizaniSurfaceBorderColor()),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
