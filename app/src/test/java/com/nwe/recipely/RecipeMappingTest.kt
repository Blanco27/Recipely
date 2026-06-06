package com.nwe.recipely

import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import com.nwe.recipely.ui.edit.EditUiState
import com.nwe.recipely.ui.edit.IngredientRow
import com.nwe.recipely.ui.edit.StepRow
import com.nwe.recipely.ui.edit.referencedPaths
import com.nwe.recipely.ui.edit.toEntities
import com.nwe.recipely.ui.edit.toUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeMappingTest {

    @Test
    fun canSave_isFalse_whenNameBlank() {
        assertFalse(EditUiState(name = "   ").canSave)
        assertTrue(EditUiState(name = "Soup").canSave)
    }

    @Test
    fun toEntities_trimsAndDropsBlankRows_andAssignsPositions() {
        val state = EditUiState(
            name = "  Carbonara  ",
            prepTime = "30",
            servings = "4",
            imagePath = "/title.jpg",
            ingredients = listOf(
                IngredientRow("  Spaghetti "),
                IngredientRow("   "),
                IngredientRow("Pancetta"),
            ),
            steps = listOf(
                StepRow(text = "Boil pasta"),
                StepRow(text = "   ", imagePath = null),
                StepRow(text = "Fry", imagePath = "/s.jpg"),
            ),
        )

        val (recipe, ingredients, steps) = state.toEntities()

        assertEquals("Carbonara", recipe.name)
        assertEquals(30, recipe.prepTimeMinutes)
        assertEquals(4, recipe.servings)
        assertEquals("/title.jpg", recipe.imageUri)

        assertEquals(listOf("Spaghetti", "Pancetta"), ingredients.map { it.text })
        assertEquals(listOf(0, 1), ingredients.map { it.position })

        // Blank-text step with no image is dropped; the image-only/text steps stay.
        assertEquals(listOf("Boil pasta", "Fry"), steps.map { it.text })
        assertEquals(listOf(0, 1), steps.map { it.position })
        assertEquals("/s.jpg", steps[1].imageUri)
    }

    @Test
    fun toEntities_parsesEmptyNumbersAsNull() {
        val (recipe, _, _) = EditUiState(name = "X", prepTime = "", servings = "abc").toEntities()
        assertEquals(null, recipe.prepTimeMinutes)
        assertEquals(null, recipe.servings)
    }

    @Test
    fun toUiState_mapsDetailsAndFallsBackToOneEmptyRow() {
        val details = RecipeWithDetails(
            recipe = Recipe(id = 7, name = "X", prepTimeMinutes = 10, servings = null, imageUri = null),
            ingredients = emptyList(),
            steps = emptyList(),
        )
        val state = details.toUiState()
        assertEquals(7L, state.id)
        assertEquals("10", state.prepTime)
        assertEquals("", state.servings)
        assertEquals(1, state.ingredients.size)
        assertEquals(1, state.steps.size)
    }

    @Test
    fun referencedPaths_collectsTitleAndStepImages() {
        val state = EditUiState(
            name = "X",
            imagePath = "/t.jpg",
            steps = listOf(StepRow(imagePath = "/a.jpg"), StepRow(imagePath = null), StepRow(imagePath = "/b.jpg")),
        )
        assertEquals(listOf("/t.jpg", "/a.jpg", "/b.jpg"), state.referencedPaths())
    }

    @Test
    fun toEntities_keepsImageOnlyStep_butDropsFullyBlankStep() {
        val state = EditUiState(
            name = "X",
            steps = listOf(
                StepRow(text = "   ", imagePath = "/photo.jpg"), // image-only -> kept
                StepRow(text = "   ", imagePath = null),         // fully blank -> dropped
            ),
        )
        val (_, _, steps) = state.toEntities()
        assertEquals(1, steps.size)
        assertEquals("", steps[0].text)
        assertEquals("/photo.jpg", steps[0].imageUri)
        assertEquals(0, steps[0].position)
    }
}
