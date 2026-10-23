package com.mizani.money.ui

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
import androidx.navigation.NavGraph.Companion.findStartDestination
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
            composable("home") {
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
            composable("transactions") {
                TransactionsScreen(
                    vm = vm,
                    state = state,
                    onEdit = { id -> navController.navigate("edit/$id") },
                    onAdd = { navController.navigate("add") },
                )
            }
            composable("plan") {
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
            composable("insights") {
                InsightsScreen(vm = vm, state = state, onOpenPlan = { openTopLevel("plan") })
            }
            composable("more") {
                MoreScreen(
                    vm = vm,
                    state = state,
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable("add") {
                AddTransactionScreen(
                    vm = vm,
                    state = state,
                    initialTransactionId = null,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("edit/{id}") { entry ->
                AddTransactionScreen(
                    vm = vm,
                    state = state,
                    initialTransactionId = entry.arguments?.getString("id"),
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("wallets") {
                WalletsScreen(vm = vm, state = state, onBack = { navController.popBackStack() })
            }
            composable("categories") {
                CategoriesScreen(vm = vm, state = state, onBack = { navController.popBackStack() })
            }
            composable("debts") {
                DebtsScreen(vm = vm, state = state, onBack = { navController.popBackStack() })
            }
            composable("budget/new") {
                BudgetScreen(
                    vm = vm,
                    state = state,
                    budgetId = null,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable("budget/{id}") { entry ->
                BudgetScreen(
                    vm = vm,
                    state = state,
                    budgetId = entry.arguments?.getString("id"),
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable("goal/new") {
                GoalScreen(
                    vm = vm,
                    state = state,
                    goalId = null,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("goal/{id}") { entry ->
                GoalScreen(
                    vm = vm,
                    state = state,
                    goalId = entry.arguments?.getString("id"),
                    onBack = { navController.popBackStack() },
                )
            }
            composable("settings") {
                SettingsScreen(
                    vm = vm,
                    state = state,
                    onBack = { navController.popBackStack() },
                    onOpenAbout = { navController.navigate("about") },
                )
            }
            composable("about") {
                AboutScreen(state = state, onBack = { navController.popBackStack() })
            }
            composable("account") {
                AccountScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
