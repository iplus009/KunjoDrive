package com.novo.drive.presentation.onboarding

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLocaleList
import androidx.compose.material.icons.filled.ContentPaste
import com.novo.drive.resources.onboarding_auto_paste
import com.novo.drive.resources.onboarding_step_1_title
import com.novo.drive.resources.onboarding_step_1_desc
import com.novo.drive.resources.onboarding_step_2_title
import com.novo.drive.resources.onboarding_step_2_desc
import com.novo.drive.resources.onboarding_step_3_title
import com.novo.drive.resources.onboarding_step_3_desc
import com.novo.drive.resources.onboarding_step_4_title
import com.novo.drive.resources.onboarding_step_4_desc
import com.novo.drive.resources.onboarding_step_5_title
import com.novo.drive.resources.onboarding_step_5_desc

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novo.drive.core.permissions.AppPermission
import com.novo.drive.core.permissions.PermissionChecker
import com.novo.drive.core.telegram.CodeDeliveryChannel
import com.novo.drive.domain.model.Country
import com.novo.drive.domain.model.DriveChannel
import com.novo.drive.presentation.common.Formatters
import com.novo.drive.presentation.common.UiText
import com.novo.drive.presentation.common.resolve
import com.novo.drive.presentation.components.ChannelAvatar
import com.novo.drive.presentation.components.QrCode
import com.novo.drive.presentation.platform.LocalPermissionRequester
import com.novo.drive.presentation.platform.LocalPlatformCapabilities
import com.novo.drive.presentation.platform.LocalSystemScreens
import com.novo.drive.presentation.platform.LocalTelegramLinkOpener
import com.novo.drive.resources.Res
import com.novo.drive.resources.app_name
import com.novo.drive.resources.backup
import com.novo.drive.resources.common_try_again
import com.novo.drive.resources.ic_launcher_monochrome
import com.novo.drive.resources.onboarding_action_continue
import com.novo.drive.resources.onboarding_allow_all_files
import com.novo.drive.resources.onboarding_allow_media
import com.novo.drive.resources.onboarding_api_access
import com.novo.drive.resources.onboarding_api_hash
import com.novo.drive.resources.onboarding_api_id
import com.novo.drive.resources.onboarding_channel_files_in_channel
import com.novo.drive.resources.onboarding_choose_api_dev_tools
import com.novo.drive.resources.onboarding_choose_country
import com.novo.drive.resources.onboarding_choose_drive
import com.novo.drive.resources.onboarding_choose_what_back_change
import com.novo.drive.resources.onboarding_code
import com.novo.drive.resources.onboarding_code_sent
import com.novo.drive.resources.onboarding_code_via_call
import com.novo.drive.resources.onboarding_code_via_email
import com.novo.drive.resources.onboarding_code_via_flash_call
import com.novo.drive.resources.onboarding_code_via_fragment
import com.novo.drive.resources.onboarding_code_via_missed_call
import com.novo.drive.resources.onboarding_code_via_sms
import com.novo.drive.resources.onboarding_code_via_sms_phrase
import com.novo.drive.resources.onboarding_code_via_sms_word
import com.novo.drive.resources.onboarding_code_via_telegram
import com.novo.drive.resources.onboarding_continue
import com.novo.drive.resources.onboarding_continue_without
import com.novo.drive.resources.onboarding_copy_api_keys
import com.novo.drive.resources.onboarding_country
import com.novo.drive.resources.onboarding_drive_created_desc
import com.novo.drive.resources.onboarding_drive_ready
import com.novo.drive.resources.onboarding_email_code_title
import com.novo.drive.resources.onboarding_email_continue
import com.novo.drive.resources.onboarding_email_description
import com.novo.drive.resources.onboarding_email_label
import com.novo.drive.resources.onboarding_email_title
import com.novo.drive.resources.onboarding_enter_code
import com.novo.drive.resources.onboarding_enter_password
import com.novo.drive.resources.onboarding_files_access_covers_documents
import com.novo.drive.resources.onboarding_files_live_own_private
import com.novo.drive.resources.onboarding_get_api_keys
import com.novo.drive.resources.onboarding_get_started
import com.novo.drive.resources.onboarding_got
import com.novo.drive.resources.onboarding_include_country_code_like
import com.novo.drive.resources.onboarding_looking_for_drives
import com.novo.drive.resources.onboarding_my_telegram_org
import com.novo.drive.resources.onboarding_no_country_match
import com.novo.drive.resources.onboarding_no_drive_yet
import com.novo.drive.resources.onboarding_number
import com.novo.drive.resources.onboarding_one_time_setup_keys
import com.novo.drive.resources.onboarding_open_dev_page
import com.novo.drive.resources.onboarding_opening
import com.novo.drive.resources.onboarding_password
import com.novo.drive.resources.onboarding_phone_number
import com.novo.drive.resources.onboarding_phone_number_telegram_account
import com.novo.drive.resources.onboarding_pick_drive_desc
import com.novo.drive.resources.onboarding_qr_code_description
import com.novo.drive.resources.onboarding_qr_confirm_instead
import com.novo.drive.resources.onboarding_qr_description
import com.novo.drive.resources.onboarding_qr_open_failed
import com.novo.drive.resources.onboarding_qr_open_telegram
import com.novo.drive.resources.onboarding_qr_same_device_description
import com.novo.drive.resources.onboarding_qr_scan_instead
import com.novo.drive.resources.onboarding_qr_sign_in
import com.novo.drive.resources.onboarding_qr_step_1
import com.novo.drive.resources.onboarding_qr_step_2
import com.novo.drive.resources.onboarding_qr_step_3
import com.novo.drive.resources.onboarding_qr_title
import com.novo.drive.resources.onboarding_qr_use_phone
import com.novo.drive.resources.onboarding_qr_waiting
import com.novo.drive.resources.onboarding_resend_code
import com.novo.drive.resources.onboarding_search_country
import com.novo.drive.resources.onboarding_section_behavior
import com.novo.drive.resources.onboarding_section_folders
import com.novo.drive.resources.onboarding_send_code
import com.novo.drive.resources.onboarding_set_up_proxy
import com.novo.drive.resources.onboarding_sign
import com.novo.drive.resources.onboarding_single_drive_desc
import com.novo.drive.resources.onboarding_step
import com.novo.drive.resources.onboarding_step_counter
import com.novo.drive.resources.onboarding_storage_access
import com.novo.drive.resources.onboarding_iplus_needs_media_files
import com.novo.drive.resources.onboarding_telegram_blocked
import com.novo.drive.resources.onboarding_toggle_auto_backup
import com.novo.drive.resources.onboarding_toggle_camera
import com.novo.drive.resources.onboarding_toggle_movies
import com.novo.drive.resources.onboarding_toggle_pictures
import com.novo.drive.resources.onboarding_toggle_wifi_only
import com.novo.drive.resources.onboarding_two_step_verification
import com.novo.drive.resources.onboarding_verify
import com.novo.drive.resources.onboarding_where_i_get_these
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    onOpenProxy: () -> Unit,
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.step == OnboardingStep.DONE) {
        onFinished()
        return
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .imePadding()
        ) {
            if (state.step != OnboardingStep.WELCOME) {
                Spacer(Modifier.height(20.dp))
                StepHeader(
                    step = state.step,
                    modifier = Modifier.padding(horizontal = PAGE_PADDING)
                )
            }
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val availableHeight = maxHeight
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = availableHeight),
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                        targetState = state.step,
                        transitionSpec = {
                            val forward = targetState.ordinal >= initialState.ordinal
                            val spec = tween<IntOffset>(
                                durationMillis = STEP_SLIDE_MS,
                                easing = EmphasizedEasing
                            )
                            slideInHorizontally(spec) { width ->
                                if (forward) width else -width
                            }.togetherWith(
                                slideOutHorizontally(spec) { width ->
                                    if (forward) -width else width
                                }
                            ).using(SizeTransform(clip = true) { _, _ -> snap() })
                        },
                        label = stringResource(Res.string.onboarding_step)
                    ) { step ->
                        when (step) {
                            OnboardingStep.WELCOME -> WelcomeStep(
                                working = state.working,
                                onSubmitCredentials = viewModel::submitCredentials,
                                onFamilyLogin = viewModel::startFamilyLogin
                            )
                            OnboardingStep.FAMILY_LOGIN -> FamilyLoginStep(
                                state = state,
                                onSubmitInvite = viewModel::submitFamilyInvite,
                                onOpenProxy = onOpenProxy,
                                onBack = viewModel::backToWelcome
                            )
                            OnboardingStep.API_CREDENTIALS -> CredentialsStep(
                                working = state.working,
                                error = state.error,
                                onSubmit = viewModel::submitCredentials
                            )

                            OnboardingStep.EMAIL_ADDRESS -> EmailAddressStep(
                                working = state.working,
                                error = state.error,
                                onSubmit = viewModel::submitEmailAddress
                            )

                            OnboardingStep.EMAIL_CODE -> EmailCodeStep(
                                working = state.working,
                                error = state.error,
                                emailPattern = state.codePhoneNumber,
                                codeLength = state.codeLength,
                                onSubmit = viewModel::submitEmailCode
                            )

                            OnboardingStep.PHONE -> if (state.qrMode) {
                                QrStep(
                                    working = state.working,
                                    error = state.error,
                                    link = state.qrLink,
                                    onCancel = viewModel::cancelQrLogin
                                )
                            } else PhoneStep(
                                countries = state.countries,
                                selectedCountry = state.selectedCountry,
                                countryLoadState = state.countryLoadState,
                                onLoadCountries = viewModel::loadCountries,
                                onSelectCountry = viewModel::selectCountry,
                                onUseQrCode = viewModel::startQrLogin,
                                onOpenProxy = onOpenProxy,
                                working = state.working,
                                error = state.error,
                                registrationRequired = state.registrationRequired,
                                onSubmit = viewModel::submitPhone
                            )

                            OnboardingStep.CODE -> CodeStep(
                                working = state.working,
                                error = state.error,
                                phoneNumber = state.codePhoneNumber,
                                channel = state.codeChannel,
                                codeLength = state.codeLength,
                                onSubmit = viewModel::submitCode,
                                onResend = viewModel::resendCode
                            )

                            OnboardingStep.PASSWORD -> PasswordStep(
                                working = state.working,
                                error = state.error,
                                hint = state.passwordHint,
                                onSubmit = viewModel::submitPassword
                            )

                            OnboardingStep.PERMISSIONS -> PermissionsStep(
                                working = state.working,
                                onResolved = viewModel::onPermissionsResolved
                            )

                            OnboardingStep.CHANNEL_SELECT -> ChannelSelectStep(
                                channels = state.channels,
                                selectedChatId = state.selectedChatId,
                                justCreated = state.channelCreated,
                                working = state.working,
                                error = state.error,
                                onSelect = viewModel::selectChannel,
                                onRetry = viewModel::retryDriveSetup,
                                onContinue = viewModel::confirmChannel
                            )

                            OnboardingStep.KUNJO_ACCOUNT_SETUP -> KunjoAccountSetupStep(
                                state = state,
                                onUsernameChange = viewModel::onKunjoUsernameChange,
                                onSubmit = viewModel::submitKunjoAccountSetup
                            )

                            OnboardingStep.BACKUP_SETUP -> BackupSetupStep(
                                state = state,
                                onDcim = viewModel::setBackupDcim,
                                onPictures = viewModel::setBackupPictures,
                                onMovies = viewModel::setBackupMovies,
                                onAutoBackup = viewModel::setAutoBackup,
                                onWifiOnly = viewModel::setWifiOnly,
                                onFinish = viewModel::finishSetup,
                                finishing = state.finishing
                            )

                            OnboardingStep.DONE -> Unit
                        }
                    }
                    Spacer(Modifier.height(32.dp + padding.calculateBottomPadding()))
                }
            }
        }
    }
}

