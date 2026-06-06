package com.nwe.recipely

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeDao
import com.nwe.recipely.data.RecipeDatabase
import com.nwe.recipely.data.Step
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeDaoTest {

    private lateinit var db: RecipeDatabase
    private lateinit var dao: RecipeDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java)
            .build()
        dao = db.recipeDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsert_thenObserve_returnsRecipeWithSortedChildren() = runTest {
        val id = dao.upsertRecipeWithChildren(
            recipe = Recipe(name = "Carbonara", prepTimeMinutes = 30, servings = 4),
            ingredients = listOf(
                Ingredient(recipeId = 0, text = "Spaghetti", position = 0),
                Ingredient(recipeId = 0, text = "Pancetta", position = 1),
            ),
            steps = listOf(
                Step(recipeId = 0, text = "Boil pasta", position = 0),
                Step(recipeId = 0, text = "Fry pancetta", imageUri = "/x.jpg", position = 1),
            ),
        )

        val details = dao.observeRecipe(id).first()!!
        assertEquals("Carbonara", details.recipe.name)
        assertEquals(2, details.ingredients.size)
        assertEquals(2, details.steps.size)
        assertEquals("Spaghetti", details.ingredients.sortedBy { it.position }[0].text)
        assertEquals("/x.jpg", details.steps.sortedBy { it.position }[1].imageUri)
    }

    @Test
    fun upsert_existingRecipe_replacesChildren() = runTest {
        val id = dao.upsertRecipeWithChildren(
            recipe = Recipe(name = "A"),
            ingredients = listOf(Ingredient(recipeId = 0, text = "old", position = 0)),
            steps = emptyList(),
        )
        dao.upsertRecipeWithChildren(
            recipe = Recipe(id = id, name = "A renamed"),
            ingredients = listOf(Ingredient(recipeId = 0, text = "new", position = 0)),
            steps = emptyList(),
        )

        val details = dao.observeRecipe(id).first()!!
        assertEquals("A renamed", details.recipe.name)
        assertEquals(1, details.ingredients.size)
        assertEquals("new", details.ingredients[0].text)
    }

    @Test
    fun deleteRecipe_cascadesToChildren() = runTest {
        val id = dao.upsertRecipeWithChildren(
            recipe = Recipe(name = "ToDelete"),
            ingredients = listOf(Ingredient(recipeId = 0, text = "x", position = 0)),
            steps = listOf(Step(recipeId = 0, text = "y", position = 0)),
        )

        dao.deleteRecipe(id)

        assertNull(dao.observeRecipe(id).first())
        assertEquals(0, dao.observeRecipes().first().size)
    }
}
