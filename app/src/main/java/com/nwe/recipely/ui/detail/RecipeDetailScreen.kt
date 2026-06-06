package com.nwe.recipely.ui.detail

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun RecipeDetailScreen(
    recipeId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
) {
    // Replaced with the full implementation in Task 14.
    Text("Detail $recipeId")
}
