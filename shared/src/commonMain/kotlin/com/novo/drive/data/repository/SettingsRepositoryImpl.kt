package com.novo.drive.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.novo.drive.core.crypto.CredentialCipher
import kotlin.io.encoding.Base64
import com.novo.drive.core.telegram.TelegramCredentials
import com.novo.drive.data.local.preferences.PreferenceKeys
import com.novo.drive.domain.model.AccountRole
import com.novo.drive.domain.model.AppLanguage
import com.novo.drive.domain.model.AppTheme
import com.novo.drive.domain.model.UserPreferences
import com.novo.drive.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val credentialCipher: CredentialCipher
) : SettingsRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data.map { it.toUserPreferences() }

    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        dataStore.edit { mutable ->
            val updated = transform(mutable.toUserPreferences())
            mutable.write(updated)
        }
    }

    override suspend fun hasStoredTelegramCredentials(): Boolean {
        val data = dataStore.data.first()
        return data[PreferenceKeys.API_ID_ENCRYPTED] != null &&
                data[PreferenceKeys.API_HASH_ENCRYPTED] != null
    }

    override suspend fun getTelegramCredentials(): TelegramCredentials? {
        val data = dataStore.data.first()
        val apiIdEnc = data[PreferenceKeys.API_ID_ENCRYPTED] ?: return null
        val apiHashEnc = data[PreferenceKeys.API_HASH_ENCRYPTED] ?: return null
        return runCatching {
            TelegramCredentials(
                apiId = credentialCipher.decrypt(Base64.decode(apiIdEnc))
                    .decodeToString()
                    .toInt(),
                apiHash = credentialCipher.decrypt(Base64.decode(apiHashEnc)).decodeToString()
            )
        }.getOrNull()
    }

    override suspend fun setTelegramCredentials(credentials: TelegramCredentials) {
        val apiIdEnc = Base64.encode(credentialCipher.encrypt(credentials.apiId.toString().encodeToByteArray()))
        val apiHashEnc = Base64.encode(credentialCipher.encrypt(credentials.apiHash.encodeToByteArray()))
        dataStore.edit {
            it[PreferenceKeys.API_ID_ENCRYPTED] = apiIdEnc
            it[PreferenceKeys.API_HASH_ENCRYPTED] = apiHashEnc
        }
    }

    override suspend fun clearTelegramCredentials() {
        dataStore.edit {
            it.remove(PreferenceKeys.API_ID_ENCRYPTED)
            it.remove(PreferenceKeys.API_HASH_ENCRYPTED)
        }
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        val defaults = UserPreferences()
        val currentTheme = enumOrDefault(this[PreferenceKeys.THEME], defaults.theme)
        val storedDarkTheme = enumOrDefault(
            this[PreferenceKeys.LAST_SELECTED_DARK_THEME],
            if (currentTheme != AppTheme.LIGHT) currentTheme else defaults.lastSelectedDarkTheme
        )
        return UserPreferences(
            onboardingComplete = this[PreferenceKeys.ONBOARDING_COMPLETE]
                ?: defaults.onboardingComplete,
            storageChatId = this[PreferenceKeys.STORAGE_CHAT_ID],
            autoBackupEnabled = this[PreferenceKeys.AUTO_BACKUP_ENABLED]
                ?: defaults.autoBackupEnabled,
            instantBackupEnabled = this[PreferenceKeys.INSTANT_BACKUP_ENABLED]
                ?: defaults.instantBackupEnabled,
            backupFolders = this[PreferenceKeys.BACKUP_FOLDERS] ?: defaults.backupFolders,
            backupWifiOnly = this[PreferenceKeys.BACKUP_WIFI_ONLY] ?: defaults.backupWifiOnly,
            downloadDirectory = this[PreferenceKeys.DOWNLOAD_DIRECTORY],
            backupChargingOnly = this[PreferenceKeys.BACKUP_CHARGING_ONLY]
                ?: defaults.backupChargingOnly,
            backupIntervalHours = this[PreferenceKeys.BACKUP_INTERVAL_HOURS]
                ?: defaults.backupIntervalHours,
            backupMaxFileSizeMb = this[PreferenceKeys.BACKUP_MAX_FILE_SIZE_MB]
                ?: defaults.backupMaxFileSizeMb,
            bedtimeSyncEnabled = this[PreferenceKeys.BEDTIME_SYNC_ENABLED]
                ?: defaults.bedtimeSyncEnabled,
            bedtimeStartHour = this[PreferenceKeys.BEDTIME_START_HOUR]
                ?: defaults.bedtimeStartHour,
            bedtimeEndHour = this[PreferenceKeys.BEDTIME_END_HOUR]
                ?: defaults.bedtimeEndHour,
            maxCacheSizeMb = this[PreferenceKeys.MAX_CACHE_SIZE_MB] ?: defaults.maxCacheSizeMb,
            trashAutoClearDays = this[PreferenceKeys.TRASH_AUTO_CLEAR_DAYS]
                ?: defaults.trashAutoClearDays,
            blockScreenCapture = this[PreferenceKeys.BLOCK_SCREEN_CAPTURE]
                ?: defaults.blockScreenCapture,
            appLockEnabled = this[PreferenceKeys.APP_LOCK_ENABLED] ?: defaults.appLockEnabled,
            autoLockTimeoutMinutes = this[PreferenceKeys.AUTO_LOCK_TIMEOUT_MINUTES]
                ?: defaults.autoLockTimeoutMinutes,
            encryptFiles = this[PreferenceKeys.ENCRYPT_FILES] ?: defaults.encryptFiles,
            deleteAfterUpload = this[PreferenceKeys.DELETE_AFTER_UPLOAD]
                ?: defaults.deleteAfterUpload,
            encryptThumbnails = this[PreferenceKeys.ENCRYPT_THUMBNAILS]
                ?: defaults.encryptThumbnails,
            folderOwnershipRepaired = this[PreferenceKeys.FOLDER_OWNERSHIP_REPAIRED]
                ?: defaults.folderOwnershipRepaired,
            keyBackupCreated = this[PreferenceKeys.KEY_BACKUP_CREATED]
                ?: defaults.keyBackupCreated,
            vaultPinSalt = this[PreferenceKeys.VAULT_PIN_SALT] ?: defaults.vaultPinSalt,
            vaultPinHash = this[PreferenceKeys.VAULT_PIN_HASH] ?: defaults.vaultPinHash,
            decoyVaultPinHash = this[PreferenceKeys.DECOY_VAULT_PIN_HASH] ?: defaults.decoyVaultPinHash,
            decoyVaultPinSalt = this[PreferenceKeys.DECOY_VAULT_PIN_SALT] ?: defaults.decoyVaultPinSalt,
            vaultBiometricEnabled = this[PreferenceKeys.VAULT_BIOMETRIC_ENABLED] ?: defaults.vaultBiometricEnabled,
            vaultLockoutUntilMs = this[PreferenceKeys.VAULT_LOCKOUT_UNTIL_MS] ?: defaults.vaultLockoutUntilMs,
            vaultFailedAttempts = this[PreferenceKeys.VAULT_FAILED_ATTEMPTS] ?: defaults.vaultFailedAttempts,
            secondVaultFileIds = this[PreferenceKeys.SECOND_VAULT_FILE_IDS] ?: defaults.secondVaultFileIds,
            secondVaultFolderIds = this[PreferenceKeys.SECOND_VAULT_FOLDER_IDS] ?: defaults.secondVaultFolderIds,
            musicPlaylistsJson = this[PreferenceKeys.MUSIC_PLAYLISTS_JSON] ?: defaults.musicPlaylistsJson,
            videoResumePositionsJson = this[PreferenceKeys.VIDEO_RESUME_POSITIONS_JSON] ?: defaults.videoResumePositionsJson,
            accountRole = enumOrDefault(this[PreferenceKeys.ACCOUNT_ROLE], defaults.accountRole),
            familySlotIndex = this[PreferenceKeys.FAMILY_SLOT_INDEX] ?: defaults.familySlotIndex,
            familyMemberName = this[PreferenceKeys.FAMILY_MEMBER_NAME] ?: defaults.familyMemberName,
            familyQuotaBytes = this[PreferenceKeys.FAMILY_QUOTA_BYTES] ?: defaults.familyQuotaBytes,
            familyExpiresAtMs = this[PreferenceKeys.FAMILY_EXPIRES_AT_MS] ?: defaults.familyExpiresAtMs,
            familyGrantSignature = this[PreferenceKeys.FAMILY_GRANT_SIGNATURE] ?: defaults.familyGrantSignature,
            familySubKeyBase64 = this[PreferenceKeys.FAMILY_SUB_KEY_BASE64] ?: defaults.familySubKeyBase64,
            kunjoUsername = this[PreferenceKeys.KUNJO_USERNAME] ?: defaults.kunjoUsername,
            kunjoAccountType = this[PreferenceKeys.KUNJO_ACCOUNT_TYPE] ?: defaults.kunjoAccountType,
            kunjoAuthHash = this[PreferenceKeys.KUNJO_AUTH_HASH] ?: defaults.kunjoAuthHash,
            kunjoMaxStreamSizeMb = this[PreferenceKeys.KUNJO_MAX_STREAM_SIZE_MB] ?: defaults.kunjoMaxStreamSizeMb,
            kunjoCanUpload = this[PreferenceKeys.KUNJO_CAN_UPLOAD] ?: defaults.kunjoCanUpload,
            avatarType = this[PreferenceKeys.AVATAR_TYPE] ?: defaults.avatarType,
            avatarValue = this[PreferenceKeys.AVATAR_VALUE] ?: defaults.avatarValue,
            profilePhotoPath = this[PreferenceKeys.PROFILE_PHOTO_PATH],
            theme = currentTheme,
            lastSelectedDarkTheme = storedDarkTheme,
            language = AppLanguage.fromCode(this[PreferenceKeys.LANGUAGE]),
            dynamicColor = this[PreferenceKeys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            viewMode = enumOrDefault(this[PreferenceKeys.VIEW_MODE], defaults.viewMode),
            galleryViewMode = enumOrDefault(
                this[PreferenceKeys.GALLERY_VIEW_MODE],
                enumOrDefault(this[PreferenceKeys.VIEW_MODE], defaults.galleryViewMode)
            ),
            galleryGridSize = this[PreferenceKeys.GALLERY_GRID_SIZE]
                ?: this[PreferenceKeys.GRID_SIZE]
                ?: defaults.galleryGridSize,
            gridSize = this[PreferenceKeys.GRID_SIZE] ?: defaults.gridSize,
            albumGridSize = this[PreferenceKeys.ALBUM_GRID_SIZE] ?: defaults.albumGridSize,
            layoutDensity = enumOrDefault(
                this[PreferenceKeys.LAYOUT_DENSITY],
                defaults.layoutDensity
            ),
            showHiddenFiles = this[PreferenceKeys.SHOW_HIDDEN_FILES] ?: defaults.showHiddenFiles,
            showArchivedFiles = this[PreferenceKeys.SHOW_ARCHIVED_FILES]
                ?: defaults.showArchivedFiles,
            showRecentFiles = this[PreferenceKeys.SHOW_RECENT_FILES] ?: defaults.showRecentFiles,
            linkPreviews = this[PreferenceKeys.LINK_PREVIEWS] ?: defaults.linkPreviews,
            updateCheckEnabled = this[PreferenceKeys.UPDATE_CHECK_ENABLED]
                ?: defaults.updateCheckEnabled,
            lastUpdateCheckAt = this[PreferenceKeys.LAST_UPDATE_CHECK_AT]
                ?: defaults.lastUpdateCheckAt,
            notifiedUpdateVersion = this[PreferenceKeys.NOTIFIED_UPDATE_VERSION]
                ?: defaults.notifiedUpdateVersion,
            skippedUpdateVersion = this[PreferenceKeys.SKIPPED_UPDATE_VERSION]
                ?: defaults.skippedUpdateVersion,
            proxyEnabled = this[PreferenceKeys.PROXY_ENABLED] ?: defaults.proxyEnabled,
            activeProxyId = this[PreferenceKeys.ACTIVE_PROXY_ID] ?: defaults.activeProxyId,
            textPreviewScale = this[PreferenceKeys.TEXT_PREVIEW_SCALE]
                ?: defaults.textPreviewScale,
            sortField = enumOrDefault(this[PreferenceKeys.SORT_FIELD], defaults.sortField),
            sortDirection = enumOrDefault(
                this[PreferenceKeys.SORT_DIRECTION],
                defaults.sortDirection
            ),
            gallerySortField = enumOrDefault(
                this[PreferenceKeys.GALLERY_SORT_FIELD],
                defaults.gallerySortField
            ),
            gallerySortDirection = enumOrDefault(
                this[PreferenceKeys.GALLERY_SORT_DIRECTION],
                defaults.gallerySortDirection
            ),
            backgroundPlayback = this[PreferenceKeys.BACKGROUND_PLAYBACK]
                ?: defaults.backgroundPlayback,
            streamBeforeDownload = this[PreferenceKeys.STREAM_BEFORE_DOWNLOAD]
                ?: defaults.streamBeforeDownload,
            preferredAudioLanguage = this[PreferenceKeys.PREFERRED_AUDIO_LANGUAGE]
                ?: defaults.preferredAudioLanguage,
            preferredSubtitleLanguage = this[PreferenceKeys.PREFERRED_SUBTITLE_LANGUAGE]
                ?: defaults.preferredSubtitleLanguage,
            backupNotifications = this[PreferenceKeys.BACKUP_NOTIFICATIONS]
                ?: defaults.backupNotifications,
            failureNotifications = this[PreferenceKeys.FAILURE_NOTIFICATIONS]
                ?: defaults.failureNotifications,
            transferConcurrency = this[PreferenceKeys.TRANSFER_CONCURRENCY]
                ?: defaults.transferConcurrency,
            transferRetryCount = this[PreferenceKeys.TRANSFER_RETRY_COUNT]
                ?: defaults.transferRetryCount,
            allowMeteredTransfers = this[PreferenceKeys.ALLOW_METERED_TRANSFERS]
                ?: defaults.allowMeteredTransfers,
            safeUploadSpeedKbps = this[PreferenceKeys.SAFE_UPLOAD_SPEED_KBPS]
                ?: defaults.safeUploadSpeedKbps,
            ocrIndexingEnabled = this[PreferenceKeys.OCR_INDEXING_ENABLED]
                ?: defaults.ocrIndexingEnabled,
            streamBridgePin = this[PreferenceKeys.STREAM_BRIDGE_PIN]
                ?: defaults.streamBridgePin,
            debugLogging = this[PreferenceKeys.DEBUG_LOGGING] ?: defaults.debugLogging
        )
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.write(prefs: UserPreferences) {
        this[PreferenceKeys.ONBOARDING_COMPLETE] = prefs.onboardingComplete
        prefs.storageChatId?.let { this[PreferenceKeys.STORAGE_CHAT_ID] = it }
            ?: remove(PreferenceKeys.STORAGE_CHAT_ID)
        this[PreferenceKeys.AUTO_BACKUP_ENABLED] = prefs.autoBackupEnabled
        this[PreferenceKeys.INSTANT_BACKUP_ENABLED] = prefs.instantBackupEnabled
        this[PreferenceKeys.BACKUP_FOLDERS] = prefs.backupFolders
        this[PreferenceKeys.BACKUP_WIFI_ONLY] = prefs.backupWifiOnly
        prefs.downloadDirectory?.let { this[PreferenceKeys.DOWNLOAD_DIRECTORY] = it }
            ?: remove(PreferenceKeys.DOWNLOAD_DIRECTORY)
        this[PreferenceKeys.BACKUP_CHARGING_ONLY] = prefs.backupChargingOnly
        this[PreferenceKeys.BACKUP_INTERVAL_HOURS] = prefs.backupIntervalHours
        this[PreferenceKeys.BACKUP_MAX_FILE_SIZE_MB] = prefs.backupMaxFileSizeMb
        this[PreferenceKeys.BEDTIME_SYNC_ENABLED] = prefs.bedtimeSyncEnabled
        this[PreferenceKeys.BEDTIME_START_HOUR] = prefs.bedtimeStartHour
        this[PreferenceKeys.BEDTIME_END_HOUR] = prefs.bedtimeEndHour
        this[PreferenceKeys.MAX_CACHE_SIZE_MB] = prefs.maxCacheSizeMb
        this[PreferenceKeys.TRASH_AUTO_CLEAR_DAYS] = prefs.trashAutoClearDays
        this[PreferenceKeys.BLOCK_SCREEN_CAPTURE] = prefs.blockScreenCapture
        this[PreferenceKeys.APP_LOCK_ENABLED] = prefs.appLockEnabled
        this[PreferenceKeys.AUTO_LOCK_TIMEOUT_MINUTES] = prefs.autoLockTimeoutMinutes
        this[PreferenceKeys.ENCRYPT_FILES] = prefs.encryptFiles
        this[PreferenceKeys.DELETE_AFTER_UPLOAD] = prefs.deleteAfterUpload
        this[PreferenceKeys.ENCRYPT_THUMBNAILS] = prefs.encryptThumbnails
        this[PreferenceKeys.KEY_BACKUP_CREATED] = prefs.keyBackupCreated
        this[PreferenceKeys.FOLDER_OWNERSHIP_REPAIRED] = prefs.folderOwnershipRepaired
        this[PreferenceKeys.VAULT_PIN_SALT] = prefs.vaultPinSalt
        this[PreferenceKeys.VAULT_PIN_HASH] = prefs.vaultPinHash
        this[PreferenceKeys.DECOY_VAULT_PIN_HASH] = prefs.decoyVaultPinHash
        this[PreferenceKeys.DECOY_VAULT_PIN_SALT] = prefs.decoyVaultPinSalt
        this[PreferenceKeys.VAULT_BIOMETRIC_ENABLED] = prefs.vaultBiometricEnabled
        this[PreferenceKeys.VAULT_LOCKOUT_UNTIL_MS] = prefs.vaultLockoutUntilMs
        this[PreferenceKeys.VAULT_FAILED_ATTEMPTS] = prefs.vaultFailedAttempts
        this[PreferenceKeys.SECOND_VAULT_FILE_IDS] = prefs.secondVaultFileIds
        this[PreferenceKeys.SECOND_VAULT_FOLDER_IDS] = prefs.secondVaultFolderIds
        this[PreferenceKeys.MUSIC_PLAYLISTS_JSON] = prefs.musicPlaylistsJson
        this[PreferenceKeys.VIDEO_RESUME_POSITIONS_JSON] = prefs.videoResumePositionsJson
        this[PreferenceKeys.ACCOUNT_ROLE] = prefs.accountRole.name
        this[PreferenceKeys.FAMILY_SLOT_INDEX] = prefs.familySlotIndex
        this[PreferenceKeys.FAMILY_MEMBER_NAME] = prefs.familyMemberName
        this[PreferenceKeys.FAMILY_QUOTA_BYTES] = prefs.familyQuotaBytes
        this[PreferenceKeys.FAMILY_EXPIRES_AT_MS] = prefs.familyExpiresAtMs
        this[PreferenceKeys.FAMILY_GRANT_SIGNATURE] = prefs.familyGrantSignature
        this[PreferenceKeys.FAMILY_SUB_KEY_BASE64] = prefs.familySubKeyBase64
        this[PreferenceKeys.KUNJO_USERNAME] = prefs.kunjoUsername
        this[PreferenceKeys.KUNJO_ACCOUNT_TYPE] = prefs.kunjoAccountType
        this[PreferenceKeys.KUNJO_AUTH_HASH] = prefs.kunjoAuthHash
        this[PreferenceKeys.KUNJO_MAX_STREAM_SIZE_MB] = prefs.kunjoMaxStreamSizeMb
        this[PreferenceKeys.KUNJO_CAN_UPLOAD] = prefs.kunjoCanUpload
        this[PreferenceKeys.AVATAR_TYPE] = prefs.avatarType
        this[PreferenceKeys.AVATAR_VALUE] = prefs.avatarValue
        if (prefs.profilePhotoPath != null) {
            this[PreferenceKeys.PROFILE_PHOTO_PATH] = prefs.profilePhotoPath
        } else {
            remove(PreferenceKeys.PROFILE_PHOTO_PATH)
        }
        this[PreferenceKeys.THEME] = prefs.theme.name
        val darkMemory = if (prefs.theme != AppTheme.LIGHT) prefs.theme else prefs.lastSelectedDarkTheme
        this[PreferenceKeys.LAST_SELECTED_DARK_THEME] = darkMemory.name
        this[PreferenceKeys.LANGUAGE] = prefs.language.code
        this[PreferenceKeys.DYNAMIC_COLOR] = prefs.dynamicColor
        this[PreferenceKeys.VIEW_MODE] = prefs.viewMode.name
        this[PreferenceKeys.GALLERY_VIEW_MODE] = prefs.galleryViewMode.name
        this[PreferenceKeys.GALLERY_GRID_SIZE] = prefs.galleryGridSize
        this[PreferenceKeys.GRID_SIZE] = prefs.gridSize
        this[PreferenceKeys.ALBUM_GRID_SIZE] = prefs.albumGridSize
        this[PreferenceKeys.LAYOUT_DENSITY] = prefs.layoutDensity.name
        this[PreferenceKeys.SHOW_HIDDEN_FILES] = prefs.showHiddenFiles
        this[PreferenceKeys.SHOW_ARCHIVED_FILES] = prefs.showArchivedFiles
        this[PreferenceKeys.SHOW_RECENT_FILES] = prefs.showRecentFiles
        this[PreferenceKeys.LINK_PREVIEWS] = prefs.linkPreviews
        this[PreferenceKeys.UPDATE_CHECK_ENABLED] = prefs.updateCheckEnabled
        this[PreferenceKeys.LAST_UPDATE_CHECK_AT] = prefs.lastUpdateCheckAt
        this[PreferenceKeys.NOTIFIED_UPDATE_VERSION] = prefs.notifiedUpdateVersion
        this[PreferenceKeys.SKIPPED_UPDATE_VERSION] = prefs.skippedUpdateVersion
        this[PreferenceKeys.PROXY_ENABLED] = prefs.proxyEnabled
        this[PreferenceKeys.ACTIVE_PROXY_ID] = prefs.activeProxyId
        this[PreferenceKeys.TEXT_PREVIEW_SCALE] = prefs.textPreviewScale
        this[PreferenceKeys.SORT_FIELD] = prefs.sortField.name
        this[PreferenceKeys.SORT_DIRECTION] = prefs.sortDirection.name
        this[PreferenceKeys.GALLERY_SORT_FIELD] = prefs.gallerySortField.name
        this[PreferenceKeys.GALLERY_SORT_DIRECTION] = prefs.gallerySortDirection.name
        this[PreferenceKeys.BACKGROUND_PLAYBACK] = prefs.backgroundPlayback
        this[PreferenceKeys.STREAM_BEFORE_DOWNLOAD] = prefs.streamBeforeDownload
        this[PreferenceKeys.PREFERRED_AUDIO_LANGUAGE] = prefs.preferredAudioLanguage
        this[PreferenceKeys.PREFERRED_SUBTITLE_LANGUAGE] = prefs.preferredSubtitleLanguage
        this[PreferenceKeys.BACKUP_NOTIFICATIONS] = prefs.backupNotifications
        this[PreferenceKeys.FAILURE_NOTIFICATIONS] = prefs.failureNotifications
        this[PreferenceKeys.TRANSFER_CONCURRENCY] = prefs.transferConcurrency
        this[PreferenceKeys.TRANSFER_RETRY_COUNT] = prefs.transferRetryCount
        this[PreferenceKeys.ALLOW_METERED_TRANSFERS] = prefs.allowMeteredTransfers
        this[PreferenceKeys.SAFE_UPLOAD_SPEED_KBPS] = prefs.safeUploadSpeedKbps
        this[PreferenceKeys.OCR_INDEXING_ENABLED] = prefs.ocrIndexingEnabled
        this[PreferenceKeys.STREAM_BRIDGE_PIN] = prefs.streamBridgePin
        this[PreferenceKeys.DEBUG_LOGGING] = prefs.debugLogging
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
