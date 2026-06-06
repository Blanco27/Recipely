package com.nwe.recipely.di

import android.content.Context

/**
 * Manual dependency container. Members (database, dao, imageStore, repository)
 * are added in later tasks. Held by [com.nwe.recipely.RecipelyApp].
 */
class AppContainer(private val context: Context)
