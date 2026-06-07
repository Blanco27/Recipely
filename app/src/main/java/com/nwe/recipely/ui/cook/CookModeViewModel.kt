package com.nwe.recipely.ui.cook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nwe.recipely.data.RecipeRepository
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CookModeViewModel(
    repository: RecipeRepository,
    recipeId: Long,
) : ViewModel() {

    val recipe: StateFlow<RecipeWithDetails?> = repository.observeRecipe(recipeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Non-empty steps (text or image), ordered by position — the pages of the cook flow. */
    val steps: StateFlow<List<Step>> = recipe
        .map { details ->
            details?.steps.orEmpty()
                .filter { it.text.isNotBlank() || it.imageUri != null }
                .sortedBy { it.position }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _finished = MutableStateFlow(false)
    /** True once the user finishes the last step → show the completion screen. */
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    fun finish() { _finished.value = true }
    fun restart() { _finished.value = false }
}
