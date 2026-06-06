package com.nwe.recipely.data

import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun observeRecipes(): Flow<List<Recipe>>
    fun observeRecipe(id: Long): Flow<RecipeWithDetails?>

    /** Saves the recipe and its children, then deletes any [removedImagePaths] (orphaned files). */
    suspend fun save(
        recipe: Recipe,
        ingredients: List<Ingredient>,
        steps: List<Step>,
        removedImagePaths: List<String>,
    ): Long

    /** Deletes the recipe (cascades children) and removes all of its image files. */
    suspend fun delete(details: RecipeWithDetails)

    /** Deletes freshly-imported images that were never persisted (used on edit cancel). */
    suspend fun discardImages(paths: List<String>)
}

class RoomRecipeRepository(
    private val dao: RecipeDao,
    private val imageStore: ImageStore,
) : RecipeRepository {

    override fun observeRecipes(): Flow<List<Recipe>> = dao.observeRecipes()

    override fun observeRecipe(id: Long): Flow<RecipeWithDetails?> = dao.observeRecipe(id)

    override suspend fun save(
        recipe: Recipe,
        ingredients: List<Ingredient>,
        steps: List<Step>,
        removedImagePaths: List<String>,
    ): Long {
        val id = dao.upsertRecipeWithChildren(recipe, ingredients, steps)
        removedImagePaths.forEach(imageStore::delete)
        return id
    }

    override suspend fun delete(details: RecipeWithDetails) {
        dao.deleteRecipe(details.recipe.id)
        imageStore.delete(details.recipe.imageUri)
        details.steps.forEach { imageStore.delete(it.imageUri) }
    }

    override suspend fun discardImages(paths: List<String>) {
        paths.forEach(imageStore::delete)
    }
}
