package com.nwe.recipely.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nwe.recipely.ui.cook.CookModeScreen
import com.nwe.recipely.ui.detail.RecipeDetailScreen
import com.nwe.recipely.ui.edit.RecipeEditScreen
import com.nwe.recipely.ui.list.RecipeListScreen
import com.nwe.recipely.ui.settings.SettingsScreen

object Routes {
    const val LIST = "list"
    const val DETAIL = "detail/{id}"
    const val EDIT = "edit?id={id}"
    const val SETTINGS = "settings"
    const val COOK = "cook/{id}"

    fun detail(id: Long) = "detail/$id"
    fun edit(id: Long? = null) = if (id == null) "edit" else "edit?id=$id"
    fun cook(id: Long) = "cook/$id"
}

@Composable
fun RecipelyNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LIST) {

        composable(Routes.LIST) {
            RecipeListScreen(
                onAdd = { navController.navigate(Routes.edit()) },
                onOpen = { id -> navController.navigate(Routes.detail(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments!!.getLong("id")
            RecipeDetailScreen(
                recipeId = id,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.edit(id)) },
                onDeleted = { navController.popBackStack() },
                onCook = { navController.navigate(Routes.cook(id)) },
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("id") {
                type = NavType.LongType
                defaultValue = 0L
            }),
        ) { entry ->
            val id = entry.arguments!!.getLong("id")
            RecipeEditScreen(
                recipeId = id,
                onClose = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.COOK,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments!!.getLong("id")
            CookModeScreen(
                recipeId = id,
                onExit = { navController.popBackStack() },
            )
        }
    }
}
