package com.makay.launcher

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makay.launcher.domain.Overlay
import com.makay.launcher.ui.HomeViewModel
import com.makay.launcher.ui.home.HomeScreen
import com.makay.launcher.ui.theme.MakayTheme

class HomeActivity : FragmentActivity() {
    private val viewModel: HomeViewModel by viewModels { HomeViewModel.factory(application) }
    private val app by lazy { application as MakayApp }

    private val bindWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        app.widgetController.onBindResult(result.resultCode == Activity.RESULT_OK)
    }

    private val configureWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        app.widgetController.onConfigureResult(result.resultCode == Activity.RESULT_OK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        app.widgetController.bindHandler = { bindWidgetLauncher.launch(it) }
        app.widgetController.configureHandler = { configureWidgetLauncher.launch(it) }
        viewModel.authenticate = { title, onResult ->
            if (viewModel.state.value.profile.biometric) promptBiometric(title, onResult)
            else onResult(true)
        }
        handleImportIntent(intent)
        enableEdgeToEdge()
        setContent {
            val state = viewModel.state.collectAsStateWithLifecycle().value
            LaunchedEffect(state.profile.widgets) {
                if (state.profile.widgets) {
                    runCatching { app.widgetController.host.startListening() }
                } else {
                    runCatching { app.widgetController.host.stopListening() }
                }
            }
            MakayTheme(dynamicColors = state.settings.dynamicColors) {
                BackHandler(enabled = true) {
                    when {
                        state.overlay == Overlay.Update -> viewModel.postponeUpdate()
                        state.overlay != Overlay.None || state.shutterApp != null -> viewModel.closeOverlays()
                    }
                }
                HomeScreen(
                    state = state,
                    viewModel = viewModel,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (viewModel.state.value.profile.widgets) {
            runCatching { app.widgetController.host.startListening() }
        }
    }

    override fun onStop() {
        runCatching { app.widgetController.host.stopListening() }
        super.onStop()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleImportIntent(intent)
        viewModel.closeOverlays()
    }

    private fun handleImportIntent(intent: android.content.Intent?) {
        if (intent?.action != "com.makay.launcher.IMPORT_SETTINGS") return
        @Suppress("DEPRECATION")
        val uri = intent.data
            ?: intent.getParcelableExtra<android.net.Uri>("uri")
            ?: intent.clipData?.getItemAt(0)?.uri
        uri?.let(viewModel::importSettingsFromAdbUri)
        val parental = intent.getStringExtra(com.makay.launcher.data.MakayAccessibilityService.EXTRA_PARENTAL_PKG)
        if (!parental.isNullOrBlank()) {
            val appInfo = viewModel.state.value.apps.find { it.packageName == parental }
            if (appInfo != null) viewModel.launch(appInfo)
        }
    }

    override fun onResume() {
        super.onResume()
        overridePendingTransition(0, 0)
        viewModel.refreshDefaultStatus()
        viewModel.refreshStats()
        viewModel.refreshFormFactor()
        viewModel.refreshGlance()
        viewModel.pruneStaleItems()
        viewModel.checkForUpdate()
        viewModel.bumpIdle()
        viewModel.consumeBootAutostart()
        val profile = viewModel.state.value.profile
        viewModel.setGestureNavigation(profile.gestures && isGestureNavigation())
        if (profile.widgets) {
            runCatching { app.widgetController.host.startListening() }
        } else {
            runCatching { app.widgetController.host.stopListening() }
        }
    }

    override fun onPause() {
        overridePendingTransition(0, 0)
        super.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.refreshFormFactor()
        viewModel.refreshGlance()
    }

    override fun onDestroy() {
        app.widgetController.bindHandler = null
        app.widgetController.configureHandler = null
        viewModel.authenticate = null
        super.onDestroy()
    }

    private fun isGestureNavigation(): Boolean {
        val root = window.decorView.rootWindowInsets ?: return false
        val compat = WindowInsetsCompat.toWindowInsetsCompat(root, window.decorView)
        val nav = compat.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        return nav < resources.displayMetrics.density * 40
    }

    private fun promptBiometric(title: String, onResult: (Boolean) -> Unit) {
        val manager = BiometricManager.from(this)
        val can = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            val allowWithoutPrompt = can == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ||
                can == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ||
                can == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
            onResult(allowWithoutPrompt)
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onResult(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onResult(false)
                }

                override fun onAuthenticationFailed() {
                    onResult(false)
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle("Makay Launcher kilit doğrulaması")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
                .build(),
        )
    }
}
