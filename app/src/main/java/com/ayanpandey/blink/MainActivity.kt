package com.ayanpandey.blink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.ayanpandey.blink.core.designsystem.theme.BlinkTheme
import com.ayanpandey.blink.navigation.BlinkNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (application as BlinkApplication).container
        appContainer.logger.i(TAG, "MainActivity onCreate starting.")

        setContent {
            BlinkTheme {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                ) {
                    BlinkNavHost(navController = navController)
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
