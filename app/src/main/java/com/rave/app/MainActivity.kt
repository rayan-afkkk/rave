package com.rave.app

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.rave.app.core.DeepLinkHandler
import com.rave.app.domain.model.ThemeMode
import com.rave.app.ui.RaveApp
import com.rave.app.ui.splash.SplashOverlay
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Single activity hosting the whole Compose UI. Handles App Links and notification deep links. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    @Inject lateinit var deepLinks: DeepLinkHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        // Black system splash; it hands over straight away to the animated SplashOverlay.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        if (savedInstanceState == null) handleIntent(intent)
        lifecycleScope.launch { deepLinks.checkInstallReferrer(applicationContext) }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val appStyle by viewModel.appStyle.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // Once per launch (not again on rotation).
            var showSplash by rememberSaveable { mutableStateOf(savedInstanceState == null) }
            // Light status-bar icons while the black splash is up.
            val darkBars = darkTheme || showSplash

            // Keep status/nav bar icon colours in sync with the in-app theme, not just the system one.
            DisposableEffect(darkBars) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkBars },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkBars },
                )
                hideNavigationBar()
                onDispose { }
            }

            RaveTheme(darkTheme = darkTheme, style = appStyle) {
                Box {
                    RaveApp(viewModel)
                    if (showSplash) SplashOverlay(onFinished = { showSplash = false })
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Coming back from another app, a dialog or the keyboard can bring the bar back.
        if (hasFocus) hideNavigationBar()
    }

    /**
     * Full-screen feel: the system navigation bar (buttons or gesture handle) is hidden.
     * Swiping up from the bottom shows it briefly, so home/back/recents stay one swipe away.
     */
    private fun hideNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No grey scrim behind the bar when it is swiped into view.
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        lifecycleScope.launch { deepLinks.handle(intent) }
    }
}
