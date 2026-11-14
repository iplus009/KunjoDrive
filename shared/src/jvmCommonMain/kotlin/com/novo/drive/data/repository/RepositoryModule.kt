package com.novo.drive.data.repository

import com.novo.drive.core.bridge.KunjoBridge
import com.novo.drive.core.bridge.KunjoBridgeImpl
import com.novo.drive.core.family.FamilyAccessManager
import com.novo.drive.core.ocr.OcrIndexService
import com.novo.drive.core.stream.LocalStreamBridgeServer
import com.novo.drive.core.telegram.TelegramSession
import com.novo.drive.data.remote.telegram.ManifestCodec
import com.novo.drive.domain.repository.BackupRepository
import com.novo.drive.domain.repository.CacheRepository
import com.novo.drive.domain.repository.ChannelRepository
import com.novo.drive.domain.repository.ExclusionRepository
import com.novo.drive.domain.repository.FileRepository
import com.novo.drive.domain.repository.KeyBackupRepository
import com.novo.drive.domain.repository.ProxyRepository
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.domain.repository.SyncRepository
import com.novo.drive.domain.repository.TelegramAuthRepository
import com.novo.drive.domain.repository.TransferRepository
import com.novo.drive.domain.repository.TrashRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::ManifestCodec)
    singleOf(::ActiveChannel)
    singleOf(::ChannelOwnership)
    singleOf(::FolderOwnershipRepair)
    singleOf(::FileManifestPublisher)
    singleOf(::FolderPathResolver)
    singleOf(::FolderMerger)
    singleOf(::IndexSnapshot)
    singleOf(::FolderStateSynchronizer)
    singleOf(::SettingsRepositoryImpl) bind SettingsRepository::class
    singleOf(::FileRepositoryImpl) bind FileRepository::class
    singleOf(::TrashRepositoryImpl) bind TrashRepository::class
    singleOf(::TransferRepositoryImpl) bind TransferRepository::class
    singleOf(::BackupRepositoryImpl) bind BackupRepository::class
    singleOf(::ExclusionRepositoryImpl) bind ExclusionRepository::class
    singleOf(::TelegramAuthRepositoryImpl) bind TelegramAuthRepository::class
    singleOf(::TelegramSession)
    singleOf(::ProxyRepositoryImpl) bind ProxyRepository::class
    singleOf(::SyncRepositoryImpl) bind SyncRepository::class
    singleOf(::ChannelRepositoryImpl) bind ChannelRepository::class
    singleOf(::CacheRepositoryImpl) bind CacheRepository::class
    singleOf(::KeyBackupRepositoryImpl) bind KeyBackupRepository::class
    singleOf(::FamilyAccessManager)
    singleOf(::KunjoAuthRepository)
    singleOf(::OcrIndexService)
    singleOf(::LocalStreamBridgeServer)
    single<KunjoBridge> {
        KunjoBridgeImpl(
            customMediaBridge = getOrNull(),
            customSecurityBridge = getOrNull(),
            customTransferBridge = getOrNull(),
            customStorageBridge = getOrNull()
        )
    }
}
