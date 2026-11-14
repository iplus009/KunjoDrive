package com.novo.drive.presentation.iplusmessage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novo.drive.data.local.entity.IplusMessageContactEntity
import com.novo.drive.data.local.entity.IplusMessageGroupEntity
import com.novo.drive.resources.Res
import com.novo.drive.resources.ic_dewan_logo
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IplusMessageScreen(
    contacts: List<IplusMessageContactEntity>,
    groups: List<IplusMessageGroupEntity>,
    searchResults: List<DewanUserSearchResult> = emptyList(),
    blockedContacts: List<IplusMessageContactEntity> = emptyList(),
    pendingShareFile: String? = null,
    isFamilyMember: Boolean = false,
    kunjoUsername: String = "",
    familyMemberName: String = "",
    flagSecureEnabled: Boolean = false,
    onFlagSecureChanged: (Boolean) -> Unit = {},
    onJoinGroup: (String) -> Unit = {},
    onClearPendingShare: () -> Unit = {},
    onOpenChat: (chatId: Long, title: String, isGroup: Boolean) -> Unit,
    onCreateGroup: (title: String, username: String?) -> Unit,
    onSearchUser: (String) -> Unit,
    onUnblockUser: (Long) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Direct Chats, 1: Groups
    var searchQuery by remember { mutableStateOf("") }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showJoinGroupDialog by remember { mutableStateOf(false) }
    var joinLinkInput by remember { mutableStateOf("") }
    var newGroupTitle by remember { mutableStateOf("") }
    var newGroupUsername by remember { mutableStateOf("") }
    var showDewanSettingsSheet by remember { mutableStateOf(false) }
    var showBlockedUsersSheet by remember { mutableStateOf(false) }
    var defaultViewOnce by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            // Sub-Accounts strictly cannot create groups!
            if (!isFamilyMember) {
                FloatingActionButton(
                    onClick = { showCreateGroupDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Group")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header with Official Dewan Logo
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.ic_dewan_logo),
                            contentDescription = "Dewan Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "দেওয়ানগ্রাম (DewanGram)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isFamilyMember) "iplus • সাব-অ্যাকাউন্ট মোড (View-Only)"
                            else "iplus ecosystem • private cloud messenger",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isFamilyMember) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    if (!isFamilyMember) {
                        IconButton(onClick = { showJoinGroupDialog = true }) {
                            Icon(Icons.Default.Link, contentDescription = "Join Group", tint = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(onClick = { showCreateGroupDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "New Group", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    IconButton(onClick = { showDewanSettingsSheet = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "দেওয়ানগ্রাম সেটিংস")
                    }
                }
            }

            // Pending Share File Notification Banner (when sharing from Kunjo Drive)
            if (pendingShareFile != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ফাইল শেয়ার করার জন্য প্রাপক নির্বাচন করুন:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = pendingShareFile.substringAfterLast("/"),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = onClearPendingShare) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Share", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            // Live Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onSearchUser(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            if (isFamilyMember) "ওনার (@owner) বা ইউজারনেম দিয়ে খুঁজুন..."
                            else "নাম, সাব-অ্যাকাউন্ট বা @ইউজারনেম দিয়ে খুঁজুন..."
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                onSearchUser("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true
                )
            }

            // If Search Query is Active: Display Live Multi-Layer Search Results!
            if (searchQuery.isNotBlank()) {
                Text(
                    text = "🔍 অনুসন্ধানের ফলাফল (${searchResults.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "'$searchQuery' নামে কোনো অ্যাকাউন্ট বা কন্টাক্ট পাওয়া যায়নি।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        items(searchResults) { result ->
                            val isAdmin = result.isVerifiedAdmin
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onOpenChat(result.userId, result.displayName, false)
                                    },
                                border = if (isAdmin) BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))) else null,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isAdmin) Color(0xFF1E1700)
                                    else if (result.isOwner) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else if (result.isFamilyMember) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isAdmin) Color(0xFFFFD700).copy(alpha = 0.25f)
                                                else if (result.isOwner) MaterialTheme.colorScheme.primaryContainer
                                                else if (result.isFamilyMember) MaterialTheme.colorScheme.secondaryContainer
                                                else MaterialTheme.colorScheme.tertiaryContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(result.avatarEmoji, fontSize = 22.sp)
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = result.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAdmin) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isAdmin) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Verified Admin",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = Color(0xFF38BDF8)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "@${result.username}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isAdmin) Color(0xFFFFD700).copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = result.roleBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isAdmin) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isAdmin) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Text(
                                        text = if (isFamilyMember) "ফাইল শেয়ার ▶" else "চ্যাট ▶",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAdmin) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Dual Segmented Tabs (Normal state when not searching)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ব্যক্তিগত চ্যাট (Direct)", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isFamilyMember) "গ্রুপ (নিষিদ্ধ)" else "গ্রুপ (২০০ জন)",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab Content
                if (selectedTab == 0) {
                    // Direct Chats Tab
                    if (contacts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💬", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "কোনো সক্রিয় চ্যাট নেই",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isFamilyMember) "উপরে সার্চ বারে '@owner' লিখে মেইন অ্যাকাউন্ট খুঁজুন এবং ড্রাইভ ফাইল লিংক শেয়ার করুন।"
                                    else "উপরে সার্চ বারে '@username' বা সাব-অ্যাকাউন্ট সার্চ করে মেসেজ বা ফাইল পাঠান। প্রথম মেসেজ পাঠালেই কন্টাক্ট সেভ হবে!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                        ) {
                            items(contacts) { contact ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            onOpenChat(contact.userId, contact.displayName, false)
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(contact.avatarValue, fontSize = 24.sp)
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = contact.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "@${contact.username}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Groups Tab
                    if (isFamilyMember) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔒", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "গ্রুপ ফিচার সাব-অ্যাকাউন্টে অনুমোদিত নয়",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "সাব-অ্যাকাউন্ট শুধুমাত্র ব্যক্তিগতভাবে ভিউ-অনলি ক্লাউড ড্রাইভ ফাইল লিংক শেয়ার করতে পারে।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else if (groups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("👥", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "কোনো গ্রুপ তৈরি করা হয়নি",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "নিচের (+) বাটনে চাপ দিয়ে সর্বোচ্চ ২০০ জন মেম্বারের নতুন গ্রুপ তৈরি করুন।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                        ) {
                            items(groups) { group ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            onOpenChat(group.groupId, group.title, true)
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("👥", fontSize = 24.sp)
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = group.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${group.memberCount}/200 জন মেম্বার",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Group Dialog
    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("নতুন গ্রুপ তৈরি করুন (New Group)") },
            text = {
                Column {
                    Text(
                        "টেলিগ্রাম বেসিক গ্রুপ আর্কিটেকচার: সর্বোচ্চ ২০০ জন মেম্বার যোগ করা যাবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newGroupTitle,
                        onValueChange = { newGroupTitle = it },
                        label = { Text("গ্রুপের নাম *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newGroupUsername,
                        onValueChange = { newGroupUsername = it },
                        label = { Text("পাবলিক ইউজারনেম (ঐচ্ছিক)") },
                        placeholder = { Text("@group_name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGroupTitle.isNotBlank()) {
                            onCreateGroup(newGroupTitle, newGroupUsername)
                            newGroupTitle = ""
                            newGroupUsername = ""
                            showCreateGroupDialog = false
                        }
                    }
                ) {
                    Text("তৈরি করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // DewanGram Dedicated Settings Bottom Sheet
    if (showDewanSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDewanSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⚙️ দেওয়ানগ্রাম সেটিংস",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showDewanSettingsSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Profile Summary Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isFamilyMember) "👤" else "👑", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val profileTitle = when {
                                isFamilyMember -> familyMemberName.ifBlank { "Family Member" }
                                kunjoUsername.isNotBlank() -> "@$kunjoUsername"
                                else -> "কুঞ্জ ড্রাইভ ব্যবহারকারী"
                            }
                            val profileSubtitle = when {
                                isFamilyMember -> "সাব-অ্যাকাউন্ট মোড"
                                kunjoUsername.isNotBlank() -> "মেইন অ্যাকাউন্ট · দেওয়ানগ্রাম"
                                else -> "মেইন অ্যাকাউন্ট"
                            }
                            Text(
                                text = profileTitle,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = profileSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("🛡️ প্রাইভেসি ও নিরাপত্তা", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("স্ক্রিনশট ব্লক (FLAG_SECURE)", fontSize = 13.sp)
                    }
                    Switch(checked = flagSecureEnabled, onCheckedChange = onFlagSecureChanged)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("১-টাইম ভিউ (View Once) ডিফল্ট", fontSize = 13.sp)
                    }
                    Switch(checked = defaultViewOnce, onCheckedChange = { defaultViewOnce = it })
                }

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBlockedUsersSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚫", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ব্লক করা ব্যবহারকারীদের তালিকা", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${blockedContacts.size} জন ব্লক করা আছেন", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Text("ম্যানেজ ▶", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("☁️ সিস্টেম ও ক্লাউড স্ট্যাটাস", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• কুঞ্জ ড্রাইভ ক্লাউড চ্যানেল: সংযুক্ত (Connected)\n• মেসেজ ট্রান্সমিশন: সক্রিয় (Telegram Cloud)\n• ফাইল শেয়ারিং: ০ মেগাবাইট ব্যান্ডউইথ (Zero Re-Upload)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { showDewanSettingsSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("সেটিংস বন্ধ করুন")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Blocked Users Management Dialog
    if (showBlockedUsersSheet) {
        AlertDialog(
            onDismissRequest = { showBlockedUsersSheet = false },
            title = { Text("🚫 ব্লক করা ব্যবহারকারী (${blockedContacts.size})", fontWeight = FontWeight.Bold) },
            text = {
                if (blockedContacts.isEmpty()) {
                    Text("বর্তমানে কোনো ব্যবহারকারী ব্লক তালিকায় নেই।", color = MaterialTheme.colorScheme.outline)
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(blockedContacts) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(contact.avatarValue, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(contact.displayName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("@${contact.username}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                                TextButton(onClick = { onUnblockUser(contact.userId) }) {
                                    Text("আনব্লক", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlockedUsersSheet = false }) {
                    Text("ঠিক আছে")
                }
            }
        )
    }

    // Join Group via Invite Link / @Username Dialog
    if (showJoinGroupDialog) {
        AlertDialog(
            onDismissRequest = { showJoinGroupDialog = false },
            title = { Text("🔗 গ্রুপ ইনভাইট লিংকে যুক্ত হোন") },
            text = {
                Column {
                    Text(
                        "টেলিগ্রাম গ্রুপ লিংক (t.me/+...), দেওয়ানগ্রাম লিংক (dewan://join/...) অথবা @username দিন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = joinLinkInput,
                        onValueChange = { joinLinkInput = it },
                        label = { Text("ইনভাইট লিংক বা @username") },
                        placeholder = { Text("https://t.me/+... অথবা dewan://join/...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinLinkInput.isNotBlank()) {
                            onJoinGroup(joinLinkInput.trim())
                            joinLinkInput = ""
                            showJoinGroupDialog = false
                        }
                    }
                ) {
                    Text("যুক্ত হোন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinGroupDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
