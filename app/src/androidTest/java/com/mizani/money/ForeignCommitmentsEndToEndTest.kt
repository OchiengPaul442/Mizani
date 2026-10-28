package com.mizani.money

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mizani.money.core.legal.LegalDocuments
import com.mizani.money.core.model.CategoryType
import com.mizani.money.core.model.TransactionType
import com.mizani.money.core.model.WalletType
import com.mizani.money.data.repo.CommitmentDraft
import com.mizani.money.data.repo.TransactionDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneOffset

/** Verifies that a foreign-currency bill uses its saved rate for progress and linking. */
@RunWith(AndroidJUnit4::class)
class ForeignCommitmentsEndToEndTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var app: MizaniApplication
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        TestState.reset()
        app = ApplicationProvider.getApplicationContext()
        runBlocking {
            app.container.financeRepository.createWorkspace(
                countryCode = "UG",
                currencyCode = "UGX",
                walletName = "Cash",
                walletType = WalletType.CASH,
                openingBalanceMinor = 500_000,
            )
            app.container.settingsRepository.completeOnboarding("UG", "UGX")
            app.container.settingsRepository.recordLegalAcceptance(
                LegalDocuments.TERMS_VERSION,
                LegalDocuments.PRIVACY_VERSION,
                System.currentTimeMillis(),
            )
            val snapshot = app.container.financeRepository.snapshot.first()
            val wallet = snapshot.wallets.single()
            val category = snapshot.categories.first { it.type == CategoryType.EXPENSE }
            app.container.financeRepository.saveTransaction(
                TransactionDraft(
                    type = TransactionType.EXPENSE,
                    amountMinor = 360_000,
                    walletId = wallet.id,
                    categoryId = category.id,
                    payee = "Dollar payment",
                    date = LocalDate.now(),
                ),
            )
            app.container.financeRepository.saveCommitment(
                id = null,
                draft = CommitmentDraft(
                    name = "USD tuition",
                    targetMinor = 10_000,
                    dueDate = LocalDate.now().plusDays(7),
                    categoryId = category.id,
                    currencyCode = "USD",
                    targetWorkspaceMinor = 360_000,
                    exchangeRate = "3600",
                    exchangeRateDate = LocalDate.now(ZoneOffset.UTC),
                ),
            )
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) scenario.close()
    }

    @Test
    fun foreignCommitmentDisplaysCurrencyAndLinksWorkspaceExpenseUsingSavedRate() {
        compose.waitUntil(timeoutMillis = 20_000) {
            compose.onAllNodesWithText("Home").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Bills").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("USD", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("Choose expense to link").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Dollar payment -", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Dollar payment -", substring = true).performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Linked expenses").fetchSemanticsNodes().isNotEmpty()
        }

        val progress = runBlocking { app.container.financeRepository.snapshot.first().commitmentProgress().single() }
        assertEquals(10_000L, progress.paidMinor)
        assertEquals(360_000L, progress.paidWorkspaceMinor)
        assertEquals(0L, progress.remainingWorkspaceMinor)
    }
}
