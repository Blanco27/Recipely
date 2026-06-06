package com.nwe.recipely

import com.nwe.recipely.data.Recipe
import com.nwe.recipely.ui.detail.NutritionFacts
import com.nwe.recipely.ui.detail.NutritionFormat
import com.nwe.recipely.ui.detail.nutritionFacts
import com.nwe.recipely.ui.detail.perPortion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class NutritionDisplayTest {

    @Test
    fun hasAny_isFalseWhenAllNull_trueWhenAnySet() {
        assertFalse(NutritionFacts().hasAny)
        assertTrue(NutritionFacts(calories = 100).hasAny)
        assertTrue(NutritionFacts(fatGrams = 1.0).hasAny)
    }

    @Test
    fun nutritionFacts_readsFromRecipe() {
        val facts = Recipe(name = "X", calories = 100, carbsGrams = 10.0).nutritionFacts()
        assertEquals(100, facts.calories)
        assertEquals(10.0, facts.carbsGrams!!, 0.0001)
        assertNull(facts.proteinGrams)
    }

    @Test
    fun perPortion_dividesAndRounds() {
        val facts = NutritionFacts(calories = 1400, carbsGrams = 160.0, proteinGrams = 62.0, fatGrams = 48.0)
        val pp = facts.perPortion(4)!!
        assertEquals(350, pp.calories)
        assertEquals(40.0, pp.carbsGrams!!, 0.0001)
        assertEquals(15.5, pp.proteinGrams!!, 0.0001)
        assertEquals(12.0, pp.fatGrams!!, 0.0001)
    }

    @Test
    fun perPortion_roundsMacrosToOneDecimal() {
        val pp = NutritionFacts(carbsGrams = 160.0).perPortion(3)!!
        assertEquals(53.3, pp.carbsGrams!!, 0.0001) // 53.333... -> 53.3
    }

    @Test
    fun perPortion_isNullWhenServingsMissingOrNonPositive() {
        val facts = NutritionFacts(calories = 100)
        assertNull(facts.perPortion(null))
        assertNull(facts.perPortion(0))
        assertNull(facts.perPortion(-2))
    }

    @Test
    fun perPortion_keepsNullValues() {
        val pp = NutritionFacts(calories = 200, carbsGrams = null).perPortion(2)!!
        assertEquals(100, pp.calories)
        assertNull(pp.carbsGrams)
    }

    @Test
    fun grams_formatsOneDecimalPerLocale() {
        assertEquals("15.5", NutritionFormat.grams(15.5, Locale.US))
        assertEquals("15,5", NutritionFormat.grams(15.5, Locale.GERMANY))
        assertEquals("53.3", NutritionFormat.grams(53.3, Locale.US))
        assertEquals("160.0", NutritionFormat.grams(160.0, Locale.US))
    }
}
