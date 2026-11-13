package com.mizani.money.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mizani.money.feature.categories.CategoriesScreen
import com.mizani.money.feature.commitments.CommitmentsScreen
import com.mizani.money.feature.debts.DebtsScreen
import com.mizani.money.feature.home.HomeScreen
import com.mizani.money.feature.insights.InsightsScreen
import com.mizani.money.feature.investments.InvestmentsScreen
import com.mizani.money.feature.more.MoreScreen
import com.mizani.money.feature.settings.AboutScreen
import com.mizani.money.feature.settings.FeedbackScreen
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

internal data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

internal val tabs = listOf(
    Tab("home", "Home", Icons.Rounded.Home),
    Tab("transactions", "Activity", Icons.AutoMirrored.Rounded.ReceiptLong),
    Tab("insights", "Insights", Icons.Rounded.BarChart),
    Tab("commitments", "Bills", Icons.Rounded.CalendarMonth),
    Tab("more", "More", Icons.Rounded.MoreHoriz),
)

private val topLevelRoutes = tabs.map { it.route }.toSet()

private fun isTopLevelRoute(route: String?): Boolean = route != null && route in topLevelRoutes

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

/**
 * NavHost keeps the previous entry composed for at least one frame while it settles a route
 * change. Every destination must therefore paint an opaque, full-size surface; otherwise a
 * transparent screen root can reveal the previous destination underneath it.
 */
