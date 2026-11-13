package com.novo.drive.presentation.family

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.novo.drive.core.common.AppResult
import com.novo.drive.core.family.FamilyAccessManager
import com.novo.drive.core.family.GeneratedFamilyInvite
import com.novo.drive.core.security.NativeSecurityBridge
import com.novo.drive.domain.model.AppTheme
import com.novo.drive.domain.model.FamilyMemberSlot
import com.novo.drive.domain.model.UserPreferences
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.presentation.common.CollectSnackbarMessages
import com.novo.drive.presentation.common.Formatters
import com.novo.drive.presentation.common.UiText
import com.novo.drive.presentation.common.toUiText
import com.novo.drive.presentation.components.BottomBarSnackbarHost
import com.novo.drive.presentation.components.ConfirmDialog
import com.novo.drive.presentation.components.QrCode
import com.novo.drive.presentation.components.glass.GlassPillButton
import com.novo.drive.presentation.components.glass.LiquidGlassCard
import com.novo.drive.presentation.components.liftedTopAppBarColors
import com.novo.drive.presentation.components.rememberToolbarLift
import com.novo.drive.presentation.theme.LocalIplusPalette
import com.novo.drive.resources.Res
import com.novo.drive.resources.common_back
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

data class FamilyDashboardUiState(
    val preferences: UserPreferences = UserPreferences(),
    val slots: List<FamilyMemberSlot> = emptyList(),
    val working: Boolean = false,
    val generatedInvite: GeneratedFamilyInvite? = null
)

