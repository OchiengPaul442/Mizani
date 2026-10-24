package com.mizani.money.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mizani.money.feature.account.AccountScreen
import com.mizani.money.feature.budget.BudgetScreen
import com.mizani.money.feature.categories.CategoriesScreen
import com.mizani.money.feature.debts.DebtsScreen
import com.mizani.money.feature.goals.GoalScreen
import com.mizani.money.feature.home.HomeScreen
import com.mizani.money.feature.insights.InsightsScreen
import com.mizani.money.feature.more.MoreScreen
import com.mizani.money.feature.plan.PlanScreen
import com.mizani.money.feature.settings.AboutScreen
import com.mizani.money.feature.settings.SettingsScreen
import com.mizani.money.feature.transactions.AddTransactionScreen
import com.mizani.money.feature.transactions.TransactionsScreen
import com.mizani.money.feature.wallets.WalletsScreen

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    Tab("home", "Home", Icons.Rounded.Home),
    Tab("transactions", "Activity", Icons.Rounded.SwapHoriz),
    Tab("plan", "Plan", Icons.Rounded.AccountBalanceWallet),
    Tab("insights", "Insights", Icons.Rounded.Insights),
    Tab("more", "More", Icons.Rounded.MoreHoriz),
)

private val topLevelRoutes = tabs.map { it.route }.toSet()

// Direction-aware slide spec shared by every destination. The scope's initialState/targetState are
// the NavBackStackEntry endpoints; their route depths decide which way the slide goes. Bottom tabs
// use their left-to-right index, detail routes sit deeper, so navigating rightward in the bar (or
// into any detail screen) slides content in from the right, and popping back reverses it.
// ~280ms horizontal tween; direction follows route depth (see navEnter/navExit).
private fun navEnter(
    scope: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>,
): EnterTransition {
    val from = depthOf(scope.initialState.destination.route)
    val to = depthOf(scope.targetState.destination.route)
    val towardsEnd = to >= from
    return slideInHorizontally(animationSpec = tween(280)) { fullWidth ->
        if (towardsEnd) fullWidth else -fullWidth
    }
}

private fun navExit(
    scope: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>,
): ExitTransition {
    val from = depthOf(scope.initialState.destination.route)
    val to = depthOf(scope.targetState.destination.route)
    val towardsEnd = to >= from
    return slideOutHorizontally(animationSpec = tween(280)) { fullWidth ->
        if (towardsEnd) -fullWidth else fullWidth
    }
}

// Pop transitions: backing out of a detail always feels like "going back", regardless of
// depthOf() tying (e.g. detail -> detail such as Settings -> About). The incoming screen
// slides in from the left and the outgoing one exits to the right.
private fun navPopEnter(
    scope: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>,
): EnterTransition = slideInHorizontally(animationSpec = tween(280)) { fullWidth -> -fullWidth }

private fun navPopExit(
    scope: androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>,
): ExitTransition = slideOutHorizontally(animationSpec = tween(280)) { fullWidth -> fullWidth }

// Maps a route to an integer depth used to decide slide direction. Bottom tabs use their index;
// everything else (detail screens) is pushed below the deepest tab so navigating into any detail
// always slides forward, and popping back reverses it.
private fun depthOf(route: String?): Int = when (route) {
    "home" -> 0
    "transactions" -> 1
    "plan" -> 2
    "insights" -> 3
    "more" -> 4
    null -> -1
    else -> 10
}

@Composable
fun MizaniNavHost(vm: AppViewModel, state: AppUiState) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = currentRoute == null || currentRoute in topLevelRoutes

    fun openTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                    onOpenPlan = { openTopLevel("plan") },
                    onOpenInsights = { openTopLevel("insights") },
                    onAdd = { navController.navigate("add") },
                    onOpenBudget = { budgetId ->
                        if (budgetId == null) navController.navigate("budget/new") else navController.navigate("budget/$budgetId")
                    },
                    onOpenSettings = { navController.navigate("settings") },
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
                    onAdd = { navController.navigate("add") },
                )
            }
            composable(
                "plan",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                PlanScreen(
                    vm = vm,
                    state = state,
                    onOpenBudget = { id ->
                        if (id == null) navController.navigate("budget/new") else navController.navigate("budget/$id")
                    },
                    onOpenGoal = { id ->
                        if (id == null) navController.navigate("goal/new") else navController.navigate("goal/$id")
                    },
                    onOpenDebts = { navController.navigate("debts") },
                )
            }
            composable(
                "insights",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                InsightsScreen(vm = vm, state = state, onOpenPlan = { openTopLevel("plan") })
            }
            composable(
                "more",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                MoreScreen(
                    state = state,
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable(
                "add",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                AddTransactionScreen(
                    vm = vm,
                    state = state,
                    initialTransactionId = null,
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
                "debts",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                DebtsScreen(vm = vm, state = state, onBack = { navController.popBackStack() })
            }
            composable(
                "budget/new",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                BudgetScreen(
                    vm = vm,
                    state = state,
                    budgetId = null,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(
                "budget/{id}",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) { entry ->
                BudgetScreen(
                    vm = vm,
                    state = state,
                    budgetId = entry.arguments?.getString("id"),
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(
                "goal/new",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                GoalScreen(
                    vm = vm,
                    state = state,
                    goalId = null,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                "goal/{id}",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) { entry ->
                GoalScreen(
                    vm = vm,
                    state = state,
                    goalId = entry.arguments?.getString("id"),
                    onBack = { navController.popBackStack() },
                )
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
                "account",
                enterTransition = ::navEnter,
                exitTransition = ::navExit,
                popEnterTransition = ::navPopEnter,
                popExitTransition = ::navPopExit,
            ) {
                AccountScreen(
                    onBack = { navController.popBackStack() },
                    onOpenBackupSettings = { navController.navigate("settings") },
                )
            }
        }
    }
}
