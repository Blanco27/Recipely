package com.nwe.recipely.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY name COLLATE NOCASE ASC")
    fun observeRecipes(): Flow<List<Recipe>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeRecipe(id: Long): Flow<RecipeWithDetails?>

    @Transaction
    @Query("SELECT * FROM recipes")
    suspend fun getAllRecipesWithDetails(): List<RecipeWithDetails>

    @Insert
    suspend fun insertRecipe(recipe: Recipe): Long

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Insert
    suspend fun insertIngredients(items: List<Ingredient>)

    @Insert
    suspend fun insertSteps(items: List<Step>)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsFor(recipeId: Long)

    @Query("DELETE FROM steps WHERE recipeId = :recipeId")
    suspend fun deleteStepsFor(recipeId: Long)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: Long)

    /** Inserts each imported recipe as a brand-new row (merge import). All-or-nothing. */
    @Transaction
    suspend fun insertImported(recipes: List<RecipeWithDetails>) {
        recipes.forEach {
            upsertRecipeWithChildren(it.recipe.copy(id = 0), it.ingredients, it.steps)
        }
    }

    /**
     * Inserts a new recipe (id == 0) or updates an existing one, then replaces all of its
     * ingredients and steps. Child rows get their [recipeId] reassigned and ids reset so they
     * are inserted fresh. Returns the recipe id.
     */
    @Transaction
    suspend fun upsertRecipeWithChildren(
        recipe: Recipe,
        ingredients: List<Ingredient>,
        steps: List<Step>,
    ): Long {
        val recipeId = if (recipe.id == 0L) {
            insertRecipe(recipe)
        } else {
            updateRecipe(recipe)
            recipe.id
        }
        deleteIngredientsFor(recipeId)
        deleteStepsFor(recipeId)
        insertIngredients(ingredients.map { it.copy(id = 0, recipeId = recipeId) })
        insertSteps(steps.map { it.copy(id = 0, recipeId = recipeId) })
        return recipeId
    }
}
