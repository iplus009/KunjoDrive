package com.novo.drive.core.media

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import com.novo.drive.core.crypto.CryptoKeys
import com.novo.drive.core.crypto.StreamCrypto
import com.novo.drive.core.crypto.WrappedKeyRepository
import com.novo.drive.core.telegram.TelegramClient

@UnstableApi
class TelegramDataSourceFactory(
    private val telegramClient: TelegramClient,
    private val streamCrypto: StreamCrypto,
    private val wrappedKeyRepository: WrappedKeyRepository
) {

    fun create(remoteFileId: String): DataSource.Factory =
        DataSource.Factory { TelegramDataSource(telegramClient, remoteFileId) }

    /**
     * The content key is read here rather than carried through the UI, keeping key material out of
     * composed state.
     */
    fun createParted(
        parts: List<MediaPart>,
        encrypted: Boolean
    ): DataSource.Factory = DataSource.Factory {
        val key = if (encrypted) wrappedKeyRepository.get(CryptoKeys.CONTENT) else null
        PartedTelegramDataSource(telegramClient, streamCrypto, parts, encrypted, key)
    }
}
