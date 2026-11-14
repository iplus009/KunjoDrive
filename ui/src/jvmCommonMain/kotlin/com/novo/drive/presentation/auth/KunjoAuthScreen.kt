package com.novo.drive.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novo.drive.presentation.components.glass.liquidGlass
import com.novo.drive.presentation.components.glass.liquidGlassBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KunjoAuthScreen(
    viewModel: KunjoAuthViewModel,
    onBack: () -> Unit,
    onLoginSuccess: (username: String, accountType: String) -> Unit,
    onFallbackPhoneLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val isBn = com.novo.drive.presentation.common.isBengaliUi()
    val accentCyan = Color(0xFF00E5FF)
    val luxuryPurple = Color(0xFF8B5CF6)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isRegisterMode) {
                            if (isBn) "নতুন অ্যাকাউন্ট তৈরি" else "Create Account"
                        } else {
                            if (isBn) "কুঞ্জ ক্লাউড লগইন" else "Kunjo Cloud Login"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = if (isDark) Color(0xFF0B0E14) else Color(0xFFF4F6F9)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Luxury Emblem
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(accentCyan.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
                    .border(1.dp, accentCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = accentCyan,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "KUNJO CLOUD DRIVE",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isDark) Color.White else Color(0xFF1E293B)
            )

            Text(
                text = "Zero-Knowledge Military-Grade Synchronization",
                fontSize = 12.sp,
                color = if (isDark) Color.Gray else Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Glass Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(24.dp),
                        elevation = 4.dp,
                        borderWidth = 1.dp
                    )
                    .liquidGlassBackground(
                        shape = RoundedCornerShape(24.dp),
                        tintAlpha = if (isDark) 0.75f else 0.85f
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mode Switcher (Login vs Register)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF161C26) else Color(0xFFE2E8F0))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!uiState.isRegisterMode) accentCyan.copy(alpha = 0.22f) else Color.Transparent)
                                .clickable { if (uiState.isRegisterMode) viewModel.toggleMode() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isBn) "লগইন" else "Log In",
                                fontWeight = if (!uiState.isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (!uiState.isRegisterMode) accentCyan else if (isDark) Color.White else Color.Black,
                                fontSize = 14.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (uiState.isRegisterMode) luxuryPurple.copy(alpha = 0.22f) else Color.Transparent)
                                .clickable { if (!uiState.isRegisterMode) viewModel.toggleMode() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isBn) "নতুন একাউন্ট" else "Create Account",
                                fontWeight = if (uiState.isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.isRegisterMode) luxuryPurple else if (isDark) Color.White else Color.Black,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Username Input
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = viewModel::onUsernameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (isBn) "ব্যবহারকারীর নাম (Username)" else "Username") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = accentCyan)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentCyan,
                            unfocusedBorderColor = if (isDark) Color(0xFF2E384D) else Color(0xFFCBD5E1)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password Input
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (isBn) "মাস্টার পাসওয়ার্ড (Master Password)" else "Master Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = accentCyan)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (uiState.isRegisterMode) {
                                viewModel.register(null, null) { u, t -> onLoginSuccess(u, t) }
                            } else {
                                viewModel.login { u, t -> onLoginSuccess(u, t) }
                            }
                        }),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentCyan,
                            unfocusedBorderColor = if (isDark) Color(0xFF2E384D) else Color(0xFFCBD5E1)
                        )
                    )

                    // Register-specific controls (Sub-account vs Main)
                    AnimatedVisibility(visible = uiState.isRegisterMode) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                            Text(
                                text = if (isBn) "অ্যাকাউন্টের ধরন নির্বাচন করুন:" else "Select Account Type:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color.LightGray else Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (!uiState.isSubAccount) accentCyan.copy(alpha = 0.18f) else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (!uiState.isSubAccount) accentCyan else Color.Gray.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.toggleSubAccount(false) }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Cloud, contentDescription = null, tint = accentCyan)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(if (isBn) "👑 মেইন ড্রাইভ" else "👑 Main Drive", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (uiState.isSubAccount) luxuryPurple.copy(alpha = 0.18f) else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (uiState.isSubAccount) luxuryPurple else Color.Gray.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.toggleSubAccount(true) }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = luxuryPurple)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(if (isBn) "👨‍👩‍👧 সাব-অ্যাকাউন্ট" else "👨‍👩‍👧 Sub-Account", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (uiState.isSubAccount) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = uiState.parentUsername,
                                    onValueChange = viewModel::onParentUsernameChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(if (isBn) "ফ্যামিলি হেডের ইউজারনেম (@ismail)" else "Family Head Username (@ismail)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Text(
                                    text = if (isBn) {
                                        "💡 সাব-অ্যাকাউন্টে সর্বোচ্চ ৫০০ এমবি স্ট্রিমিং ও ডিলিট লক সক্রিয় থাকবে।"
                                    } else {
                                        "💡 Sub-accounts have a 500 MB streaming limit and delete lock enabled."
                                    },
                                    fontSize = 11.sp,
                                    color = luxuryPurple,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Error Message display
                    AnimatedVisibility(visible = uiState.errorMessage != null) {
                        uiState.errorMessage?.let { msg ->
                            Text(
                                text = "⚠️ $msg",
                                color = Color(0xFFFF5252),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }

                    // Success Message display
                    AnimatedVisibility(visible = uiState.successMessage != null) {
                        uiState.successMessage?.let { msg ->
                            Text(
                                text = "✅ $msg",
                                color = Color(0xFF69F0AE),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Action Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (uiState.isRegisterMode) {
                                viewModel.register(null, null) { u, t -> onLoginSuccess(u, t) }
                            } else {
                                viewModel.login { u, t -> onLoginSuccess(u, t) }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isRegisterMode) luxuryPurple else accentCyan,
                            contentColor = if (uiState.isRegisterMode) Color.White else Color.Black
                        ),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (uiState.isRegisterMode) {
                                    if (isBn) "অ্যাকাউন্ট তৈরি সম্পন্ন করুন" else "Complete Registration"
                                } else {
                                    if (isBn) "ড্রাইভ আনলক করুন" else "Unlock Drive"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Forgot Password Toggle
                    if (!uiState.isRegisterMode) {
                        TextButton(
                            onClick = { viewModel.openForgotPassword(true) },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = if (isBn) "পাসওয়ার্ড ভুলে গেছেন? (টেলিগ্রাম দিয়ে রিসেট)" else "Forgot password? (Reset via Telegram)",
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Fallback Button: Phone & Telegram OTP
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF131822) else Color(0xFFE2E8F0))
                    .clickable { onFallbackPhoneLogin() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = if (isDark) Color.LightGray else Color(0xFF334155),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBn) "📲 সরাসরি ফোন নাম্বার ও টেলিগ্রাম দিয়ে লগইন" else "📲 Login directly with Phone & Telegram",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.LightGray else Color(0xFF334155)
                )
            }
        }
    }

    if (uiState.showForgotDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.openForgotPassword(false) },
            title = {
                Text(
                    text = if (!uiState.forgotOtpSent) {
                        if (isBn) "পাসওয়ার্ড রিসেট (টেলিগ্রাম ওটিপি)" else "Reset Password (Telegram OTP)"
                    } else {
                        if (isBn) "টেলিগ্রাম ওটিপি যাচাই ও পাসওয়ার্ড" else "Verify OTP & New Password"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!uiState.forgotOtpSent) {
                        Text(
                            text = if (isBn) {
                                "আপনার রেজিস্টার্ড কুঞ্জ ইউজারনেম এবং টেলিগ্রামের ফোন নম্বর দিন:"
                            } else {
                                "Enter your registered Kunjo username and Telegram phone number:"
                            },
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = uiState.forgotUsername,
                            onValueChange = viewModel::onForgotUsernameChange,
                            label = { Text(if (isBn) "ইউনিক ইউজারনেম (@username)" else "Unique Username (@username)") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = accentCyan) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.forgotPhoneNumber,
                            onValueChange = viewModel::onForgotPhoneNumberChange,
                            label = { Text(if (isBn) "টেলিগ্রাম ফোন নম্বর (+8801...)" else "Telegram Phone Number (+8801...)") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = accentCyan) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = if (isBn) {
                                "আপনার টেলিগ্রাম অ্যাপে একটি ৫-ডিজিটের কোড পাঠানো হয়েছে। কোড এবং নতুন পাসওয়ার্ড প্রদান করুন:"
                            } else {
                                "A 5-digit code was sent to your Telegram app. Enter the code and your new password:"
                            },
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = uiState.forgotOtpCode,
                            onValueChange = viewModel::onForgotOtpCodeChange,
                            label = { Text(if (isBn) "৫ ডিজিটের ওটিপি কোড (Telegram Code)" else "5-Digit Telegram OTP Code") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = accentCyan) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.newPasswordInput,
                            onValueChange = viewModel::onNewPasswordChange,
                            label = { Text(if (isBn) "নতুন মাস্টার পাসওয়ার্ড (সর্বনিম্ন ৬ অক্ষর)" else "New Master Password (min 6 chars)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = accentCyan) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            onClick = { viewModel.resendForgotOtp() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(if (isBn) "কোড পাননি? পুনরায় পাঠান" else "Didn't receive code? Resend", fontSize = 12.sp, color = accentCyan)
                        }
                    }

                    if (uiState.forgotDialogError != null) {
                        Text(
                            text = "⚠️ ${uiState.forgotDialogError}",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                if (!uiState.forgotOtpSent) {
                    Button(
                        onClick = { viewModel.sendForgotOtp() },
                        enabled = !uiState.isSendingForgotOtp,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentCyan, contentColor = Color.Black)
                    ) {
                        if (uiState.isSendingForgotOtp) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.Black)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isBn) "ওটিপি পাঠান" else "Send OTP")
                    }
                } else {
                    Button(
                        onClick = { viewModel.verifyOtpAndResetPassword {} },
                        enabled = !uiState.isResettingPassword,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentCyan, contentColor = Color.Black)
                    ) {
                        if (uiState.isResettingPassword) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.Black)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isBn) "যাচাই ও রিসেট করুন" else "Verify & Reset")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.openForgotPassword(false) }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
