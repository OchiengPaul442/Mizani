package com.mizani.money.core.importing

import java.util.Locale

/** Canonical normalization and bounds shared by the import, repository and restore paths. */
object ImportIdentifiers {
    const val MAX_PROVIDER_LENGTH = 40
    const val MAX_REFERENCE_LENGTH = 128

    private val providerPattern = Regex("^[a-z0-9][a-z0-9_-]*$")
    private val referencePattern = Regex("^[A-Z0-9][A-Z0-9._/-]*$")

    fun normalizeProvider(value: String): String = value.trim().lowercase(Locale.ROOT)

    fun normalizeReference(value: String): String = value.trim().uppercase(Locale.ROOT)

    fun isValidProvider(value: String): Boolean =
        value.length in 1..MAX_PROVIDER_LENGTH && providerPattern.matches(value)

    fun isValidReference(value: String): Boolean =
        value.length in 1..MAX_REFERENCE_LENGTH && referencePattern.matches(value)
}
