package com.novo.drive.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novo.drive.core.common.AppResult
import com.novo.drive.core.common.bi
import com.novo.drive.core.family.FamilyAccessManager
import com.novo.drive.core.family.NovoFamilyGrantPayload
import com.novo.drive.core.telegram.CodeDeliveryChannel
import com.novo.drive.core.telegram.TelegramAuthState
import com.novo.drive.core.telegram.TelegramCredentials
import com.novo.drive.core.transfer.MaintenanceScheduler
import com.novo.drive.domain.model.AccountRole
import com.novo.drive.domain.model.Country
import com.novo.drive.domain.model.DriveChannel
import com.novo.drive.domain.repository.ChannelRepository
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.domain.repository.SyncRepository
import com.novo.drive.domain.repository.TelegramAuthRepository
import com.novo.drive.presentation.common.UiText
import com.novo.drive.presentation.common.toUiText
import com.novo.drive.presentation.platform.PlatformCapabilities
import com.novo.drive.presentation.platform.StandardFolderPaths
import com.novo.drive.resources.Res
import com.novo.drive.resources.onboarding_api_hash_short
import com.novo.drive.resources.onboarding_api_id_number
import com.novo.drive.data.repository.KunjoAuthRepository
import com.novo.drive.data.repository.KunjoAuthResult
import com.novo.drive.resources.onboarding_enter_phone
import com.novo.drive.resources.onboarding_no_account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

