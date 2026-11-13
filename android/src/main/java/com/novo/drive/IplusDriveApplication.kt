package com.novo.drive

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.novo.drive.core.common.AppNotifications
import com.novo.drive.core.common.SafeLog
import com.novo.drive.core.family.SessionKeepAliveWorker
import com.novo.drive.core.logging.DebugLogController
import com.novo.drive.core.proxy.ProxyFailover
import com.novo.drive.core.publish.PublishScheduler
import com.novo.drive.core.security.AndroidNativeCoreJni
import com.novo.drive.core.transfer.MaintenanceScheduler
import com.novo.drive.core.transfer.MediaStoreWatcher
import com.novo.drive.di.appModules
import com.novo.drive.data.repository.FolderOwnershipRepair
import com.novo.drive.domain.repository.FileRepository
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.domain.repository.TransferRepository
import com.novo.drive.domain.repository.TrashRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

open class IplusDriveApplication : Application(), SingletonImageLoader.Factory {

    private val imageLoader: ImageLoader by inject()

    private val appNotifications: AppNotifications by inject()

    private val maintenanceScheduler: MaintenanceScheduler by inject()

    private val settingsRepository: SettingsRepository by inject()

    private val transferRepository: TransferRepository by inject()

    private val fileRepository: FileRepository by inject()

    private val folderOwnershipRepair: FolderOwnershipRepair by inject()

    private val trashRepository: TrashRepository by inject()

    private val mediaStoreWatcher: MediaStoreWatcher by inject()

    private val publishScheduler: PublishScheduler by inject()

    private val proxyFailover: ProxyFailover by inject()

    private val debugLogController: DebugLogController by inject()

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader

    override fun onCreate() {
        super.onCreate()
        SafeLog.verbose = BuildConfig.DEBUG
        AndroidNativeCoreJni.install(this)
        startKoin {
            androidContext(this@IplusDriveApplication)
            workManagerFactory()
            modules(appModules)
        }
        appNotifications.createChannels()
        SessionKeepAliveWorker.schedule(this)
        mediaStoreWatcher.start()
        proxyFailover.start(applicationScope)
        debugLogController.start(applicationScope, alwaysVerbose = BuildConfig.DEBUG)
        applicationScope.launch {
            transferRepository.recoverOrphanedTransfers()
            fileRepository.sweepImportOrphans()
            folderOwnershipRepair.runOnce()
            folderOwnershipRepair.refileStrayFiles()
            publishScheduler.kick()
            trashRepository.repairTrashTree()
            val prefs = settingsRepository.preferences.first()
            maintenanceScheduler.scheduleAll(
                backupEnabled = prefs.autoBackupEnabled && prefs.backupIntervalHours > 0,
                backupIntervalHours = prefs.backupIntervalHours,
                wifiOnly = prefs.backupWifiOnly,
                chargingOnly = prefs.backupChargingOnly,
                instantBackup = prefs.instantBackupEnabled,
                updateChecks = prefs.updateCheckEnabled
            )
        }
    }
}

class KunjoDriveApplication : IplusDriveApplication()