class FamilyDashboardViewModel(
    private val familyAccessManager: FamilyAccessManager,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val working = MutableStateFlow(false)
    private val generatedInvite = MutableStateFlow<GeneratedFamilyInvite?>(null)
    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    val uiState: StateFlow<FamilyDashboardUiState> = combine(
        settingsRepository.preferences,
        familyAccessManager.observeSlots(),
        working,
        generatedInvite
    ) { prefs, slots, isWorking, invite ->
        FamilyDashboardUiState(
            preferences = prefs,
            slots = slots,
            working = isWorking,
            generatedInvite = invite
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FamilyDashboardUiState())

    fun createInvite(slotIndex: Int, memberName: String, quotaGb: Int, expiryDays: Int) {
        if (working.value) return
        working.value = true
        viewModelScope.launch {
            when (val res = familyAccessManager.createSlotInvite(slotIndex, memberName, quotaGb, expiryDays)) {
                is AppResult.Success -> {
                    generatedInvite.value = res.value
                    _messages.tryEmit(UiText.Plain("Slot #$slotIndex এর ফ্যামিলি ইনভাইট ও পিন তৈরি হয়েছে!"))
                }
                is AppResult.Failure -> {
                    _messages.tryEmit(res.error.toUiText())
                }
            }
            working.value = false
        }
    }

    fun dismissGeneratedInvite() {
        generatedInvite.value = null
    }

    fun approveDeviceSession(tgLoginLink: String, onDone: () -> Unit) {
        if (working.value) return
        working.value = true
        viewModelScope.launch {
            when (val res = familyAccessManager.approveMemberDeviceQr(tgLoginLink)) {
                is AppResult.Success -> {
                    _messages.tryEmit(UiText.Plain("ফ্যামিলি মেম্বারের ডিভাইস সফলভাবে অনুমোদিত ও লগইন হয়েছে!"))
                    onDone()
                }
                is AppResult.Failure -> {
                    _messages.tryEmit(res.error.toUiText())
                }
            }
            working.value = false
        }
    }

    fun revokeSlot(slotIndex: Int, deleteRemoteChannel: Boolean) {
        if (working.value) return
        working.value = true
        viewModelScope.launch {
            when (val res = familyAccessManager.revokeSlot(slotIndex, deleteRemoteChannel)) {
                is AppResult.Success -> {
                    _messages.tryEmit(UiText.Plain("Slot #$slotIndex এর অ্যাক্সেস বাতিল করা হয়েছে।"))
                }
                is AppResult.Failure -> {
                    _messages.tryEmit(res.error.toUiText())
                }
            }
            working.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun FamilyDashboardScreen(
    onBack: () -> Unit,
    viewModel: FamilyDashboardViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalIplusPalette.current
    val isLight = palette.theme == AppTheme.LIGHT
    val primaryText = if (isLight) Color(0xFF0F172A) else Color.White
    val secondaryText = if (isLight) Color(0xFF475569) else Color.White.copy(alpha = 0.70f)

    val snackbarHostState = remember { SnackbarHostState() }
    CollectSnackbarMessages(viewModel.messages, snackbarHostState)

    var configureSlotIndex by remember { mutableStateOf<Int?>(null) }
    var showApproveDialog by remember { mutableStateOf(false) }
    var revokeTargetSlot by remember { mutableStateOf<FamilyMemberSlot?>(null) }

    val scrollState = rememberScrollState()
    val lifted by rememberToolbarLift(scrollState)

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { BottomBarSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = liftedTopAppBarColors(lifted),
                title = {
                    Text(
                        text = "Family Cloud (ফ্যামিলি ক্লাউড)",
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.common_back),
                            tint = primaryText
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.preferences.isFamilyMember) {
                FamilyMemberPassView(
                    prefs = state.preferences,
                    primaryText = primaryText,
                    secondaryText = secondaryText
                )
            } else {
                val now = System.currentTimeMillis()
                val slotMap = state.slots.associateBy { it.slotIndex }
                val activeCount = state.slots.count { !it.isRevoked && (it.expiresAtMs <= 0L || now < it.expiresAtMs) }
                val maxMembers = NativeSecurityBridge.maxFamilyMembers()

                // Owner Hero Shield Card
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    glowColor = palette.accentPrimary
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(palette.accentPrimary.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FamilyRestroom,
                                    contentDescription = null,
                                    tint = palette.accentPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Kunjo Family Cloud • $activeCount / $maxMembers সক্রিয়",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText
                                )
                                Text(
                                    text = "সর্বোচ্চ ৩ জন সদস্য • ২ জিবি/ফাইল সীমা • ফ্যামিলি ফোনে টেলিগ্রাম অ্যাপ নিষ্প্রয়োজন",
                                    fontSize = 12.sp,
                                    color = secondaryText
                                )
                            }
                        }

                        Button(
                            onClick = { showApproveDialog = true },
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("ধাপ ২: ফ্যামিলি ডিভাইসের সেশন অনুমোদন করুন (Approve Login)")
                        }
                    }
                }

                // 3 Hard-Capped Family Slots
                (1..maxMembers).forEach { slotIndex ->
                    val slot = slotMap[slotIndex]
                    FamilySlotCard(
                        slotIndex = slotIndex,
                        slot = slot,
                        nowMs = now,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        working = state.working,
                        onInvite = { configureSlotIndex = slotIndex },
                        onRevoke = { if (slot != null) revokeTargetSlot = slot }
                    )
                }

                // Worldwide Keep-Alive & Security Notice
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = palette.accentSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "বিশ্বব্যাপী অটো কিপ-অ্যালাইভ ও আইসোলেটেড চ্যানেল",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = primaryText
                            )
                            Text(
                                text = "প্রতিটি স্লট আলাদা লকড চ্যানেল ও AES-256 সাব-কী ব্যবহার করে। ৭ দিন পর পর অটো হার্টবিট সেশন সচল রাখে।",
                                fontSize = 11.sp,
                                color = secondaryText
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp + padding.calculateBottomPadding()))
        }
    }

    configureSlotIndex?.let { slotIdx ->
        val existing = state.slots.firstOrNull { it.slotIndex == slotIdx }
        CreateFamilyInviteDialog(
            slotIndex = slotIdx,
            initialName = existing?.memberName.orEmpty(),
            working = state.working,
            onDismiss = { configureSlotIndex = null },
            onConfirm = { name, quotaGb, expiryDays ->
                configureSlotIndex = null
                viewModel.createInvite(slotIdx, name, quotaGb, expiryDays)
            }
        )
    }

    state.generatedInvite?.let { invite ->
        GeneratedInviteDialog(
            invite = invite,
            onDismiss = viewModel::dismissGeneratedInvite,
            onOpenApprove = {
                viewModel.dismissGeneratedInvite()
                showApproveDialog = true
            }
        )
    }

    if (showApproveDialog) {
        ApproveFamilySessionDialog(
            working = state.working,
            onDismiss = { showApproveDialog = false },
            onApprove = { link ->
                viewModel.approveDeviceSession(link) {
                    showApproveDialog = false
                }
            }
        )
    }

    revokeTargetSlot?.let { slot ->
        ConfirmDialog(
            title = "Slot #${slot.slotIndex} (${slot.memberName}) বাতিল করবেন?",
            message = "এই ফ্যামিলি মেম্বারের ইনভাইট ও কোটা অ্যাক্সেস সাথে সাথে নিষ্ক্রিয় হয়ে যাবে। ফাইলগুলো ক্লাউড চ্যানেলে অক্ষত থাকবে।",
            confirmLabel = "অ্যাক্সেস বাতিল করুন",
            destructive = true,
            onConfirm = {
                revokeTargetSlot = null
                viewModel.revokeSlot(slot.slotIndex, deleteRemoteChannel = false)
            },
            onDismiss = { revokeTargetSlot = null }
        )
    }
}

