package com.novo.drive.domain.usecase

import com.novo.drive.domain.model.BackupDecision
import com.novo.drive.domain.model.Exclusion

/** Checks size and mtime first, and the content hash only when the mtime alone changed. */
class DecideBackupActionUseCase(
    private val evaluateExclusions: EvaluateExclusionsUseCase
) {

    operator fun invoke(
        candidate: ExclusionCandidate,
        modifiedAt: Long,
        existingRecord: ExistingBackupRecord?,
        exclusions: List<Exclusion>,
        maxFileSizeBytes: Long,
        contentHashProvider: () -> String?
    ): BackupDecision {
        if (evaluateExclusions(candidate, exclusions)) return BackupDecision.SKIP_EXCLUDED
        if (maxFileSizeBytes in 1..<candidate.sizeBytes) return BackupDecision.SKIP_TOO_LARGE

        if (existingRecord != null) {
            val sameSizeAndTime = existingRecord.sizeBytes == candidate.sizeBytes &&
                    existingRecord.modifiedAt == modifiedAt
            if (sameSizeAndTime) return BackupDecision.SKIP_UNCHANGED

            if (existingRecord.sizeBytes == candidate.sizeBytes &&
                existingRecord.contentHash != null
            ) {
                val currentHash = contentHashProvider()
                if (currentHash != null && currentHash == existingRecord.contentHash) {
                    return BackupDecision.SKIP_UNCHANGED
                }
            }
        }
        return BackupDecision.BACKUP
    }
}
