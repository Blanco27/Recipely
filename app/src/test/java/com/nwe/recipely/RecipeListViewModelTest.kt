package com.nwe.recipely

import com.nwe.recipely.data.Recipe
import com.nwe.recipely.ui.list.RecipeListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun exposesRecipesFromRepository() = runTest {
        val repo = FakeRecipeRepository()
        val vm = RecipeListViewModel(repo)
        // WhileSubscribed only activates the upstream flow while there is a
        // collector, so subscribe in the background to drive stateIn.
        backgroundScope.launch { vm.recipes.collect {} }
        repo.recipes.value = listOf(Recipe(id = 1, name = "A"), Recipe(id = 2, name = "B"))
        advanceUntilIdle()
        assertEquals(listOf("A", "B"), vm.recipes.value.map { it.name })
    }
}
