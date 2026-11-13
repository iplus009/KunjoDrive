package com.novo.drive.data.local.database

import org.koin.dsl.module

val daosModule = module {
    single { get<IplusDriveDatabase>().storageChannelDao() }
    single { get<IplusDriveDatabase>().fileDao() }
    single { get<IplusDriveDatabase>().folderDao() }
    single { get<IplusDriveDatabase>().transferDao() }
    single { get<IplusDriveDatabase>().backupDao() }
    single { get<IplusDriveDatabase>().exclusionDao() }
    single { get<IplusDriveDatabase>().thumbnailDao() }
    single { get<IplusDriveDatabase>().cacheDao() }
    single { get<IplusDriveDatabase>().pendingDeleteDao() }
    single { get<IplusDriveDatabase>().filePartDao() }
    single { get<IplusDriveDatabase>().proxyDao() }
    single { get<IplusDriveDatabase>().folderTombstoneDao() }
    single { get<IplusDriveDatabase>().familyDao() }
    single { get<IplusDriveDatabase>().ocrTextDao() }
    single { get<IplusDriveDatabase>().iplusMessageDao() }
}
