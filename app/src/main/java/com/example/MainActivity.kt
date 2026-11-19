package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.KigoApp

class MainActivity : ComponentActivity() {
    private var deepLinkKigoId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLinkKigoId = extractKigoIdFromIntent(intent)

        setContent {
            KigoApp(
                deepLinkKigoId = deepLinkKigoId,
                onConsumeDeepLink = { deepLinkKigoId = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkKigoId = extractKigoIdFromIntent(intent)
    }

    private fun extractKigoIdFromIntent(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme == "kigochat" && data.host == "connect") {
            return data.getQueryParameter("id")?.trim()?.uppercase()?.takeIf { it.startsWith("KIGO-") }
        }
        return null
    }
}