class OnboardingViewModel(
    private val telegramAuthRepository: TelegramAuthRepository,
    private val settingsRepository: SettingsRepository,
    private val syncRepository: SyncRepository,
    private val maintenanceScheduler: MaintenanceScheduler,
    private val channelRepository: ChannelRepository,
    private val standardFolderPaths: StandardFolderPaths,
    private val platformCapabilities: PlatformCapabilities,
    private val familyAccessManager: FamilyAccessManager,
    private val kunjoAuthRepository: KunjoAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var pendingFamilyGrant: NovoFamilyGrantPayload? = null

    init {
        telegramAuthRepository.authState
            .onEach(::onAuthStateChanged)
            .launchIn(viewModelScope)
    }

    private fun onAuthStateChanged(state: TelegramAuthState) {
        _uiState.update { current ->
            when (state) {
                // Falling back from a QR wait means the token died, so the cached link is dropped.
                is TelegramAuthState.WaitingForPhoneNumber -> when {
                    (current.step == OnboardingStep.API_CREDENTIALS || current.step == OnboardingStep.WELCOME) && current.working ->
                        current.copy(step = OnboardingStep.PHONE, working = false, error = null)

                    current.step == OnboardingStep.FAMILY_LOGIN && current.familyAwaitingOwnerApproval -> {
                        viewModelScope.launch { telegramAuthRepository.requestQrCodeAuthentication() }
                        current.copy(working = true, qrLink = null)
                    }

                    current.qrLink != null -> current.copy(working = false, qrLink = null)
                    else -> current
                }

                is TelegramAuthState.WaitingForCode -> current.copy(
                    step = OnboardingStep.CODE,
                    working = false,
                    error = null,
                    codePhoneNumber = state.phoneNumber,
                    codeChannel = state.channel,
                    codeLength = state.codeLength
                )
                is TelegramAuthState.WaitingForQrScan -> current.copy(
                    working = false,
                    qrLink = state.link
                )

                is TelegramAuthState.WaitingForEmailAddress -> current.copy(
                    step = OnboardingStep.EMAIL_ADDRESS,
                    working = false,
                    error = null
                )

                is TelegramAuthState.WaitingForEmailCode -> current.copy(
                    step = OnboardingStep.EMAIL_CODE,
                    working = false,
                    error = null,
                    codePhoneNumber = state.emailPattern,
                    codeChannel = CodeDeliveryChannel.EMAIL,
                    codeLength = state.codeLength
                )

                is TelegramAuthState.WaitingForPassword -> current.copy(
                    step = OnboardingStep.PASSWORD,
                    working = false,
                    error = null,
                    passwordHint = state.passwordHint
                )

                is TelegramAuthState.RegistrationRequired -> current.copy(
                    working = false,
                    registrationRequired = true,
                    error = UiText.Resource(Res.string.onboarding_no_account)
                )

                is TelegramAuthState.Failed -> current.copy(
                    working = false,
                    error = UiText.Plain(state.message)
                )

                is TelegramAuthState.Ready ->
                    // A signed-in user reopening the app resumes past sign-in.
                    if (current.step in listOf(
                            OnboardingStep.WELCOME,
                            OnboardingStep.FAMILY_LOGIN,
                            OnboardingStep.PHONE,
                            OnboardingStep.CODE,
                            OnboardingStep.PASSWORD,
                            OnboardingStep.API_CREDENTIALS
                        )
                    ) {
                        if (!platformCapabilities.requiresPermissions) {
                            viewModelScope.launch { resolvePostAuthDestination() }
                            current.copy(working = true, error = null)
                        } else {
                            current.copy(
                                step = OnboardingStep.PERMISSIONS,
                                working = false,
                                error = null
                            )
                        }
                    } else current

                else -> current
            }
        }
    }

    fun start() {
        pendingFamilyGrant = null
        viewModelScope.launch {
            familyAccessManager.clearFamilyGrantState()
        }
        _uiState.update {
            it.copy(
                step = OnboardingStep.API_CREDENTIALS,
                isFamilyFlow = false,
                familyAwaitingOwnerApproval = false,
                error = null
            )
        }
    }

    fun startFamilyLogin() {
        pendingFamilyGrant = null
        _uiState.update {
            it.copy(
                step = OnboardingStep.FAMILY_LOGIN,
                isFamilyFlow = true,
                familyAwaitingOwnerApproval = false,
                qrLink = null,
                error = null
            )
        }
    }

    fun backToWelcome() {
        pendingFamilyGrant = null
        viewModelScope.launch {
            familyAccessManager.clearFamilyGrantState()
        }
        _uiState.update {
            it.copy(
                step = OnboardingStep.WELCOME,
                isFamilyFlow = false,
                familyAwaitingOwnerApproval = false,
                qrLink = null,
                working = false,
                error = null
            )
        }
    }

    fun submitFamilyInvite(inviteToken: String, pairingPin: String) {
        _uiState.update { it.copy(working = true, error = null) }
        viewModelScope.launch {
            when (val result = familyAccessManager.acceptFamilyInvite(inviteToken, pairingPin)) {
                is AppResult.Success -> {
                    val payload = result.value
                    pendingFamilyGrant = payload
                    _uiState.update {
                        it.copy(
                            isFamilyFlow = true,
                            familyAwaitingOwnerApproval = true,
                            familyGrantedMemberName = payload.memberName,
                            familyGrantedSlot = payload.slotIndex,
                            familyGrantedQuotaBytes = payload.quotaBytes,
                            working = true,
                            error = null
                        )
                    }
                    when (val qrReq = telegramAuthRepository.requestQrCodeAuthentication()) {
                        is AppResult.Success -> Unit
                        is AppResult.Failure -> {
                            onAuthStateChanged(telegramAuthRepository.authState.value)
                        }
                    }
                }
                is AppResult.Failure -> {
                    _uiState.update {
                        it.copy(working = false, error = result.error.toUiText())
                    }
                }
            }
        }
    }

    fun submitCredentials(apiIdText: String, apiHash: String) {
        val apiId = apiIdText.trim().toIntOrNull()
        if (apiId == null || apiId <= 0) {
            _uiState.update { it.copy(error = UiText.Resource(Res.string.onboarding_api_id_number)) }
            return
        }
        if (apiHash.trim().length < 16) {
            _uiState.update { it.copy(error = UiText.Resource(Res.string.onboarding_api_hash_short)) }
            return
        }
        runAction {
            val result =
                telegramAuthRepository.configure(TelegramCredentials(apiId, apiHash.trim()))
            if (result is AppResult.Success) {
                val currentAuth = telegramAuthRepository.authState.value
                if (currentAuth is TelegramAuthState.WaitingForPhoneNumber) {
                    _uiState.update { it.copy(step = OnboardingStep.PHONE, working = false, error = null) }
                } else {
                    onAuthStateChanged(currentAuth)
                }
            }
            result
        }
    }

    /**
     * A failure is not surfaced: the field still takes a full number, and whatever broke this
     * breaks login too.
     */
    fun loadCountries() {
        if (_uiState.value.countries.isNotEmpty()) return
        _uiState.update { it.copy(countryLoadState = CountryLoadState.LOADING) }
        viewModelScope.launch {
            val fetched = withTimeoutOrNull(COUNTRY_LOAD_TIMEOUT_MS.milliseconds) {
                telegramAuthRepository.countries()
            }
            if (fetched == null) {
                _uiState.update { it.copy(countryLoadState = CountryLoadState.FAILED) }
                return@launch
            }
            when (fetched) {
                is AppResult.Success -> _uiState.update { current ->
                    current.copy(
                        countries = fetched.value.countries,
                        selectedCountry = current.selectedCountry ?: fetched.value.detected,
                        countryLoadState = if (fetched.value.countries.isEmpty()) {
                            CountryLoadState.FAILED
                        } else {
                            CountryLoadState.READY
                        }
                    )
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(countryLoadState = CountryLoadState.FAILED)
                }
            }
        }
    }

    fun selectCountry(country: Country) {
        _uiState.update { it.copy(selectedCountry = country) }
    }

    fun submitPhone(phone: String) {
        if (phone.isBlank()) {
            _uiState.update { it.copy(error = UiText.Resource(Res.string.onboarding_enter_phone)) }
            return
        }
        _uiState.update { it.copy(qrLink = null, qrMode = false) }
        runAction { telegramAuthRepository.submitPhoneNumber(phone.trim()) }
    }

    /**
     * TDLib stays in the QR state and repeating the request there is an error, so a link in hand
     * just reopens the page.
     */
    fun startQrLogin() {
        _uiState.update { it.copy(qrMode = true, error = null) }
        if (_uiState.value.qrLink != null) return
        runAction { telegramAuthRepository.requestQrCodeAuthentication() }
    }

    /**
     * TDLib rejects a phone number while waiting for another device, so the session restarts to
     * reach the phone step.
     */
    fun cancelQrLogin() {
        _uiState.update { it.copy(qrMode = false, qrLink = null, error = null) }
        viewModelScope.launch { telegramAuthRepository.restartAuthentication() }
    }

    fun submitEmailAddress(email: String) {
        if (email.isBlank()) return
        runAction { telegramAuthRepository.submitEmailAddress(email.trim()) }
    }

    fun submitEmailCode(code: String) {
        if (code.isBlank()) return
        runAction { telegramAuthRepository.submitEmailCode(code.trim()) }
    }

    fun submitCode(code: String) {
        if (code.isBlank()) return
        runAction { telegramAuthRepository.submitCode(code.trim()) }
    }

    fun submitPassword(password: String) {
        if (password.isEmpty()) return
        runAction { telegramAuthRepository.submitPassword(password) }
    }

    fun resendCode() {
        runAction { telegramAuthRepository.resendCode() }
    }

    /**
     * The drive is always shown first, so the user sees where files will live; an account with none
     * gets one.
     */
    fun onPermissionsResolved() {
        _uiState.update { it.copy(working = true, error = null) }
        viewModelScope.launch { resolvePostAuthDestination() }
    }

    private suspend fun resolvePostAuthDestination() {
        val grant = pendingFamilyGrant
        if (grant != null) {
            familyAccessManager.activateAcceptedGrant(grant)
            pendingFamilyGrant = null
        }
        val prefs = settingsRepository.preferences.first()
        if (prefs.isFamilyMember || _uiState.value.isFamilyFlow) {
            completeFamilyOnboarding()
        } else {
            loadOrCreateDrive()
        }
    }

    private suspend fun completeFamilyOnboarding() {
        val prefs = settingsRepository.preferences.first()
        val assignedChatId = prefs.storageChatId
        channelRepository.refresh()
        if (assignedChatId != null && assignedChatId != 0L) {
            channelRepository.switchTo(assignedChatId, index = false)
        }
        settingsRepository.update {
            it.copy(
                onboardingComplete = true,
                autoBackupEnabled = false
            )
        }
        maintenanceScheduler.scheduleAll(
            backupEnabled = false,
            backupIntervalHours = 24,
            wifiOnly = true,
            chargingOnly = false,
            instantBackup = false
        )
        syncRepository.fullResync()
        _uiState.update {
            it.copy(step = OnboardingStep.DONE, working = false, finishing = false)
        }
    }

    fun retryDriveSetup() {
        if (_uiState.value.working) return
        _uiState.update { it.copy(working = true, error = null) }
        viewModelScope.launch { loadOrCreateDrive() }
    }

    private suspend fun loadOrCreateDrive() {
        var channels = refreshChannels()
        var created = false
        var failure: UiText? = null
        if (channels.isEmpty()) {
            when (val result = channelRepository.create(DEFAULT_DRIVE_LABEL)) {
                is AppResult.Success -> created = true
                is AppResult.Failure -> failure = result.error.toUiText()
            }
            channels = refreshChannels()
        }
        _uiState.update {
            it.copy(
                step = OnboardingStep.CHANNEL_SELECT,
                channels = channels,
                channelCreated = created,
                selectedChatId = channels.firstOrNull { channel -> channel.isActive }?.chatId
                    ?: channels.firstOrNull()?.chatId,
                error = failure.takeIf { channels.isEmpty() },
                working = false
            )
        }
    }

    private suspend fun refreshChannels(): List<DriveChannel> =
        when (val result = channelRepository.refresh()) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> emptyList()
        }

    fun selectChannel(chatId: Long) = _uiState.update { it.copy(selectedChatId = chatId) }

    fun confirmChannel() {
        val chatId = _uiState.value.selectedChatId ?: run {
            advancePastChannel()
            return
        }
        _uiState.update { it.copy(working = true) }
        viewModelScope.launch {
            channelRepository.switchTo(chatId, index = false)
            _uiState.update { it.copy(working = false) }
            advancePastChannel()
        }
    }

    private fun advancePastChannel() {
        viewModelScope.launch {
            val existingUsername = settingsRepository.preferences.first().kunjoUsername
            if (existingUsername.isNotBlank()) {
                advancePastKunjoAccount()
            } else {
                _uiState.update { it.copy(step = OnboardingStep.KUNJO_ACCOUNT_SETUP) }
            }
        }
    }

    fun onKunjoUsernameChange(username: String) {
        _uiState.update { it.copy(kunjoUsernameInput = username, kunjoAccountError = null) }
    }

    fun submitKunjoAccountSetup() {
        val state = _uiState.value
        val cleanUser = state.kunjoUsernameInput.trim().lowercase().removePrefix("@")
        if (cleanUser.length < 3) {
            _uiState.update {
                it.copy(kunjoAccountError = bi("Username must be at least 3 characters.", "ইউজারনেম সর্বনিম্ন ৩ অক্ষরের হতে হবে"))
            }
            return
        }
        if (!cleanUser.matches(Regex("^[a-z0-9_]{3,20}$"))) {
            _uiState.update {
                it.copy(
                    kunjoAccountError = bi(
                        "Username can only contain lowercase letters (a-z), numbers (0-9), and _.",
                        "ইউজারনেম শুধুমাত্র ছোট হাতের ইংরেজি অক্ষর (a-z), সংখ্যা (0-9) এবং _ হতে হবে"
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingKunjoUsername = true, kunjoAccountError = null) }
            val tgUser = (telegramAuthRepository.getCurrentUser() as? AppResult.Success)?.value
            val result = kunjoAuthRepository.registerUsernameOnly(
                username = cleanUser,
                storageChannelId = state.selectedChatId,
                telegramUserId = tgUser?.id,
                telegramUsername = tgUser?.username
            )
            when (result) {
                is KunjoAuthResult.Success -> {
                    _uiState.update { it.copy(isCheckingKunjoUsername = false) }
                    advancePastKunjoAccount()
                }
                is KunjoAuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCheckingKunjoUsername = false,
                            kunjoAccountError = result.message
                        )
                    }
                }
            }
        }
    }

    private fun advancePastKunjoAccount() {
        if (platformCapabilities.supportsAutoBackup) {
            _uiState.update { it.copy(step = OnboardingStep.BACKUP_SETUP) }
        } else {
            _uiState.update { it.copy(autoBackupEnabled = false) }
            finishSetup()
        }
    }

    fun setBackupDcim(enabled: Boolean) = _uiState.update { it.copy(backupDcim = enabled) }

    fun setBackupPictures(enabled: Boolean) = _uiState.update { it.copy(backupPictures = enabled) }

    fun setBackupMovies(enabled: Boolean) = _uiState.update { it.copy(backupMovies = enabled) }

    fun setAutoBackup(enabled: Boolean) = _uiState.update { it.copy(autoBackupEnabled = enabled) }

    fun setWifiOnly(enabled: Boolean) = _uiState.update { it.copy(wifiOnly = enabled) }

    fun finishSetup() {
        val state = _uiState.value
        _uiState.update { it.copy(finishing = true) }
        viewModelScope.launch {
            val folders = buildSet {
                if (state.backupDcim) standardFolderPaths.camera?.let(::add)
                if (state.backupPictures) standardFolderPaths.pictures?.let(::add)
                if (state.backupMovies) standardFolderPaths.movies?.let(::add)
            }
            settingsRepository.update {
                it.copy(
                    onboardingComplete = true,
                    autoBackupEnabled = state.autoBackupEnabled,
                    backupWifiOnly = state.wifiOnly
                )
            }
            maintenanceScheduler.scheduleAll(
                backupEnabled = state.autoBackupEnabled,
                backupIntervalHours = 24,
                wifiOnly = state.wifiOnly,
                chargingOnly = false,
                instantBackup = state.autoBackupEnabled
            )
            syncRepository.fullResync()
            channelRepository.refresh()
            settingsRepository.preferences.first().storageChatId?.let { chatId ->
                channelRepository.setBackupFolders(chatId, folders)
            }
            _uiState.update { it.copy(step = OnboardingStep.DONE, finishing = false) }
        }
    }

    private fun runAction(block: suspend () -> AppResult<*>) {
        _uiState.update { it.copy(working = true, error = null) }
        viewModelScope.launch {
            when (val result = block()) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> _uiState.update {
                    it.copy(working = false, error = result.error.toUiText())
                }
            }
        }
    }

    private companion object {
        const val COUNTRY_LOAD_TIMEOUT_MS = 8_000L
        const val DEFAULT_DRIVE_LABEL = ""
    }
}
