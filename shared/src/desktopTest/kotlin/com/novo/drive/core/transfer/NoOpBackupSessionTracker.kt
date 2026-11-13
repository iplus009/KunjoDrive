package com.novo.drive.core.transfer

internal class NoOpBackupSessionTracker : BackupSessionTracker {
    override suspend fun refreshActive() = Unit
    override suspend fun refresh(sessionId: String?) = Unit
}
