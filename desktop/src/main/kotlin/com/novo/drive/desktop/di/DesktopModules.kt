package com.novo.drive.desktop.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.novo.drive.core.crypto.CredentialCipher
import com.novo.drive.core.crypto.FileWrappedKeyRepository
import com.novo.drive.core.crypto.KeyBackupCodec
import com.novo.drive.core.crypto.PassphraseKdf
import com.novo.drive.core.crypto.SecureFileDeleter
import com.novo.drive.core.crypto.StreamCrypto
import com.novo.drive.core.crypto.TdlibDatabaseKeyProviderImpl
import com.novo.drive.core.crypto.WrappedKeyRepository
import com.novo.drive.core.dispatchers.DefaultDispatcherProvider
import com.novo.drive.core.dispatchers.DispatcherProvider
import com.novo.drive.core.files.AppStoragePaths
import com.novo.drive.core.files.DownloadWriter
import com.novo.drive.core.files.FileImporter
import com.novo.drive.core.files.LocalCopyDeleter
import com.novo.drive.core.files.PendingShare
import com.novo.drive.core.files.sharedFilesModule
import com.novo.drive.core.logging.debugLogModule
import com.novo.drive.core.media.MediaMetadataExtractor
import com.novo.drive.core.media.ThumbnailMemoryCache
import com.novo.drive.core.media.ThumbnailStore
import com.novo.drive.core.network.NetworkMonitor
import com.novo.drive.core.permissions.PermissionChecker
import com.novo.drive.core.power.PowerMonitor
import com.novo.drive.core.proxy.ProxyProbe
import com.novo.drive.core.publish.PublishOutboxDrainer
import com.novo.drive.core.publish.PublishScheduler
import com.novo.drive.core.security.AppLockManager
import com.novo.drive.core.security.VaultSecurityManager
import com.novo.drive.core.telegram.DesktopTelegramClient
import com.novo.drive.core.telegram.TdlibDatabaseKeyProvider
import com.novo.drive.core.telegram.TelegramClient
import com.novo.drive.core.telegram.TelegramPacer
import com.novo.drive.core.transfer.BackupResumeScheduler
import com.novo.drive.core.transfer.BackupSessionTracker
import com.novo.drive.core.transfer.CountingBackupSessionTracker
import com.novo.drive.core.transfer.MaintenanceScheduler
import com.novo.drive.core.transfer.TransferErrorMessages
import com.novo.drive.core.transfer.TransferScheduler
import com.novo.drive.core.transfer.transferEngineModule
import com.novo.drive.core.update.UpdateChecker
import com.novo.drive.data.local.database.MIGRATION_10_11
import com.novo.drive.data.local.database.MIGRATION_11_12
import com.novo.drive.data.local.database.MIGRATION_12_13
import com.novo.drive.data.local.database.MIGRATION_13_14
import com.novo.drive.data.local.database.MIGRATION_14_15
import com.novo.drive.data.local.database.MIGRATION_15_16
import com.novo.drive.data.local.database.MIGRATION_16_17
import com.novo.drive.data.local.database.MIGRATION_17_18
import com.novo.drive.data.local.database.MIGRATION_18_19
import com.novo.drive.data.local.database.MIGRATION_19_20
import com.novo.drive.data.local.database.MIGRATION_1_2
import com.novo.drive.data.local.database.MIGRATION_20_21
import com.novo.drive.data.local.database.MIGRATION_2_3
import com.novo.drive.data.local.database.MIGRATION_3_4
import com.novo.drive.data.local.database.MIGRATION_4_5
import com.novo.drive.data.local.database.MIGRATION_5_6
import com.novo.drive.data.local.database.MIGRATION_6_7
import com.novo.drive.data.local.database.MIGRATION_7_8
import com.novo.drive.data.local.database.MIGRATION_8_9
import com.novo.drive.data.local.database.MIGRATION_9_10
import com.novo.drive.data.local.database.IplusDriveDatabase
import com.novo.drive.data.local.database.daosModule
import com.novo.drive.data.repository.LocalDataWiper
import com.novo.drive.data.repository.repositoryModule
import com.novo.drive.desktop.BuildInfo
import com.novo.drive.desktop.DesktopPlatformCapabilities
import com.novo.drive.desktop.crypto.DpapiCredentialCipher
import com.novo.drive.desktop.crypto.LocalKeyCredentialCipher
import com.novo.drive.desktop.data.DesktopLocalDataWiper
import com.novo.drive.desktop.files.DesktopDownloadWriter
import com.novo.drive.desktop.files.DesktopFileImporter
import com.novo.drive.desktop.files.DesktopLocalCopyDeleter
import com.novo.drive.desktop.files.DesktopPendingShare
import com.novo.drive.desktop.files.DesktopStandardFolderPaths
import com.novo.drive.desktop.files.DesktopStoragePaths
import com.novo.drive.desktop.media.DesktopMediaMetadataExtractor
import com.novo.drive.desktop.media.DesktopOcrExtractor
import com.novo.drive.desktop.media.DesktopThumbnailStore
import com.novo.drive.core.ocr.OcrExtractor
import com.novo.drive.desktop.media.ExternalMediaPlayer
import com.novo.drive.desktop.media.MediaStreamServer
import com.novo.drive.desktop.media.NoopThumbnailMemoryCache
import com.novo.drive.desktop.network.DesktopNetworkMonitor
import com.novo.drive.desktop.permissions.DesktopPermissionChecker
import com.novo.drive.desktop.power.DesktopPowerMonitor
import com.novo.drive.desktop.publish.DesktopPublishScheduler
import com.novo.drive.desktop.transfer.DesktopBackupResumeScheduler
import com.novo.drive.desktop.transfer.DesktopMaintenanceScheduler
import com.novo.drive.desktop.transfer.DesktopTransferErrorMessages
import com.novo.drive.desktop.transfer.DesktopTransferScheduler
import com.novo.drive.domain.usecase.useCaseModule
import com.novo.drive.presentation.di.sharedUiModule
import com.novo.drive.presentation.platform.PlatformCapabilities
import com.novo.drive.presentation.platform.StandardFolderPaths
import java.io.File
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toOkioPath
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val desktopModule = module {
    includes(
        daosModule,
        repositoryModule,
        useCaseModule,
        sharedFilesModule,
        debugLogModule,
        transferEngineModule,
        sharedUiModule
    )

    singleOf(::DesktopStoragePaths) bind AppStoragePaths::class
    single<CredentialCipher> {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        if (os.contains("win")) DpapiCredentialCipher() else LocalKeyCredentialCipher(get())
    }
    singleOf(::FileWrappedKeyRepository) bind WrappedKeyRepository::class
    singleOf(::TdlibDatabaseKeyProviderImpl) bind TdlibDatabaseKeyProvider::class
    singleOf(::DefaultDispatcherProvider) bind DispatcherProvider::class
    singleOf(::StreamCrypto)
    singleOf(::SecureFileDeleter)
    singleOf(::PassphraseKdf)
    singleOf(::KeyBackupCodec)
    singleOf(::TelegramPacer)
    singleOf(::ProxyProbe)
    singleOf(::AppLockManager)
    singleOf(::VaultSecurityManager)
    single { UpdateChecker(BuildInfo.VERSION) }
    singleOf(::DesktopPermissionChecker) bind PermissionChecker::class
    singleOf(::DesktopStandardFolderPaths) bind StandardFolderPaths::class
    singleOf(::DesktopPlatformCapabilities) bind PlatformCapabilities::class
    singleOf(::PublishOutboxDrainer)
    singleOf(::DesktopNetworkMonitor) bind NetworkMonitor::class
    singleOf(::DesktopPowerMonitor) bind PowerMonitor::class
    singleOf(::DesktopTransferErrorMessages) bind TransferErrorMessages::class
    singleOf(::DesktopDownloadWriter) bind DownloadWriter::class
    singleOf(::MediaStreamServer)
    singleOf(::ExternalMediaPlayer)
    singleOf(::DesktopFileImporter) bind FileImporter::class
    singleOf(::DesktopPendingShare) bind PendingShare::class
    singleOf(::DesktopLocalCopyDeleter) bind LocalCopyDeleter::class
    singleOf(::DesktopMediaMetadataExtractor) bind MediaMetadataExtractor::class
    singleOf(::DesktopOcrExtractor) bind OcrExtractor::class
    singleOf(::DesktopThumbnailStore) bind ThumbnailStore::class
    singleOf(::NoopThumbnailMemoryCache) bind ThumbnailMemoryCache::class
    singleOf(::CountingBackupSessionTracker) bind BackupSessionTracker::class
    singleOf(::DesktopTransferScheduler) bind TransferScheduler::class
    singleOf(::DesktopBackupResumeScheduler) bind BackupResumeScheduler::class
    singleOf(::DesktopMaintenanceScheduler) bind MaintenanceScheduler::class
    singleOf(::DesktopLocalDataWiper) bind LocalDataWiper::class
    singleOf(::DesktopPublishScheduler) bind PublishScheduler::class
    single<TelegramClient> {
        DesktopTelegramClient(get(), get(), get(), BuildInfo.VERSION)
    }
    single<DataStore<Preferences>> {
        val storagePaths = get<AppStoragePaths>()
        PreferenceDataStoreFactory.createWithPath {
            File(File(storagePaths.filesDir, "datastore"), "settings.preferences_pb")
                .apply { parentFile?.mkdirs() }
                .toOkioPath()
        }
    }
    single<IplusDriveDatabase> {
        val storagePaths = get<AppStoragePaths>()
        Room.databaseBuilder<IplusDriveDatabase>(
            name = File(storagePaths.filesDir, "novodrive.db").absolutePath
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
                MIGRATION_19_20,
                MIGRATION_20_21
            )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
    single { get<IplusDriveDatabase>().iplusMessageDao() }
}
