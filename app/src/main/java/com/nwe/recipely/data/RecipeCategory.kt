package com.nwe.recipely.data

import androidx.annotation.StringRes
import com.nwe.recipely.R

/**
 * Fixed set of recipe categories. The [key] is the locale-independent value stored in the DB
 * ([Recipe.category]); [emoji] and [labelRes] are for display only.
 */
enum class RecipeCategory(
    val key: String,
    val emoji: String,
    @StringRes val labelRes: Int,
) {
    MAIN("MAIN", "🍝", R.string.category_main),
    BREAKFAST("BREAKFAST", "🥞", R.string.category_breakfast),
    SALAD("SALAD", "🥗", R.string.category_salad),
    BAKING("BAKING", "🧁", R.string.category_baking),
    DESSERT("DESSERT", "🍰", R.string.category_dessert),
    SNACK("SNACK", "🥪", R.string.category_snack);

    companion object {
        /** Resolves a stored key to a constant; returns null for null/blank/unknown keys. */
        fun fromKey(key: String?): RecipeCategory? =
            entries.firstOrNull { it.key == key }
    }
}