@Composable
private fun FamilyMemberPassView(
    prefs: UserPreferences,
    primaryText: Color,
    secondaryText: Color
) {
    val palette = LocalIplusPalette.current
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        glowColor = palette.accentSecondary
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = palette.accentSecondary,
                    modifier = Modifier.size(34.dp)
                )
                Column {
                    Text(
                        text = prefs.familyMemberName.ifBlank { "Family Member" },
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    Text(
                        text = "Family Slot #${prefs.familySlotIndex} • লকড ফ্যামিলি ভল্ট",
                        fontSize = 12.sp,
                        color = palette.accentSecondary
                    )
                }
            }
            Text(
                text = "বরাদ্দকৃত ক্লাউড কোটা: ${Formatters.bytes(prefs.familyQuotaBytes)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = primaryText
            )
            Text(
                text = "সর্বোচ্চ ফাইল আপলোড সীমা: ২ জিবি / ফাইল (Native Shield Enforced)",
                fontSize = 13.sp,
                color = secondaryText
            )
            Text(
                text = if (prefs.familyExpiresAtMs > 0L) {
                    "মেয়াদ: ${Formatters.dateTime(prefs.familyExpiresAtMs)} পর্যন্ত"
                } else {
                    "মেয়াদ: আজীবন (Lifetime Family Access • Auto Keep-Alive সক্রিয়)"
                },
                fontSize = 13.sp,
                color = secondaryText
            )
        }
    }
}

