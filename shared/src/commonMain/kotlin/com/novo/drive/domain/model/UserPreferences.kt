package com.novo.drive.domain.model

data class UserPreferences(
    val onboardingComplete: Boolean = false,
    val storageChatId: Long? = null,

    val autoBackupEnabled: Boolean = false,
    val instantBackupEnabled: Boolean = true,
    val backupFolders: Set<String> = emptySet(),
    val backupWifiOnly: Boolean = false,
    val downloadDirectory: String? = null,
    val backupChargingOnly: Boolean = false,
    val backupIntervalHours: Int = 24,
    val backupMaxFileSizeMb: Int = 0,
    val bedtimeSyncEnabled: Boolean = false,
    val bedtimeStartHour: Int = 0,
    val bedtimeEndHour: Int = 6,

    val maxCacheSizeMb: Int = 1024,
    val trashAutoClearDays: Int = 30,

    val appLockEnabled: Boolean = false,
    val blockScreenCapture: Boolean = false,
    val autoLockTimeoutMinutes: Int = 5,
    val encryptFiles: Boolean = false,
    val deleteAfterUpload: Boolean = false,
    val encryptThumbnails: Boolean = false,
    val keyBackupCreated: Boolean = false,
    val folderOwnershipRepaired: Boolean = false,

    val vaultPinSalt: String = "",
    val vaultPinHash: String = "",
    val decoyVaultPinHash: String = "",
    /**
     * Dedicated salt for the decoy PIN. Vaults that only had the legacy setup (decoy hash derived
     * with [vaultPinSalt]) keep working through the fallback in [com.novo.drive.core.security.VaultSecurityManager].
     */
    val decoyVaultPinSalt: String = "",
    val vaultBiometricEnabled: Boolean = true,
    val vaultLockoutUntilMs: Long = 0L,
    val vaultFailedAttempts: Int = 0,

    val accountRole: AccountRole = AccountRole.OWNER,
    val familySlotIndex: Int = 0,
    val familyMemberName: String = "",
    val familyQuotaBytes: Long = 0L,
    val familyExpiresAtMs: Long = 0L,
    val familyGrantSignature: String = "",
    val familySubKeyBase64: String = "",

    val kunjoUsername: String = "",
    val kunjoAccountType: String = "MAIN",
    val kunjoAuthHash: String = "",
    val kunjoMaxStreamSizeMb: Int = 0,
    val kunjoCanUpload: Boolean = true,

    val avatarType: String = "EMOJI",
    val avatarValue: String = "👤",
    val profilePhotoPath: String? = null,

    val theme: AppTheme = AppTheme.LIGHT,
    val lastSelectedDarkTheme: AppTheme = AppTheme.CYBER_GUARD,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val dynamicColor: Boolean = true,
    val viewMode: ViewMode = ViewMode.LIST,
    val gridSize: Int = 3,
    val galleryViewMode: ViewMode = ViewMode.GRID,
    val galleryGridSize: Int = 3,
    val albumGridSize: Int = 3,
    val layoutDensity: LayoutDensity = LayoutDensity.COMFORTABLE,
    val showHiddenFiles: Boolean = true,
    val showArchivedFiles: Boolean = true,
    val showRecentFiles: Boolean = true,
    val linkPreviews: Boolean = true,
    val textPreviewScale: Float = 1f,

    val sortField: FileSortField = FileSortField.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val gallerySortField: FileSortField = FileSortField.DATE_MODIFIED,
    val gallerySortDirection: SortDirection = SortDirection.DESCENDING,

    val backgroundPlayback: Boolean = true,
    val streamBeforeDownload: Boolean = true,
    val preferredAudioLanguage: String = "",
    val preferredSubtitleLanguage: String = "",

    val backupNotifications: Boolean = true,
    val failureNotifications: Boolean = true,

    val secondVaultFileIds: Set<String> = emptySet(),
    val secondVaultFolderIds: Set<String> = emptySet(),
    val musicPlaylistsJson: String = "{}",
    val videoResumePositionsJson: String = "{}",

    val transferConcurrency: Int = 6,
    val transferRetryCount: Int = 3,
    val allowMeteredTransfers: Boolean = true,
    val safeUploadSpeedKbps: Int = 0,
    val ocrIndexingEnabled: Boolean = true,
    val streamBridgePin: String = "",
    val debugLogging: Boolean = false,

    val updateCheckEnabled: Boolean = true,
    val lastUpdateCheckAt: Long = 0L,
    val notifiedUpdateVersion: String = "",
    val skippedUpdateVersion: String = "",

    val proxyEnabled: Boolean = false,
    val activeProxyId: String = ""
) {
    val vaultPinConfigured: Boolean
        get() = vaultPinHash.isNotBlank()

    val decoyVaultPinConfigured: Boolean
        get() = decoyVaultPinHash.isNotBlank()

    val isFamilyMember: Boolean
        get() = accountRole == AccountRole.FAMILY_MEMBER
}