/** Shown only when the account holds more than one drive, before anything is indexed. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChannelSelectStep(
    channels: List<DriveChannel>,
    selectedChatId: Long?,
    justCreated: Boolean,
    working: Boolean,
    error: UiText?,
    onSelect: (Long) -> Unit,
    onRetry: () -> Unit,
    onContinue: () -> Unit
) {
    OnboardingPage(
        icon = Icons.Filled.CloudQueue,
        title = when {
            channels.isEmpty() && error != null ->
                stringResource(Res.string.onboarding_no_drive_yet)

            justCreated -> stringResource(Res.string.onboarding_drive_ready)
            else -> stringResource(Res.string.onboarding_choose_drive)
        },
        description = when {
            channels.isEmpty() && error != null -> error.resolve()

            justCreated -> stringResource(Res.string.onboarding_drive_created_desc)

            channels.size > 1 -> stringResource(Res.string.onboarding_pick_drive_desc)

            else -> stringResource(Res.string.onboarding_single_drive_desc)
        }
    ) {
        channels.forEach { channel ->
            val selected = channel.chatId == selectedChatId
            val single = channels.size == 1
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    )
                    .selectable(
                        selected = selected,
                        enabled = !single,
                        role = Role.RadioButton,
                        onClick = { onSelect(channel.chatId) }
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (single) {
                    ChannelAvatar(channel = channel, size = 40.dp)
                } else {
                    RadioButton(selected = selected, onClick = null)
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = channel.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            Res.string.onboarding_channel_files_in_channel,
                            channel.remoteFileCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (channels.isEmpty()) {
            BigButton(
                label = stringResource(
                    if (working) Res.string.onboarding_looking_for_drives
                    else Res.string.common_try_again
                ),
                onClick = onRetry,
                enabled = !working
            )
            return@OnboardingPage
        }
        BigButton(
            label = stringResource(
                if (working) Res.string.onboarding_opening else Res.string.onboarding_continue
            ),
            onClick = onContinue,
            enabled = selectedChatId != null && !working,
            loading = working
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StepHeader(step: OnboardingStep, modifier: Modifier = Modifier) {
    val capabilities = LocalPlatformCapabilities.current
    val counted = OnboardingStep.counted(
        includePermissions = capabilities.requiresPermissions,
        includeBackup = capabilities.supportsAutoBackup
    )
    val stepNumber = step.displayNumber(counted)
    val totalSteps = counted.size
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Surface(
            shape = RoundedCornerShape(100),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = stringResource(Res.string.onboarding_step_counter, stepNumber, totalSteps),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        LinearWavyProgressIndicator(
            progress = { stepNumber.toFloat() / totalSteps },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OnboardingPage(
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_PADDING, vertical = PAGE_VERTICAL_PADDING)
    ) {
        HeroShape(icon = icon, iconPainter = iconPainter)
        Spacer(Modifier.height(24.dp))
        StepTitle(title = title, description = description)
        Spacer(Modifier.height(28.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroShape(icon: ImageVector?, iconPainter: Painter?) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(28.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            iconPainter != null -> Icon(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(128.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            icon != null -> Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StepTitle(title: String, description: String) {
    Column(
        modifier = Modifier
            .semantics(mergeDescendants = true) { heading() }
            .focusable()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WelcomeStep(
    working: Boolean,
    onSubmitCredentials: (String, String) -> Unit,
    onFamilyLogin: () -> Unit
) {
    val isBn = LocalLocaleList.current.firstOrNull()?.language?.startsWith("bn") == true
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    var apiId by rememberSaveable { mutableStateOf("21724456") }
    var apiHash by rememberSaveable { mutableStateOf("9e36814b7e9bfcf9d56ffcfbe4d19326") }
    var hashVisible by rememberSaveable { mutableStateOf(false) }

    val cardBg = if (isDark) Color(0xFF1E222A) else Color(0xFFF2EFE9)
    val textPrimary = if (isDark) Color(0xFFECEFF4) else Color(0xFF1A1D20)
    val textSecondary = if (isDark) Color(0xFF8C93A0) else Color(0xFF7A7D85)
    val buttonBg = if (isDark) Color(0xFF00E5FF) else Color(0xFF18212D)
    val buttonText = if (isDark) Color(0xFF0A0F14) else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_PADDING, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Luxury Airplane Window Emblem matching reference photo "Window Seat"
        Box(
            modifier = Modifier
                .size(76.dp, 94.dp)
                .clip(RoundedCornerShape(38.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFEADBCE), Color(0xFFD4C1AB), Color(0xFFC4AD94))
                    )
                )
                .border(
                    width = 4.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFF3E7DC), Color(0xFFB89E84))
                    ),
                    shape = RoundedCornerShape(38.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner glowing window pane
            Box(
                modifier = Modifier
                    .size(56.dp, 72.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFFF2D9),
                                Color(0xFFFCE1B3),
                                Color(0xFFF3C78B)
                            )
                        )
                    )
            ) {
                // Bottom curved window sill / shade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x33C49A62), Color(0x66B0864E))
                            )
                        )
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Title matching "Log in to Window Seat"
        Text(
            text = if (isBn) "Log in to সঞ্চয়িকা" else "Log in to Sanchayika",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        // Field 1: API ID Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Key,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "API ID",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )
                BasicTextField(
                    value = apiId,
                    onValueChange = { apiId = it.filter(Char::isDigit) },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Field 2: API Hash Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "API Hash",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )
                BasicTextField(
                    value = apiHash,
                    onValueChange = { apiHash = it.trim() },
                    singleLine = true,
                    visualTransformation = if (hashVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )
            }
            IconButton(
                onClick = { hashVisible = !hashVisible },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (hashVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Primary Button matching reference photo dark pill button
        Button(
            onClick = {
                val finalId = apiId.ifBlank { "21724456" }
                val finalHash = apiHash.ifBlank { "9e36814b7e9bfcf9d56ffcfbe4d19326" }
                onSubmitCredentials(finalId, finalHash)
            },
            enabled = !working,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonBg,
                contentColor = buttonText
            )
        ) {
            if (working) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = buttonText,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isBn) "এগিয়ে যান (Continue ➔)" else "Continue ➔",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Divider: "─ Or log in with ─"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(if (isDark) Color(0xFF2A313E) else Color(0xFFDDD8CE))
            )
            Text(
                text = "  Or log in with  ",
                fontSize = 12.sp,
                color = textSecondary
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(if (isDark) Color(0xFF2A313E) else Color(0xFFDDD8CE))
            )
        }

        Spacer(Modifier.height(16.dp))

        // Sub / Family Login Card
        Surface(
            onClick = onFamilyLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(18.dp),
            color = cardBg
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(text = "👨‍👩‍👧", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Login with Sub / Family",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FamilyLoginStep(
    state: OnboardingUiState,
    onSubmitInvite: (String, String) -> Unit,
    onOpenProxy: () -> Unit,
    onBack: () -> Unit
) {
    val isBn = LocalLocaleList.current.firstOrNull()?.language?.startsWith("bn") == true
    val clipboardManager = LocalClipboardManager.current
    val filePicker = com.novo.drive.presentation.platform.LocalFilePicker.current
    var inviteToken by rememberSaveable { mutableStateOf("") }
    var pairingPin by rememberSaveable { mutableStateOf("") }
    var copiedSessionLink by remember { mutableStateOf(false) }
    var qrError by remember { mutableStateOf<String?>(null) }
    var showLiveQrScanner by remember { mutableStateOf(false) }

    fun parseAndApplyFamilyInput(raw: String): Boolean {
        val text = raw.trim()
        if (text.isEmpty()) return false
        val tokenMatch = Regex("""(?:KUNJO-FAM-V2|NOVO-FAM-V5):[A-Za-z0-9_-]+""").find(text)?.value
        val pinLabelMatch = Regex("""PIN[:\s]+(\d{6})""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.getOrNull(1)
        val standalonePin = Regex("""(?<!\d)(\d{6})(?!\d)""").find(text)?.value
        val resolvedPin = pinLabelMatch ?: standalonePin
        if (tokenMatch != null) inviteToken = tokenMatch
        if (resolvedPin != null) pairingPin = resolvedPin
        return tokenMatch != null || resolvedPin != null
    }

    if (showLiveQrScanner) {
        com.novo.drive.presentation.components.LiveQrScanner(
            onResult = { decoded ->
                showLiveQrScanner = false
                if (parseAndApplyFamilyInput(decoded)) {
                    qrError = null
                } else {
                    qrError = if (isBn) "QR কোড পড়া যায়নি। আবার চেষ্টা করুন।" else "Could not read QR code. Try again."
                }
            },
            onCancel = { showLiveQrScanner = false }
        )
        return
    }

    fun autoPasteFamilyInvite() {
        val clip = clipboardManager.getText()?.text?.trim().orEmpty()
        if (!parseAndApplyFamilyInput(clip) && clip.isNotBlank()) {
            inviteToken = clip
        }
    }

    OnboardingPage(
        icon = Icons.Filled.FamilyRestroom,
        title = if (state.familyAwaitingOwnerApproval) {
            if (isBn) "ধাপ ২: মালিকের ফোনে অনুমোদন দিন" else "Step 2: Approve on owner's phone"
        } else {
            if (isBn) "ধাপ ১: ফ্যামিলি ইনভাইট ও পিন দিন" else "Step 1: Enter family invite & PIN"
        },
        description = if (state.familyAwaitingOwnerApproval) {
            if (isBn) "ইনভাইট সফলভাবে যাচাই হয়েছে (${state.familyGrantedMemberName ?: "Family"} • Slot #${state.familyGrantedSlot ?: 1} • কোটা ${Formatters.bytes(state.familyGrantedQuotaBytes)})। এখন ক্লাউড মালিকের Kunjo Drive ➔ Family Cloud ➔ 'ধাপ ২: সেশন অনুমোদন'-এ গিয়ে নিচের QR কোড স্ক্যান করুন অথবা লিংকটি কপি করে পাঠান।"
            else "Invite verified successfully (${state.familyGrantedMemberName ?: "Family"} • Slot #${state.familyGrantedSlot ?: 1} • Quota ${Formatters.bytes(state.familyGrantedQuotaBytes)}). Now scan this QR code or send this link to the drive owner to approve."
        } else {
            if (isBn) "ক্লাউড মালিকের ফোনে তৈরি হওয়া ফ্যামিলি QR কোডটি স্ক্যান করুন অথবা কপি করা টেক্সটটি নিচে পেস্ট করুন (কীবোর্ড পেস্ট বা অটো-পেস্ট দুটোই কাজ করবে)।"
            else "Scan the family invite QR code from the drive owner's phone or paste the copied invite text below."
        }
    ) {
        if (!state.familyAwaitingOwnerApproval) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { autoPasteFamilyInvite() },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isBn) "ক্লিপবোর্ড পেস্ট" else "Paste clipboard")
                }
                FilledTonalButton(
                    onClick = {
                        qrError = null
                        filePicker.pick { result ->
                            val path = (result as? com.novo.drive.presentation.platform.PickResult.Picked)?.path ?: return@pick
                            val decoded = com.novo.drive.presentation.components.QrScannerBridge.decodeFromImageFile(path)
                            if (decoded != null && parseAndApplyFamilyInput(decoded)) {
                                qrError = null
                            } else {
                                qrError = if (isBn) "QR কোড পড়া যায়নি। স্পষ্ট QR ছবি বা স্ক্রিনশট নির্বাচন করুন।" else "Could not read QR code. Please pick a clear QR image or screenshot."
                            }
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isBn) "🖼️ ছবি থেকে" else "🖼️ From image")
                }
                FilledTonalButton(
                    onClick = { showLiveQrScanner = true },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isBn) "📷 ক্যামেরা স্ক্যান" else "📷 Camera scan")
                }
            }
            qrError?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(14.dp))
            OnboardingField(
                value = inviteToken,
                onChange = { input ->
                    if (!parseAndApplyFamilyInput(input)) {
                        inviteToken = input.trim()
                    }
                },
                label = if (isBn) "Family Invite Token (কীবোর্ড দিয়ে পেস্ট করুন)" else "Family invite token",
                enabled = !state.working
            )
            Spacer(Modifier.height(12.dp))
            OnboardingField(
                value = pairingPin,
                onChange = { input ->
                    if (!parseAndApplyFamilyInput(input)) {
                        pairingPin = input.filter(Char::isDigit).take(6)
                    }
                },
                label = if (isBn) "৬-ডিজিট পেয়ারিং পিন" else "6-digit pairing PIN",
                keyboardType = KeyboardType.Number,
                enabled = !state.working
            )
            Spacer(Modifier.height(16.dp))
            BlockedNetworkCard(onOpenProxy = onOpenProxy)
            val isTokenValid = inviteToken.trim().let {
                it.startsWith("KUNJO-FAM-V2:") || it.startsWith("NOVO-FAM-V5:")
            }
            ErrorAndAction(
                working = state.working,
                error = state.error,
                actionLabel = if (isBn) "ধাপ ১ সম্পন্ন করুন ➔ সেশন QR তৈরি করুন" else "Complete step 1 ➔ Generate session QR",
                enabled = isTokenValid && pairingPin.length == 6,
                onAction = { onSubmitInvite(inviteToken.trim(), pairingPin.trim()) }
            )
        } else {
            val sessionLink = state.qrLink
            if (sessionLink != null) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = QR_SURFACE,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth(QR_WIDTH_FRACTION)
                    ) {
                        QrCode(
                            content = sessionLink,
                            contentDescription = "Family Session Approval QR",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(QR_PADDING)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                FilledTonalButton(
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(sessionLink))
                        copiedSessionLink = true
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (copiedSessionLink) {
                            if (isBn) "✅ সেশন লিংক কপি হয়েছে! এখন মালিকের ফোনে পাঠান" else "✅ Session link copied! Send it to the owner"
                        } else {
                            if (isBn) "১-ক্লিকে সেশন অনুমোদন লিংক কপি করুন" else "Copy session approval link"
                        }
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(SPINNER_SIZE),
                        strokeWidth = SPINNER_STROKE
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(if (isBn) "এনক্রিপ্টেড সেশন QR ও লিংক তৈরি হচ্ছে..." else "Generating encrypted session QR & link...")
                }
            }
            state.error?.let { err ->
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = err.resolve(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(if (isBn) "পিছনে ফিরে যান" else "Back")
        }
    }
}

@Composable
private fun CredentialsStep(
    working: Boolean,
    error: UiText?,
    onSubmit: (String, String) -> Unit
) {
    var apiId by rememberSaveable { mutableStateOf("") }
    var apiHash by rememberSaveable { mutableStateOf("") }
    var showGuide by rememberSaveable { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    fun autoPasteFromClipboard() {
        val clip = clipboardManager.getText()?.text?.trim() ?: ""
        if (clip.isNotEmpty()) {
            val hexMatch = Regex("""[0-9a-fA-F]{32}""").find(clip)?.value
            val idMatch = Regex("""(?<!\w)(\d{5,10})(?!\w)""").find(clip)?.value
            if (idMatch != null) apiId = idMatch
            if (hexMatch != null) apiHash = hexMatch
            if (idMatch == null && hexMatch == null) {
                if (clip.all { it.isDigit() }) apiId = clip
                else if (clip.length == 32) apiHash = clip
            }
        }
    }

    OnboardingPage(
        icon = Icons.Filled.Key,
        title = stringResource(Res.string.onboarding_api_access),
        description = stringResource(Res.string.onboarding_one_time_setup_keys)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { showGuide = true },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(stringResource(Res.string.onboarding_where_i_get_these))
            }
            FilledTonalButton(
                onClick = { autoPasteFromClipboard() },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(stringResource(Res.string.onboarding_auto_paste))
            }
        }
        Spacer(Modifier.height(20.dp))
        OnboardingField(
            value = apiId,
            onChange = { apiId = it },
            label = stringResource(Res.string.onboarding_api_id),
            keyboardType = KeyboardType.Number,
            enabled = !working,
            trailingIcon = {
                IconButton(onClick = {
                    val clip = clipboardManager.getText()?.text?.trim() ?: ""
                    if (clip.isNotEmpty()) {
                        val idMatch = Regex("""(?<!\w)(\d{5,10})(?!\w)""").find(clip)?.value ?: clip.filter { it.isDigit() }
                        if (idMatch.isNotEmpty()) apiId = idMatch
                    }
                }) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = "Paste API ID", modifier = Modifier.size(18.dp))
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        OnboardingField(
            value = apiHash,
            onChange = { apiHash = it },
            label = stringResource(Res.string.onboarding_api_hash),
            enabled = !working,
            trailingIcon = {
                IconButton(onClick = {
                    val clip = clipboardManager.getText()?.text?.trim() ?: ""
                    if (clip.isNotEmpty()) {
                        val hexMatch = Regex("""[0-9a-fA-F]{32}""").find(clip)?.value ?: clip
                        apiHash = hexMatch
                    }
                }) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = "Paste API Hash", modifier = Modifier.size(18.dp))
                }
            }
        )
        ErrorAndAction(
            working = working,
            error = error,
            actionLabel = stringResource(Res.string.onboarding_action_continue),
            enabled = apiId.isNotBlank() && apiHash.isNotBlank(),
            onAction = { onSubmit(apiId, apiHash) }
        )
    }

    if (showGuide) {
        ApiGuideDialog(onDismiss = { showGuide = false })
    }
}

/**
 * Offered where users with a blocked network first stall, not in a screen they cannot reach yet.
 */