@Composable
private fun FamilySlotCard(
    slotIndex: Int,
    slot: FamilyMemberSlot?,
    nowMs: Long,
    primaryText: Color,
    secondaryText: Color,
    working: Boolean,
    onInvite: () -> Unit,
    onRevoke: () -> Unit
) {
    val palette = LocalIplusPalette.current
    val active = slot != null && !slot.isRevoked && (slot.expiresAtMs <= 0L || nowMs < slot.expiresAtMs)
    val expired = slot != null && !slot.isRevoked && slot.expiresAtMs > 0L && nowMs >= slot.expiresAtMs

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        glowColor = if (active) palette.accentSecondary else palette.accentPrimary
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) palette.accentSecondary.copy(alpha = 0.20f)
                                else palette.accentPrimary.copy(alpha = 0.14f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#$slotIndex",
                            fontWeight = FontWeight.Bold,
                            color = if (active) palette.accentSecondary else palette.accentPrimary
                        )
                    }
                    Column {
                        Text(
                            text = slot?.memberName?.ifBlank { "Family Slot #$slotIndex" }
                                ?: "খালি স্লট (Slot #$slotIndex)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                        Text(
                            text = when {
                                active -> "সক্রিয় • কোটা ${Formatters.bytes((slot?.quotaBytes ?: 0L))} • ২ জিবি/ফাইল"
                                expired -> "মেয়াদোত্তীর্ণ (Expired)"
                                slot?.isRevoked == true -> "বাতিলকৃত (Revoked)"
                                else -> "নতুন ফ্যামিলি মেম্বার যুক্ত করার জন্য প্রস্তুত"
                            },
                            fontSize = 12.sp,
                            color = secondaryText
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                active -> Color(0xFF00E676).copy(alpha = 0.18f)
                                slot?.isRevoked == true || expired -> Color(0xFFFF5252).copy(alpha = 0.18f)
                                else -> palette.accentPrimary.copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            active -> "ACTIVE"
                            expired -> "EXPIRED"
                            slot?.isRevoked == true -> "REVOKED"
                            else -> "EMPTY"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            active -> Color(0xFF00E676)
                            slot?.isRevoked == true || expired -> Color(0xFFFF5252)
                            else -> palette.accentPrimary
                        }
                    )
                }
            }

            if (slot != null && active) {
                val progress = if (slot.quotaBytes > 0L) {
                    (slot.usedBytes.toFloat() / slot.quotaBytes.toFloat()).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = palette.accentSecondary
                )
                Text(
                    text = if (slot.expiresAtMs > 0L) {
                        "মেয়াদ: ${Formatters.date(slot.expiresAtMs)} পর্যন্ত"
                    } else {
                        "মেয়াদ: আজীবন (Lifetime)"
                    },
                    fontSize = 11.sp,
                    color = secondaryText
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassPillButton(
                    text = if (active) "নতুন ইনভাইট ও পিন" else "ইনভাইট তৈরি করুন",
                    icon = if (active) Icons.Filled.Refresh else Icons.Filled.Add,
                    isActive = !active,
                    onClick = { if (!working) onInvite() },
                    modifier = Modifier.weight(1f)
                )
                if (active) {
                    GlassPillButton(
                        text = "Revoke",
                        icon = Icons.Filled.Block,
                        accentColor = Color(0xFFFF5252),
                        onClick = { if (!working) onRevoke() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CreateFamilyInviteDialog(
    slotIndex: Int,
    initialName: String,
    working: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, quotaGb: Int, expiryDays: Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName.ifBlank { "Family Member #$slotIndex" }) }
    var quotaGb by remember { mutableIntStateOf(50) }
    var expiryDays by remember { mutableIntStateOf(0) }

    val quotaChoices = listOf(10, 25, 50, 100, 200)
    val expiryChoices = listOf(
        0 to "Lifetime",
        7 to "7 Days",
        30 to "30 Days",
        90 to "90 Days"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Slot #$slotIndex • ফ্যামিলি ইনভাইট সেটআপ") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("সদস্যের নাম (যেমন: Ammu / Spouse)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("ক্লাউড স্টোরেজ কোটা নির্বাচন করুন:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quotaChoices.forEach { gb ->
                        FilterChip(
                            selected = quotaGb == gb,
                            onClick = { quotaGb = gb },
                            label = { Text("$gb GB") }
                        )
                    }
                }
                Text("অ্যাক্সেসের মেয়াদ:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    expiryChoices.forEach { (days, label) ->
                        FilterChip(
                            selected = expiryDays == days,
                            onClick = { expiryDays = days },
                            label = { Text(label) }
                        )
                    }
                }
                Text(
                    text = "নোট: প্রতিটি ফাইলের সর্বোচ্চ আকার ২ জিবি (Native C++ Shield দ্বারা লক করা)।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), quotaGb, expiryDays) },
                enabled = !working && name.isNotBlank(),
                shapes = ButtonDefaults.shapes()
            ) {
                Text("QR ও ৬-ডিজিট PIN তৈরি করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text("বাতিল")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GeneratedInviteDialog(
    invite: GeneratedFamilyInvite,
    onDismiss: () -> Unit,
    onOpenApprove: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copiedToken by remember { mutableStateOf(false) }
    var copiedBundle by remember { mutableStateOf(false) }
    val qrPayload = "${invite.inviteToken}|PIN:${invite.pairingPin}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ধাপ ১: ${invite.memberName} (Slot #${invite.slotIndex})") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.size(205.dp)
                ) {
                    QrCode(
                        content = qrPayload,
                        contentDescription = "Kunjo Family Invite QR",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                }
                Text(
                    text = "৬-ডিজিট পিন: ${invite.pairingPin}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "মেম্বারের ফোনে 'Login with Family'-তে গিয়ে উপরের QR কোডটি স্ক্যান করুন অথবা নিচের বাটন দিয়ে কপি করে পাঠান:",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                FilledTonalButton(
                    onClick = {
                        val bundle = "Kunjo Family Invite (${invite.memberName} • Slot #${invite.slotIndex})\n" +
                            "Token: ${invite.inviteToken}\n" +
                            "PIN: ${invite.pairingPin}"
                        clipboard.setText(AnnotatedString(bundle))
                        copiedBundle = true
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (copiedBundle) "✅ টোকেন ও পিন কপি হয়েছে!" else "১-ক্লিকে টোকেন ও পিন একসাথে কপি করুন")
                }
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(invite.inviteToken))
                        copiedToken = true
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (copiedToken) "✅ শুধু টোকেন কপি হয়েছে!" else "শুধু টোকেন কপি করুন")
                }
            }
        },
        confirmButton = {
            Button(onClick = onOpenApprove, shapes = ButtonDefaults.shapes()) {
                Text("পরবর্তী ধাপ (ধাপ ২: সেশন অনুমোদন) ➔")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text("বন্ধ করুন")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ApproveFamilySessionDialog(
    working: Boolean,
    onDismiss: () -> Unit,
    onApprove: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val filePicker = com.novo.drive.presentation.platform.LocalFilePicker.current
    var link by remember { mutableStateOf("") }
    var qrError by remember { mutableStateOf<String?>(null) }
    var showLiveQrScanner by remember { mutableStateOf(false) }

    fun extractSessionLink(raw: String): String {
        val text = raw.trim()
        return Regex("""tg://login\?token=[A-Za-z0-9_-]+""").find(text)?.value ?: text
    }

    if (showLiveQrScanner) {
        com.novo.drive.presentation.components.LiveQrScanner(
            onResult = { decoded ->
                showLiveQrScanner = false
                if (decoded.contains("tg://login?token=")) {
                    link = extractSessionLink(decoded)
                    qrError = null
                } else {
                    qrError = "সেশন QR কোড পড়া যায়নি। আবার চেষ্টা করুন।"
                }
            },
            onCancel = { showLiveQrScanner = false }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ধাপ ২: ফ্যামিলি সেশন অনুমোদন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ফ্যামিলি মেম্বার ধাপ ১ সম্পন্ন করার পর তাঁর স্ক্রিনে যে QR কোড বা সেশন লিংক ('tg://login?token=...') এসেছে, সেটি স্ক্যান বা পেস্ট করুন:",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val clip = clipboard.getText()?.text.orEmpty()
                            link = extractSessionLink(clip)
                        },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("পেস্ট করুন")
                    }
                    FilledTonalButton(
                        onClick = {
                            qrError = null
                            filePicker.pick { result ->
                                val path = (result as? com.novo.drive.presentation.platform.PickResult.Picked)?.path ?: return@pick
                                val decoded = com.novo.drive.presentation.components.QrScannerBridge.decodeFromImageFile(path)
                                if (decoded != null && decoded.contains("tg://login?token=")) {
                                    link = extractSessionLink(decoded)
                                    qrError = null
                                } else {
                                    qrError = "সেশন QR কোড পড়া যায়নি। স্পষ্ট ছবি দিন।"
                                }
                            }
                        },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🖼️ ছবি থেকে")
                    }
                    FilledTonalButton(
                        onClick = { showLiveQrScanner = true },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📷 ক্যামেরা স্ক্যান")
                    }
                }
                qrError?.let { msg ->
                    Text(text = msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = link,
                    onValueChange = { input -> link = extractSessionLink(input) },
                    supportingText = {
                        val isInviteToken = link.contains("KUNJO-FAM") || link.contains("Family Invite") || link.contains("PIN:")
                        if (isInviteToken) {
                            Text(
                                text = "⚠️ এটি সদস্যের ইনভাইট টোকেন। সদস্যের স্ক্রিনে আসা 'tg://login?token=...' লিংকটি এখানে দিন।",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    label = { Text("সেশন লিংক (কীবোর্ড দিয়ে পেস্ট করুন)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApprove(extractSessionLink(link)) },
                enabled = !working && extractSessionLink(link).startsWith("tg://login?token="),
                shapes = ButtonDefaults.shapes()
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("অনুমোদন ও লগইন করান")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text("বাতিল")
            }
        }
    )
}
