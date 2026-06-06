package com.nwe.recipely.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeCategory
import com.nwe.recipely.data.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RecipeListViewModel(repository: RecipeRepository) : ViewModel() {

    private val allRecipes = repository.observeRecipes()

    /** Currently selected filter category key; null means "All". */
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    /** Categories that currently have at least one recipe, in enum order. */
    val availableCategories: StateFlow<List<RecipeCategory>> = allRecipes
        .map { recipes ->
            val present = recipes.mapNotNull { RecipeCategory.fromKey(it.category) }.toSet()
            RecipeCategory.entries.filter { it in present }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Recipes after applying the selected filter (all when nothing is selected). */
    val recipes: StateFlow<List<Recipe>> =
        combine(allRecipes, _selectedCategory) { recipes, selected ->
            if (selected == null) recipes else recipes.filter { it.category == selected }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectCategory(key: String?) {
        _selectedCategory.value = key
    }
}
