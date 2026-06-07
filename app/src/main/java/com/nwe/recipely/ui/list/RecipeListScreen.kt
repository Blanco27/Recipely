package com.nwe.recipely.ui.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.recipely.R
import com.nwe.recipely.RecipelyApp
import com.nwe.recipely.data.RecipeCategory
import com.nwe.recipely.ui.theme.Fraunces
import com.nwe.recipely.ui.theme.Paper
import com.nwe.recipely.ui.theme.PaperDark

@Composable
fun RecipeListScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val container = (LocalContext.current.applicationContext as RecipelyApp).container
    val vm: RecipeListViewModel = viewModel(
        factory = viewModelFactory { initializer { RecipeListViewModel(container.repository) } }
    )
    val recipes by vm.recipes.collectAsState()
    val availableCategories by vm.availableCategories.collectAsState()
    val selectedCategory by vm.selectedCategory.collectAsState()

    // If the selected category no longer has any recipes (e.g. the last one was deleted),
    // fall back to "All" so the list never shows an empty filtered result with no matching pill.
    LaunchedEffect(availableCategories, selectedCategory) {
        if (selectedCategory != null && availableCategories.none { it.key == selectedCategory }) {
            vm.selectCategory(null)
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_recipe)) },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = RoundedCornerShape(20.dp),
            )
        },
    ) { padding ->
        if (recipes.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize()) {
                SettingsButton(onOpenSettings, Modifier.align(Alignment.TopEnd))
                EmptyState(Modifier.fillMaxSize())
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { ListHeader(count = recipes.size, onOpenSettings = onOpenSettings) }
                if (availableCategories.isNotEmpty()) {
                    item {
                        FilterRow(
                            categories = availableCategories,
                            selected = selectedCategory,
                            onSelect = vm::selectCategory,
                        )
                    }
                }
                items(recipes, key = { it.id }) { recipe ->
                    RecipeCard(recipe = recipe, onClick = { onOpen(recipe.id) })
                }
            }
        }
    }
}

@Composable
private fun ListHeader(count: Int, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pluralStringResource(R.plurals.recipe_count, count, count).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = stringResource(R.string.list_title),
                style = MaterialTheme.typography.displaySmall,
                fontFamily = Fraunces,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        SettingsButton(onOpenSettings)
    }
}

@Composable
private fun SettingsButton(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onOpenSettings, modifier = modifier) {
        Icon(
            Icons.Outlined.Settings,
            contentDescription = stringResource(R.string.settings),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Restaurant,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = Fraunces,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun FilterRow(
    categories: List<RecipeCategory>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterPill(
            label = stringResource(R.string.filter_all),
            selected = selected == null,
            onClick = { onSelect(null) },
        )
        categories.forEach { category ->
            FilterPill(
                label = category.emoji + " " + stringResource(category.labelRes),
                selected = selected == category.key,
                onClick = { onSelect(category.key) },
            )
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else if (isSystemInDarkTheme()) PaperDark else Paper
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier.selectable(
            selected = selected,
            role = Role.Tab,
            onClick = onClick,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
