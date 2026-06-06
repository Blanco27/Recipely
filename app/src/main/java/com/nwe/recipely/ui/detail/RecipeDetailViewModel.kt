package com.nwe.recipely.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nwe.recipely.data.RecipeRepository
import com.nwe.recipely.data.RecipeWithDetails
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipeDetailViewModel(
    private val repository: RecipeRepository,
    recipeId: Long,
) : ViewModel() {

    val recipe: StateFlow<RecipeWithDetails?> = repository.observeRecipe(recipeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDeleted: () -> Unit) {
        val current = recipe.value ?: return
        viewModelScope.launch {
            repository.delete(current)
            onDeleted()
        }
    }
}
