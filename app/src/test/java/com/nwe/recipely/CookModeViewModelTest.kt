package com.nwe.recipely

import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import com.nwe.recipely.ui.cook.CookModeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CookModeViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private fun details(vararg steps: Step) = RecipeWithDetails(
        recipe = Recipe(id = 1, name = "Bolognese"),
        ingredients = emptyList(),
        steps = steps.toList(),
    )

    @Test
    fun steps_areSortedByPosition_andEmptyOnesDropped() = runTest {
        val repo = FakeRecipeRepository()
        val vm = CookModeViewModel(repo, recipeId = 1)
        backgroundScope.launch { vm.steps.collect {} }
        repo.detail.value = details(
            Step(id = 3, recipeId = 1, text = "Third", position = 2),
            Step(id = 1, recipeId = 1, text = "First", position = 0),
            Step(id = 4, recipeId = 1, text = "", imageUri = "/img.jpg", position = 3), // image-only: kept
            Step(id = 2, recipeId = 1, text = "   ", imageUri = null, position = 1),     // truly empty: dropped
        )
        advanceUntilIdle()
        assertEquals(listOf("First", "Third", ""), vm.steps.value.map { it.text })
    }

    @Test
    fun recipe_exposesLoadedRecipeFromRepository() = runTest {
        val repo = FakeRecipeRepository()
        val vm = CookModeViewModel(repo, recipeId = 1)
        backgroundScope.launch { vm.recipe.collect {} }
        repo.detail.value = details(
            Step(id = 1, recipeId = 1, text = "First", position = 0),
        )
        advanceUntilIdle()
        assertEquals("Bolognese", vm.recipe.value?.recipe?.name)
    }

    @Test
    fun finished_defaultsFalse_finishSetsTrue_restartClears() = runTest {
        val repo = FakeRecipeRepository()
        val vm = CookModeViewModel(repo, recipeId = 1)
        backgroundScope.launch { vm.finished.collect {} }
        assertFalse(vm.finished.value)
        vm.finish()
        advanceUntilIdle()
        assertTrue(vm.finished.value)
        vm.restart()
        advanceUntilIdle()
        assertFalse(vm.finished.value)
    }
}
