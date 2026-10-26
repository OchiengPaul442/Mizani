package com.mizani.money.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mizani.money.feature.categories.CategoriesScreen
import com.mizani.money.feature.home.HomeScreen
import com.mizani.money.feature.insights.InsightsScreen
import com.mizani.money.feature.more.MoreScreen
import com.mizani.money.feature.settings.AboutScreen
import com.mizani.money.feature.settings.SettingsScreen
import com.mizani.money.feature.transactions.AddTransactionScreen
import com.mizani.money.feature.transactions.TransactionsScreen
import com.mizani.money.feature.wallets.WalletsScreen
import com.mizani.money.core.model.TransactionType

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    Tab("home", "Home", Icons.Rounded.Home),
    Tab("transactions", "Activity", Icons.Rounded.SwapHoriz),
    Tab("insights", "Insights", Icons.Rounded.Insights),
    Tab("more", "More", Icons.Rounded.MoreHoriz),
)

private val topLevelRoutes = tabs.map { it.route }.toSet()

// Keep every route change consistent and restrained: full-screen-width movement caused a visible
// jump between tabs and made details feel as if they were thrown off screen.
private fun navEnter(
    scope: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>,
): EnterTransition {
    val from = depthOf(scope.initialState.destination.route)
    val to = depthOf(scope.targetState.destination.route)
    val towardsEnd = to >= from
    return fadeIn(tween(190)) + slideInHorizontally(
        animationSpec = tween(220, easing = FastOutSlowInEasing),
    ) { fullWidth -> if (towardsEnd) fullWidth / 14 else -fullWidth / 14 }
}

private fun navExit(
    scope: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>,
): ExitTransition {
    val from = depthOf(scope.initialState.destination.route)
    val to = depthOf(scope.targetState.destination.route)
    val towardsEnd = to >= from
    return fadeOut(tween(150)) + slideOutHorizontally(
        animationSpec = tween(220, easing = FastOutSlowInEasing),
    ) { fullWidth -> if (towardsEnd) -fullWidth / 14 else fullWidth / 14 }
}

// Pop transitions: backing out of a detail always feels like "going back", regardless of
// depthOf() tying (e.g. detail -> detail such as Settings -> About). The incoming screen
// slides in from the left and the outgoing one exits to the right.
private fun navPopEnter(
    scope: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>,
): EnterTransition = fadeIn(tween(190)) + slideInHorizontally(
    animationSpec = tween(220, easing = FastOutSlowInEasing),
) { fullWidth -> -fullWidth / 14 }

private fun navPopExit(
    scope: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>,
): ExitTransition = fadeOut(tween(150)) + slideOutHorizontally(
    animationSpec = tween(220, easing = FastOutSlowInEasing),
) { fullWidth -> fullWidth / 14 }

// Maps a route to an integer depth used to decide slide direction. Bottom tabs use their index;
// everything else (detail screens) is pushed below the deepest tab so navigating into any detail
// always slides forward, and popping back reverses it.
private fun depthOf(route: String?): Int = when (route) {
    "home" -> 0
    "transactions" -> 1
    "insights" -> 2
    "more" -> 3
    null -> -1
    else -> 10
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MizaniNavHost(vm: AppViewModel, state: AppUiState) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = currentRoute == null || currentRoute in topLevelRoutes
    var showAddSheet by remember { mutableStateOf(false) }

    fun openAdd(type: TransactionType) {
        navController.navigate("add/${type.name}")
    }

    fun openTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (isTopLevel) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Add") },
                )
            }
        },
        bottomBar = {
            if (isTopLevel) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    )
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
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
            modifier = Modifier.padding(padding),
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
                    onOpenInsights = { openTopLevel("insights") },
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
                    onEdit = { id -> navController.navigate("edit/$id") },
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
                    onExportData = { navController.navigate("export") },
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
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable(
                "add/{type}",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
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
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
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
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                "wallets",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                WalletsScreen(vm = vm, state = state, onBack = { navController.popBackStack() })
            }
            composable(
                "categories",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                CategoriesScreen(vm = vm, state = state, onBack = { navController.popBackStack() })
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
                    onBack = { navController.popBackStack() },
                    onOpenAbout = { navController.navigate("about") },
                )
            }
            composable(
                "about",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                AboutScreen(state = state, onBack = { navController.popBackStack() })
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
                    onBack = { navController.popBackStack() },
                    exportOnly = true,
                )
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("What would you like to record?", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Choose the money that came in or went out.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                FilledTonalButton(
                    onClick = { showAddSheet = false; openAdd(TransactionType.EXPENSE) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Record expense")
                }
                FilledTonalButton(
                    onClick = { showAddSheet = false; openAdd(TransactionType.INCOME) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Record income")
                }
                TextButton(
                    onClick = { showAddSheet = false; openAdd(TransactionType.TRANSFER) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Move money between wallets")
                }
            }
        }
    }
}
