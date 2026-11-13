package com.novo.drive.core.family

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.novo.drive.core.family.FamilyPolicyGuard
import com.novo.drive.domain.repository.ChannelRepository
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.domain.repository.TelegramAuthRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Keeps Owner and Family Cloud sessions permanently alive anywhere in the world by issuing an
 * authenticated lightweight heartbeat every 7 days — preventing 180-day account/session expiry
 * and verifying active Family Cloud slot validity.
 */
class SessionKeepAliveWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val settingsRepository: SettingsRepository by inject()
    private val telegramAuthRepository: TelegramAuthRepository by inject()
    private val channelRepository: ChannelRepository by inject()

    override suspend fun doWork(): Result {
        val prefs = settingsRepository.preferences.first()
        if (!prefs.onboardingComplete) return Result.success()

        if (!FamilyPolicyGuard.verifyActiveFamilyGrant(prefs)) {
            return Result.success()
        }
        if (prefs.isFamilyMember && prefs.familyExpiresAtMs > 0L &&
            System.currentTimeMillis() > prefs.familyExpiresAtMs
        ) {
            return Result.success()
        }

        runCatching {
            telegramAuthRepository.startFromStoredCredentials()
            telegramAuthRepository.getCurrentUser()
            channelRepository.refreshKnown()
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "novo_drive_session_keep_alive"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SessionKeepAliveWorker>(
                7, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

