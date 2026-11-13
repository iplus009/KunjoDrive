package com.novo.drive.presentation.onboarding

enum class OnboardingStep {
    WELCOME,
    FAMILY_LOGIN,
    API_CREDENTIALS,
    PHONE,
    EMAIL_ADDRESS,
    EMAIL_CODE,
    CODE,
    PASSWORD,
    PERMISSIONS,
    CHANNEL_SELECT,
    KUNJO_ACCOUNT_SETUP,
    BACKUP_SETUP,
    DONE;

    fun displayNumber(counted: List<OnboardingStep>): Int =
        counted.indexOfLast { it.ordinal <= ordinal }.coerceAtLeast(0) + 1

    companion object {
        fun counted(
            includePermissions: Boolean,
            includeBackup: Boolean
        ): List<OnboardingStep> = buildList {
            add(WELCOME)
            add(API_CREDENTIALS)
            add(PHONE)
            add(CODE)
            if (includePermissions) add(PERMISSIONS)
            add(CHANNEL_SELECT)
            add(KUNJO_ACCOUNT_SETUP)
            if (includeBackup) add(BACKUP_SETUP)
        }
    }
}
