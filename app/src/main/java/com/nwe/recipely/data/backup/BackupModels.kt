package com.nwe.recipely.data.backup

import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import kotlinx.serialization.Serializable

/** Current backup schema version. Bump when the JSON shape changes. */
const val SCHEMA_VERSION = 1

/** ZIP entry name of the recipe JSON. */
const val JSON_ENTRY = "recipes.json"

/** ZIP folder (prefix) that holds image files. */
const val IMAGES_DIR = "images/"

@Serializable
data class BackupFile(
    val schemaVersion: Int,
    val exportedAt: String,
    val recipes: List<BackupRecipe>,
)

@Serializable
data class BackupRecipe(
    val name: String,
    val prepTimeMinutes: Int? = null,
    val servings: Int? = null,
    val calories: Int? = null,
    val carbsGrams: Double? = null,
    val proteinGrams: Double? = null,
    val fatGrams: Double? = null,
    val category: String? = null,
    /** ZIP-relative path (e.g. "images/x.jpg") or null. */
    val image: String? = null,
    val ingredients: List<BackupIngredient> = emptyList(),
    val steps: List<BackupStep> = emptyList(),
)

@Serializable
data class BackupIngredient(val text: String, val position: Int)

@Serializable
data class BackupStep(val text: String, val image: String? = null, val position: Int)

/**
 * Maps a stored recipe to its backup DTO. Children are sorted by [position]; [image] is the
 * ZIP-relative title-image path and [stepImages] are the ZIP-relative step-image paths aligned
 * to the position-sorted step order (null where a step has no image).
 */
fun RecipeWithDetails.toBackupRecipe(image: String?, stepImages: List<String?>): BackupRecipe {
    val sortedSteps = steps.sortedBy { it.position }
    return BackupRecipe(
        name = recipe.name,
        prepTimeMinutes = recipe.prepTimeMinutes,
        servings = recipe.servings,
        calories = recipe.calories,
        carbsGrams = recipe.carbsGrams,
        proteinGrams = recipe.proteinGrams,
        fatGrams = recipe.fatGrams,
        category = recipe.category,
        image = image,
        ingredients = ingredients.sortedBy { it.position }
            .map { BackupIngredient(it.text, it.position) },
        steps = sortedSteps.mapIndexed { i, s -> BackupStep(s.text, stepImages.getOrNull(i), s.position) },
    )
}

/**
 * Builds fresh Room entities (id == 0) from a backup DTO. [image] is the new absolute title-image
 * path and [stepImages] the new absolute step-image paths aligned to [BackupRecipe.steps] order.
 */
fun BackupRecipe.toRecipeWithDetails(image: String?, stepImages: List<String?>): RecipeWithDetails =
    RecipeWithDetails(
        recipe = Recipe(
            name = name,
            imageUri = image,
            prepTimeMinutes = prepTimeMinutes,
            servings = servings,
            calories = calories,
            carbsGrams = carbsGrams,
            proteinGrams = proteinGrams,
            fatGrams = fatGrams,
            category = category,
        ),
        ingredients = ingredients.map { Ingredient(recipeId = 0, text = it.text, position = it.position) },
        steps = steps.mapIndexed { i, s ->
            Step(recipeId = 0, text = s.text, imageUri = stepImages.getOrNull(i), position = s.position)
        },
    )
