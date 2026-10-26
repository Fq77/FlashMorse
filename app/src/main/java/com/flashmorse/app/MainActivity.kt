package com.flashmorse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.flashmorse.app.navigation.FlashMorseApp
import com.flashmorse.core.designsystem.theme.FlashMorseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as FlashMorseApplication).container
        setContent {
            FlashMorseTheme {
                FlashMorseApp(container = container)
            }
        }
    }
}
