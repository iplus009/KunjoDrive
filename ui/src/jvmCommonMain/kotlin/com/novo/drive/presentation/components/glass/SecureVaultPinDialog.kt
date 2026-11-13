package com.novo.drive.presentation.components.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.novo.drive.core.security.PinVerificationResult
import com.novo.drive.core.security.VaultSecurityManager
import com.novo.drive.presentation.platform.LocalDeviceOwnerGate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

enum class VaultDialogMode {
    LOADING,
    SETUP_ENTER_NEW,
    SETUP_CONFIRM,
    OPTIONAL_SECOND_PIN,
    OPTIONAL_SECOND_CONFIRM,
    NORMAL_UNLOCK,
    LOCKED_OUT,
    CHANGE_OLD_PIN,
    CHANGE_NEW_PIN,
    CHANGE_CONFIRM
}

@Composable
fun SecureVaultPinDialog(
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit,
    onDecoyUnlocked: () -> Unit = onDismiss,
    initialMode: VaultDialogMode? = null,
    vaultSecurity: VaultSecurityManager = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    val deviceOwnerGate = LocalDeviceOwnerGate.current

    var mode by remember { mutableStateOf(VaultDialogMode.LOADING) }
    var lockoutSeconds by remember { mutableIntStateOf(0) }

    var enteredPin by remember { mutableStateOf("") }
    var tempPinBuffer by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val configured = vaultSecurity.isPinConfigured()
        val locked = vaultSecurity.isLockedOut()
        if (locked) {
            mode = VaultDialogMode.LOCKED_OUT
            lockoutSeconds = vaultSecurity.remainingLockoutSeconds()
        } else if (!configured) {
            mode = VaultDialogMode.SETUP_ENTER_NEW
        } else {
            mode = initialMode ?: VaultDialogMode.NORMAL_UNLOCK
        }
    }

    LaunchedEffect(mode, lockoutSeconds) {
        if (mode == VaultDialogMode.LOCKED_OUT && lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds -= 1
            if (lockoutSeconds <= 0) {
                vaultSecurity.resetLockout()
                mode = if (vaultSecurity.isPinConfigured()) {
                    VaultDialogMode.NORMAL_UNLOCK
                } else {
                    VaultDialogMode.SETUP_ENTER_NEW
                }
                errorMessage = null
            }
        }
    }

    val headerTitle = when (mode) {
        VaultDialogMode.LOADING -> "SECURITY VAULT"
        VaultDialogMode.SETUP_ENTER_NEW -> "SET VAULT PIN"
        VaultDialogMode.SETUP_CONFIRM -> "CONFIRM VAULT PIN"
        VaultDialogMode.OPTIONAL_SECOND_PIN -> "2ND PIN (OPTIONAL)"
        VaultDialogMode.OPTIONAL_SECOND_CONFIRM -> "CONFIRM 2ND PIN"
        VaultDialogMode.NORMAL_UNLOCK -> "SECURITY VAULT"
        VaultDialogMode.LOCKED_OUT -> "VAULT LOCKED"
        VaultDialogMode.CHANGE_OLD_PIN -> "CURRENT PIN"
        VaultDialogMode.CHANGE_NEW_PIN -> "NEW 4-DIGIT PIN"
        VaultDialogMode.CHANGE_CONFIRM -> "CONFIRM NEW PIN"
    }

    val promptText = when (mode) {
        VaultDialogMode.LOADING -> "INITIALISING VAULT..."
        VaultDialogMode.SETUP_ENTER_NEW -> "প্রথম ৪-ডিজিট পিন সেট করুন"
        VaultDialogMode.SETUP_CONFIRM -> "একই ৪-ডিজিট পিন পুনরায় দিন"
        VaultDialogMode.OPTIONAL_SECOND_PIN -> "২য় পিন দিতে চাইলে দিন, নতুবা নিচে 'স্কিপ' করুন"
        VaultDialogMode.OPTIONAL_SECOND_CONFIRM -> "২য় পিনটি পুনরায় দিয়ে নিশ্চিত করুন"
        VaultDialogMode.NORMAL_UNLOCK -> "ENTER PIN TO UNLOCK"
        VaultDialogMode.LOCKED_OUT -> "অতিরিক্ত ভুল পিন • ৩০ সেকেন্ড অপেক্ষা করুন"
        VaultDialogMode.CHANGE_OLD_PIN -> "পিন পরিবর্তনের জন্য বর্তমান পিন দিন"
        VaultDialogMode.CHANGE_NEW_PIN -> "নতুন ৪-ডিজিট পিন দিন"
        VaultDialogMode.CHANGE_CONFIRM -> "নতুন পিনটি পুনরায় দিন"
    }

    val panelShape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp)
    val keyShape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(panelShape)
                .background(Color(0xFF06080D))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.55f),
                            Color(0xFF8B5CF6).copy(alpha = 0.30f),
                            Color(0xFFF035C7).copy(alpha = 0.22f)
                        )
                    ),
                    shape = panelShape
                )
                .padding(horizontal = 22.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // HUSSUU Appbar Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = headerTitle,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp,
                            color = Color.White
                        )
                        Text(
                            text = "YOUR PRIVATE SPACE",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.8.sp,
                            color = Color(0xFF61738A)
                        )
                    }
                    Text(
                        text = "✕",
                        fontSize = 16.sp,
                        color = Color(0xFF93A6BC),
                        modifier = Modifier
                            .clickable(role = Role.Button, onClick = onDismiss)
                            .padding(8.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // 06 — HUSSUU Faceted Shield with Glowing Lock
                Canvas(modifier = Modifier.size(width = 116.dp, height = 130.dp)) {
                    val w = size.width
                    val h = size.height
                    val outerShield = Path().apply {
                        moveTo(w * 0.5f, h * 0.04f)
                        lineTo(w * 0.93f, h * 0.18f)
                        lineTo(w * 0.93f, h * 0.50f)
                        cubicTo(w * 0.93f, h * 0.75f, w * 0.70f, h * 0.89f, w * 0.5f, h * 0.97f)
                        cubicTo(w * 0.30f, h * 0.89f, w * 0.07f, h * 0.75f, w * 0.07f, h * 0.50f)
                        lineTo(w * 0.07f, h * 0.18f)
                        close()
                    }
                    val innerShield = Path().apply {
                        moveTo(w * 0.5f, h * 0.12f)
                        lineTo(w * 0.85f, h * 0.23f)
                        lineTo(w * 0.85f, h * 0.49f)
                        cubicTo(w * 0.85f, h * 0.69f, w * 0.66f, h * 0.81f, w * 0.5f, h * 0.89f)
                        cubicTo(w * 0.34f, h * 0.81f, w * 0.15f, h * 0.69f, w * 0.15f, h * 0.49f)
                        lineTo(w * 0.15f, h * 0.23f)
                        close()
                    }
                    drawPath(
                        path = outerShield,
                        color = Color(0xFF00E5FF).copy(alpha = 0.06f)
                    )
                    drawPath(
                        path = outerShield,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF00E5FF), Color(0xFF14F1D9), Color(0xFF8B5CF6))
                        ),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawPath(
                        path = innerShield,
                        color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Lock Shackle
                    drawArc(
                        color = Color(0xFF14F1D9),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(w * 0.41f, h * 0.33f),
                        size = Size(w * 0.18f, h * 0.18f),
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Lock Body
                    drawRoundRect(
                        color = Color(0xFF00E5FF),
                        topLeft = Offset(w * 0.36f, h * 0.43f),
                        size = Size(w * 0.28f, h * 0.20f),
                        cornerRadius = CornerRadius(5.dp.toPx()),
                        style = Stroke(width = 2.2.dp.toPx())
                    )
                    // Keyhole
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = 3.2.dp.toPx(),
                        center = Offset(w * 0.5f, h * 0.51f)
                    )
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(w * 0.5f, h * 0.53f),
                        end = Offset(w * 0.5f, h * 0.58f),
                        strokeWidth = 2.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = promptText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.8.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF93A6BC)
                )

                Spacer(Modifier.height(14.dp))

                // PIN Dots or Lockout Readout
                if (mode == VaultDialogMode.LOCKED_OUT) {
                    Text(
                        text = "LOCKED · ${lockoutSeconds}S REMAINING",
                        color = Color(0xFFFF5C7A),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { index ->
                            val isFilled = index < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) Color(0xFF00E5FF) else Color.Transparent
                                    )
                                    .border(
                                        width = 1.2.dp,
                                        color = if (isFilled) Color(0xFF00E5FF) else Color(0xFF00E5FF).copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                errorMessage?.let { error ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = error,
                        color = Color(0xFFFF5C7A),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }

                successMessage?.let { success ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = success,
                        color = Color(0xFF14F1D9),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(18.dp))

                // HUSSUU 3x4 Keypad
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    keypadRows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { key ->
                                when (key) {
                                    "BIO" -> {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(54.dp)
                                                .clip(keyShape)
                                                .clickable(
                                                    enabled = mode == VaultDialogMode.NORMAL_UNLOCK && !isVerifying,
                                                    role = Role.Button
                                                ) {
                                                    deviceOwnerGate.require(
                                                        title = "Security Vault",
                                                        subtitle = "Authenticate to unlock",
                                                        onDenied = { errorMessage = it }
                                                    ) {
                                                        coroutineScope.launch {
                                                            vaultSecurity.resetLockout()
                                                            onUnlocked()
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (mode == VaultDialogMode.NORMAL_UNLOCK) {
                                                Icon(
                                                    imageVector = Icons.Filled.Fingerprint,
                                                    contentDescription = "Biometric",
                                                    tint = Color(0xFF00E5FF),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                    "DEL" -> {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(54.dp)
                                                .clip(keyShape)
                                                .clickable(
                                                    enabled = mode != VaultDialogMode.LOCKED_OUT && enteredPin.isNotEmpty(),
                                                    role = Role.Button
                                                ) {
                                                    if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                        errorMessage = null
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Backspace",
                                                tint = if (enteredPin.isNotEmpty()) Color(0xFF93A6BC) else Color(0xFF61738A).copy(alpha = 0.4f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(54.dp)
                                                .clip(keyShape)
                                                .background(Color(0xFF10141F))
                                                .border(
                                                    width = 1.dp,
                                                    color = Color(0xFF8CAACA).copy(alpha = 0.14f),
                                                    shape = keyShape
                                                )
                                                .clickable(
                                                    enabled = mode != VaultDialogMode.LOCKED_OUT && !isVerifying,
                                                    role = Role.Button
                                                ) {
                                                    if (enteredPin.length < 4) {
                                                        val newPin = enteredPin + key
                                                        enteredPin = newPin
                                                        errorMessage = null

                                                        if (newPin.length == 4) {
                                                            isVerifying = true
                                                            coroutineScope.launch {
                                                                try {
                                                                    when (mode) {
                                                                        VaultDialogMode.SETUP_ENTER_NEW -> {
                                                                            tempPinBuffer = newPin
                                                                            enteredPin = ""
                                                                            mode = VaultDialogMode.SETUP_CONFIRM
                                                                        }
                                                                        VaultDialogMode.SETUP_CONFIRM -> {
                                                                            if (newPin == tempPinBuffer) {
                                                                                vaultSecurity.setupPin(newPin)
                                                                                enteredPin = ""
                                                                                tempPinBuffer = ""
                                                                                // Stealthily ask for optional 2nd PIN right after 1st PIN setup
                                                                                mode = VaultDialogMode.OPTIONAL_SECOND_PIN
                                                                            } else {
                                                                                errorMessage = "পিন দুটি মেলেনি! আবার চেষ্টা করুন।"
                                                                                enteredPin = ""
                                                                                tempPinBuffer = ""
                                                                                mode = VaultDialogMode.SETUP_ENTER_NEW
                                                                            }
                                                                        }
                                                                        VaultDialogMode.OPTIONAL_SECOND_PIN -> {
                                                                            tempPinBuffer = newPin
                                                                            enteredPin = ""
                                                                            mode = VaultDialogMode.OPTIONAL_SECOND_CONFIRM
                                                                        }
                                                                        VaultDialogMode.OPTIONAL_SECOND_CONFIRM -> {
                                                                            if (newPin == tempPinBuffer) {
                                                                                val ok = vaultSecurity.setupDecoyPin(newPin)
                                                                                if (ok) {
                                                                                    successMessage = "পিন সেটআপ সম্পন্ন হয়েছে!"
                                                                                    delay(450L)
                                                                                    onUnlocked()
                                                                                } else {
                                                                                    errorMessage = "২য় পিনটি ১ম পিনের চেয়ে আলাদা হতে হবে!"
                                                                                    enteredPin = ""
                                                                                    tempPinBuffer = ""
                                                                                    mode = VaultDialogMode.OPTIONAL_SECOND_PIN
                                                                                }
                                                                            } else {
                                                                                errorMessage = "পিন দুটি মেলেনি!"
                                                                                enteredPin = ""
                                                                                tempPinBuffer = ""
                                                                                mode = VaultDialogMode.OPTIONAL_SECOND_PIN
                                                                            }
                                                                        }
                                                                        VaultDialogMode.NORMAL_UNLOCK -> {
                                                                            when (val res = vaultSecurity.verifyPin(newPin)) {
                                                                                is PinVerificationResult.Success -> onUnlocked()
                                                                                is PinVerificationResult.DecoySuccess -> onDecoyUnlocked()
                                                                                is PinVerificationResult.Incorrect -> {
                                                                                    enteredPin = ""
                                                                                    errorMessage = if (res.remainingAttempts > 0) {
                                                                                        "ভুল পিন! অবশিষ্ট চেষ্টা: ${res.remainingAttempts} বার"
                                                                                    } else {
                                                                                        "ভুল পিন! ভল্ট সাময়িক লক করা হয়েছে।"
                                                                                    }
                                                                                }
                                                                                is PinVerificationResult.LockedOut -> {
                                                                                    enteredPin = ""
                                                                                    mode = VaultDialogMode.LOCKED_OUT
                                                                                    lockoutSeconds = res.remainingSeconds
                                                                                }
                                                                            }
                                                                        }
                                                                        VaultDialogMode.CHANGE_OLD_PIN -> {
                                                                            when (vaultSecurity.verifyPin(newPin)) {
                                                                                is PinVerificationResult.Success -> {
                                                                                    enteredPin = ""
                                                                                    mode = VaultDialogMode.CHANGE_NEW_PIN
                                                                                }
                                                                                else -> {
                                                                                    enteredPin = ""
                                                                                    errorMessage = "বর্তমান পিনটি সঠিক নয়!"
                                                                                }
                                                                            }
                                                                        }
                                                                        VaultDialogMode.CHANGE_NEW_PIN -> {
                                                                            tempPinBuffer = newPin
                                                                            enteredPin = ""
                                                                            mode = VaultDialogMode.CHANGE_CONFIRM
                                                                        }
                                                                        VaultDialogMode.CHANGE_CONFIRM -> {
                                                                            if (newPin == tempPinBuffer) {
                                                                                vaultSecurity.setupPin(newPin)
                                                                                enteredPin = ""
                                                                                tempPinBuffer = ""
                                                                                mode = VaultDialogMode.OPTIONAL_SECOND_PIN
                                                                            } else {
                                                                                errorMessage = "নতুন পিন দুটি মেলেনি!"
                                                                                enteredPin = ""
                                                                                tempPinBuffer = ""
                                                                                mode = VaultDialogMode.CHANGE_NEW_PIN
                                                                            }
                                                                        }
                                                                        else -> Unit
                                                                    }
                                                                } finally {
                                                                    isVerifying = false
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = key,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE9F6FF)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Bottom action: Skip (when in optional 2nd PIN step) or Change PIN (when unlocking)
                if (mode == VaultDialogMode.OPTIONAL_SECOND_PIN || mode == VaultDialogMode.OPTIONAL_SECOND_CONFIRM) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(keyShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.45f), keyShape)
                            .clickable(role = Role.Button) {
                                onUnlocked()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "স্কিপ করুন (SKIP — ২য় পিন লাগবে না) →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                } else if (mode == VaultDialogMode.NORMAL_UNLOCK) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "পিন ভুলে গেছেন?",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF93A6BC),
                            modifier = Modifier
                                .clickable(role = Role.Button) {
                                    deviceOwnerGate.require(
                                        title = "Security Vault",
                                        subtitle = "বায়োমেট্রিক বা স্ক্রিন লক দিয়ে ভল্ট রিসেট করুন",
                                        onDenied = { errorMessage = it }
                                    ) {
                                        coroutineScope.launch {
                                            vaultSecurity.resetLockout()
                                            enteredPin = ""
                                            tempPinBuffer = ""
                                            mode = VaultDialogMode.SETUP_ENTER_NEW
                                        }
                                    }
                                }
                                .padding(4.dp)
                        )
                        Text(
                            text = "CHANGE PIN",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.6.sp,
                            color = Color(0xFF00E5FF),
                            modifier = Modifier
                                .clickable(role = Role.Button) {
                                    enteredPin = ""
                                    errorMessage = null
                                    mode = VaultDialogMode.CHANGE_OLD_PIN
                                }
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "AES-256 ENCRYPTED VAULT · LOCAL + TELEGRAM KEY",
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF61738A)
                )
            }
        }
    }
}
