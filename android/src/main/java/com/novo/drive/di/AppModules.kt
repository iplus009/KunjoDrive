package com.novo.drive.di

import com.novo.drive.core.common.commonModule
import com.novo.drive.core.crypto.cryptoModule
import com.novo.drive.core.dispatchers.dispatchersModule
import com.novo.drive.core.files.filesModule
import com.novo.drive.core.files.sharedFilesModule
import com.novo.drive.core.logging.debugLogModule
import com.novo.drive.core.media.mediaModule
import com.novo.drive.core.network.networkModule
import com.novo.drive.core.power.powerModule
import com.novo.drive.core.permissions.permissionsModule
import com.novo.drive.core.proxy.proxyModule
import com.novo.drive.core.publish.publishModule
import com.novo.drive.core.security.securityModule
import com.novo.drive.core.telegram.telegramModule
import com.novo.drive.core.transfer.transferEngineModule
import com.novo.drive.core.transfer.transferModule
import com.novo.drive.core.update.updateModule
import com.novo.drive.data.local.database.daosModule
import com.novo.drive.data.local.database.databaseModule
import com.novo.drive.data.local.preferences.preferencesModule
import com.novo.drive.data.repository.localDataModule
import com.novo.drive.data.repository.repositoryModule
import com.novo.drive.domain.usecase.useCaseModule
import com.novo.drive.presentation.presentationModule

val appModules = listOf(
    commonModule,
    cryptoModule,
    dispatchersModule,
    filesModule,
    mediaModule,
    networkModule,
    powerModule,
    permissionsModule,
    proxyModule,
    publishModule,
    securityModule,
    telegramModule,
    transferModule,
    updateModule,
    databaseModule,
    daosModule,
    preferencesModule,
    repositoryModule,
    localDataModule,
    sharedFilesModule,
    debugLogModule,
    transferEngineModule,
    useCaseModule,
    presentationModule
)
