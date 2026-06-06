package com.nwe.recipely.di

import android.content.Context
import androidx.room.Room
import com.nwe.recipely.data.ImageStore
import com.nwe.recipely.data.RecipeDatabase
import com.nwe.recipely.data.RecipeRepository
import com.nwe.recipely.data.RoomRecipeRepository

class AppContainer(context: Context) {

    private val database: RecipeDatabase = Room.databaseBuilder(
        context,
        RecipeDatabase::class.java,
        "recipely.db",
    ).build()

    val imageStore: ImageStore = ImageStore(context)

    val repository: RecipeRepository = RoomRecipeRepository(database.recipeDao(), imageStore)
}
