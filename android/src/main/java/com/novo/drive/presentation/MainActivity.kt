package com.novo.drive.presentation

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.IntentCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.novo.drive.core.common.AppNotifications
import com.novo.drive.core.files.PendingShare
import com.novo.drive.core.media.PipController
import com.novo.drive.domain.model.AppLanguage
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.presentation.platform.ProvidePlatformActions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Locale

/** BiometricPrompt requires a FragmentActivity. */
class MainActivity : FragmentActivity() {

    private val settingsRepository: SettingsRepository by inject()
    private val pipController: PipController by inject()


    /**
     * The launch theme can only name a static color, so the window is repainted with the theme's
     * color to hide the seam.
     */
    private fun applyWindowBackground() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val dark = nightMode == Configuration.UI_MODE_NIGHT_YES
        val scheme = if (dark) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
        window.setBackgroundDrawable(scheme.background.toArgb().toDrawable())
    }

    private fun observeScreenshotProtection() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.preferences
                    .map { it.appLockEnabled || it.blockScreenCapture }
                    .distinctUntilChanged()
                    .collect { locked ->
                        if (locked) {
                            window.setFlags(
                                WindowManager.LayoutParams.FLAG_SECURE,
                                WindowManager.LayoutParams.FLAG_SECURE
                            )
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                        pipController.setSecureWindow(locked)
                    }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        pipController.enter(this)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipController.onModeChanged(isInPictureInPictureMode)
    }

    private val pendingShare: PendingShare by inject()

    private val destination = MutableStateFlow<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val dataUri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data
        if (dataUri != null && (dataUri.scheme == "dewan" || dataUri.scheme == "kunjodrive")) {
            destination.value = "${com.novo.drive.core.common.NotificationDestinations.DEWAN_JOIN_PREFIX}$dataUri"
            return
        }
        val shared = sharedUris(intent)
        val sharedText = intent?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)
        pendingShare.offer(shared.map { it.toString() })
        pendingShare.offerText(sharedText)
        destination.value = when {
            shared.isNotEmpty() -> AppNotifications.DESTINATION_FILES
            !sharedText.isNullOrBlank() -> AppNotifications.DESTINATION_NOTE
            else -> destinationOf(intent)
        }
    }

    private fun sharedUris(intent: Intent?): List<Uri> = when (intent?.action) {
        Intent.ACTION_SEND ->
            listOfNotNull(
                IntentCompat.getParcelableExtra(
                    intent,
                    Intent.EXTRA_STREAM,
                    Uri::class.java
                )
            )

        Intent.ACTION_SEND_MULTIPLE ->
            IntentCompat.getParcelableArrayListExtra(
                intent,
                Intent.EXTRA_STREAM,
                Uri::class.java
            ).orEmpty()

        else -> emptyList()
    }

    private fun destinationOf(intent: Intent?): String? = intent
        ?.takeIf { it.action == AppNotifications.ACTION_OPEN_DESTINATION }
        ?.getStringExtra(AppNotifications.EXTRA_DESTINATION)

    private var appliedLegacyLangCode: String = AppLanguage.ENGLISH.code

    override fun attachBaseContext(newBase: Context) {
        val cached = newBase.getSharedPreferences(LOCALE_CACHE_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOCALE_CODE, AppLanguage.ENGLISH.code)
            ?: AppLanguage.ENGLISH.code
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (cached == AppLanguage.ENGLISH.code) {
                Locale.setDefault(Locale.forLanguageTag("en"))
            }
            super.attachBaseContext(newBase)
            return
        }
        appliedLegacyLangCode = cached
        if (cached == AppLanguage.ENGLISH.code || cached == AppLanguage.BENGALI.code) {
            val target = Locale.forLanguageTag(cached)
            Locale.setDefault(target)
            val config = Configuration(newBase.resources.configuration).apply {
                setLocale(target)
            }
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    private fun observeLanguage() {
        val systemLocale = Resources.getSystem().configuration.locales[0]
        lifecycleScope.launch {
            settingsRepository.preferences
                .map { it.language }
                .distinctUntilChanged()
                .collect { lang ->
                    val target = when (lang) {
                        AppLanguage.ENGLISH -> Locale.forLanguageTag("en")
                        AppLanguage.BENGALI -> Locale.forLanguageTag("bn")
                        AppLanguage.SYSTEM -> systemLocale
                    }
                    Locale.setDefault(target)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val lm = getSystemService(LocaleManager::class.java)
                        val desired = when (lang) {
                            AppLanguage.SYSTEM -> LocaleList.getEmptyLocaleList()
                            AppLanguage.ENGLISH -> LocaleList.forLanguageTags("en")
                            AppLanguage.BENGALI -> LocaleList.forLanguageTags("bn")
                        }
                        if (lm != null && lm.applicationLocales != desired) {
                            lm.applicationLocales = desired
                        }
                    } else if (lang.code != appliedLegacyLangCode) {
                        getSharedPreferences(LOCALE_CACHE_PREFS, Context.MODE_PRIVATE)
                            .edit()
                            .putString(KEY_LOCALE_CODE, lang.code)
                            .apply()
                        appliedLegacyLangCode = lang.code
                        recreate()
                    }
                }
        }
    }

    private companion object {
        const val LOCALE_CACHE_PREFS = "locale_cache"
        const val KEY_LOCALE_CODE = "lang"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeLanguage()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        applyWindowBackground()
        observeScreenshotProtection()
        handleIntent(intent)
        setContent {
            ProvidePlatformActions {
                IplusDriveApp(
                    pendingShare = pendingShare,
                    notificationDestination = destination.collectAsStateWithLifecycle().value,
                    onDestinationHandled = { destination.value = null }
                )
            }
        }
    }
}
