package com.nwe.recipely

import com.nwe.recipely.data.RecipeCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeCategoryTest {

    @Test
    fun fromKey_returnsMatchingConstant() {
        assertEquals(RecipeCategory.MAIN, RecipeCategory.fromKey("MAIN"))
        assertEquals(RecipeCategory.SNACK, RecipeCategory.fromKey("SNACK"))
        assertEquals(RecipeCategory.SALAD, RecipeCategory.fromKey("SALAD"))
    }

    @Test
    fun fromKey_returnsNull_forNullBlankOrUnknown() {
        assertNull(RecipeCategory.fromKey(null))
        assertNull(RecipeCategory.fromKey(""))
        assertNull(RecipeCategory.fromKey("BOGUS"))
        assertNull(RecipeCategory.fromKey("   "))
    }

    @Test
    fun entries_areInDocumentedOrder() {
        assertEquals(
            listOf("MAIN", "BREAKFAST", "SALAD", "BAKING", "DESSERT", "SNACK"),
            RecipeCategory.entries.map { it.key },
        )
    }
}
