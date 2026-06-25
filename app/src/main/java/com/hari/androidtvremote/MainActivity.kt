package com.hari.androidtvremote

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hari.androidtvremote.navigation.AppNavGraph

import com.hari.androidtvremote.preference.LocalDarkTheme
import com.hari.androidtvremote.preference.LocalThemeIndex

import com.hari.androidtvremote.preference.DarkThemePreference
import com.hari.androidtvremote.preference.darkThemeFlow
import com.hari.androidtvremote.preference.themeIndexFlow
import com.hari.androidtvremote.ui.theme.AndroidTVRemoteTheme
import com.hari.androidtvremote.utils.Constant

class MainActivity : ComponentActivity() {

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val prefs = getSharedPreferences(Constant.PREFS_NAME, Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean(Constant.PREF_FIRST_LAUNCH, true)

        setContent {
            val context = LocalContext.current

            // Collect appearance DataStore flows as Compose state
            val darkThemeInt by context.darkThemeFlow()
                .collectAsStateWithLifecycle(initialValue = DarkThemePreference.UseDeviceTheme.value)
            val themeIndex by context.themeIndexFlow()
                .collectAsStateWithLifecycle(initialValue = 0)

            val darkTheme = DarkThemePreference.fromInt(darkThemeInt)
            CompositionLocalProvider(
                LocalDarkTheme provides darkTheme,
                LocalThemeIndex provides themeIndex,
            ) {
                AndroidTVRemoteTheme {
                    AppNavGraph(
                        showOnboarding = isFirstLaunch,
                        onFinishOnboarding = {
                            prefs.edit { putBoolean(Constant.PREF_FIRST_LAUNCH, false) }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences(Constant.PREFS_NAME, Context.MODE_PRIVATE)
        val autoReconnect = prefs.getBoolean(Constant.PREF_AUTO_RECONNECT, true)
        val lastHost = prefs.getString(Constant.HOST, null)
        val isPaired = prefs.getBoolean(Constant.PIN, false)

        if (autoReconnect && !lastHost.isNullOrBlank() && isPaired) {
            if (Constant.isConnected.value != true) {
                // SearchViewModel will handle reconnection when launched
            }
        }
    }

    override fun onStop() {
        super.onStop()
    }
}
