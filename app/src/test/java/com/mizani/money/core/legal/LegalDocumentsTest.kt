package com.mizani.money.core.legal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalDocumentsTest {
    @Test
    fun currentAcceptanceDependsOnDocumentVersionsOnly() {
        assertTrue(
            LegalDocuments.isCurrentAcceptance(
                termsVersion = LegalDocuments.TERMS_VERSION,
                privacyVersion = LegalDocuments.PRIVACY_VERSION,
            ),
        )
        assertFalse(
            LegalDocuments.isCurrentAcceptance(
                termsVersion = "previous-terms-version",
                privacyVersion = LegalDocuments.PRIVACY_VERSION,
            ),
        )
        assertFalse(LegalDocuments.isCurrentAcceptance(null, null))
    }
}