@Composable
private fun MizaniRouteSurface(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        content()
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MizaniNavHost(
    vm: AppViewModel,
    state: AppUiState,
) {
    val navController = rememberNavController()
    val pendingImport by vm.pendingImport.collectAsState()
    val sharedImportReady by vm.sharedImportReady.collectAsState()
    val sharedImportNotice by vm.sharedImportNotice.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // The back stack entry is briefly null while NavHost attaches its start destination. Do not
    // render route-scoped controls during that frame, or the bottom bar/FAB can flash over an
    // otherwise empty surface on first entry.
    val isTopLevel = isTopLevelRoute(currentRoute)
    val hasTransactions = state.snapshot.transactions.any { it.deletedAt == null }
    // Home owns its own actions and must not show the global FAB; Bills records from its
    // own screen, and Activity hides the FAB until the ledger has content.
    val showGlobalAdd = isTopLevel && currentRoute != "commitments" && currentRoute != "home" &&
        (hasTransactions || currentRoute != "transactions")
    // Saved across Activity recreation on purpose: Material 3's ModalBottomSheet re-runs
    // sheetState.show() every time it is composed, so a restored true re-opens the flat sheet
    // instead of leaving an open flag behind an invisible chooser. The half-open branch renders a
    // Dialog, which is visible on composition for the same reason.
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState()
    val sheetScope = androidx.compose.runtime.rememberCoroutineScope()
    val foldLayout = LocalFoldWindowLayout.current ?: rememberFoldWindowLayout(null)
    val isHalfOpen = foldLayout.isHalfOpen

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
        if (isHalfOpen) {
            // The half-open chooser is a fold-aware dialog, not a bottom sheet, so there is no
            // sheet state to animate. Close it directly and navigate.
            showAddSheet = false
            destination()
            return
        }
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

    FoldNavigationLayout(
        layout = foldLayout,
        sidebar = {
            MizaniFoldSidebar(
                currentRoute = currentRoute,
                onNavigate = { route -> openTopLevel(route) },
            )
        },
        dock = {
            MizaniTabletopDock(
                currentRoute = currentRoute,
                onNavigate = { route -> openTopLevel(route) },
            )
        },
        content = {
            // The content slot keeps the NavHost composed in one place so its back stack and
            // saveable form state survive Flat/Book/Tabletop posture changes.
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val useRail = !isHalfOpen && isTopLevel &&
                    maxWidth >= AdaptiveLayout.MEDIUM_WIDTH_DP.dp
                // Keep labels legible at 320–360dp; hide them only for very narrow panes or large type.
                val hideBottomLabels = maxWidth < 300.dp ||
                    density.fontScale > AdaptiveLayout.EXTRA_LARGE_FONT_SCALE
                val hideRailLabels = density.fontScale > AdaptiveLayout.EXTRA_LARGE_FONT_SCALE
                Row(Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = Spacing.sm, vertical = Spacing.lg)
                        .clip(MaterialTheme.shapes.large)
                        .border(
                            width = 1.dp,
                            color = mizaniSurfaceBorderColor(),
                            shape = MaterialTheme.shapes.large,
                        ),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { openTopLevel(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = if (hideRailLabels) tab.label else null) },
                            label = if (hideRailLabels) null else { { Text(tab.label, maxLines = 1) } },
                            colors = NavigationRailItemDefaults.colors(
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

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = if (isHalfOpen) {
                    WindowInsets(0, 0, 0, 0)
                } else {
                    ScaffoldDefaults.contentWindowInsets
                },
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
                    if (!isHalfOpen && isTopLevel && !useRail) {
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
                                        icon = { Icon(tab.icon, contentDescription = if (hideBottomLabels) tab.label else null) },
                                        label = if (hideBottomLabels) null else { { Text(tab.label, maxLines = 1) } },
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
                .clipToBounds()
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
                MizaniRouteSurface {
                    HomeScreen(
                        vm = vm,
                        state = state,
                        onOpenTransactions = { openTopLevel("transactions") },
                        onEditTransaction = { id -> openDetail("edit/$id") },
                        onOpenInsights = { openTopLevel("insights") },
                        onOpenWallets = { openDetail("wallets") },
                        onSetupPin = { openDetail("settings/app-lock") },
                        onAdd = { showAddSheet = true },
                        onAddIncome = { openAdd(TransactionType.INCOME) },
                        onAddExpense = { openAdd(TransactionType.EXPENSE) },
                        onAddTransfer = { openAdd(TransactionType.TRANSFER) },
                        onImportMessage = {
                            vm.clearPendingImport()
                            openDetail("import-message")
                        },
                        onOpenCommitments = { openTopLevel("commitments") },
                    )
                }
            }
            composable(
                "transactions",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                MizaniRouteSurface {
                    TransactionsScreen(
                        vm = vm,
                        state = state,
                        onEdit = { id -> openDetail("edit/$id") },
                        onAdd = { showAddSheet = true },
                    )
                }
            }
            composable(
                "insights",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                MizaniRouteSurface {
                    InsightsScreen(
                        vm = vm,
                        state = state,
                        onExportData = { openDetail("export") },
                        onSetupPin = { openDetail("settings/app-lock") },
                    )
                }
            }
            composable(
                "more",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                MizaniRouteSurface {
                    MoreScreen(
                        onNavigate = ::openDetail,
                    )
                }
            }
            composable(
                "investments",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                MizaniRouteSurface {
                    InvestmentsScreen(
                        vm = vm,
                        state = state,
                        onBack = ::navigateBack,
                        onSetupPin = { openDetail("settings/app-lock") },
                    )
                }
            }
            composable(
                "commitments",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                MizaniRouteSurface {
                    CommitmentsScreen(
                        vm = vm,
                        state = state,
                        onRecordExpense = { openDetail("add/EXPENSE") },
                    )
                }
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
                MizaniRouteSurface {
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
                MizaniRouteSurface {
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
            }
            composable(
                "import-review",
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                MizaniRouteSurface {
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
            }
            composable(
                "edit/{id}",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) { entry ->
                MizaniRouteSurface {
                    AddTransactionScreen(
                        vm = vm,
                        state = state,
                        initialTransactionId = entry.arguments?.getString("id"),
                        onDone = ::navigateBack,
                        onBack = ::navigateBack,
                    )
                }
            }
            composable(
                "wallets",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                MizaniRouteSurface {
                    WalletsScreen(
                        vm = vm,
                        state = state,
                        onBack = ::navigateBack,
                        onSetupPin = { openDetail("settings/app-lock") },
                    )
                }
            }
            composable(
                "debts",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                MizaniRouteSurface {
                    DebtsScreen(
                        vm = vm,
                        state = state,
                        onBack = ::navigateBack,
                        onSetupPin = { openDetail("settings/app-lock") },
                    )
                }
            }
            composable(
                "categories",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                MizaniRouteSurface {
                    CategoriesScreen(vm = vm, state = state, onBack = ::navigateBack)
                }
            }
            composable(
                "settings",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                MizaniRouteSurface {
                    SettingsScreen(
                        vm = vm,
                        state = state,
                        onBack = ::navigateBack,
                        highlightAppLock = false,
                    )
                }
            }
            composable(
                "settings/app-lock",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                MizaniRouteSurface {
                    SettingsScreen(
                        vm = vm,
                        state = state,
                        onBack = ::navigateBack,
                        highlightAppLock = true,
                    )
                }
            }
            composable(
                "about",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                MizaniRouteSurface {
                    AboutScreen(
                        state = state,
                        onBack = ::navigateBack,
                        onFeedback = { openDetail("feedback") },
                    )
                }
            }
            composable(
                "feedback",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                MizaniRouteSurface {
                    FeedbackScreen(onBack = ::navigateBack)
                }
            }
            composable(
                "export",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                MizaniRouteSurface {
                    SettingsScreen(
                        vm = vm,
                        state = state,
                        onBack = ::navigateBack,
                        dataToolsOnly = true,
                    )
                }
            }
            }
        }
        }
        }
    },
)

    if (showAddSheet) {
        val onExpense = { openFromAddSheet { openAdd(TransactionType.EXPENSE) } }
        val onIncome = { openFromAddSheet { openAdd(TransactionType.INCOME) } }
        val onTransfer = { openFromAddSheet { openAdd(TransactionType.TRANSFER) } }
        val onImport = {
            vm.clearPendingImport()
            openFromAddSheet { openDetail("import-message") }
        }
        if (isHalfOpen) {
            // A bottom sheet would stretch across the hinge, so the half-open chooser is a
            // fold-aware dialog constrained to the content pane.
            Dialog(
                onDismissRequest = { showAddSheet = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    securePolicy = SecureFlagPolicy.SecureOn,
                ),
            ) {
                FoldAwareDialogHost {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        AddActionChooserContent(
                            onExpense = onExpense,
                            onIncome = onIncome,
                            onTransfer = onTransfer,
                            onImport = onImport,
                        )
                    }
                }
            }
        } else {
            ModalBottomSheet(
                onDismissRequest = { showAddSheet = false },
                sheetState = addSheetState,
            ) {
                AddActionChooserContent(
                    onExpense = onExpense,
                    onIncome = onIncome,
                    onTransfer = onTransfer,
                    onImport = onImport,
                )
            }
        }
    }
}

/**
 * The four recording destinations shared by the flat bottom sheet and the half-open fold-aware
 * chooser. Copy and actions are identical in both postures.
 */
@Composable
private fun AddActionChooserContent(
    onExpense: () -> Unit,
    onIncome: () -> Unit,
    onTransfer: () -> Unit,
    onImport: () -> Unit,
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
            onClick = onExpense,
        )
        AddActionButton(
            icon = Icons.Rounded.ArrowDownward,
            iconTint = tokens.income,
            title = "Record income",
            supportingText = "Money received into a wallet",
            onClick = onIncome,
        )
        AddActionButton(
            icon = Icons.Rounded.SwapHoriz,
            iconTint = tokens.transfer,
            title = "Move between wallets",
            supportingText = "Does not affect income or spending",
            onClick = onTransfer,
        )
        AddActionButton(
            icon = Icons.Rounded.ContentPaste,
            iconTint = MaterialTheme.colorScheme.primary,
            title = "Import a message",
            supportingText = "Review an MTN or Airtel confirmation",
            onClick = onImport,
        )
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
