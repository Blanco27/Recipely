package com.nwe.recipely

import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeCategory
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

    @Test
    fun selectCategory_filtersRecipes() = runTest {
        val repo = FakeRecipeRepository()
        val vm = RecipeListViewModel(repo)
        backgroundScope.launch { vm.recipes.collect {} }
        repo.recipes.value = listOf(
            Recipe(id = 1, name = "Pasta", category = "MAIN"),
            Recipe(id = 2, name = "Cake", category = "DESSERT"),
            Recipe(id = 3, name = "Toast", category = null),
        )
        advanceUntilIdle()
        vm.selectCategory("MAIN")
        advanceUntilIdle()
        assertEquals(listOf("Pasta"), vm.recipes.value.map { it.name })
    }

    @Test
    fun nullSelection_returnsAllRecipes() = runTest {
        val repo = FakeRecipeRepository()
        val vm = RecipeListViewModel(repo)
        backgroundScope.launch { vm.recipes.collect {} }
        repo.recipes.value = listOf(
            Recipe(id = 1, name = "Pasta", category = "MAIN"),
            Recipe(id = 2, name = "Cake", category = "DESSERT"),
        )
        advanceUntilIdle()
        assertEquals(listOf("Pasta", "Cake"), vm.recipes.value.map { it.name })
    }

    @Test
    fun availableCategories_listsPresentCategoriesInEnumOrder() = runTest {
        val repo = FakeRecipeRepository()
        val vm = RecipeListViewModel(repo)
        backgroundScope.launch { vm.availableCategories.collect {} }
        repo.recipes.value = listOf(
            Recipe(id = 1, name = "Cake", category = "DESSERT"),
            Recipe(id = 2, name = "Pasta", category = "MAIN"),
            Recipe(id = 3, name = "Toast", category = null),
            Recipe(id = 4, name = "Junk", category = "BOGUS"),
        )
        advanceUntilIdle()
        // enum order puts MAIN before DESSERT; null and unknown keys are excluded.
        assertEquals(listOf(RecipeCategory.MAIN, RecipeCategory.DESSERT), vm.availableCategories.value)
    }

    @Test
    fun removingLastRecipeOfCategory_dropsItFromAvailable() = runTest {
        val repo = FakeRecipeRepository()
        val vm = RecipeListViewModel(repo)
        backgroundScope.launch { vm.availableCategories.collect {} }
        repo.recipes.value = listOf(Recipe(id = 1, name = "Cake", category = "DESSERT"))
        advanceUntilIdle()
        assertEquals(listOf(RecipeCategory.DESSERT), vm.availableCategories.value)
        repo.recipes.value = emptyList()
        advanceUntilIdle()
        assertEquals(emptyList<RecipeCategory>(), vm.availableCategories.value)
    }
}
