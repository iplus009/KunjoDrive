package com.novo.drive.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novo.drive.core.common.AppResult
import com.novo.drive.core.network.SupabaseRestClient
import com.novo.drive.core.telegram.TelegramAuthState
import com.novo.drive.data.repository.KunjoAuthRepository
import com.novo.drive.data.repository.KunjoAuthResult
import com.novo.drive.domain.repository.TelegramAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KunjoAuthUiState(
    val username: String = "",
    val password: String = "",
    val isRegisterMode: Boolean = false,
    val isSubAccount: Boolean = false,
    val parentUsername: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showForgotDialog: Boolean = false,
    val forgotUsername: String = "",
    val forgotPhoneNumber: String = "",
    val forgotOtpCode: String = "",
    val forgotOtpSent: Boolean = false,
    val isSendingForgotOtp: Boolean = false,
    val isResettingPassword: Boolean = false,
    val forgotDialogError: String? = null,
    val newPasswordInput: String = ""
)

class KunjoAuthViewModel(
    private val authRepository: KunjoAuthRepository,
    private val telegramAuthRepository: TelegramAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KunjoAuthUiState())
    val uiState: StateFlow<KunjoAuthUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun toggleMode() {
        _uiState.update {
            it.copy(
                isRegisterMode = !it.isRegisterMode,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun toggleSubAccount(isSub: Boolean) {
        _uiState.update { it.copy(isSubAccount = isSub) }
    }

    fun onParentUsernameChange(value: String) {
        _uiState.update { it.copy(parentUsername = value) }
    }

    fun openForgotPassword(open: Boolean) {
        _uiState.update {
            it.copy(
                showForgotDialog = open,
                forgotDialogError = null,
                forgotOtpSent = false,
                forgotOtpCode = "",
                newPasswordInput = ""
            )
        }
    }

    fun onForgotUsernameChange(value: String) {
        _uiState.update { it.copy(forgotUsername = value, forgotDialogError = null) }
    }

    fun onForgotPhoneNumberChange(value: String) {
        _uiState.update { it.copy(forgotPhoneNumber = value, forgotDialogError = null) }
    }

    fun onForgotOtpCodeChange(value: String) {
        _uiState.update { it.copy(forgotOtpCode = value, forgotDialogError = null) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPasswordInput = value, forgotDialogError = null) }
    }

    fun sendForgotOtp() {
        val state = _uiState.value
        val cleanUser = state.forgotUsername.trim().lowercase().removePrefix("@")
        val cleanPhone = state.forgotPhoneNumber.trim()

        if (cleanUser.length < 3) {
            _uiState.update { it.copy(forgotDialogError = "সঠিক ইউজারনেম দিন (সর্বনিম্ন ৩ অক্ষর)") }
            return
        }
        if (cleanPhone.length < 9) {
            _uiState.update { it.copy(forgotDialogError = "সঠিক টেলিগ্রাম ফোন নম্বর দিন (যেমন: +8801...)") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingForgotOtp = true, forgotDialogError = null) }
            val existing = SupabaseRestClient.getUserByUsername(cleanUser)
            if (existing == null) {
                _uiState.update {
                    it.copy(
                        isSendingForgotOtp = false,
                        forgotDialogError = "অ্যাকাউন্ট খুঁজে পাওয়া যায়নি! ইউজারনেম সঠিক কিনা যাচাই করুন।"
                    )
                }
                return@launch
            }
            if (existing.telegram_user_id == null || existing.telegram_user_id == 0L) {
                _uiState.update {
                    it.copy(
                        isSendingForgotOtp = false,
                        forgotDialogError = "এই অ্যাকাউন্টে ভেরিফাইড টেলিগ্রাম আইডি লিংক করা নেই, তাই ওটিপি দিয়ে রিসেট করা সম্ভব নয়।"
                    )
                }
                return@launch
            }

            when (telegramAuthRepository.submitPhoneNumber(cleanPhone)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSendingForgotOtp = false,
                            forgotOtpSent = true,
                            forgotDialogError = null
                        )
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSendingForgotOtp = false,
                            forgotDialogError = "টেলিগ্রামে ওটিপি পাঠানো যায়নি। ফোন নম্বর ঠিক আছে কি না চেক করুন।"
                        )
                    }
                }
            }
        }
    }

    fun resendForgotOtp() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingForgotOtp = true, forgotDialogError = null) }
            telegramAuthRepository.resendCode()
            _uiState.update { it.copy(isSendingForgotOtp = false) }
        }
    }

    fun verifyOtpAndResetPassword(onSuccess: () -> Unit) {
        val state = _uiState.value
        val cleanUser = state.forgotUsername.trim().lowercase().removePrefix("@")
        val cleanCode = state.forgotOtpCode.trim()
        val newPass = state.newPasswordInput

        if (cleanCode.length < 4) {
            _uiState.update { it.copy(forgotDialogError = "টেলিগ্রামের সম্পূর্ণ ওটিপি কোডটি লিখুন") }
            return
        }
        if (newPass.length < 6) {
            _uiState.update { it.copy(forgotDialogError = "নতুন পাসওয়ার্ড সর্বনিম্ন ৬ অক্ষরের হতে হবে") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isResettingPassword = true, forgotDialogError = null) }
            val codeResult = telegramAuthRepository.submitCode(cleanCode)
            when (codeResult) {
                is AppResult.Success -> {
                    if (telegramAuthRepository.authState.value !is TelegramAuthState.Ready) {
                        telegramAuthRepository.logout()
                        _uiState.update {
                            it.copy(
                                isResettingPassword = false,
                                forgotDialogError = "টেলিগ্রাম ২-ধাপ পাসওয়ার্ড (2FA) সক্রিয় থাকায় ভেরিফিকেশন সম্পন্ন হয়নি। অ্যাপের ভেতর থেকে পাসওয়ার্ড পরিবর্তন করুন।"
                            )
                        }
                        return@launch
                    }
                    val currentTgUser = (telegramAuthRepository.getCurrentUser() as? AppResult.Success)?.value
                    val resetResult = authRepository.resetPassword(
                        username = cleanUser,
                        newPassword = newPass,
                        freshSessionString = "",
                        verifiedTelegramUserId = currentTgUser?.id
                    )
                    when (resetResult) {
                        is KunjoAuthResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isResettingPassword = false,
                                    showForgotDialog = false,
                                    successMessage = "✅ পাসওয়ার্ড সফলভাবে পরিবর্তিত হয়েছে! নতুন পাসওয়ার্ড দিয়ে লগইন করুন।"
                                )
                            }
                            onSuccess()
                        }
                        is KunjoAuthResult.Error -> {
                            telegramAuthRepository.logout()
                            _uiState.update {
                                it.copy(
                                    isResettingPassword = false,
                                    forgotDialogError = resetResult.message
                                )
                            }
                        }
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isResettingPassword = false,
                            forgotDialogError = "⚠️ ভুল ওটিপি কোড! টেলিগ্রাম অ্যাপের সঠিক কোডটি দিন।"
                        )
                    }
                }
            }
        }
    }

    fun login(onSuccess: (String, String) -> Unit) {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "ইউজারনেম এবং পাসওয়ার্ড লিখুন") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.loginWithPassword(state.username, state.password)
            when (result) {
                is KunjoAuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "লগইন সফল!") }
                    onSuccess(result.username, result.accountType)
                }
                is KunjoAuthResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun register(sessionData: String?, storageChannelId: Long?, onSuccess: (String, String) -> Unit) {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "ইউজারনেম ও পাসওয়ার্ড পূরণ করুন") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val accountType = if (state.isSubAccount) "SUB" else "MAIN"
            val result = authRepository.registerAccount(
                username = state.username,
                password = state.password,
                accountType = accountType,
                parentUsername = if (state.isSubAccount) state.parentUsername else null,
                sessionString = sessionData,
                storageChannelId = storageChannelId
            )
            when (result) {
                is KunjoAuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, successMessage = "অ্যাকাউন্ট তৈরি সম্পন্ন!") }
                    onSuccess(result.username, result.accountType)
                }
                is KunjoAuthResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }
}

