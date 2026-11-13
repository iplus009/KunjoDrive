package com.novo.drive.presentation.settings

import com.novo.drive.presentation.components.glass.SecureVaultPinDialog
import com.novo.drive.presentation.components.glass.VaultDialogMode

import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novo.drive.domain.model.AppLanguage
import com.novo.drive.resources.settings_delete_after_upload
import com.novo.drive.resources.settings_delete_after_upload_summary
import com.novo.drive.resources.Res
import com.novo.drive.resources.language_english
import com.novo.drive.resources.language_bengali
import com.novo.drive.resources.language_labels
import com.novo.drive.resources.language_system
import com.novo.drive.resources.settings_language
import com.novo.drive.resources.backup_folder_remove_message
import com.novo.drive.resources.backup_folder_subtitle
import com.novo.drive.resources.backup_interval_labels
import com.novo.drive.resources.backup_interval_with_sweep
import com.novo.drive.resources.backup_size_limit_labels
import com.novo.drive.resources.common_back
import com.novo.drive.resources.common_clear
import com.novo.drive.resources.common_free_space
import com.novo.drive.resources.common_storage_channels
import com.novo.drive.resources.lock_timeout_labels
import com.novo.drive.resources.rebuild_indexed_files
import com.novo.drive.resources.settings_add_another_folder
import com.novo.drive.resources.settings_app_lock
import com.novo.drive.resources.settings_archived_shortcut
import com.novo.drive.resources.settings_auto_clear_trash
import com.novo.drive.resources.settings_auto_lock
import com.novo.drive.resources.settings_automatic_backup
import com.novo.drive.resources.settings_back_new_media_instantly
import com.novo.drive.resources.settings_background_playback
import com.novo.drive.resources.settings_backup_folders_title
import com.novo.drive.resources.settings_backup_interval
import com.novo.drive.resources.settings_backup_results
import com.novo.drive.resources.settings_block_screenshots
import com.novo.drive.resources.settings_cache_current_size
import com.novo.drive.resources.settings_cache_limit
import com.novo.drive.resources.settings_cached_previews_thumbnails_temporary
import com.novo.drive.resources.settings_change_passphrase
import com.novo.drive.resources.settings_choose_folder_device
import com.novo.drive.resources.settings_clear_cache_action
import com.novo.drive.resources.settings_clear_cache_title
import com.novo.drive.resources.settings_clear_thumbnails_action
import com.novo.drive.resources.settings_clear_thumbnails_title
import com.novo.drive.resources.settings_compact_layout
import com.novo.drive.resources.settings_confirm_removing_lock
import com.novo.drive.resources.settings_debug_logging
import com.novo.drive.resources.settings_dynamic_color
import com.novo.drive.resources.settings_encrypt_thumbnails
import com.novo.drive.resources.settings_encrypt_uploads
import com.novo.drive.resources.settings_encryption_stays_off
import com.novo.drive.resources.settings_exclusions
import com.novo.drive.resources.settings_failures
import com.novo.drive.resources.settings_files_folders_skipped_backup
import com.novo.drive.resources.settings_files_sealed_leaving_device
import com.novo.drive.resources.settings_files_stay_telegram_channel
import com.novo.drive.resources.settings_folder_unreadable
import com.novo.drive.resources.settings_free_up_space_title
import com.novo.drive.resources.settings_gallery_previews_regenerated_browse
import com.novo.drive.resources.settings_gallery_grid_columns
import com.novo.drive.resources.settings_gallery_grid_view
import com.novo.drive.resources.settings_grid_columns
import com.novo.drive.resources.settings_grid_view
import com.novo.drive.resources.settings_hidden_shortcut
import com.novo.drive.resources.settings_hides_app_screenshots_screen
import com.novo.drive.resources.settings_key_backup_stored
import com.novo.drive.resources.settings_link_previews
import com.novo.drive.resources.settings_link_previews_subtitle
import com.novo.drive.resources.settings_local_copies_files_already
import com.novo.drive.resources.settings_log
import com.novo.drive.resources.settings_log_telegram
import com.novo.drive.resources.settings_no_folders_selected
import com.novo.drive.resources.settings_no_recovery_passphrase
import com.novo.drive.resources.settings_only_charging
import com.novo.drive.resources.settings_parallel_transfers
import com.novo.drive.resources.settings_play_videos_load_instead
import com.novo.drive.resources.settings_protects_cached_previews_device
import com.novo.drive.resources.settings_proxy
import com.novo.drive.resources.settings_proxy_subtitle
import com.novo.drive.resources.settings_reading_channel
import com.novo.drive.resources.settings_rebuild_index_telegram
import com.novo.drive.resources.settings_recent_files
import com.novo.drive.resources.settings_recent_files_subtitle
import com.novo.drive.resources.settings_remove
import com.novo.drive.resources.settings_remove_backup_folder
import com.novo.drive.resources.settings_removes_session_stored_api
import com.novo.drive.resources.settings_require_fingerprint_screen_lock
import com.novo.drive.resources.settings_required_before_encrypt
import com.novo.drive.resources.settings_restore_encryption_key
import com.novo.drive.resources.settings_restores_file_list
import com.novo.drive.resources.settings_retry_attempts
import com.novo.drive.resources.settings_set
import com.novo.drive.resources.settings_set_passphrase
import com.novo.drive.resources.settings_share_debug_log
import com.novo.drive.resources.settings_share_debug_log_subtitle
import com.novo.drive.resources.settings_shown_channels_private_owned
import com.novo.drive.resources.settings_shows_archived_collection_home
import com.novo.drive.resources.settings_shows_hidden_collection_home
import com.novo.drive.resources.settings_skip_files_larger_than
import com.novo.drive.resources.settings_state_connected
import com.novo.drive.resources.settings_state_connecting
import com.novo.drive.resources.settings_state_syncing
import com.novo.drive.resources.settings_state_waiting_network
import com.novo.drive.resources.settings_stream_downloading
import com.novo.drive.resources.settings_switch_drives_create_another
import com.novo.drive.resources.settings_telegram_account
import com.novo.drive.resources.settings_theme
import com.novo.drive.resources.settings_tighter_list_rows_without
import com.novo.drive.resources.settings_transfers_wi_fi_only
import com.novo.drive.resources.settings_transfers_wi_fi_only_summary
import com.novo.drive.resources.settings_turn_off_app_lock
import com.novo.drive.resources.settings_unlock_files_encrypted_earlier
import com.novo.drive.resources.settings_uploads_photos_videos_soon
import com.novo.drive.resources.settings_uploads_stay_unencrypted_set
import com.novo.drive.resources.settings_use_wallpaper_colors_available
import com.novo.drive.resources.settings_wi_fi_only
import com.novo.drive.resources.settings_wi_fi_only_summary
import com.novo.drive.resources.theme_dark
import com.novo.drive.resources.theme_labels
import com.novo.drive.resources.theme_light
import com.novo.drive.resources.theme_system
import com.novo.drive.resources.trash_clear_labels
import com.novo.drive.core.telegram.TelegramConnectionState
import com.novo.drive.domain.model.AppTheme
import com.novo.drive.domain.model.LayoutDensity
import com.novo.drive.domain.model.ViewMode
import com.novo.drive.presentation.platform.LocalDownloadLocationConfigurable
import com.novo.drive.resources.settings_download_location
import com.novo.drive.resources.settings_download_location_default
import com.novo.drive.resources.settings_download_location_reset
import com.novo.drive.resources.settings_free_up_none
import com.novo.drive.resources.settings_free_up_subtitle
import com.novo.drive.presentation.platform.LocalDeleteConsentLauncher
import com.novo.drive.presentation.platform.LocalStandardFolders
import com.novo.drive.presentation.platform.LocalDeviceOwnerGate
import com.novo.drive.presentation.platform.LocalFileSharer
import com.novo.drive.presentation.platform.LocalFolderPicker
import com.novo.drive.presentation.platform.LocalPlatformCapabilities
import com.novo.drive.presentation.platform.PickResult
import com.novo.drive.presentation.common.UiText
import com.novo.drive.presentation.common.CollectSnackbarMessages
import com.novo.drive.presentation.common.Formatters
import com.novo.drive.presentation.components.ChoiceDialog
import com.novo.drive.presentation.components.ConfirmDialog
import com.novo.drive.presentation.components.LoadingState
import com.novo.drive.presentation.components.liftedTopAppBarColors
import com.novo.drive.presentation.components.rememberToolbarLift

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsSectionScreen(
    section: SettingsSectionType,
    onOpenChannels: () -> Unit,
    onBack: () -> Unit,
    onOpenExclusions: () -> Unit,
    onOpenProxy: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmClearCache by remember { mutableStateOf(false) }

    val deleteConsentLauncher = LocalDeleteConsentLauncher.current

    CollectSnackbarMessages(viewModel.messages, snackbarHostState)
    val fileSharer = LocalFileSharer.current
    val logChooserTitle = stringResource(Res.string.settings_share_debug_log)
    LaunchedEffect(Unit) {
        viewModel.shareRequests.collect { request ->
            fileSharer.share(request.paths, request.mimeType, logChooserTitle)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.deleteConsentRequests.collect { request ->
            deleteConsentLauncher.launch(request) { granted ->
                if (granted) viewModel.freeUpSpace()
            }
        }
    }

    val scrollState = rememberScrollState()
    val lifted by rememberToolbarLift(scrollState)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = liftedTopAppBarColors(lifted),
                title = { Text(stringResource(section.titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.common_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(8.dp))
            when (section) {
                SettingsSectionType.ACCOUNT -> AccountSection(
                    onPickChannel = onOpenChannels,
                    state = state,
                    viewModel = viewModel,
                    onLogout = { confirmLogout = true }
                )

                SettingsSectionType.BACKUP -> BackupSection(
                    state = state,
                    viewModel = viewModel,
                    onOpenExclusions = onOpenExclusions
                )

                SettingsSectionType.STORAGE -> StorageSection(
                    state = state,
                    viewModel = viewModel,
                    onClearCache = { confirmClearCache = true }
                )

                SettingsSectionType.PERMISSIONS ->
                    PermissionsSection(viewModel.permissionChecker)

                SettingsSectionType.SECURITY -> SecuritySection(state, viewModel)
                SettingsSectionType.APPEARANCE -> AppearanceSection(state, viewModel)
                SettingsSectionType.PLAYBACK -> PlaybackSection(state, viewModel)
                SettingsSectionType.NOTIFICATIONS -> NotificationsSection(state, viewModel)
                SettingsSectionType.ADVANCED -> AdvancedSection(
                    state = state,
                    viewModel = viewModel,
                    onOpenProxy = onOpenProxy
                )
                SettingsSectionType.ABOUT -> AboutSection(state, viewModel)
            }
            Spacer(Modifier.height(24.dp + padding.calculateBottomPadding()))
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = stringResource(Res.string.settings_log_telegram),
            message = stringResource(Res.string.settings_files_stay_telegram_channel),
            confirmLabel = stringResource(Res.string.settings_log),
            destructive = true,
            onConfirm = {
                confirmLogout = false
                viewModel.logout(onLoggedOut)
            },
            onDismiss = { confirmLogout = false }
        )
    }
    if (confirmClearCache) {
        ConfirmDialog(
            title = stringResource(Res.string.settings_clear_cache_title),
            message = stringResource(Res.string.settings_cached_previews_thumbnails_temporary),
            confirmLabel = stringResource(Res.string.settings_clear_cache_action),
            onConfirm = {
                confirmClearCache = false
                viewModel.clearCache()
            },
            onDismiss = { confirmClearCache = false }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AccountSection(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onPickChannel: () -> Unit,
    onLogout: () -> Unit
) {
    val indexedSoFar by viewModel.indexedSoFar.collectAsStateWithLifecycle()

    var showUsernameEditDialog by remember { mutableStateOf(false) }
    var editUsernameInput by remember(state.preferences.kunjoUsername) { mutableStateOf(state.preferences.kunjoUsername) }
    var editError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    if (showUsernameEditDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showUsernameEditDialog = false },
            title = { Text(if (state.preferences.kunjoUsername.isBlank()) "ইউনিক ইউজারনেম নির্ধারণ" else "কুঞ্জ ইউজারনেম পরিবর্তন", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    Text("আপনার ইউনিক ইউজারনেম লিখুন (a-z, 0-9, _)", fontSize = 12.sp)
                    OutlinedTextField(
                        value = editUsernameInput,
                        onValueChange = { editUsernameInput = it; editError = null },
                        placeholder = { Text("username") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (editError != null) {
                        Text("⚠️ $editError", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true
                        viewModel.editKunjoUsername(editUsernameInput) { success, errorMsg ->
                            isSaving = false
                            if (success) {
                                showUsernameEditDialog = false
                            } else {
                                editError = errorMsg
                            }
                        }
                    },
                    enabled = !isSaving && editUsernameInput.isNotBlank()
                ) {
                    Text(if (isSaving) "যাচাই হচ্ছে..." else "সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsernameEditDialog = false }, enabled = !isSaving) {
                    Text("বাতিল")
                }
            }
        )
    }

    SettingsGroup {
        add {
            val username = state.preferences.kunjoUsername
            val accType = if (state.preferences.kunjoAccountType == "SUB") "👨‍👩‍👧 সাব-অ্যাকাউন্ট" else "👑 মেইন ড্রাইভ"
            SettingsClickRow(
                title = if (username.isNotBlank()) "KUNJO ID: @$username" else "কুঞ্জ ইউজারনেম (@username)",
                subtitle = if (username.isNotBlank()) "$accType · ইউনিক ইউজারনেম পরিবর্তন করতে ট্যাপ করুন" else "ইউনিক ইউজারনেম নির্ধারণ করতে ট্যাপ করুন",
                titleColor = MaterialTheme.colorScheme.primary,
                onClick = {
                    editUsernameInput = username
                    editError = null
                    showUsernameEditDialog = true
                }
            )
        }
        add {
            SettingsClickRow(
                title = state.user?.let {
                    listOf(it.firstName, it.lastName).filter(String::isNotBlank)
                        .joinToString(" ")
                } ?: stringResource(Res.string.settings_telegram_account),
                subtitle = buildString {
                    state.user?.phoneNumber?.let { append("+$it · ") }
                    append(
                        when (state.connection) {
                            TelegramConnectionState.READY -> stringResource(Res.string.settings_state_connected)
                            TelegramConnectionState.UPDATING -> stringResource(Res.string.settings_state_syncing)
                            TelegramConnectionState.CONNECTING -> stringResource(Res.string.settings_state_connecting)
                            TelegramConnectionState.WAITING_FOR_NETWORK ->
                                stringResource(Res.string.settings_state_waiting_network)
                        }
                    )
                    if (state.user?.isPremium == true) append(" · Premium (4 GB uploads)")
                },
                onClick = { }
            )
        }
        add {
            SettingsClickRow(
                title = stringResource(Res.string.settings_rebuild_index_telegram),
                subtitle = when {
                    state.syncing && indexedSoFar > 0 -> pluralStringResource(
                        Res.plurals.rebuild_indexed_files,
                        indexedSoFar,
                        indexedSoFar
                    )
                    state.syncing -> stringResource(Res.string.settings_reading_channel)
                    else -> stringResource(Res.string.settings_restores_file_list)
                },
                onClick = { if (!state.syncing) viewModel.resync() },
                trailing = if (state.syncing) {
                    { LoadingIndicator(modifier = Modifier.size(24.dp)) }
                } else null
            )
        }
        add(visible = !state.preferences.isFamilyMember) {
            SettingsClickRow(
                title = stringResource(Res.string.common_storage_channels),
                subtitle = stringResource(Res.string.settings_switch_drives_create_another),
                onClick = onPickChannel
            )
        }
        add {
            SettingsClickRow(
                title = stringResource(Res.string.settings_log),
                subtitle = stringResource(Res.string.settings_removes_session_stored_api),
                titleColor = MaterialTheme.colorScheme.error,
                onClick = onLogout
            )
        }
    }
}

@Composable
private fun BackupSection(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onOpenExclusions: () -> Unit
) {
    val prefs = state.preferences
    var folderToRemove by remember { mutableStateOf<String?>(null) }
    val folderUnreadableMessage = UiText.Resource(Res.string.settings_folder_unreadable)
    val folderPicker = LocalFolderPicker.current
    val onPickBackupFolder = {
        folderPicker.pick { result ->
            when (result) {
                is PickResult.Canceled -> Unit
                is PickResult.Unreadable -> viewModel.notify(folderUnreadableMessage)
                is PickResult.Picked -> viewModel.addBackupFolder(result.path)
            }
        }
    }
    var showIntervalDialog by remember { mutableStateOf(false) }
    var showSizeLimitDialog by remember { mutableStateOf(false) }

    if (showIntervalDialog) {
        ChoiceDialog(
            title = stringResource(Res.string.settings_backup_interval),
            options = intervalOptions.map { it to intervalLabel(it) },
            selected = prefs.backupIntervalHours,
            onSelect = { hours ->
                showIntervalDialog = false
                viewModel.update { it.copy(backupIntervalHours = hours) }
            },
            onDismiss = { showIntervalDialog = false }
        )
    }
    if (showSizeLimitDialog) {
        ChoiceDialog(
            title = stringResource(Res.string.settings_skip_files_larger_than),
            options = sizeLimitOptions.map { it to sizeLimitLabel(it) },
            selected = prefs.backupMaxFileSizeMb,
            onSelect = { limit ->
                showSizeLimitDialog = false
                viewModel.update { it.copy(backupMaxFileSizeMb = limit) }
            },
            onDismiss = { showSizeLimitDialog = false }
        )
    }

    folderToRemove?.let { folder ->
        ConfirmDialog(
            title = stringResource(Res.string.settings_remove_backup_folder),
            message = stringResource(Res.string.backup_folder_remove_message, folder),
            confirmLabel = stringResource(Res.string.settings_remove),
            destructive = true,
            onConfirm = {
                folderToRemove = null
                viewModel.removeBackupFolder(folder)
            },
            onDismiss = { folderToRemove = null }
        )
    }

    SettingsGroup {
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_automatic_backup),
                checked = prefs.autoBackupEnabled,
                onChange = { value ->
                    viewModel.update { it.copy(autoBackupEnabled = value) }
                }
            )
        }
        add(visible = prefs.autoBackupEnabled) {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_back_new_media_instantly),
                subtitle = stringResource(Res.string.settings_uploads_photos_videos_soon),
                checked = prefs.instantBackupEnabled,
                onChange = { value ->
                    viewModel.update { it.copy(instantBackupEnabled = value) }
                }
            )
        }
        add {
            SettingsClickRow(
                title = stringResource(Res.string.settings_backup_interval),
                subtitle = if (prefs.instantBackupEnabled && prefs.autoBackupEnabled) {
                    stringResource(
                        Res.string.backup_interval_with_sweep,
                        intervalLabel(prefs.backupIntervalHours)
                    )
                } else {
                    intervalLabel(prefs.backupIntervalHours)
                },
                onClick = { showIntervalDialog = true }
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_wi_fi_only),
                subtitle = stringResource(Res.string.settings_wi_fi_only_summary),
                checked = prefs.backupWifiOnly,
                onChange = { value -> viewModel.update { it.copy(backupWifiOnly = value) } }
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_only_charging),
                checked = prefs.backupChargingOnly,
                onChange = { value ->
                    viewModel.update { it.copy(backupChargingOnly = value) }
                }
            )
        }
        add {
            SettingsSwitchRow(
                title = "বেডটাইম নাইট-উইন্ডো সিঙ্ক (Bedtime Sync: 12 AM – 6 AM)",
                subtitle = "চালু থাকলে শুধুমাত্র রাত ১২টা থেকে ভোর ৬টার মধ্যে ব্যাকআপ আপলোড হবে",
                checked = prefs.bedtimeSyncEnabled,
                onChange = { value ->
                    viewModel.update { it.copy(bedtimeSyncEnabled = value) }
                }
            )
        }
        add {
            SettingsClickRow(
                title = stringResource(Res.string.settings_skip_files_larger_than),
                subtitle = sizeLimitLabel(prefs.backupMaxFileSizeMb),
                onClick = { showSizeLimitDialog = true }
            )
        }
        add {
            SettingsClickRow(
                title = stringResource(Res.string.settings_exclusions),
                subtitle = stringResource(Res.string.settings_files_folders_skipped_backup),
                onClick = onOpenExclusions
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    SettingsSectionTitle(stringResource(Res.string.settings_backup_folders_title))

    val backupFolders by viewModel.backupFolders.collectAsStateWithLifecycle()

    if (prefs.autoBackupEnabled && backupFolders.isEmpty()) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(Res.string.settings_no_folders_selected),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    val standardFolders = LocalStandardFolders.current
    val standardPaths = standardFolders.map { it.path }.toSet()
    val customFolders = backupFolders.filterNot { it in standardPaths }
    SettingsGroup {
        standardFolders.forEach { folder ->
            add {
                SettingsSwitchRow(
                    title = stringResource(folder.label),
                    subtitle = folder.path,
                    checked = folder.path in backupFolders,
                    onChange = { checked ->
                        if (checked) {
                            viewModel.addBackupFolder(folder.path)
                        } else {
                            viewModel.removeBackupFolder(folder.path)
                        }
                    }
                )
            }
        }
        customFolders.forEach { folder ->
            add {
                SettingsClickRow(
                    title = folder.substringAfterLast('/'),
                    subtitle = stringResource(Res.string.backup_folder_subtitle, folder),
                    onClick = { folderToRemove = folder }
                )
            }
        }
        add {
            SettingsClickRow(
                title = stringResource(Res.string.settings_add_another_folder),
                subtitle = stringResource(Res.string.settings_choose_folder_device),
                onClick = { onPickBackupFolder() }
            )
        }
    }
}

@Composable
private fun StorageSection(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onClearCache: () -> Unit
) {
    val prefs = state.preferences
    val reclaimable by viewModel.reclaimableBytes.collectAsStateWithLifecycle()
    var showTrashDialog by remember { mutableStateOf(false) }
    var confirmFreeUpSpace by remember { mutableStateOf(false) }
    var confirmClearThumbnails by remember { mutableStateOf(false) }

    if (confirmClearThumbnails) {
        ConfirmDialog(
            title = stringResource(Res.string.settings_clear_thumbnails_title),
            message = stringResource(Res.string.settings_gallery_previews_regenerated_browse),
            confirmLabel = stringResource(Res.string.common_clear),
            onConfirm = {
                confirmClearThumbnails = false
                viewModel.clearThumbnails()
            },
            onDismiss = { confirmClearThumbnails = false }
        )
    }

    if (confirmFreeUpSpace) {
        ConfirmDialog(
            title = stringResource(
                Res.string.settings_free_up_space_title,
                Formatters.bytes(reclaimable)
            ),
            message = stringResource(Res.string.settings_local_copies_files_already),
            confirmLabel = stringResource(Res.string.common_free_space),
            destructive = true,
            onConfirm = {
                confirmFreeUpSpace = false
                viewModel.freeUpSpace()
            },
            onDismiss = { confirmFreeUpSpace = false }
        )
    }

    if (showTrashDialog) {
        ChoiceDialog(
            title = stringResource(Res.string.settings_auto_clear_trash),
            options = trashDayOptions.map { it to trashDaysLabel(it) },
            selected = prefs.trashAutoClearDays,
            onSelect = { days ->
                showTrashDialog = false
                viewModel.update { it.copy(trashAutoClearDays = days) }
            },
            onDismiss = { showTrashDialog = false }
        )
    }

    val downloadLocationConfigurable = LocalDownloadLocationConfigurable.current
    val downloadFolderPicker = LocalFolderPicker.current
    SettingsGroup {
        add(visible = downloadLocationConfigurable) {
            SettingsClickRow(
                title = stringResource(Res.string.settings_download_location),
                subtitle = prefs.downloadDirectory
                    ?: stringResource(Res.string.settings_download_location_default),
                onClick = {
                    downloadFolderPicker.pick { result ->
                        if (result is PickResult.Picked) {
                            viewModel.update { it.copy(downloadDirectory = result.path) }
                        }
                    }
                }
            )
        }
        add(visible = downloadLocationConfigurable && prefs.downloadDirectory != null) {
            SettingsClickRow(
                title = stringResource(Res.string.settings_download_location_reset),
                subtitle = stringResource(Res.string.settings_download_location_default),
                onClick = { viewModel.update { it.copy(downloadDirectory = null) } }
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_delete_after_upload),
                subtitle = stringResource(Res.string.settings_delete_after_upload_summary),
                checked = prefs.deleteAfterUpload,
                onChange = { value ->
                    viewModel.update { it.copy(deleteAfterUpload = value) }
                }
            )
        }
        add {
            SettingsClickRow(
                title = stringResource(Res.string.common_free_space),
                subtitle = if (reclaimable > 0) {
                    stringResource(
                        Res.string.settings_free_up_subtitle,
                        Formatters.bytes(reclaimable)
                    )
                } else {
                    stringResource(Res.string.settings_free_up_none)
                },
                onClick = {
                    if (reclaimable > 0) {
                        confirmFreeUpSpace = true
                    } else {
                        viewModel.reportNothingToFree()
                    }
                }
            )
        }
        add {
            SettingsClickRow(
                title = stringResource(Res.string.settings_clear_cache_action),
                subtitle = stringResource(
                    Res.string.settings_cache_current_size,
                    Formatters.bytes(state.cacheStats.totalBytes)
                ),
                onClick = onClearCache
            )
        }
        add {
            SettingsClickRow(
                title = stringResource(Res.string.settings_clear_thumbnails_action),
                subtitle = Formatters.bytes(state.cacheStats.thumbnailBytes),
                onClick = { confirmClearThumbnails = true }
            )
        }
        add {
            SettingsSliderRow(
                title = stringResource(Res.string.settings_cache_limit),
                value = prefs.maxCacheSizeMb / 256,
                range = 1..16,
                onChange = { value ->
                    viewModel.update { it.copy(maxCacheSizeMb = value * 256) }
                },
                valueLabel = { Formatters.bytes(it * 256L * 1024 * 1024) }
            )
        }
        add {
            SettingsClickRow(
                title = stringResource(Res.string.settings_auto_clear_trash),
                subtitle = trashDaysLabel(prefs.trashAutoClearDays),
                onClick = { showTrashDialog = true }
            )
        }
    }
}

@Composable
private fun SecuritySection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val encryptionOffMessage = UiText.Resource(Res.string.settings_encryption_stays_off)
    val prefs = state.preferences
    val deviceOwnerGate = LocalDeviceOwnerGate.current
    val keyBackupWorking by viewModel.keyBackupWorking.collectAsStateWithLifecycle()
    val lockPromptTitle = stringResource(Res.string.settings_turn_off_app_lock)
    val lockPromptSubtitle = stringResource(Res.string.settings_confirm_removing_lock)
    var showLockTimeoutDialog by remember { mutableStateOf(false) }
    var showKeyBackupDialog by remember { mutableStateOf(false) }
    var showKeyRestoreDialog by remember { mutableStateOf(false) }
    val keyHint by viewModel.keyHint.collectAsStateWithLifecycle()
    var enablingEncryption by remember { mutableStateOf(false) }
    var showVaultSetupDialog by remember { mutableStateOf(false) }

    if (showVaultSetupDialog) {
        SecureVaultPinDialog(
            onDismiss = { showVaultSetupDialog = false },
            onUnlocked = { showVaultSetupDialog = false },
            initialMode = if (prefs.vaultPinHash.isNotBlank()) VaultDialogMode.CHANGE_OLD_PIN else VaultDialogMode.SETUP_ENTER_NEW
        )
    }

    if (showKeyBackupDialog) {
        KeyBackupDialog(
            working = keyBackupWorking,
            onConfirm = { passphrase, hint ->
                showKeyBackupDialog = false
                viewModel.backUpEncryptionKey(passphrase, hint, enablingEncryption)
                enablingEncryption = false
            },
            onDismiss = {
                showKeyBackupDialog = false
                if (enablingEncryption) {
                    enablingEncryption = false
                    viewModel.notify(encryptionOffMessage)
                }
            }
        )
    }
    if (showKeyRestoreDialog) {
        KeyRestoreDialog(
            working = keyBackupWorking,
            hint = keyHint,
            onShowHint = viewModel::loadKeyHint,
            onConfirm = { passphrase ->
                showKeyRestoreDialog = false
                viewModel.restoreEncryptionKey(passphrase)
            },
            onDismiss = { showKeyRestoreDialog = false }
        )
    }

    if (prefs.encryptFiles && !prefs.keyBackupCreated) {
        KeyBackupWarning(
            onSetPassphrase = {
                enablingEncryption = false
                showKeyBackupDialog = true
            }
        )
        Spacer(Modifier.height(12.dp))
    }

    if (showLockTimeoutDialog) {
        ChoiceDialog(
            title = stringResource(Res.string.settings_auto_lock),
            options = lockTimeoutOptions.map { it to lockTimeoutLabel(it) },
            selected = prefs.autoLockTimeoutMinutes,
            onSelect = { minutes ->
                showLockTimeoutDialog = false
                viewModel.update { it.copy(autoLockTimeoutMinutes = minutes) }
            },
            onDismiss = { showLockTimeoutDialog = false }
        )
    }

    val capabilities = LocalPlatformCapabilities.current
    SettingsGroup {
        add(visible = capabilities.supportsAppLock) {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_app_lock),
                subtitle = stringResource(Res.string.settings_require_fingerprint_screen_lock),
                checked = prefs.appLockEnabled,
                onChange = { value ->
                    if (value) {
                        viewModel.update { it.copy(appLockEnabled = true) }
                    } else {
                        deviceOwnerGate.require(
                            title = lockPromptTitle,
                            subtitle = lockPromptSubtitle,
                            onDenied = { error ->
                                error?.let { viewModel.notify(UiText.Plain(it)) }
                            }
                        ) {
                            viewModel.update { it.copy(appLockEnabled = false) }
                        }
                    }
                }
            )
        }
        add(visible = capabilities.supportsAppLock && prefs.appLockEnabled) {
            SettingsClickRow(
                title = stringResource(Res.string.settings_auto_lock),
                subtitle = lockTimeoutLabel(prefs.autoLockTimeoutMinutes),
                onClick = { showLockTimeoutDialog = true }
            )
        }
        add(visible = capabilities.supportsScreenCaptureBlocking) {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_block_screenshots),
                subtitle = stringResource(Res.string.settings_hides_app_screenshots_screen),
                checked = prefs.blockScreenCapture,
                onChange = { value ->
                    viewModel.update { it.copy(blockScreenCapture = value) }
                }
            )
        }
        add {
            SettingsClickRow(
                title = "সিকিউরিটি ভল্ট পিন (Vault PIN)",
                subtitle = if (prefs.vaultPinHash.isNotBlank()) "ভল্ট পিন সক্রিয় রয়েছে • পিন পরিবর্তন করতে ক্লিক করুন" else "কোনো পিন সেট করা নেই • এখনই ৪-ডিজিট পিন সেট করুন",
                onClick = { showVaultSetupDialog = true }
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_encrypt_uploads),
                subtitle = stringResource(Res.string.settings_files_sealed_leaving_device),
                checked = prefs.encryptFiles,
                onChange = { value ->
                    if (value) {
                        if (prefs.keyBackupCreated) {
                            viewModel.enableEncryption()
                        } else {
                            enablingEncryption = true
                            showKeyBackupDialog = true
                        }
                    } else {
                        viewModel.disableEncryption()
                    }
                }
            )
        }
        add(visible = prefs.encryptFiles) {
            SettingsClickRow(
                title = if (prefs.keyBackupCreated) {
                    stringResource(Res.string.settings_change_passphrase)
                } else {
                    stringResource(Res.string.settings_set_passphrase)
                },
                subtitle = if (prefs.keyBackupCreated) {
                    stringResource(Res.string.settings_key_backup_stored)
                } else {
                    stringResource(Res.string.settings_required_before_encrypt)
                },
                onClick = {
                    enablingEncryption = false
                    showKeyBackupDialog = true
                }
            )
        }
        add {
            SettingsClickRow(
                title = stringResource(Res.string.settings_restore_encryption_key),
                subtitle = stringResource(Res.string.settings_unlock_files_encrypted_earlier),
                onClick = { showKeyRestoreDialog = true }
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_encrypt_thumbnails),
                subtitle = stringResource(Res.string.settings_protects_cached_previews_device),
                checked = prefs.encryptThumbnails,
                onChange = { value ->
                    viewModel.update { it.copy(encryptThumbnails = value) }
                }
            )
        }
    }
}