@Composable
private fun BlockedNetworkCard(onOpenProxy: () -> Unit) {
    val shape = MaterialTheme.shapes.extraLarge
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(role = Role.Button, onClick = onOpenProxy)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.VpnKey,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.onboarding_telegram_blocked),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.onboarding_set_up_proxy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ApiGuideDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.onboarding_get_api_keys)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                InstructionCard(number = 1) {
                    Column {
                        Text(
                            stringResource(Res.string.onboarding_step_1_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(Res.string.onboarding_step_1_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = { uriHandler.openUri("https://my.telegram.org") },
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(Res.string.onboarding_my_telegram_org))
                        }
                    }
                }
                InstructionCard(number = 2) {
                    Column {
                        Text(
                            stringResource(Res.string.onboarding_step_2_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(Res.string.onboarding_step_2_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                InstructionCard(number = 3) {
                    Column {
                        Text(
                            stringResource(Res.string.onboarding_step_3_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(Res.string.onboarding_step_3_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                InstructionCard(number = 4) {
                    Column {
                        Text(
                            stringResource(Res.string.onboarding_step_4_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(Res.string.onboarding_step_4_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                InstructionCard(number = 5) {
                    Column {
                        Text(
                            stringResource(Res.string.onboarding_step_5_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(Res.string.onboarding_step_5_desc),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes()
            ) { Text(stringResource(Res.string.onboarding_got)) }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InstructionCard(number: Int, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .semantics(mergeDescendants = true) {}
            .focusable()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(Modifier.size(14.dp))
            Box(modifier = Modifier.weight(1f)) { content() }
        }
    }
}

@Composable
private fun QrStep(
    working: Boolean,
    error: UiText?,
    link: String?,
    onCancel: () -> Unit
) {
    var openFailed by remember { mutableStateOf(false) }

    // The QR payload is a tg: link, so Telegram on this device can confirm directly.
    val telegramLinkOpener = LocalTelegramLinkOpener.current
    val canConfirmHere = telegramLinkOpener.canOpenTelegram
    // One route at a time: both at once reads like two required steps.
    var scanning by rememberSaveable(canConfirmHere) { mutableStateOf(!canConfirmHere) }

    OnboardingPage(
        icon = Icons.Filled.QrCode2,
        title = stringResource(Res.string.onboarding_qr_title),
        description = if (scanning) {
            stringResource(Res.string.onboarding_qr_description)
        } else {
            stringResource(Res.string.onboarding_qr_same_device_description)
        }
    ) {
        if (scanning) {
            Surface(
                color = QR_SURFACE,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth(QR_WIDTH_FRACTION)
                    .align(Alignment.CenterHorizontally)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(QR_PADDING)) {
                    if (link == null) {
                        CircularProgressIndicator(modifier = Modifier.padding(QR_PADDING))
                    } else {
                        QrCode(
                            content = link,
                            contentDescription = stringResource(
                                Res.string.onboarding_qr_code_description
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            QrInstruction(1, stringResource(Res.string.onboarding_qr_step_1))
            QrInstruction(2, stringResource(Res.string.onboarding_qr_step_2))
            QrInstruction(3, stringResource(Res.string.onboarding_qr_step_3))
        } else {
            BigButton(
                label = stringResource(Res.string.onboarding_qr_open_telegram),
                enabled = !working && link != null,
                onClick = {
                    openFailed = link == null || !telegramLinkOpener.open(link)
                }
            )
            if (openFailed) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.onboarding_qr_open_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        if (error != null) {
            Text(
                text = error.resolve(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(SPINNER_SIZE),
                    strokeWidth = SPINNER_STROKE
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(Res.string.onboarding_qr_waiting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        if (canConfirmHere) {
            TextButton(
                onClick = { scanning = !scanning },
                enabled = !working,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    stringResource(
                        if (scanning) {
                            Res.string.onboarding_qr_confirm_instead
                        } else {
                            Res.string.onboarding_qr_scan_instead
                        }
                    )
                )
            }
        }
        TextButton(
            onClick = onCancel,
            enabled = !working,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { Text(stringResource(Res.string.onboarding_qr_use_phone)) }
    }
}

@Composable
private fun QrInstruction(number: Int, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(QR_STEP_BADGE)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PhoneStep(
    working: Boolean,
    error: UiText?,
    registrationRequired: Boolean,
    countries: List<Country>,
    selectedCountry: Country?,
    countryLoadState: CountryLoadState,
    onLoadCountries: () -> Unit,
    onSelectCountry: (Country) -> Unit,
    onUseQrCode: () -> Unit,
    onOpenProxy: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var national by rememberSaveable { mutableStateOf("") }
    var picking by rememberSaveable { mutableStateOf(false) }
    var typedBeforeCountries by rememberSaveable { mutableStateOf(false) }
    val manual = countryLoadState == CountryLoadState.FAILED || typedBeforeCountries
    val callingCode = if (manual) "" else selectedCountry?.callingCode.orEmpty()

    LaunchedEffect(Unit) { onLoadCountries() }

    OnboardingPage(
        icon = Icons.Filled.PhoneAndroid,
        title = stringResource(Res.string.onboarding_number),
        description = stringResource(Res.string.onboarding_phone_number_telegram_account)
    ) {
        if (!manual) {
            CountryField(
                country = selectedCountry,
                loading = countryLoadState == CountryLoadState.LOADING,
                enabled = !working && countries.isNotEmpty(),
                onClick = { picking = true }
            )
            Spacer(Modifier.height(12.dp))
        }
        OnboardingField(
            value = national,
            onChange = { entry ->
                if (countryLoadState == CountryLoadState.LOADING) typedBeforeCountries = true
                national = entry.filter { it.isDigit() }
            },
            label = stringResource(Res.string.onboarding_phone_number),
            supportingText = if (manual) {
                stringResource(Res.string.onboarding_include_country_code_like)
            } else {
                null
            },
            keyboardType = KeyboardType.Phone,
            enabled = !working,
            prefix = "+$callingCode"
        )
        ErrorAndAction(
            working = working,
            error = error,
            actionLabel = stringResource(Res.string.onboarding_send_code),
            enabled = national.isNotBlank() && !registrationRequired,
            onAction = { onSubmit("+$callingCode$national") }
        )
        TextButton(
            onClick = onUseQrCode,
            enabled = !working,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Filled.QrCode2,
                contentDescription = null,
                modifier = Modifier.size(QR_BUTTON_ICON)
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.onboarding_qr_sign_in))
        }
        Spacer(Modifier.height(12.dp))
        BlockedNetworkCard(onOpenProxy = onOpenProxy)
    }

    if (picking) {
        CountryPickerSheet(
            countries = countries,
            onDismiss = { picking = false },
            onSelect = { country ->
                onSelectCountry(country)
                picking = false
            }
        )
    }
}

@Composable
private fun CountryField(
    country: Country?,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val fieldLabel = stringResource(Res.string.onboarding_country)
    val fieldDescription = country?.name?.let { "$fieldLabel, $it" } ?: fieldLabel
    Box {
        OutlinedTextField(
            value = country?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(Res.string.onboarding_country)) },
            trailingIcon = {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(SPINNER_SIZE),
                        strokeWidth = SPINNER_STROKE
                    )
                } else {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { canFocus = false }
                .clearAndSetSemantics {}
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(OutlinedTextFieldDefaults.shape)
                .clickable(enabled = enabled, role = Role.DropdownList, onClick = onClick)
                .semantics { contentDescription = fieldDescription }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryPickerSheet(
    countries: List<Country>,
    onDismiss: () -> Unit,
    onSelect: (Country) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val matches = remember(countries, query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            countries
        } else {
            countries.filter { country ->
                country.name.contains(trimmed, ignoreCase = true) ||
                        country.callingCode.contains(trimmed) ||
                        country.isoCode.equals(trimmed, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(Res.string.onboarding_choose_country),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(12.dp))
            OnboardingField(
                value = query,
                onChange = { query = it },
                label = stringResource(Res.string.onboarding_search_country)
            )
            Spacer(Modifier.height(12.dp))
            if (matches.isEmpty()) {
                Text(
                    text = stringResource(Res.string.onboarding_no_country_match),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(matches, key = { "${it.isoCode}_${it.callingCode}" }) { country ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .clickable(role = Role.Button) { onSelect(country) }
                            .padding(horizontal = 8.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = country.flag, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = country.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "+${country.callingCode}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun codeDeliveryText(channel: CodeDeliveryChannel, target: String): String =
    when (channel) {
        CodeDeliveryChannel.TELEGRAM_APP -> stringResource(Res.string.onboarding_code_via_telegram)
        CodeDeliveryChannel.SMS -> stringResource(Res.string.onboarding_code_via_sms, target)
        CodeDeliveryChannel.SMS_WORD -> stringResource(
            Res.string.onboarding_code_via_sms_word,
            target
        )

        CodeDeliveryChannel.SMS_PHRASE ->
            stringResource(Res.string.onboarding_code_via_sms_phrase, target)

        CodeDeliveryChannel.CALL -> stringResource(Res.string.onboarding_code_via_call, target)
        CodeDeliveryChannel.FLASH_CALL ->
            stringResource(Res.string.onboarding_code_via_flash_call, target)

        CodeDeliveryChannel.MISSED_CALL ->
            stringResource(Res.string.onboarding_code_via_missed_call, target)

        CodeDeliveryChannel.FRAGMENT -> stringResource(
            Res.string.onboarding_code_via_fragment,
            target
        )

        CodeDeliveryChannel.FIREBASE -> stringResource(Res.string.onboarding_code_via_sms, target)
        CodeDeliveryChannel.EMAIL -> stringResource(Res.string.onboarding_code_via_email, target)
        CodeDeliveryChannel.OTHER -> stringResource(Res.string.onboarding_code_sent)
    }

@Composable
private fun EmailAddressStep(
    working: Boolean,
    error: UiText?,
    onSubmit: (String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    OnboardingPage(
        icon = Icons.Filled.Email,
        title = stringResource(Res.string.onboarding_email_title),
        description = stringResource(Res.string.onboarding_email_description)
    ) {
        OnboardingField(
            value = email,
            onChange = { email = it },
            label = stringResource(Res.string.onboarding_email_label),
            keyboardType = KeyboardType.Email,
            enabled = !working
        )
        ErrorAndAction(
            working = working,
            error = error,
            actionLabel = stringResource(Res.string.onboarding_email_continue),
            enabled = email.isNotBlank(),
            onAction = { onSubmit(email) }
        )
    }
}

@Composable
private fun EmailCodeStep(
    working: Boolean,
    error: UiText?,
    emailPattern: String,
    codeLength: Int?,
    onSubmit: (String) -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }
    OnboardingPage(
        icon = Icons.Filled.Email,
        title = stringResource(Res.string.onboarding_email_code_title),
        description = codeDeliveryText(CodeDeliveryChannel.EMAIL, emailPattern)
    ) {
        OnboardingField(
            value = code,
            onChange = { input ->
                code = input.filter(Char::isDigit).let {
                    if (codeLength != null) it.take(codeLength) else it
                }
            },
            label = stringResource(Res.string.onboarding_code),
            keyboardType = KeyboardType.NumberPassword,
            enabled = !working
        )
        ErrorAndAction(
            working = working,
            error = error,
            actionLabel = stringResource(Res.string.onboarding_verify),
            enabled = code.isNotBlank(),
            onAction = { onSubmit(code) }
        )
    }
}

@Composable
private fun CodeStep(
    working: Boolean,
    error: UiText?,
    phoneNumber: String,
    channel: CodeDeliveryChannel,
    codeLength: Int?,
    onSubmit: (String) -> Unit,
    onResend: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }
    OnboardingPage(
        icon = Icons.Filled.Sms,
        title = stringResource(Res.string.onboarding_enter_code),
        description = codeDeliveryText(channel, phoneNumber)
    ) {
        OnboardingField(
            value = code,
            onChange = { input ->
                code = input.filter(Char::isDigit).let {
                    if (codeLength != null) it.take(codeLength) else it
                }
            },
            label = stringResource(Res.string.onboarding_code),
            keyboardType = KeyboardType.NumberPassword,
            enabled = !working
        )
        ErrorAndAction(
            working = working,
            error = error,
            actionLabel = stringResource(Res.string.onboarding_verify),
            enabled = code.isNotBlank(),
            onAction = { onSubmit(code) }
        )
        TextButton(
            onClick = onResend,
            enabled = !working,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { Text(stringResource(Res.string.onboarding_resend_code)) }
    }
}

@Composable
private fun PasswordStep(
    working: Boolean,
    error: UiText?,
    hint: String?,
    onSubmit: (String) -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }
    OnboardingPage(
        icon = Icons.Filled.Lock,
        title = stringResource(Res.string.onboarding_two_step_verification),
        description = hint?.let { "Enter your Telegram password. Hint: $it" }
            ?: stringResource(Res.string.onboarding_enter_password)
    ) {
        OnboardingField(
            value = password,
            onChange = { password = it },
            label = stringResource(Res.string.onboarding_password),
            keyboardType = KeyboardType.Password,
            password = true,
            enabled = !working
        )
        ErrorAndAction(
            working = working,
            error = error,
            actionLabel = stringResource(Res.string.onboarding_sign),
            enabled = password.isNotEmpty(),
            onAction = { onSubmit(password) }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PermissionsStep(working: Boolean, onResolved: () -> Unit) {
    var mediaRequested by rememberSaveable { mutableStateOf(false) }
    val mediaPermissions = listOf(
        AppPermission.MEDIA_IMAGES,
        AppPermission.MEDIA_VIDEO,
        AppPermission.NOTIFICATIONS
    )
    val permissionChecker = koinInject<PermissionChecker>()
    val permissionRequester = LocalPermissionRequester.current
    val systemScreens = LocalSystemScreens.current

    fun mediaAccess(): Boolean = mediaPermissions.all { permissionChecker.isGranted(it) }

    var mediaGranted by remember { mutableStateOf(mediaAccess()) }

    var allFilesGranted by remember { mutableStateOf(permissionChecker.hasAllFilesAccess()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mediaGranted = mediaAccess()
                allFilesGranted = permissionChecker.hasAllFilesAccess()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    OnboardingPage(
        icon = Icons.Filled.Photo,
        title = stringResource(Res.string.onboarding_storage_access),
        description = stringResource(Res.string.onboarding_iplus_needs_media_files)
    ) {
        val askForMedia = !mediaGranted && !mediaRequested
        BigButton(
            label = when {
                working -> stringResource(Res.string.onboarding_looking_for_drives)
                askForMedia -> stringResource(Res.string.onboarding_allow_media)
                !allFilesGranted -> stringResource(Res.string.onboarding_allow_all_files)
                else -> stringResource(Res.string.onboarding_continue)
            },
            enabled = !working,
            loading = working,
            onClick = {
                when {
                    askForMedia -> permissionRequester.request(mediaPermissions) { mediaRequested = true }
                    !allFilesGranted -> systemScreens.openAllFilesAccess()
                    else -> onResolved()
                }
            }
        )
        if ((mediaRequested || mediaGranted) && !allFilesGranted) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.onboarding_files_access_covers_documents),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onResolved,
                shapes = ButtonDefaults.shapes(),
                enabled = !working,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(Res.string.onboarding_continue_without)) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BackupSetupStep(
    state: OnboardingUiState,
    onDcim: (Boolean) -> Unit,
    onPictures: (Boolean) -> Unit,
    onMovies: (Boolean) -> Unit,
    onAutoBackup: (Boolean) -> Unit,
    onWifiOnly: (Boolean) -> Unit,
    onFinish: () -> Unit,
    finishing: Boolean
) {
    OnboardingPage(
        icon = Icons.Filled.CloudDone,
        title = stringResource(Res.string.backup),
        description = stringResource(Res.string.onboarding_choose_what_back_change)
    ) {
        SectionLabel(stringResource(Res.string.onboarding_section_folders))
        ToggleCard(stringResource(Res.string.onboarding_toggle_camera), state.backupDcim, onDcim)
        ToggleCard(
            stringResource(Res.string.onboarding_toggle_pictures),
            state.backupPictures,
            onPictures
        )
        ToggleCard(stringResource(Res.string.onboarding_toggle_movies), state.backupMovies, onMovies)
        Spacer(Modifier.height(16.dp))
        SectionLabel(stringResource(Res.string.onboarding_section_behavior))
        ToggleCard(
            stringResource(Res.string.onboarding_toggle_auto_backup),
            state.autoBackupEnabled,
            onAutoBackup
        )
        ToggleCard(stringResource(Res.string.onboarding_toggle_wifi_only), state.wifiOnly, onWifiOnly)
        Spacer(Modifier.height(32.dp))
        BigButton(
            label = if (finishing) "Syncing your drive…" else "Finish setup",
            onClick = onFinish,
            enabled = !finishing,
            loading = finishing
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 4.dp, bottom = 6.dp)
            .semantics { heading() }
    )
}

@Composable
private fun ToggleCard(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (checked) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics {}
            )
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                modifier = Modifier.semantics { contentDescription = label }
            )
        }
    }
}

@Composable
private fun OnboardingField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    enabled: Boolean = true,
    prefix: String? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        label = { Text(label) },
        trailingIcon = trailingIcon,
        prefix = prefix?.let {
            {
                Text(text = it, modifier = Modifier.padding(end = PREFIX_GAP))
            }
        },
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BigButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (loading) {
            LoadingIndicator(
                color = LocalContentColor.current,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.size(10.dp))
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ErrorAndAction(
    working: Boolean,
    error: UiText?,
    actionLabel: String,
    enabled: Boolean,
    onAction: () -> Unit
) {
    Column {
        error?.let {
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = it.resolve(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        BigButton(
            label = actionLabel,
            onClick = onAction,
            enabled = enabled && !working,
            loading = working
        )
    }
}

@Composable
private fun KunjoAccountSetupStep(
    state: OnboardingUiState,
    onUsernameChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val isBn = LocalLocaleList.current.firstOrNull()?.language?.startsWith("bn") == true

    OnboardingPage(
        iconPainter = rememberVectorPainter(Icons.Filled.Person),
        title = if (isBn) "আপনার ইউনিক ইউজারনেম নির্বাচন করুন" else "Choose Your Unique Username",
        description = if (isBn)
            "অন্যান্য ব্যবহারকারীরা আপনাকে এই ইউজারনেম দিয়ে খুঁজে পাবে এবং ফাইল শেয়ার করতে পারবে।"
        else
            "Other users will be able to search and share files with you using this unique handle."
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.kunjoUsernameInput,
                onValueChange = onUsernameChange,
                label = { Text(if (isBn) "ইউনিক ইউজারনেম (@ismail)" else "Unique Username (@username)") },
                placeholder = { Text("username (a-z, 0-9, _)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = !state.isCheckingKunjoUsername,
                leadingIcon = {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFF00E5FF))
                }
            )

            if (state.kunjoAccountError != null) {
                Text(
                    text = "⚠️ ${state.kunjoAccountError}",
                    color = Color(0xFFFF5252),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            BigButton(
                label = if (state.isCheckingKunjoUsername) {
                    if (isBn) "যাচাই করা হচ্ছে..." else "Verifying..."
                } else {
                    if (isBn) "ইউজারনেম সংরক্ষণ করে এগিয়ে যান ➔" else "Save Username & Continue ➔"
                },
                onClick = onSubmit,
                enabled = !state.isCheckingKunjoUsername &&
                        state.kunjoUsernameInput.trim().length >= 3
            )
        }
    }
}

private val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val STEP_SLIDE_MS = 420

private val PAGE_PADDING = 28.dp
private val PAGE_VERTICAL_PADDING = 24.dp

private val PREFIX_GAP = 4.dp
private val SPINNER_SIZE = 20.dp
private val SPINNER_STROKE = 2.dp

// Scanners expect dark modules on light.
private val QR_SURFACE = Color.White
private const val QR_WIDTH_FRACTION = 0.8f
private val QR_PADDING = 16.dp
private val QR_STEP_BADGE = 28.dp
private val QR_BUTTON_ICON = 18.dp
private const val TELEGRAM_PROBE_URI = "tg://login"
