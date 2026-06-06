package com.nwe.recipely

import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeRepository
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeRecipeRepository : RecipeRepository {

    val recipes = MutableStateFlow<List<Recipe>>(emptyList())
    var detail = MutableStateFlow<RecipeWithDetails?>(null)

    // Recorded calls for assertions
    var lastSavedRecipe: Recipe? = null
    var lastSavedIngredients: List<Ingredient> = emptyList()
    var lastSavedSteps: List<Step> = emptyList()
    var lastRemovedImagePaths: List<String> = emptyList()
    var discardedImages: List<String> = emptyList()
    var deletedDetails: RecipeWithDetails? = null
    var saveCount = 0

    override fun observeRecipes(): Flow<List<Recipe>> = recipes
    override fun observeRecipe(id: Long): Flow<RecipeWithDetails?> = detail

    override suspend fun save(
        recipe: Recipe,
        ingredients: List<Ingredient>,
        steps: List<Step>,
        removedImagePaths: List<String>,
    ): Long {
        saveCount++
        lastSavedRecipe = recipe
        lastSavedIngredients = ingredients
        lastSavedSteps = steps
        lastRemovedImagePaths = removedImagePaths
        return if (recipe.id == 0L) 1L else recipe.id
    }

    override suspend fun delete(details: RecipeWithDetails) {
        deletedDetails = details
    }

    override suspend fun discardImages(paths: List<String>) {
        discardedImages = paths
    }
}