@Composable
private fun KeyBackupWarning(onSetPassphrase: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.settings_no_recovery_passphrase),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = stringResource(Res.string.settings_uploads_stay_unencrypted_set),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = onSetPassphrase) {
                Text(
                    text = stringResource(Res.string.settings_set),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun AppearanceSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val prefs = state.preferences
    val capabilities = LocalPlatformCapabilities.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        ChoiceDialog(
            title = stringResource(Res.string.settings_theme),
            options = listOf(
                AppTheme.SYSTEM to "System (সিস্টেম থিম অনুসরণ)",
                AppTheme.LIGHT to "Clean Light (ক্লিন লাইট থিম)",
                AppTheme.CYBER_GUARD to "Cyber Guard (নিয়ন শিল্ড থিম)",
                AppTheme.HUSSUU_CYBER to "Hussuu Cyber (হাই-টেক সাইবার ডেক)",
                AppTheme.MINIMAL_DRIVE to "Minimal Drive (ক্লিন মিডনাইট)",
                AppTheme.DONUT_NEON to "Neon Glow (ডোনাট ও নিয়ন থিম)"
            ),
            selected = prefs.theme,
            onSelect = { choice ->
                showThemeDialog = false
                viewModel.update {
                    if (choice != AppTheme.LIGHT && choice != AppTheme.SYSTEM) {
                        it.copy(theme = choice, lastSelectedDarkTheme = choice)
                    } else {
                        it.copy(theme = choice)
                    }
                }
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showLanguageDialog) {
        ChoiceDialog(
            title = stringResource(Res.string.settings_language),
            options = AppLanguage.entries.zip(stringArrayResource(Res.array.language_labels)),
            selected = prefs.language,
            onSelect = { choice ->
                showLanguageDialog = false
                viewModel.update { it.copy(language = choice) }
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    SettingsGroup {
        add {
            SettingsClickRow(
                title = stringResource(Res.string.settings_theme),
                subtitle = when (prefs.theme) {
                    AppTheme.SYSTEM -> "System (সিস্টেম থিম অনুসরণ)"
                    AppTheme.LIGHT -> "Clean Light (ক্লিন লাইট থিম)"
                    AppTheme.CYBER_GUARD -> "Cyber Guard (নিয়ন শিল্ড থিম)"
                    AppTheme.HUSSUU_CYBER -> "Hussuu Cyber (হাই-টেক সাইবার ডেক)"
                    AppTheme.MINIMAL_DRIVE -> "Minimal Drive (ক্লিন মিডনাইট)"
                    AppTheme.DONUT_NEON -> "Neon Glow (ডোনাট ও নিয়ন থিম)"
                    AppTheme.DARK -> "Hussuu Cyber (ডার্ক মোড)"
                },
                onClick = { showThemeDialog = true }
            )
        }
        add {
            SettingsClickRow(
                title = stringResource(Res.string.settings_language),
                subtitle = when (prefs.language) {
                    AppLanguage.SYSTEM -> stringResource(Res.string.language_system)
                    AppLanguage.ENGLISH -> stringResource(Res.string.language_english)
                    AppLanguage.BENGALI -> stringResource(Res.string.language_bengali)
                },
                onClick = { showLanguageDialog = true }
            )
        }
        add(visible = capabilities.supportsDynamicColor) {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_dynamic_color),
                subtitle = stringResource(Res.string.settings_use_wallpaper_colors_available),
                checked = prefs.dynamicColor,
                onChange = { value -> viewModel.update { it.copy(dynamicColor = value) } }
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_grid_view),
                checked = prefs.viewMode == ViewMode.GRID,
                onChange = { value ->
                    viewModel.update {
                        it.copy(viewMode = if (value) ViewMode.GRID else ViewMode.LIST)
                    }
                }
            )
        }
        add(visible = prefs.viewMode == ViewMode.GRID) {
            SettingsSliderRow(
                title = stringResource(Res.string.settings_grid_columns),
                value = prefs.gridSize,
                range = 2..6,
                onChange = { value -> viewModel.update { it.copy(gridSize = value) } }
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_gallery_grid_view),
                checked = prefs.galleryViewMode == ViewMode.GRID,
                onChange = { value ->
                    viewModel.update {
                        it.copy(galleryViewMode = if (value) ViewMode.GRID else ViewMode.LIST)
                    }
                }
            )
        }
        add(visible = prefs.galleryViewMode == ViewMode.GRID) {
            SettingsSliderRow(
                title = stringResource(Res.string.settings_gallery_grid_columns),
                value = prefs.galleryGridSize,
                range = 2..6,
                onChange = { value -> viewModel.update { it.copy(galleryGridSize = value) } }
            )
        }
        add(visible = prefs.viewMode == ViewMode.LIST || prefs.galleryViewMode == ViewMode.LIST) {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_compact_layout),
                subtitle = stringResource(Res.string.settings_tighter_list_rows_without),
                checked = prefs.layoutDensity == LayoutDensity.COMPACT,
                onChange = { value ->
                    viewModel.update {
                        it.copy(
                            layoutDensity = if (value) LayoutDensity.COMPACT
                            else LayoutDensity.COMFORTABLE
                        )
                    }
                }
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_link_previews),
                subtitle = stringResource(Res.string.settings_link_previews_subtitle),
                checked = prefs.linkPreviews,
                onChange = { value -> viewModel.update { it.copy(linkPreviews = value) } }
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_recent_files),
                subtitle = stringResource(Res.string.settings_recent_files_subtitle),
                checked = prefs.showRecentFiles,
                onChange = { value -> viewModel.update { it.copy(showRecentFiles = value) } }
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_hidden_shortcut),
                subtitle = stringResource(Res.string.settings_shows_hidden_collection_home),
                checked = prefs.showHiddenFiles,
                onChange = { value -> viewModel.update { it.copy(showHiddenFiles = value) } }
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_archived_shortcut),
                subtitle = stringResource(Res.string.settings_shows_archived_collection_home),
                checked = prefs.showArchivedFiles,
                onChange = { value ->
                    viewModel.update { it.copy(showArchivedFiles = value) }
                }
            )
        }
    }
}

