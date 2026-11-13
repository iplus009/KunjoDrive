package com.novo.drive.domain.usecase

import com.novo.drive.core.common.AppResult
import com.novo.drive.domain.repository.TransferRepository
import com.novo.drive.testing.unused

internal class FakeTransferRepository : TransferRepository by unused() {
    val downloads = mutableListOf<String>()

    override suspend fun enqueueDownload(fileId: String, priority: Int): AppResult<String> {
        downloads += fileId
        return AppResult.Success(fileId)
    }
}
