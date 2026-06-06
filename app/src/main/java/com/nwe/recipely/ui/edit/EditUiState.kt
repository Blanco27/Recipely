package com.nwe.recipely.ui.edit

import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step

data class IngredientRow(val text: String = "")

data class StepRow(val text: String = "", val imagePath: String? = null)

data class EditUiState(
    val id: Long = 0,
    val name: String = "",
    val prepTime: String = "",
    val servings: String = "",
    val imagePath: String? = null,
    val ingredients: List<IngredientRow> = listOf(IngredientRow()),
    val steps: List<StepRow> = listOf(StepRow()),
) {
    val canSave: Boolean get() = name.isNotBlank()
}

/** Title image plus every step image, in order. */
fun EditUiState.referencedPaths(): List<String> =
    listOfNotNull(imagePath) + steps.mapNotNull { it.imagePath }

/** Maps the form to entities, trimming text and dropping empty rows. recipeId on children is
 * a placeholder — the DAO reassigns it during upsert. */
fun EditUiState.toEntities(): Triple<Recipe, List<Ingredient>, List<Step>> {
    val recipe = Recipe(
        id = id,
        name = name.trim(),
        imageUri = imagePath,
        prepTimeMinutes = prepTime.trim().toIntOrNull(),
        servings = servings.trim().toIntOrNull(),
    )
    val ingredients = ingredients
        .map { it.text.trim() }
        .filter { it.isNotEmpty() }
        .mapIndexed { index, text -> Ingredient(recipeId = id, text = text, position = index) }
    val steps = steps
        .filter { it.text.isNotBlank() || it.imagePath != null }
        .mapIndexed { index, row ->
            Step(recipeId = id, text = row.text.trim(), imageUri = row.imagePath, position = index)
        }
    return Triple(recipe, ingredients, steps)
}

fun RecipeWithDetails.toUiState(): EditUiState = EditUiState(
    id = recipe.id,
    name = recipe.name,
    prepTime = recipe.prepTimeMinutes?.toString() ?: "",
    servings = recipe.servings?.toString() ?: "",
    imagePath = recipe.imageUri,
    ingredients = ingredients.sortedBy { it.position }
        .map { IngredientRow(it.text) }
        .ifEmpty { listOf(IngredientRow()) },
    steps = steps.sortedBy { it.position }
        .map { StepRow(it.text, it.imageUri) }
        .ifEmpty { listOf(StepRow()) },
)
