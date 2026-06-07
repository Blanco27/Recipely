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

/** Root object serialized into [JSON_ENTRY] inside the backup ZIP. */
@Serializable
data class BackupFile(
    val schemaVersion: Int,
    val exportedAt: String,
    val recipes: List<BackupRecipe>,
)

/** A single recipe in a backup. [image]/[BackupStep.image] are ZIP-relative paths (or null). */
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
    /** ZIP-relative title-image path (e.g. "images/x.jpg") or null. */
    val image: String? = null,
    val ingredients: List<BackupIngredient> = emptyList(),
    val steps: List<BackupStep> = emptyList(),
)

@Serializable
data class BackupIngredient(val text: String, val position: Int)

@Serializable
data class BackupStep(val text: String, val position: Int, val image: String? = null)

/**
 * Maps a stored recipe to its backup DTO. Children are sorted by [position]. [imageEntries] maps an
 * absolute source image path (as stored on the entity) to its ZIP-relative entry name; an image not
 * present in the map (or a null source path) becomes null in the DTO.
 */
fun RecipeWithDetails.toBackupRecipe(imageEntries: Map<String, String>): BackupRecipe =
    BackupRecipe(
        name = recipe.name,
        prepTimeMinutes = recipe.prepTimeMinutes,
        servings = recipe.servings,
        calories = recipe.calories,
        carbsGrams = recipe.carbsGrams,
        proteinGrams = recipe.proteinGrams,
        fatGrams = recipe.fatGrams,
        category = recipe.category,
        image = recipe.imageUri?.let { imageEntries[it] },
        ingredients = ingredients.sortedBy { it.position }
            .map { BackupIngredient(it.text, it.position) },
        steps = steps.sortedBy { it.position }
            .map { s -> BackupStep(s.text, s.position, s.imageUri?.let { imageEntries[it] }) },
    )

/**
 * Builds fresh Room entities (id == 0, recipeId == 0) from a backup DTO. [imagePaths] maps a
 * ZIP-relative entry name to the new absolute path it was copied to; a path not present in the map
 * (or a null DTO image) yields a null image on the entity.
 */
fun BackupRecipe.toRecipeWithDetails(imagePaths: Map<String, String>): RecipeWithDetails =
    RecipeWithDetails(
        recipe = Recipe(
            name = name,
            imageUri = image?.let { imagePaths[it] },
            prepTimeMinutes = prepTimeMinutes,
            servings = servings,
            calories = calories,
            carbsGrams = carbsGrams,
            proteinGrams = proteinGrams,
            fatGrams = fatGrams,
            category = category,
        ),
        ingredients = ingredients.map { Ingredient(recipeId = 0, text = it.text, position = it.position) },
        steps = steps.map { s ->
            Step(recipeId = 0, text = s.text, imageUri = s.image?.let { imagePaths[it] }, position = s.position)
        },
    )