@Composable
private fun PlaybackSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val prefs = state.preferences
    SettingsGroup {
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_stream_downloading),
                subtitle = stringResource(Res.string.settings_play_videos_load_instead),
                checked = prefs.streamBeforeDownload,
                onChange = { value ->
                    viewModel.update { it.copy(streamBeforeDownload = value) }
                }
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_background_playback),
                checked = prefs.backgroundPlayback,
                onChange = { value ->
                    viewModel.update { it.copy(backgroundPlayback = value) }
                }
            )
        }
    }
}

@Composable
private fun NotificationsSection(state: SettingsUiState, viewModel: SettingsViewModel) {
    val prefs = state.preferences
    SettingsGroup {
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_backup_results),
                checked = prefs.backupNotifications,
                onChange = { value ->
                    viewModel.update { it.copy(backupNotifications = value) }
                }
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_failures),
                checked = prefs.failureNotifications,
                onChange = { value ->
                    viewModel.update { it.copy(failureNotifications = value) }
                }
            )
        }
    }
}

@Composable
private fun AdvancedSection(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onOpenProxy: () -> Unit
) {
    val prefs = state.preferences
    var showSpeedLimitDialog by remember { mutableStateOf(false) }
    val speedOptions = listOf(
        0 to "সীমাহীন (Unlimited Speed)",
        1024 to "1 MB/s (Ultra Anti-Ban Safe)",
        2048 to "2 MB/s (Balanced Safe)",
        4096 to "4 MB/s (Fast Safe)"
    )

    if (showSpeedLimitDialog) {
        ChoiceDialog(
            title = "অ্যান্টি-ব্যান সেফ আপলোড স্পিড",
            options = speedOptions,
            selected = prefs.safeUploadSpeedKbps,
            onSelect = { kbps ->
                showSpeedLimitDialog = false
                viewModel.update { it.copy(safeUploadSpeedKbps = kbps) }
            },
            onDismiss = { showSpeedLimitDialog = false }
        )
    }

    SettingsGroup {
        add {
            SettingsClickRow(
                title = stringResource(Res.string.settings_proxy),
                subtitle = stringResource(Res.string.settings_proxy_subtitle),
                onClick = onOpenProxy
            )
        }
        add {
            SettingsClickRow(
                title = "অ্যান্টি-ব্যান সেফ আপলোড স্পিড লিমিটার",
                subtitle = speedOptions.firstOrNull { it.first == prefs.safeUploadSpeedKbps }?.second
                    ?: "${prefs.safeUploadSpeedKbps} KB/s",
                onClick = { showSpeedLimitDialog = true }
            )
        }
        add {
            SettingsSwitchRow(
                title = "অফলাইন OCR ইমেজ টেক্সট সার্চ",
                subtitle = "১০০% অফলাইনে স্ক্রিনশট, রসিদ ও ডকুমেন্ট ছবির ভেতরের লেখা সার্চযোগ্য রাখুন",
                checked = prefs.ocrIndexingEnabled,
                onChange = { value ->
                    viewModel.update { it.copy(ocrIndexingEnabled = value) }
                }
            )
        }
        add(visible = !prefs.isFamilyMember) {
            SettingsSliderRow(
                title = stringResource(Res.string.settings_parallel_transfers),
                value = prefs.transferConcurrency,
                range = 1..6,
                onChange = { value ->
                    viewModel.update { it.copy(transferConcurrency = value) }
                }
            )
        }
        add {
            SettingsSliderRow(
                title = stringResource(Res.string.settings_retry_attempts),
                value = prefs.transferRetryCount,
                range = 0..10,
                onChange = { value ->
                    viewModel.update { it.copy(transferRetryCount = value) }
                }
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_transfers_wi_fi_only),
                subtitle = stringResource(Res.string.settings_transfers_wi_fi_only_summary),
                checked = !prefs.allowMeteredTransfers,
                onChange = { value ->
                    viewModel.update { it.copy(allowMeteredTransfers = !value) }
                }
            )
        }
        add {
            SettingsSwitchRow(
                title = stringResource(Res.string.settings_debug_logging),
                checked = prefs.debugLogging,
                onChange = { value -> viewModel.update { it.copy(debugLogging = value) } }
            )
        }
        if (prefs.debugLogging) {
            add {
                SettingsClickRow(
                    title = stringResource(Res.string.settings_share_debug_log),
                    subtitle = stringResource(Res.string.settings_share_debug_log_subtitle),
                    onClick = viewModel::shareDebugLog
                )
            }
        }
    }
}

