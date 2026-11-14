package com.novo.drive.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novo.drive.core.update.AppLinks
import com.novo.drive.presentation.components.UpdateDialog
import com.novo.drive.presentation.platform.LocalAppIcon
import com.novo.drive.presentation.platform.LocalAppVersion
import com.novo.drive.presentation.platform.LocalUrlOpener
import com.novo.drive.resources.Res
import com.novo.drive.resources.about_auto_check
import com.novo.drive.resources.about_auto_check_summary
import com.novo.drive.resources.about_check_updates
import com.novo.drive.resources.about_check_updates_summary
import com.novo.drive.resources.about_checking
import com.novo.drive.resources.about_section_updates
import com.novo.drive.resources.about_tagline
import com.novo.drive.resources.about_update_ready
import com.novo.drive.resources.about_version
import com.novo.drive.resources.app_name
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val urlOpener = LocalUrlOpener.current
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val checking = updateState is UpdateState.Checking

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        LocalAppIcon.current(
            Modifier
                .size(88.dp)
                .clip(MaterialTheme.shapes.extraLarge)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(Res.string.about_version, LocalAppVersion.current),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(Res.string.about_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    SettingsSectionTitle(stringResource(Res.string.about_section_updates))
    SettingsGroup {
        add {
            SettingsClickRow(
                title = stringResource(Res.string.about_check_updates),
                icon = Icons.Filled.Update,
                subtitle = when (val current = updateState) {
                    is UpdateState.Available -> stringResource(
                        Res.string.about_update_ready,
                        current.release.version
                    )

                    UpdateState.Checking -> stringResource(Res.string.about_checking)
                    UpdateState.Idle -> stringResource(Res.string.about_check_updates_summary)
                },
                onClick = { if (!checking) viewModel.checkForUpdates() },
                trailing = if (checking) {
                    { LoadingIndicator(modifier = Modifier.size(24.dp)) }
                } else null
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.about_auto_check),
                icon = Icons.Filled.Autorenew,
                subtitle = stringResource(Res.string.about_auto_check_summary),
                checked = state.preferences.updateCheckEnabled,
                onChange = { value ->
                    viewModel.update { it.copy(updateCheckEnabled = value) }
                }
            )
        }
    }

    val isBn = com.novo.drive.presentation.common.isBengaliUi()
    SettingsSectionTitle(if (isBn) "কমিউনিটি ও অফিশিয়াল স্টোর" else "Community & Official Store")
    SettingsGroup {
        add {
            SettingsClickRow(
                title = if (isBn) "টেলিগ্রাম চ্যানেল (Official Store)" else "Telegram Channel (Official Store)",
                icon = Icons.AutoMirrored.Filled.Send,
                subtitle = "https://t.me/ismailq01appsstore",
                onClick = { urlOpener.open(AppLinks.TELEGRAM) }
            )
        }
    }

    (updateState as? UpdateState.Available)?.let { available ->
        UpdateDialog(
            currentVersion = LocalAppVersion.current,
            onOpenUrl = urlOpener::open,
            release = available.release,
            onDownload = {
                viewModel.dismissUpdate()
                urlOpener.open(available.release.apkUrl ?: available.release.pageUrl)
            },
            onDismiss = viewModel::dismissUpdate
        )
    }
}
