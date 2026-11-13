package com.novo.drive.presentation.onboarding

import com.novo.drive.presentation.common.UiText

import com.novo.drive.core.telegram.CodeDeliveryChannel
import com.novo.drive.domain.model.Country
import com.novo.drive.domain.model.DriveChannel

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val working: Boolean = false,
    val error: UiText? = null,
    val codePhoneNumber: String = "",
    val codeChannel: CodeDeliveryChannel = CodeDeliveryChannel.TELEGRAM_APP,
    val codeLength: Int? = null,
    val passwordHint: String? = null,
    val registrationRequired: Boolean = false,
    val qrLink: String? = null,
    val qrMode: Boolean = false,
    val countries: List<Country> = emptyList(),
    val countryLoadState: CountryLoadState = CountryLoadState.LOADING,
    val selectedCountry: Country? = null,
    val channels: List<DriveChannel> = emptyList(),
    val selectedChatId: Long? = null,
    val channelCreated: Boolean = false,
    val backupDcim: Boolean = true,
    val backupPictures: Boolean = true,
    val backupMovies: Boolean = false,
    val autoBackupEnabled: Boolean = true,
    val wifiOnly: Boolean = false,
    val finishing: Boolean = false,
    val isFamilyFlow: Boolean = false,
    val familyAwaitingOwnerApproval: Boolean = false,
    val familyGrantedMemberName: String? = null,
    val familyGrantedSlot: Int? = null,
    val familyGrantedQuotaBytes: Long = 0L,
    val kunjoUsernameInput: String = "",
    val kunjoAccountError: String? = null,
    val isCheckingKunjoUsername: Boolean = false
)