private val intervalOptions = listOf(1, 2, 4, 6, 12, 24, 48)

@Composable
private fun intervalLabel(hours: Int): String =
    labelFor(Res.array.backup_interval_labels, intervalOptions, hours) { "$it h" }

private val sizeLimitOptions = listOf(0, 100, 500, 1000, 2000, 4000)

@Composable
private fun sizeLimitLabel(mb: Int): String =
    labelFor(Res.array.backup_size_limit_labels, sizeLimitOptions, mb) { "$it MB" }

private val lockTimeoutOptions = listOf(0, 1, 5, 10, 15, 30)

@Composable
private fun lockTimeoutLabel(minutes: Int): String =
    labelFor(Res.array.lock_timeout_labels, lockTimeoutOptions, minutes) { "$it min" }

private val trashDayOptions = listOf(0, 1, 7, 30, 90, 365)

@Composable
private fun trashDaysLabel(days: Int): String =
    labelFor(Res.array.trash_clear_labels, trashDayOptions, days) { "$it d" }

/**
 * Labels live in arrays.xml so translations can reword them; a value outside the set shows as a
 * number.
 */
@Composable
private fun labelFor(
    arrayId: StringArrayResource,
    values: List<Int>,
    value: Int,
    fallback: (Int) -> String
): String {
    val labels = stringArrayResource(arrayId)
    val index = values.indexOf(value)
    return labels.getOrNull(index) ?: fallback(value)
}

@Composable
private fun ChannelCriteria() {
    Text(
        text = stringResource(Res.string.settings_shown_channels_private_owned),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
