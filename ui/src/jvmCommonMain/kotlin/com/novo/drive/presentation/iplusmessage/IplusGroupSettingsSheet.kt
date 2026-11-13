package com.novo.drive.presentation.iplusmessage

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novo.drive.data.local.entity.IplusMessageGroupEntity

import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton

data class GroupMemberItem(
    val userId: Long,
    val name: String,
    val username: String,
    val role: String, // OWNER, ADMIN, MEMBER
    val isAllowedSender: Boolean = false,
    val avatarEmoji: String = "👤"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IplusGroupSettingsSheet(
    group: IplusMessageGroupEntity,
    currentUserId: Long,
    members: List<GroupMemberItem>,
    onDismiss: () -> Unit,
    onAddMember: () -> Unit,
    onRemoveMember: (Long) -> Unit,
    onBlockMember: (Long) -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateUsername: (String) -> Unit = {},
    onToggleReadOnly: (Boolean) -> Unit = {},
    onToggleAdmin: (userId: Long, makeAdmin: Boolean) -> Unit = { _, _ -> },
    onToggleSendPermission: (userId: Long, allowed: Boolean) -> Unit = { _, _ -> },
    onCopyInviteLink: (String) -> Unit,
    onDeleteGroup: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isEditingTitle by remember { mutableStateOf(false) }
    var titleText by remember { mutableStateOf(group.title) }
    var isEditingUsername by remember { mutableStateOf(false) }
    var usernameText by remember { mutableStateOf(group.groupUsername ?: "") }

    val inviteLink = group.inviteLink ?: "dewan://join/g_${group.groupId}"
    val isOwner = currentUserId == group.ownerId
    val currentMember = members.firstOrNull { it.userId == currentUserId }
    val isAdmin = isOwner || currentMember?.role == "ADMIN"

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
                    text = "গ্রুপ সেটিংস (Group Settings)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Group Card with Title & Member Limit
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isEditingTitle) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = titleText,
                                onValueChange = { titleText = it },
                                label = { Text("গ্রুপের নাম") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                onUpdateTitle(titleText)
                                isEditingTitle = false
                            }) {
                                Text("সেভ")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = group.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${members.size}/200 জন মেম্বার (সর্বোচ্চ ২০০ জন)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (isOwner) {
                                IconButton(onClick = { isEditingTitle = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Name")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Invite Link Section
                    Text(
                        text = "ইনভাইট লিংক (Invite Link)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = inviteLink,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = { onCopyInviteLink(inviteLink) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "গ্রুপ ইউজারনেম (Username)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isEditingUsername) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = usernameText,
                                onValueChange = { usernameText = it },
                                placeholder = { Text("@group_name") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                onUpdateUsername(usernameText)
                                isEditingUsername = false
                            }) {
                                Text("সেভ")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (group.groupUsername.isNullOrBlank()) "কোনো ইউজারনেম নেই" else "@${group.groupUsername}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (isOwner) {
                                IconButton(onClick = { isEditingUsername = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Username")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Read-Only Governance Toggle
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🔒 চ্যাট বন্ধ রাখুন (Read-Only Mode)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("অন থাকলে সাধারণ মেম্বাররা লিখতে পারবে না, শুধু অ্যাডমিনরা পারবে।", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = group.isReadOnly,
                        onCheckedChange = { onToggleReadOnly(it) },
                        enabled = isAdmin
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Member Button
            OutlinedButton(
                onClick = onAddMember,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("নতুন সদস্য যুক্ত করুন (+ Add Member)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "সদস্য তালিকা (Members)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Members List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                items(members) { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(member.avatarEmoji, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (member.role == "OWNER") {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "Owner",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (member.role == "ADMIN") {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Shield,
                                            contentDescription = "Admin",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                Text(
                                    text = "@${member.username} • ${member.role.lowercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        if (isOwner && member.userId != currentUserId) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isTargetAdmin = member.role == "ADMIN"
                                TextButton(onClick = { onToggleAdmin(member.userId, !isTargetAdmin) }) {
                                    Text(if (isTargetAdmin) "ডিমোট" else "অ্যাডমিন", fontSize = 11.sp)
                                }
                                if (group.isReadOnly && !isTargetAdmin) {
                                    TextButton(onClick = { onToggleSendPermission(member.userId, !member.isAllowedSender) }) {
                                        Text(if (member.isAllowedSender) "অনুমতি বাতিল" else "পারমিশন", fontSize = 10.sp)
                                    }
                                }
                                IconButton(onClick = { onRemoveMember(member.userId) }) {
                                    Icon(
                                        Icons.Default.PersonRemove,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else if (isAdmin && !isOwner && member.userId != currentUserId && member.role != "OWNER" && member.role != "ADMIN") {
                            IconButton(onClick = { onRemoveMember(member.userId) }) {
                                Icon(
                                    Icons.Default.PersonRemove,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            if (isOwner) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDeleteGroup,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("গ্রুপ ডিলিট করুন (Delete Group)")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

