package com.nwe.recipely

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nwe.recipely.navigation.RecipelyNavHost
import com.nwe.recipely.ui.theme.RecipelyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecipelyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RecipelyNavHost()
                }
            }
        }
    }
}
