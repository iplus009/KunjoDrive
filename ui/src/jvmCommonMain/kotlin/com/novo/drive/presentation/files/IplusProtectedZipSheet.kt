package com.novo.drive.presentation.files

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novo.drive.presentation.components.custom.KunjoCapsuleSwitch
import com.novo.drive.presentation.components.custom.KunjoSteppedSlider
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import com.novo.drive.presentation.theme.LocalIplusPalette

/**
 * iPlus Protected AES-256 ZIP Creator & Extractor Sheet.
 * In-app password-locked encrypted zip creator with custom Liquid Glass controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IplusProtectedZipSheet(
    targetCount: Int,
    onDismiss: () -> Unit,
    onCreateZip: (name: String, pass: String, encryptAes256: Boolean, level: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val palette = LocalIplusPalette.current

    var zipName by remember { mutableStateOf("iPlus_Encrypted_${System.currentTimeMillis() % 10000}.zip") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var enableAes256 by remember { mutableStateOf(true) }
    var compressionLevel by remember { mutableFloatStateOf(1f) } // 0=Store, 1=Normal, 2=Max
    var isCompressing by remember { mutableStateOf(false) }

    val canProceed = !isCompressing && zipName.trim().isNotBlank() && (!enableAes256 || password.isNotBlank())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F131A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(palette.accentPrimary.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderZip,
                        contentDescription = null,
                        tint = palette.accentPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "সুরক্ষিত জিপ তৈরি করুন · Protected ZIP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$targetCount টি ফাইল সুরক্ষিত এনক্রিপ্টেড আর্কাইভে প্যাক হবে",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            // Archive File Name
            OutlinedTextField(
                value = zipName,
                onValueChange = { zipName = it },
                label = { Text("আর্কাইভ ফাইলের নাম", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Password Field
            AnimatedVisibility(visible = enableAes256) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("সুরক্ষা পাসওয়ার্ড (AES-256 Password)", fontSize = 12.sp) },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle password",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // AES-256 Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AES-256 মিলিটারি গ্রেড এনক্রিপশন",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "পাসওয়ার্ড ছাড়া এই জিপটি কেউ খুলতে বা প্রিভিউ করতে পারবে না",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
                Spacer(Modifier.width(12.dp))
                KunjoCapsuleSwitch(
                    checked = enableAes256,
                    onCheckedChange = { enableAes256 = it }
                )
            }

            // Compression Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "কম্প্রেশন লেভেল:",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = when (compressionLevel.toInt()) {
                            0 -> "ফাস্ট স্টোর (0% No-Compress)"
                            1 -> "স্ট্যান্ডার্ড নরমাল (ব্যালেন্সড)"
                            else -> "ম্যাক্সিমাম আল্ট্রা (সর্বোচ্চ ছোট সাইজ)"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentPrimary
                    )
                }
                KunjoSteppedSlider(
                    value = compressionLevel,
                    onValueChange = { compressionLevel = it },
                    steps = 2,
                    valueRange = 0f..2f
                )
            }

            if (isCompressing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = palette.accentPrimary
                )
            }

            Spacer(Modifier.height(4.dp))

            // Action Button
            val buttonShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(buttonShape)
                    .liquidGlassBackground(
                        shape = buttonShape,
                        tintAlpha = 0.85f,
                        borderAlpha = 0.60f,
                        borderWidth = 1.dp
                    )
                    .clickable(enabled = canProceed) {
                        isCompressing = true
                        onCreateZip(zipName, password, enableAes256, compressionLevel.toInt())
                        onDismiss()
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = if (canProceed) palette.accentPrimary else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (!canProceed) "পাসওয়ার্ড দিন" else "এনক্রিপ্ট করে জিপ তৈরি করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (canProceed) palette.accentPrimary else Color.White.copy(alpha = 0.4f)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectedZipSheet(
    targetCount: Int,
    onDismiss: () -> Unit,
    onCreateZip: (name: String, pass: String, level: Int) -> Unit
) = IplusProtectedZipSheet(
    targetCount = targetCount,
    onDismiss = onDismiss,
    onCreateZip = { name, pass, _, level -> onCreateZip(name, pass, level) }
)

