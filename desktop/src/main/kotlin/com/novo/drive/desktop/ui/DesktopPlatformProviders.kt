package com.novo.drive.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.novo.drive.desktop.BuildInfo
import com.novo.drive.desktop.files.DesktopFileRevealer
import com.novo.drive.presentation.platform.DeleteConsentLauncher
import com.novo.drive.presentation.platform.DeviceOwnerGate
import com.novo.drive.presentation.platform.FileOpener
import com.novo.drive.presentation.platform.FilePicker
import com.novo.drive.presentation.platform.LocalFileOpener
import com.novo.drive.presentation.platform.FileSharer
import com.novo.drive.presentation.platform.FolderPicker
import com.novo.drive.presentation.platform.LocalAppIcon
import com.novo.drive.presentation.platform.LocalAppVersion
import com.novo.drive.presentation.platform.LocalDeleteConsentLauncher
import com.novo.drive.presentation.platform.LocalDeviceOwnerGate
import com.novo.drive.presentation.platform.LocalDownloadLocationConfigurable
import com.novo.drive.presentation.platform.LocalFilePicker
import com.novo.drive.presentation.platform.LocalFileRevealer
import com.novo.drive.presentation.platform.LocalFileSharer
import com.novo.drive.presentation.platform.LocalFolderPicker
import com.novo.drive.presentation.platform.LocalMultiFilePicker
import com.novo.drive.presentation.platform.LocalPermissionRequester
import com.novo.drive.presentation.platform.LocalPlatformCapabilities
import com.novo.drive.presentation.platform.LocalPlatformScreens
import com.novo.drive.presentation.platform.LocalSystemScreens
import com.novo.drive.presentation.platform.LocalTelegramLinkOpener
import com.novo.drive.presentation.platform.LocalUrlOpener
import com.novo.drive.presentation.platform.MultiFilePicker
import com.novo.drive.presentation.platform.PermissionRequester
import com.novo.drive.presentation.platform.PickResult
import com.novo.drive.presentation.platform.PlatformCapabilities
import com.novo.drive.presentation.platform.SystemScreens
import com.novo.drive.presentation.platform.UrlOpener
import com.novo.drive.resources.Res
import com.novo.drive.resources.ic_launcher
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI
import javax.swing.JFileChooser
import kotlin.concurrent.thread

@Composable
fun ProvideDesktopPlatformActions(content: @Composable () -> Unit) {
    val capabilities = koinInject<PlatformCapabilities>()
    val urlOpener = remember {
        UrlOpener { url ->
            runCatching { Desktop.getDesktop().browse(URI(url)) }
        }
    }
    val folderPicker = remember {
        FolderPicker { onPicked ->
            thread {
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                }
                val result = if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    PickResult.Picked(chooser.selectedFile.absolutePath)
                } else {
                    PickResult.Canceled
                }
                onPicked(result)
            }
        }
    }
    val filePicker = remember {
        FilePicker { onPicked ->
            thread {
                val dialog = FileDialog(null as Frame?, "", FileDialog.LOAD)
                dialog.isVisible = true
                val file = dialog.file
                val result = if (file == null) {
                    PickResult.Canceled
                } else {
                    PickResult.Picked(File(dialog.directory, file).absolutePath)
                }
                onPicked(result)
            }
        }
    }
    val multiFilePicker = remember {
        MultiFilePicker { onPicked ->
            thread {
                val dialog = FileDialog(null as Frame?, "", FileDialog.LOAD)
                dialog.isMultipleMode = true
                dialog.isVisible = true
                onPicked(dialog.files.orEmpty().map { it.absolutePath })
            }
        }
    }
    val fileSharer = remember {
        FileSharer { paths, _, _ ->
            paths.firstOrNull()?.let { path ->
                runCatching {
                    Desktop.getDesktop().open(File(path).parentFile)
                }
            }
        }
    }
    val deleteConsentLauncher = remember {
        DeleteConsentLauncher { _, onResult -> onResult(true) }
    }
    val fileRevealer = remember { DesktopFileRevealer() }
    val telegramLinkOpener = remember { DesktopTelegramLinkOpener() }
    val permissionRequester = remember {
        PermissionRequester { _, onDone -> onDone() }
    }
    val systemScreens = remember {
        object : SystemScreens {
            override fun openAppSettings() = Unit
            override fun openAllFilesAccess() = Unit
            override fun openBatteryOptimization() = Unit
        }
    }
    val deviceOwnerGate = remember {
        DeviceOwnerGate { _, _, onDenied, _ ->
            onDenied(
                com.novo.drive.core.common.bi(
                    "Biometric or device-owner verification is not supported on desktop. Please use your Vault PIN.",
                    "ডেস্কটপ ওএসে বায়োমেট্রিক বা ডিভাইস ওনার ভেরিফিকেশন সাপোর্টেড নয়। অনুগ্রহ করে ভল্ট পিন ব্যবহার করুন।"
                )
            )
        }
    }

    val appIcon: @Composable (Modifier) -> Unit = { modifier ->
        Image(
            painter = painterResource(Res.drawable.ic_launcher),
            contentDescription = null,
            modifier = modifier
        )
    }

    val fileOpener = remember {
        FileOpener { file ->
            runCatching {
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                    java.awt.Desktop.getDesktop().open(file)
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalAppIcon provides appIcon,
        LocalFileOpener provides fileOpener,
        LocalUrlOpener provides urlOpener,
        LocalTelegramLinkOpener provides telegramLinkOpener,
        LocalFolderPicker provides folderPicker,
        LocalFilePicker provides filePicker,
        LocalMultiFilePicker provides multiFilePicker,
        LocalFileSharer provides fileSharer,
        LocalDeleteConsentLauncher provides deleteConsentLauncher,
        LocalPermissionRequester provides permissionRequester,
        LocalSystemScreens provides systemScreens,
        LocalDeviceOwnerGate provides deviceOwnerGate,
        LocalAppVersion provides BuildInfo.VERSION,
        LocalDownloadLocationConfigurable provides true,
        LocalPlatformScreens provides DesktopPlatformScreens,
        LocalFileRevealer provides fileRevealer,
        LocalPlatformCapabilities provides capabilities,
        content = content
    )
}
