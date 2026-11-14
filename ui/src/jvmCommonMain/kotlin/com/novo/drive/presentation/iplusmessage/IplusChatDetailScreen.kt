package com.novo.drive.presentation.iplusmessage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novo.drive.data.local.entity.IplusMessageEntity
import com.novo.drive.domain.model.DriveFile
import com.novo.drive.presentation.common.Formatters

import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import coil3.compose.AsyncImage
import com.novo.drive.presentation.platform.LocalFileOpener
import com.novo.drive.presentation.platform.LocalMultiFilePicker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_SUBACCOUNT_FILE_BYTES: Long = 2L * 1024L * 1024L * 1024L // 2 GB hard gate

enum class ChatAttachmentCategory(val title: String) {
    ALL("সব"),
    VIDEO("🎬 ভিডিও"),
    IMAGE("🖼️ ছবি"),
    AUDIO("🎵 অডিও"),
    DOCUMENTS("📄 ডক"),
    APPS("📦 অ্যাপস")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IplusChatDetailScreen(
    chatId: Long,
    title: String,
    isGroup: Boolean,
    messages: List<IplusMessageEntity>,
    isFamilyMember: Boolean = false,
    recentDriveFiles: List<DriveFile> = emptyList(),
    isReadOnly: Boolean = false,
    canSendInReadOnly: Boolean = true,
    flagSecureEnabled: Boolean = false,
    onFlagSecureChanged: (Boolean) -> Unit = {},
    onMarkViewOnceViewed: (Long) -> Unit = {},
    onBack: () -> Unit,
    onSendMessage: (text: String, driveLink: String?, isViewOnce: Boolean) -> Unit,
    onOpenGroupSettings: () -> Unit,
    onPlayVideo: (String, String) -> Unit,
    onPlayAudio: (String, String) -> Unit,
    onOpenPdf: (String, String) -> Unit,
    onSaveToMyDrive: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var policyBlockAlert by remember { mutableStateOf<String?>(null) }
    val multiFilePicker = LocalMultiFilePicker.current
    val fileOpener = LocalFileOpener.current

    // Security toggles for sharing
    var isViewOnceEnabled by remember { mutableStateOf(false) }
    var selectedCloudFileName by remember { mutableStateOf<String?>(null) }
    var selectedCloudFileLink by remember { mutableStateOf<String?>(null) }
    var dialogFilterCategory by remember { mutableStateOf(ChatAttachmentCategory.ALL) }
    var dialogSearchQuery by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isGroup) "👥" else "👤", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isGroup) "গ্রুপ চ্যাট (সর্বোচ্চ ২০০ জন)"
                                else if (isFamilyMember) "ভিউ-অনলি ফাইল ব্রিজ"
                                else "অনলাইনে আছেন",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isGroup && !isFamilyMember) {
                        IconButton(onClick = onOpenGroupSettings) {
                            Icon(Icons.Default.Info, contentDescription = "Group Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // Selected attachment preview pill
            if (selectedCloudFileLink != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📎 যুক্ত ফাইল: ${selectedCloudFileName ?: "কুঞ্জ ফাইল"}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            selectedCloudFileLink = null
                            selectedCloudFileName = null
                        }) {
                            Text("✕", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    IplusMessageBubble(
                        message = msg,
                        isFamilyMember = isFamilyMember,
                        onMarkViewOnceViewed = { onMarkViewOnceViewed(msg.messageId) },
                        onPlayVideo = { link ->
                            val fileId = link.removePrefix("kunjo://file/")
                            val targetFile = recentDriveFiles.firstOrNull { it.id == fileId || link.contains(it.id) || link.contains(it.name) }
                            if (isFamilyMember && (targetFile?.sizeBytes ?: 0L) > MAX_SUBACCOUNT_FILE_BYTES) {
                                policyBlockAlert = "⛔ সাব-অ্যাকাউন্ট ২ GB-এর বড় ফাইল প্লে বা ওপেন করতে পারে না (সাইজ: ${Formatters.bytes(targetFile?.sizeBytes ?: 0L)})।"
                            } else {
                                onPlayVideo(fileId, msg.text ?: targetFile?.name ?: "Video")
                            }
                        },
                        onPlayAudio = { link ->
                            val fileId = link.removePrefix("kunjo://file/")
                            val targetFile = recentDriveFiles.firstOrNull { it.id == fileId || link.contains(it.id) || link.contains(it.name) }
                            if (isFamilyMember && (targetFile?.sizeBytes ?: 0L) > MAX_SUBACCOUNT_FILE_BYTES) {
                                policyBlockAlert = "⛔ সাব-অ্যাকাউন্ট ২ GB-এর বড় অডিও ওপেন করতে পারে না (সাইজ: ${Formatters.bytes(targetFile?.sizeBytes ?: 0L)})।"
                            } else {
                                onPlayAudio(fileId, msg.text ?: targetFile?.name ?: "Audio")
                            }
                        },
                        onOpenPdf = { link ->
                            val fileId = link.removePrefix("kunjo://file/")
                            val targetFile = recentDriveFiles.firstOrNull { it.id == fileId || link.contains(it.id) || link.contains(it.name) }
                            if (isFamilyMember && (targetFile?.sizeBytes ?: 0L) > MAX_SUBACCOUNT_FILE_BYTES) {
                                policyBlockAlert = "⛔ সাব-অ্যাকাউন্ট ২ GB-এর বড় ফাইল ওপেন করতে পারে না (সাইজ: ${Formatters.bytes(targetFile?.sizeBytes ?: 0L)})।"
                            } else {
                                onOpenPdf(fileId, msg.text ?: targetFile?.name ?: "Document")
                            }
                        },
                        onSaveToMyDrive = { link ->
                            val fileId = link.removePrefix("kunjo://file/")
                            val targetFile = recentDriveFiles.firstOrNull { it.id == fileId || link.contains(it.id) || link.contains(it.name) }
                            if (isFamilyMember && (targetFile?.sizeBytes ?: 0L) > MAX_SUBACCOUNT_FILE_BYTES) {
                                policyBlockAlert = "⛔ সাব-অ্যাকাউন্টে ২ GB-এর চেয়ে বড় ফাইল যুক্ত করা সম্পূর্ণ নিষিদ্ধ।"
                            } else {
                                onSaveToMyDrive(link)
                            }
                        }
                    )
                }
            }

            // Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                if (isReadOnly && !canSendInReadOnly) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔒", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "এই গ্রুপে চ্যাট বন্ধ আছে। শুধুমাত্র অ্যাডমিনরা মেসেজ দিতে পারবেন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showAttachmentSheet = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Attach Content",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (isFamilyMember) {
                            // Sub-Accounts strictly cannot type text messages!
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "🔒 সাব-অ্যাকাউন্ট মোড: চ্যাট নিষিদ্ধ। শুধুমাত্র (+) চেপে ক্লাউড ফাইল লিঙ্ক শেয়ার করতে পারবেন।",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("মেসেজ লিখুন...") },
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 4
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                if (selectedCloudFileLink != null) {
                                    onSendMessage(if (isFamilyMember) "" else inputText, selectedCloudFileLink, isViewOnceEnabled)
                                    inputText = ""
                                    selectedCloudFileLink = null
                                    selectedCloudFileName = null
                                } else if (!isFamilyMember && inputText.isNotBlank()) {
                                    onSendMessage(inputText, null, isViewOnceEnabled)
                                    inputText = ""
                                }
                            },
                            enabled = (isFamilyMember && selectedCloudFileLink != null) || (!isFamilyMember && (inputText.isNotBlank() || selectedCloudFileLink != null))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if ((isFamilyMember && selectedCloudFileLink != null) || (!isFamilyMember && (inputText.isNotBlank() || selectedCloudFileLink != null))) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Attachment Options Sheet (Real Phone Media & Cloud Drive)
    if (showAttachmentSheet) {
        AlertDialog(
            onDismissRequest = { showAttachmentSheet = false },
            title = {
                Text("ফাইল ও মিডিয়া শেয়ার করুন (Share)", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    multiFilePicker.pick { paths ->
                                        val p = paths.firstOrNull()
                                        if (!p.isNullOrBlank()) {
                                            selectedCloudFileName = p.substringAfterLast('/').substringAfterLast('\\')
                                            selectedCloudFileLink = if (p.startsWith("content://") || p.startsWith("file://")) p else "file://$p"
                                            showAttachmentSheet = false
                                        }
                                    }
                                },
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🖼️", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("গ্যালারি", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("ছবি ও ভিডিও", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    multiFilePicker.pick { paths ->
                                        val p = paths.firstOrNull()
                                        if (!p.isNullOrBlank()) {
                                            selectedCloudFileName = p.substringAfterLast('/').substringAfterLast('\\')
                                            selectedCloudFileLink = if (p.startsWith("content://") || p.startsWith("file://")) p else "file://$p"
                                            showAttachmentSheet = false
                                        }
                                    }
                                },
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📦", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("অ্যাপ্স / APK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("অ্যাপ্লিকেশন শেয়ার", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    multiFilePicker.pick { paths ->
                                        val p = paths.firstOrNull()
                                        if (!p.isNullOrBlank()) {
                                            selectedCloudFileName = p.substringAfterLast('/').substringAfterLast('\\')
                                            selectedCloudFileLink = if (p.startsWith("content://") || p.startsWith("file://")) p else "file://$p"
                                            showAttachmentSheet = false
                                        }
                                    }
                                },
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📄", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("ডকুমেন্ট", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("PDF, Office, Zip", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showAttachmentSheet = false
                                    showAttachmentDialog = true
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("☁️", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("কুঞ্জ ড্রাইভ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("ক্লাউড ফাইলস", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🛡️ স্ক্রিনশট ব্লক (FLAG_SECURE)", fontSize = 12.sp)
                        }
                        Switch(checked = flagSecureEnabled, onCheckedChange = onFlagSecureChanged)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⏱️ ১-টাইম ভিউ (View Once)", fontSize = 12.sp)
                        }
                        Switch(checked = isViewOnceEnabled, onCheckedChange = { isViewOnceEnabled = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAttachmentSheet = false }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    // Attachment & Kunjo Cloud Picker Dialog (with REAL files)
    if (showAttachmentDialog) {
        AlertDialog(
            onDismissRequest = { showAttachmentDialog = false },
            title = {
                Text("কুঞ্জ ক্লাউড ড্রাইভ ফাইল নির্বাচন", fontWeight = FontWeight.Bold)
            },
            text = {
                val filteredDriveFiles = remember(recentDriveFiles, dialogFilterCategory, dialogSearchQuery) {
                    recentDriveFiles.filter { file ->
                        val matchesCat = when (dialogFilterCategory) {
                            ChatAttachmentCategory.ALL -> true
                            ChatAttachmentCategory.VIDEO -> file.mimeType.startsWith("video/")
                            ChatAttachmentCategory.IMAGE -> file.mimeType.startsWith("image/")
                            ChatAttachmentCategory.AUDIO -> file.mimeType.startsWith("audio/")
                            ChatAttachmentCategory.DOCUMENTS -> file.mimeType == "application/pdf" || file.mimeType.contains("document") || file.name.endsWith(".pdf", ignoreCase = true)
                            ChatAttachmentCategory.APPS -> file.name.endsWith(".apk", ignoreCase = true) || file.mimeType.contains("android.package-archive")
                        }
                        val matchesQuery = dialogSearchQuery.isBlank() || file.name.contains(dialogSearchQuery, ignoreCase = true)
                        matchesCat && matchesQuery
                    }
                }

                Column {
                    Text(
                        "কুঞ্জ ড্রাইভ থেকে জিরো-রিআপলোড মেকানিজমে ফাইলটি লিঙ্ক আকারে চ্যাটে যুক্ত হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Dialog Live Search
                    OutlinedTextField(
                        value = dialogSearchQuery,
                        onValueChange = { dialogSearchQuery = it },
                        placeholder = { Text("🔍 ফাইলের নাম দিয়ে খুঁজুন...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 6 Category Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChatAttachmentCategory.entries.forEach { cat ->
                            val isSelected = dialogFilterCategory == cat
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { dialogFilterCategory = cat }
                            ) {
                                Text(
                                    text = cat.title,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Security Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🛡️ স্ক্রিনশট ব্লক (FLAG_SECURE)", fontSize = 12.sp)
                        }
                        Switch(checked = flagSecureEnabled, onCheckedChange = onFlagSecureChanged)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⏱️ ১-টাইম ভিউ (View Once)", fontSize = 12.sp)
                        }
                        Switch(checked = isViewOnceEnabled, onCheckedChange = { isViewOnceEnabled = it })
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "আপনার কুঞ্জ ড্রাইভের ফাইলসমূহ (${filteredDriveFiles.size}টি):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (filteredDriveFiles.isEmpty()) {
                        Text(
                            text = if (recentDriveFiles.isEmpty()) "কুঞ্জ ড্রাইভে কোনো সাম্প্রতিক ফাইল পাওয়া যায়নি।"
                                   else "নির্বাচিত ক্যাটাগরি বা সার্চে কোনো ফাইল পাওয়া যায়নি।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 14.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(220.dp)) {
                            items(filteredDriveFiles) { driveFile ->
                                val isOverLimit = isFamilyMember && driveFile.sizeBytes > MAX_SUBACCOUNT_FILE_BYTES
                                val fileLink = "kunjo://file/${driveFile.id}"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = !isOverLimit) {
                                            selectedCloudFileName = driveFile.name
                                            selectedCloudFileLink = fileLink
                                        }
                                        .background(
                                            if (selectedCloudFileLink == fileLink) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        when {
                                            driveFile.mimeType.startsWith("video/") -> "🎬"
                                            driveFile.mimeType.startsWith("image/") -> "🖼️"
                                            driveFile.mimeType.startsWith("audio/") -> "🎵"
                                            driveFile.mimeType == "application/pdf" || driveFile.name.endsWith(".pdf", ignoreCase = true) -> "📄"
                                            driveFile.name.endsWith(".apk", ignoreCase = true) -> "📦"
                                            else -> "📁"
                                        },
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = driveFile.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = Formatters.bytes(driveFile.sizeBytes) + if (isOverLimit) " (২ GB এর বেশি - সাব-অ্যাকাউন্টে নিষিদ্ধ)" else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAttachmentDialog = false }) {
                    Text("যুক্ত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedCloudFileLink = null
                    selectedCloudFileName = null
                    showAttachmentDialog = false
                }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Policy Violation Dialog
    policyBlockAlert?.let { msg ->
        AlertDialog(
            onDismissRequest = { policyBlockAlert = null },
            title = { Text("⛔ পলিসি ব্লকার (Policy Restriction)", fontWeight = FontWeight.Bold) },
            text = { Text(msg, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(onClick = { policyBlockAlert = null }) {
                    Text("বুঝেছি")
                }
            }
        )
    }
}

@Composable
fun IplusMessageBubble(
    message: IplusMessageEntity,
    isFamilyMember: Boolean = false,
    onMarkViewOnceViewed: () -> Unit = {},
    onPlayVideo: (String) -> Unit,
    onPlayAudio: (String) -> Unit,
    onOpenPdf: (String) -> Unit,
    onSaveToMyDrive: (String) -> Unit
) {
    val isOut = message.isOutgoing
    val fileOpener = LocalFileOpener.current
    var revealedThisSession by remember(message.messageId) { mutableStateOf(false) }
    val isViewOnceLocked = message.isViewOnce && message.isViewed && !isOut && !revealedThisSession
    val needsTapToReveal = message.isViewOnce && !message.isViewed && !isOut && !revealedThisSession &&
        !message.text.isNullOrBlank() && message.driveLink.isNullOrBlank()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isOut) 18.dp else 4.dp,
                bottomEnd = if (isOut) 4.dp else 18.dp
            ),
            color = if (isOut) Color(0xFF003852) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // If it's a View-Once message
                if (message.isViewOnce) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "View Once",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFB300)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isViewOnceLocked) "View Once (মেয়াদ শেষ)" else "View Once (একবার দেখলেই লিংক এক্সপায়ার)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300)
                        )
                    }
                }

                if (isViewOnceLocked) {
                    Text(
                        text = "⏱️ এই View-Once বার্তাটি দেখা হয়ে গেছে ও লক করা হয়েছে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = (if (isOut) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f)
                    )
                } else if (needsTapToReveal) {
                    TextButton(
                        onClick = {
                            revealedThisSession = true
                            onMarkViewOnceViewed()
                        }
                    ) {
                        Text("👁️ একবার দেখতে ট্যাপ করুন", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                    }
                } else {
                // Text Content
                val msgText = message.text
                if (!msgText.isNullOrBlank()) {
                    Text(
                        text = msgText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOut) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // If message has a file or media link
                message.driveLink?.let { link ->
                    Spacer(modifier = Modifier.height(8.dp))
                    val isTgRemote = link.startsWith("tgremote://")
                    val tgParts = if (isTgRemote) link.removePrefix("tgremote://").split("|") else emptyList()
                    val fileName = if (isTgRemote) {
                        tgParts.getOrNull(1).orEmpty().ifBlank { "DewanGram_File" }
                    } else {
                        link.substringAfterLast('/').substringAfterLast('\\').removePrefix("file://")
                    }
                    val tgMime = tgParts.getOrNull(2).orEmpty()
                    val lowerLink = if (isTgRemote) "$fileName $tgMime".lowercase() else link.lowercase()
                    val isImage = lowerLink.endsWith(".jpg") || lowerLink.endsWith(".png") || lowerLink.endsWith(".jpeg") ||
                            lowerLink.endsWith(".webp") || lowerLink.contains("image")
                    val isApk = lowerLink.endsWith(".apk") || lowerLink.contains("android.package-archive")
                    val isVideo = lowerLink.endsWith(".mp4") || lowerLink.endsWith(".mkv") || lowerLink.contains("vid_") || lowerLink.contains("video/")
                    val isAudio = lowerLink.endsWith(".mp3") || lowerLink.endsWith(".m4a") || lowerLink.contains("aud_") || lowerLink.contains("audio/")
                    val isPdf = lowerLink.endsWith(".pdf") || lowerLink.contains("doc_")

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isOut) Color(0xFF002435) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (isImage && !isTgRemote) {
                                val localFilePath = link.removePrefix("file://")
                                val imageFile = File(localFilePath)
                                AsyncImage(
                                    model = when {
                                        localFilePath.startsWith("content://") -> localFilePath
                                        imageFile.exists() -> imageFile
                                        else -> link
                                    },
                                    contentDescription = "Photo Preview",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.2f))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(fileName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when {
                                            isApk -> Icons.Default.Android
                                            isVideo -> Icons.Default.PlayCircle
                                            isAudio -> Icons.Default.Audiotrack
                                            isPdf -> Icons.Default.PictureAsPdf
                                            else -> Icons.Default.AttachFile
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = when {
                                                isApk -> "অ্যাপ্লিকেশন / APK (দেওয়ানগ্রাম)"
                                                isVideo -> "ভিডিও (কুঞ্জ সিনেমা প্লেয়ার)"
                                                isAudio -> "অডিও (কুঞ্জ মিউজিক স্টুডিও)"
                                                isPdf -> "পিডিএফ ডকুমেন্ট (কুঞ্জ রিডার)"
                                                else -> "সংযুক্ত ফাইল (Attachment)"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = fileName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = {
                                        if (message.isViewOnce && !message.isViewed && !isOut) {
                                            revealedThisSession = true
                                            onMarkViewOnceViewed()
                                        }
                                        val localFilePath = link.removePrefix("file://")
                                        val localFile = File(localFilePath)
                                        if (isApk && !isTgRemote && localFile.exists()) {
                                            fileOpener.openFile(localFile)
                                        } else if (isVideo) {
                                            if (!isTgRemote && localFile.exists()) fileOpener.openFile(localFile) else onPlayVideo(link)
                                        } else if (isAudio) {
                                            if (!isTgRemote && localFile.exists()) fileOpener.openFile(localFile) else onPlayAudio(link)
                                        } else if (!isTgRemote && localFile.exists()) {
                                            fileOpener.openFile(localFile)
                                        } else {
                                            onOpenPdf(link)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        when {
                                            isApk -> "📦 ইন্সটল / ওপেন করুন"
                                            isImage -> "🔍 ফুল স্ক্রিন ছবি"
                                            isVideo -> "প্লে করুন 🎬"
                                            isAudio -> "শুনুন 🎵"
                                            else -> "খুলুন 📄"
                                        }
                                    )
                                }

                                if (!isOut) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = {
                                        if (message.isViewOnce && !message.isViewed && !isOut) {
                                            revealedThisSession = true
                                            onMarkViewOnceViewed()
                                        }
                                        onSaveToMyDrive(link)
                                    }) {
                                        Icon(
                                            Icons.Default.CloudDownload,
                                            contentDescription = "Save to My Drive",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }

                // Timestamp & Ticks (Real dynamic time)
                val timeString = remember(message.timestamp) {
                    runCatching {
                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
                    }.getOrDefault("Now")
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeString,
                        fontSize = 10.sp,
                        color = (if (isOut) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f)
                    )
                    if (isOut) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val (statusIcon, statusTint) = when (message.status) {
                            "READ" -> Icons.Default.DoneAll to Color(0xFF00E5FF)
                            "FAILED", "RATE_LIMITED" -> Icons.Default.ErrorOutline to Color(0xFFEF4444)
                            "SENDING" -> Icons.Default.Schedule to Color.White.copy(alpha = 0.55f)
                            else -> Icons.Default.Done to Color.White.copy(alpha = 0.7f)
                        }
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = message.status,
                            modifier = Modifier.size(14.dp),
                            tint = statusTint
                        )
                    }
                }
            }
        }
    }
}
