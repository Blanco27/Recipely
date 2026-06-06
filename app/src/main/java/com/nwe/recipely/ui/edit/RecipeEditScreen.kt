package com.nwe.recipely.ui.edit

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun RecipeEditScreen(
    recipeId: Long,
    onClose: () -> Unit,
) {
    // Replaced with the full implementation in Task 15.
    Text("Edit $recipeId")
}
