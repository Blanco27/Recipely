package com.nwe.recipely.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Recipe::class, Ingredient::class, Step::class],
    version = 1,
    exportSchema = false,
)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
}
