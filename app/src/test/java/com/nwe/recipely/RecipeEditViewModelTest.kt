package com.nwe.recipely

import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import com.nwe.recipely.ui.edit.RecipeEditViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun newViewModel(repo: FakeRecipeRepository, id: Long = 0L) =
        RecipeEditViewModel(repo, id)

    @Test
    fun newRecipe_cannotSaveUntilNameEntered() {
        val vm = newViewModel(FakeRecipeRepository())
        assertFalse(vm.state.value.canSave)
        vm.setName("Soup")
        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun addAndRemoveIngredient() {
        val vm = newViewModel(FakeRecipeRepository())
        assertEquals(1, vm.state.value.ingredients.size)
        vm.addIngredient()
        assertEquals(2, vm.state.value.ingredients.size)
        vm.setIngredient(0, "Salt")
        assertEquals("Salt", vm.state.value.ingredients[0].text)
        vm.removeIngredient(0)
        assertEquals(1, vm.state.value.ingredients.size)
    }

    @Test
    fun addAndRemoveStep() {
        val vm = newViewModel(FakeRecipeRepository())
        vm.addStep()
        assertEquals(2, vm.state.value.steps.size)
        vm.setStepText(1, "Stir")
        assertEquals("Stir", vm.state.value.steps[1].text)
        vm.removeStep(1)
        assertEquals(1, vm.state.value.steps.size)
    }

    @Test
    fun save_passesMappedEntitiesToRepository() = runTest {
        val repo = FakeRecipeRepository()
        val vm = newViewModel(repo)
        vm.setName("Carbonara")
        vm.setPrepTime("30")
        vm.setIngredient(0, "Spaghetti")
        vm.setStepText(0, "Boil")

        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()

        assertTrue(saved)
        assertEquals("Carbonara", repo.lastSavedRecipe?.name)
        assertEquals(30, repo.lastSavedRecipe?.prepTimeMinutes)
        assertEquals(listOf("Spaghetti"), repo.lastSavedIngredients.map { it.text })
        assertEquals(listOf("Boil"), repo.lastSavedSteps.map { it.text })
    }

    @Test
    fun save_doesNothing_whenNameBlank() = runTest {
        val repo = FakeRecipeRepository()
        val vm = newViewModel(repo)
        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()
        assertFalse(saved)
        assertEquals(null, repo.lastSavedRecipe)
    }

    @Test
    fun replacingTitleImage_marksOldImageAsOrphanOnSave() = runTest {
        val repo = FakeRecipeRepository()
        val vm = newViewModel(repo)
        vm.setName("X")
        vm.setTitleImage("/a.jpg")   // newly imported
        vm.setTitleImage("/b.jpg")   // replaces it; /a.jpg now orphaned
        vm.save {}
        advanceUntilIdle()
        assertTrue(repo.lastRemovedImagePaths.contains("/a.jpg"))
        assertFalse(repo.lastRemovedImagePaths.contains("/b.jpg"))
    }

    @Test
    fun discardChanges_deletesNewlyImportedImages() = runTest {
        val repo = FakeRecipeRepository()
        val vm = newViewModel(repo)
        vm.setName("X")
        vm.setTitleImage("/a.jpg")
        vm.discardChanges()
        advanceUntilIdle()
        assertEquals(listOf("/a.jpg"), repo.discardedImages)
    }

    @Test
    fun loadingExistingRecipe_populatesState() = runTest {
        val repo = FakeRecipeRepository()
        repo.detail.value = RecipeWithDetails(
            recipe = Recipe(id = 5, name = "Loaded", prepTimeMinutes = 12),
            ingredients = emptyList(),
            steps = listOf(Step(recipeId = 5, text = "step", imageUri = "/s.jpg", position = 0)),
        )
        val vm = newViewModel(repo, id = 5L)
        advanceUntilIdle()
        assertEquals("Loaded", vm.state.value.name)
        assertEquals("12", vm.state.value.prepTime)
        assertEquals("/s.jpg", vm.state.value.steps[0].imagePath)
    }
}
