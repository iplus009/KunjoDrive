package com.novo.drive.presentation.components

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChibiItem(
    val emoji: String,
    val name: String,
    val description: String
)

val CHIBI_AVATARS = listOf(
    ChibiItem("🐱", "Chibi Cat", "Sweet cute cat"),
    ChibiItem("🐶", "Puppy Dog", "Playful puppy"),
    ChibiItem("🦊", "Clever Fox", "Smart clever fox"),
    ChibiItem("🐯", "Brave Tiger", "Brave chibi tiger"),
    ChibiItem("🦁", "King Lion", "Mighty lion king"),
    ChibiItem("🐵", "Fun Monkey", "Playful monkey"),
    ChibiItem("🫏", "Cute Donkey", "Sweet cute donkey"),
    ChibiItem("🐼", "Fluffy Panda", "Round fluffy panda"),
    ChibiItem("🦧", "Smart Chimp", "Intelligent chimp"),
    ChibiItem("⚡", "Anime Boy", "Cyber anime hero"),
    ChibiItem("🌸", "Anime Girl", "Kawaii anime princess")
)

val DEFAULT_EMOJIS = listOf("👤", "✨", "🚀", "🛡️", "💎", "🔥")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerSheet(
    currentType: String,
    currentValue: String,
    onDismiss: () -> Unit,
    onSelectAvatar: (type: String, value: String, customPath: String?) -> Unit,
    onPickFromGallery: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isBn = com.novo.drive.presentation.common.isBengaliUi()
    var selectedType by remember { mutableStateOf(currentType) }
    var selectedValue by remember { mutableStateOf(currentValue) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBn) "প্রোফাইল অবতার নির্বাচন করুন" else "Choose Profile Avatar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Preview
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedValue.ifBlank { "🛡️" },
                    fontSize = 40.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Choose from Gallery Button
            OutlinedButton(
                onClick = onPickFromGallery,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isBn) "📷 Choose from Gallery (গ্যালারি থেকে ছবি)" else "📷 Choose from Gallery")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    selectedType = "DEFAULT"
                    selectedValue = ""
                    onSelectAvatar("DEFAULT", "", null)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (isBn) "🔄 ডিফল্ট অ্যাপ লোগোতে ফিরুন" else "🔄 Reset to Default App Logo")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isBn) "১১টি কিউট চিবী ও অ্যানিমে ক্যারেক্টার" else "11 Cute Chibi & Anime Characters",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 11 Chibi Characters Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CHIBI_AVATARS) { chibi ->
                    val isSelected = selectedType == "CHIBI" && selectedValue == chibi.emoji
                    Surface(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selectedType = "CHIBI"
                                selectedValue = chibi.emoji
                                onSelectAvatar("CHIBI", chibi.emoji, null)
                            },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.primary
                        ) else null
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(text = chibi.emoji, fontSize = 28.sp)
                            Text(
                                text = chibi.name,
                                fontSize = 10.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isBn) "ডিফল্ট মিনিমালিস্ট অবতার" else "Minimalist Avatars",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Default Emojis
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DEFAULT_EMOJIS.forEach { emoji ->
                    val isSelected = selectedType == "EMOJI" && selectedValue == emoji
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                selectedType = "EMOJI"
                                selectedValue = emoji
                                onSelectAvatar("EMOJI", emoji, null)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 22.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

