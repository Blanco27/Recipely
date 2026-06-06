package com.nwe.recipely.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nwe.recipely.data.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RecipeEditViewModel(
    private val repository: RecipeRepository,
    private val recipeId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(EditUiState())
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    /** Image paths that already existed when the recipe was loaded. */
    private var originalImagePaths: Set<String> = emptySet()

    /** Image paths imported during this editing session (may or may not survive). */
    private val pendingNewImages = mutableSetOf<String>()

    init {
        if (recipeId != 0L) {
            viewModelScope.launch {
                val details = repository.observeRecipe(recipeId).first() ?: return@launch
                originalImagePaths = (listOfNotNull(details.recipe.imageUri) +
                    details.steps.mapNotNull { it.imageUri }).toSet()
                _state.value = details.toUiState()
            }
        }
    }

    fun setName(value: String) = update { it.copy(name = value) }
    fun setPrepTime(value: String) = update { it.copy(prepTime = value) }
    fun setServings(value: String) = update { it.copy(servings = value) }

    fun setTitleImage(path: String?) {
        if (path != null) pendingNewImages += path
        update { it.copy(imagePath = path) }
    }

    fun addIngredient() = update { it.copy(ingredients = it.ingredients + IngredientRow()) }

    fun setIngredient(index: Int, text: String) = update { s ->
        s.copy(ingredients = s.ingredients.mapIndexed { i, row ->
            if (i == index) row.copy(text = text) else row
        })
    }

    fun removeIngredient(index: Int) = update { s ->
        s.copy(ingredients = s.ingredients.filterIndexed { i, _ -> i != index })
    }

    fun addStep() = update { it.copy(steps = it.steps + StepRow()) }

    fun setStepText(index: Int, text: String) = update { s ->
        s.copy(steps = s.steps.mapIndexed { i, row -> if (i == index) row.copy(text = text) else row })
    }

    fun setStepImage(index: Int, path: String?) {
        if (path != null) pendingNewImages += path
        update { s ->
            s.copy(steps = s.steps.mapIndexed { i, row ->
                if (i == index) row.copy(imagePath = path) else row
            })
        }
    }

    fun removeStep(index: Int) = update { s ->
        s.copy(steps = s.steps.filterIndexed { i, _ -> i != index })
    }

    private var isSaving = false

    fun save(onSaved: () -> Unit) {
        val current = _state.value
        if (!current.canSave || isSaving) return
        isSaving = true
        val (recipe, ingredients, steps) = current.toEntities()
        val referenced = current.referencedPaths().toSet()
        val orphans = (originalImagePaths + pendingNewImages) - referenced
        viewModelScope.launch {
            try {
                repository.save(recipe, ingredients, steps, orphans.toList())
                // Persisted paths are now the baseline; nothing imported-but-unsaved remains.
                originalImagePaths = referenced
                pendingNewImages.clear()
                onSaved()
            } finally {
                isSaving = false
            }
        }
    }

    /** Call when the user cancels: deletes images imported during this session. */
    fun discardChanges() {
        val toDelete = pendingNewImages.toList()
        if (toDelete.isNotEmpty()) {
            viewModelScope.launch { repository.discardImages(toDelete) }
        }
    }

    private inline fun update(block: (EditUiState) -> EditUiState) {
        _state.value = block(_state.value)
    }
}
