package com.nwe.recipely

import android.app.Application
import com.nwe.recipely.di.AppContainer

class RecipelyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
