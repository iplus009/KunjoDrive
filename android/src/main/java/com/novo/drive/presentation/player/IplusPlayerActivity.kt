package com.novo.drive.presentation.player

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi
import com.novo.drive.core.media.PipController
import com.novo.drive.core.media.TelegramDataSourceFactory
import com.novo.drive.presentation.preview.IplusMediaPlayer
import com.novo.drive.presentation.preview.PreviewContent
import com.novo.drive.presentation.theme.DedicatedAcousticStudioTheme
import com.novo.drive.presentation.theme.DedicatedCinemaTheaterTheme
import java.io.File
import org.koin.android.ext.android.inject

@androidx.annotation.OptIn(UnstableApi::class)
class IplusPlayerActivity : FragmentActivity() {

    private val dataSourceFactory: TelegramDataSourceFactory by inject()
    private val pipController: PipController by inject()

    companion object {
        const val EXTRA_FILE_PATH = "com.novo.drive.extra.FILE_PATH"
        const val EXTRA_TITLE = "com.novo.drive.extra.TITLE"
        const val EXTRA_IS_AUDIO = "com.novo.drive.extra.IS_AUDIO"

        fun play(context: Context, file: File, isAudio: Boolean) {
            val intent = Intent(context, IplusPlayerActivity::class.java).apply {
                putExtra(EXTRA_FILE_PATH, file.absolutePath)
                putExtra(EXTRA_TITLE, file.name)
                putExtra(EXTRA_IS_AUDIO, isAudio)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val targetPath = resolvePath()
        val targetTitle = resolveTitle(targetPath)
        val isAudio = resolveIsAudio(targetPath)

        if (targetPath.isNullOrBlank()) {
            finish()
            return
        }

        setContent {
            if (isAudio) {
                DedicatedAcousticStudioTheme {
                    IplusPlayerContent(
                        path = targetPath,
                        title = targetTitle,
                        isAudio = true,
                        dataSourceFactory = dataSourceFactory,
                        onBack = { finish() }
                    )
                }
            } else {
                DedicatedCinemaTheaterTheme {
                    IplusPlayerContent(
                        path = targetPath,
                        title = targetTitle,
                        isAudio = false,
                        dataSourceFactory = dataSourceFactory,
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    private fun resolvePath(): String? {
        val pathExtra = intent.getStringExtra(EXTRA_FILE_PATH)
        if (!pathExtra.isNullOrBlank()) return pathExtra

        val dataUri: Uri? = intent.data
        if (dataUri != null) {
            if (dataUri.scheme == "file") {
                return dataUri.path
            }
            return dataUri.toString()
        }
        return null
    }

    private fun resolveTitle(resolvedPath: String?): String {
        val titleExtra = intent.getStringExtra(EXTRA_TITLE)
        if (!titleExtra.isNullOrBlank()) return titleExtra

        val dataUri: Uri? = intent.data
        if (dataUri != null && dataUri.scheme == "content") {
            runCatching {
                contentResolver.query(dataUri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        val name = cursor.getString(nameIdx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        }

        if (resolvedPath != null) {
            return File(resolvedPath).name
        }
        return com.novo.drive.core.common.bi("Kunjo Player", "কুঞ্জ প্লেয়ার")
    }

    private fun resolveIsAudio(path: String?): Boolean {
        if (intent.hasExtra(EXTRA_IS_AUDIO)) {
            return intent.getBooleanExtra(EXTRA_IS_AUDIO, false)
        }
        val type = intent.type.orEmpty().lowercase()
        if (type.startsWith("audio/")) return true

        val ext = path?.substringAfterLast('.', "")?.lowercase().orEmpty()
        return ext in listOf("mp3", "m4a", "flac", "wav", "ogg", "aac", "opus", "wma")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun IplusPlayerContent(
    path: String,
    title: String,
    isAudio: Boolean,
    dataSourceFactory: TelegramDataSourceFactory,
    onBack: () -> Unit
) {
    var chromeVisible by remember { mutableStateOf(true) }
    var preferredAudioLang by remember { mutableStateOf("") }
    var preferredSubLang by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        IplusMediaPlayer(
            content = PreviewContent.LocalMedia(path = path, isAudio = isAudio),
            title = title,
            fileId = path,
            dataSourceFactory = dataSourceFactory,
            isActivePage = true,
            controlsVisible = chromeVisible,
            allowBackgroundPlayback = true,
            preferredAudioLanguage = preferredAudioLang,
            preferredSubtitleLanguage = preferredSubLang,
            onPreferredAudioLanguage = { preferredAudioLang = it },
            onPreferredSubtitleLanguage = { preferredSubLang = it },
            onControlsVisibilityChanged = { chromeVisible = it },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = chromeVisible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.65f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            )
        }
    }
}

