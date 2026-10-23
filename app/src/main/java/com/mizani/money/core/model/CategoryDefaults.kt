package com.mizani.money.core.model

/** Maps a seeded category id back to its country-pack default policy. */
object CategoryDefaults {
    fun isEssential(categoryId: String): Boolean =
        DefaultCategories.all.firstOrNull { definition ->
            DefaultCategories.defaultEntityId("category", definition.key) == categoryId
        }?.essential ?: false
}
