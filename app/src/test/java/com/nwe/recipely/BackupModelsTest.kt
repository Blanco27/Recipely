package com.nwe.recipely

import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import com.nwe.recipely.data.backup.BackupFile
import com.nwe.recipely.data.backup.SCHEMA_VERSION
import com.nwe.recipely.data.backup.toBackupRecipe
import com.nwe.recipely.data.backup.toRecipeWithDetails
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupModelsTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun sample() = RecipeWithDetails(
        recipe = Recipe(
            id = 7, name = "Lasagne", imageUri = "/data/img_title.jpg",
            prepTimeMinutes = 60, servings = 4, calories = 850,
            carbsGrams = 60.0, proteinGrams = 35.0, fatGrams = 40.0, category = "MAIN",
        ),
        // intentionally out of order to prove sorting by position
        ingredients = listOf(
            Ingredient(recipeId = 7, text = "Sheets", position = 1),
            Ingredient(recipeId = 7, text = "Beef", position = 0),
        ),
        steps = listOf(
            Step(recipeId = 7, text = "Bake", imageUri = "/data/img_step.jpg", position = 1),
            Step(recipeId = 7, text = "Brown beef", imageUri = null, position = 0),
        ),
    )

    @Test
    fun toBackupRecipe_sortsChildrenByPosition_andSetsImages() {
        val backup = sample().toBackupRecipe(
            image = "images/title.jpg",
            stepImages = listOf(null, "images/step.jpg"), // aligned to position-sorted steps
        )

        assertEquals("Lasagne", backup.name)
        assertEquals("MAIN", backup.category)
        assertEquals("images/title.jpg", backup.image)
        assertEquals(listOf("Beef", "Sheets"), backup.ingredients.map { it.text })
        assertEquals(listOf("Brown beef", "Bake"), backup.steps.map { it.text })
        assertEquals(listOf(null, "images/step.jpg"), backup.steps.map { it.image })
    }

    @Test
    fun jsonRoundTrip_preservesAllFields() {
        val original = BackupFile(
            schemaVersion = SCHEMA_VERSION,
            exportedAt = "2026-06-07T10:15:30Z",
            recipes = listOf(sample().toBackupRecipe(null, listOf(null, null))),
        )

        val decoded = json.decodeFromString<BackupFile>(json.encodeToString(original))

        assertEquals(original, decoded)
    }

    @Test
    fun toRecipeWithDetails_buildsFreshEntities_withNewImagePaths() {
        val backup = sample().toBackupRecipe(image = "images/t.jpg", stepImages = listOf(null, "images/s.jpg"))

        val rwd = backup.toRecipeWithDetails(
            image = "/new/title.jpg",
            stepImages = listOf("/new/a.jpg", "/new/b.jpg"),
        )

        assertEquals(0L, rwd.recipe.id) // id reset for insert
        assertEquals("/new/title.jpg", rwd.recipe.imageUri)
        assertEquals(850, rwd.recipe.calories)
        assertEquals(listOf("Beef", "Sheets"), rwd.ingredients.map { it.text })
        assertEquals(listOf("Brown beef", "Bake"), rwd.steps.map { it.text })
        assertEquals(listOf("/new/a.jpg", "/new/b.jpg"), rwd.steps.map { it.imageUri })
    }
}
