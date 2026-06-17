package com.ayanpandey.blink

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.ayanpandey.blink.core.designsystem.theme.BlinkTheme
import com.ayanpandey.blink.core.navigation.Screen
import com.ayanpandey.blink.navigation.BlinkNavHost

class MainActivity : ComponentActivity() {
    private var intentUri = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (application as BlinkApplication).container
        appContainer.logger.i(TAG, "MainActivity onCreate starting.")

        intentUri.value = getUriFromIntent(intent)

        setContent {
            BlinkTheme {
                val navController = rememberNavController()

                val currentUri by intentUri
                LaunchedEffect(currentUri) {
                    currentUri?.let { uri ->
                        appContainer.logger.i(TAG, "Navigating to metadata screen for intent URI: $uri")
                        navController.navigate(Screen.Metadata.createRoute(uri))
                        intentUri.value = null
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                ) {
                    BlinkNavHost(navController = navController, appContainer = appContainer)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val appContainer = (application as BlinkApplication).container
        appContainer.logger.i(TAG, "MainActivity onNewIntent received.")
        intentUri.value = getUriFromIntent(intent)
    }

    private fun getUriFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        val action = intent.action
        val type = intent.type

        val uri =
            when (action) {
                Intent.ACTION_VIEW -> intent.data
                Intent.ACTION_SEND -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                    }
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    val list =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                        }
                    if (!list.isNullOrEmpty()) {
                        (application as BlinkApplication).container.logger.i(
                            TAG,
                            "ACTION_SEND_MULTIPLE received ${list.size} files, processing first one.",
                        )
                        list.firstOrNull()
                    } else {
                        null
                    }
                }
                else -> null
            }

        uri?.let {
            if (it.scheme == "content") {
                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (e: SecurityException) {
                    (application as BlinkApplication).container.logger.w(
                        TAG,
                        "Failed to take persistable URI permission: ${e.message}",
                    )
                }
            }
            (application as BlinkApplication).container.logger.i(
                TAG,
                "Acquired URI: $it from action: $action, type: $type",
            )
        }
        return uri?.toString()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
