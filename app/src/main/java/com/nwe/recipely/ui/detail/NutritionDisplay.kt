package com.nwe.recipely.ui.detail

import com.nwe.recipely.data.Recipe
import java.util.Locale
import kotlin.math.roundToInt

/** The four optional energy/macro values, all nullable. */
data class NutritionFacts(
    val calories: Int? = null,        // kcal
    val carbsGrams: Double? = null,
    val proteinGrams: Double? = null,
    val fatGrams: Double? = null,
) {
    val hasAny: Boolean
        get() = calories != null || carbsGrams != null || proteinGrams != null || fatGrams != null
}

/** Reads the recipe's stored (total) nutrition values. */
fun Recipe.nutritionFacts(): NutritionFacts =
    NutritionFacts(calories, carbsGrams, proteinGrams, fatGrams)

/**
 * Per-portion values: each total divided by [servings] (kcal rounded to a whole number,
 * macros rounded to one decimal). Returns null when [servings] is missing or non-positive —
 * the caller then shows only the totals.
 */
fun NutritionFacts.perPortion(servings: Int?): NutritionFacts? {
    if (servings == null || servings <= 0) return null
    return NutritionFacts(
        calories = calories?.let { (it.toDouble() / servings).roundToInt() },
        carbsGrams = carbsGrams?.let { roundTo1(it / servings) },
        proteinGrams = proteinGrams?.let { roundTo1(it / servings) },
        fatGrams = fatGrams?.let { roundTo1(it / servings) },
    )
}

private fun roundTo1(value: Double): Double =
    String.format(Locale.US, "%.1f", value).toDouble()

object NutritionFormat {
    /** A gram value with exactly one decimal, using [locale]'s decimal separator. */
    fun grams(value: Double, locale: Locale = Locale.getDefault()): String =
        String.format(locale, "%.1f", value)
}
