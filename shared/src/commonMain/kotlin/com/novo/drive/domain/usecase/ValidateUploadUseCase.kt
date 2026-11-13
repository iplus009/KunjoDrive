package com.novo.drive.domain.usecase

import com.novo.drive.core.common.AppError
import com.novo.drive.core.telegram.TelegramLimits

class ValidateUploadUseCase {

    /**
     * @param requiredScratchBytes extra local space needed, e.g. for an
     * encrypted staging copy of the file.
     */
    operator fun invoke(
        fileSizeBytes: Long,
        limits: TelegramLimits,
        availableLocalBytes: Long,
        requiredScratchBytes: Long = 0,
        splitsIfTooLarge: Boolean = false
    ): AppError? = when {
        fileSizeBytes <= 0 ->
            AppError.NotFound

        fileSizeBytes > limits.maxFileBytes && !splitsIfTooLarge ->
            AppError.FileTooLarge(fileSizeBytes, limits.maxFileBytes)

        requiredScratchBytes > 0 &&
            (requiredScratchBytes > Long.MAX_VALUE - STORAGE_MARGIN_BYTES ||
                availableLocalBytes < requiredScratchBytes + STORAGE_MARGIN_BYTES) ->
            AppError.InsufficientStorage(
                (requiredScratchBytes + STORAGE_MARGIN_BYTES).coerceAtLeast(requiredScratchBytes),
                availableLocalBytes
            )

        else -> null
    }

    companion object {
        private const val STORAGE_MARGIN_BYTES = 100L * 1024 * 1024
    }
}
