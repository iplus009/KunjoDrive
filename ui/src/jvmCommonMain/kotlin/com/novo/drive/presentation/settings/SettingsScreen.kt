package com.novo.drive.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.novo.drive.presentation.components.AvatarPickerSheet
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocaleList
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.novo.drive.presentation.platform.LocalMultiFilePicker
import java.io.File
import com.novo.drive.domain.model.AppTheme
import com.novo.drive.domain.model.UserPreferences
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.presentation.components.glass.LiquidGlassSearchBar
import com.novo.drive.presentation.components.glass.LiquidGlassSwitch
import com.novo.drive.presentation.components.liftedTopAppBarColors
import com.novo.drive.presentation.components.rememberToolbarLift
import com.novo.drive.presentation.navigation.LocalBottomBarInset
import com.novo.drive.presentation.platform.LocalAppVersion
import com.novo.drive.presentation.platform.LocalPlatformCapabilities
import com.novo.drive.presentation.theme.LocalIplusPalette
import com.novo.drive.presentation.theme.IplusTheme
import com.novo.drive.presentation.theme.systemDarkTheme
import com.novo.drive.resources.Res
import com.novo.drive.resources.common_back
import com.novo.drive.resources.ic_launcher
import com.novo.drive.resources.settings
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: (() -> Unit)?,
    onOpenSection: (SettingsSectionType) -> Unit,
    onOpenFamilyDashboard: () -> Unit = {},
    onOpenAdminDashboard: () -> Unit = {},
    settingsRepository: SettingsRepository = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val lifted by rememberToolbarLift(scrollState)
    val prefs by settingsRepository.preferences.collectAsState(initial = UserPreferences())
    val palette = LocalIplusPalette.current
    val isLight = palette.theme == AppTheme.LIGHT
    val primaryText = if (isLight) Color(0xFF0F172A) else Color(0xFFDDE8F4)
    val secondaryText = if (isLight) Color(0xFF475569) else Color(0xFF61738A)
    val cyan = palette.accentPrimary
    val violet = palette.accentSecondary
    val appVersion = LocalAppVersion.current
    val multiFilePicker = LocalMultiFilePicker.current

    var searchQuery by remember { mutableStateOf("") }
    var showAvatarSheet by remember { mutableStateOf(false) }

    val capabilities = LocalPlatformCapabilities.current
    val hiddenSections = buildSet {
        if (!capabilities.supportsAutoBackup) add(SettingsSectionType.BACKUP)
        if (!capabilities.requiresPermissions) add(SettingsSectionType.PERMISSIONS)
    }

    val sectionMeta = SettingsSectionType.entries
        .filter { it !in hiddenSections }
        .map { section ->
            Triple(
                section,
                stringResource(section.titleRes),
                stringResource(section.subtitleRes)
            )
        }

    val isBn = LocalLocaleList.current.firstOrNull()?.language?.startsWith("bn") == true

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = liftedTopAppBarColors(lifted),
                title = {
                    Column {
                        Text(
                            text = if (isBn) "কন্ট্রোল প্যানেল" else "Control panel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp,
                            color = cyan
                        )
                        Text(
                            text = stringResource(Res.string.settings),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = primaryText
                        )
                    }
                },
                navigationIcon = {
                    onBack?.let {
                        IconButton(onClick = it) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.common_back),
                                tint = primaryText
                            )
                        }
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(2.dp))

            // HUSSUU 05 — Profile Header Card
            val profileShape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(profileShape)
                    .background(
                        Brush.linearGradient(
                            if (isLight) {
                                listOf(Color.White, Color(0xFFF1F5F9))
                            } else {
                                listOf(Color(0xFF101926), Color(0xFF0C0F19))
                            }
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(cyan.copy(alpha = 0.45f), violet.copy(alpha = 0.30f))
                        ),
                        shape = profileShape
                    )
                    .clickable(role = Role.Button) { onOpenSection(SettingsSectionType.ACCOUNT) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                        .border(
                            1.dp,
                            cyan.copy(alpha = 0.55f),
                            CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp)
                        )
                        .clickable { showAvatarSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    val photoPath = prefs.profilePhotoPath?.takeIf { it.isNotBlank() }
                    val isValidPhoto = photoPath != null && (photoPath.startsWith("content://") || photoPath.startsWith("file://") || File(photoPath).exists())
                    if (isValidPhoto) {
                        AsyncImage(
                            model = if (photoPath!!.startsWith("content://") || photoPath.startsWith("file://")) photoPath else File(photoPath),
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (prefs.avatarValue.isNotBlank()) {
                        Text(prefs.avatarValue, fontSize = 24.sp)
                    } else {
                        Image(
                            painter = painterResource(Res.drawable.ic_launcher),
                            contentDescription = "Kunjo Drive",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (prefs.isFamilyMember) {
                            prefs.familyMemberName.ifBlank { "Family Member #${prefs.familySlotIndex}" }
                        } else {
                            if (isBn) "কুঞ্জ ড্রাইভ · ক্লাউড ভল্ট" else "Kunjo Drive · Cloud Vault"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    Text(
                        text = if (prefs.isFamilyMember) {
                            "FAMILY SLOT #${prefs.familySlotIndex} · MAX 2 GB/FILE"
                        } else {
                            if (isBn) "আনলিমিটেড টেলিগ্রাম ক্লাউড · E2E সুরক্ষিত" else "Unlimited Telegram cloud · E2E secured"
                        },
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                        color = secondaryText
                    )
                }
                val badgeShape = CutCornerShape(topEnd = 6.dp, bottomStart = 6.dp)
                Box(
                    modifier = Modifier
                        .clip(badgeShape)
                        .background(cyan.copy(alpha = 0.12f))
                        .border(1.dp, cyan.copy(alpha = 0.45f), badgeShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (prefs.isFamilyMember) "FAMILY" else "OWNER",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        color = cyan
                    )
                }
            }

            // Kunjo Cloud Profile Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cyan.copy(alpha = 0.08f))
                    .border(
                        1.dp,
                        cyan.copy(alpha = 0.28f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onOpenSection(SettingsSectionType.ACCOUNT) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(cyan.copy(alpha = 0.2f))
                        .clickable { showAvatarSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(prefs.avatarValue.ifBlank { "👤" }, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (prefs.kunjoUsername.isNotBlank()) "KUNJO ID: @${prefs.kunjoUsername}" else "আমার প্রোফাইল ও ইউজারনেম",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    Text(
                        text = if (prefs.kunjoUsername.isNotBlank()) {
                            val accType = if (prefs.kunjoAccountType == "SUB") "👨‍👩‍👧 সাব-অ্যাকাউন্ট" else "👑 মেইন ড্রাইভ"
                            "$accType · প্রোফাইল ও সেটিংস"
                        } else "ইউনিক ইউজারনেম ও অ্যাকাউন্ট বিবরণী",
                        fontSize = 11.sp,
                        color = secondaryText
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = cyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            LiquidGlassSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = if (isBn) "সেটিংস খুঁজুন (থিম, পিন, ব্যাকআপ)..." else "Search settings (theme, pin, backup)..."
            )

            if (searchQuery.isNotBlank()) {
                val queryLower = searchQuery.trim().lowercase()
                val matchesFamily = listOf("family", "ফ্যামিলি", "slot", "qr", "pin", "মেম্বার")
                    .any { it.contains(queryLower) || queryLower.contains(it) }
                val matchedSections = sectionMeta.filter { (section, title, subtitle) ->
                    section.name.lowercase().contains(queryLower) ||
                        title.lowercase().contains(queryLower) ||
                        subtitle.lowercase().contains(queryLower) ||
                        sectionKeywords(section).any { kw ->
                            kw.contains(queryLower) || queryLower.contains(kw)
                        }
                }
                SettingsCardContainer(
                    title = if (isBn) "অনুসন্ধানের ফলাফল (${matchedSections.size + if (matchesFamily) 1 else 0})" else "Search results (${matchedSections.size + if (matchesFamily) 1 else 0})",
                    isLight = isLight,
                    secondaryText = secondaryText
                ) {
                    if (matchesFamily) {
                        SettingsNavRow(
                            icon = Icons.Filled.FamilyRestroom,
                            title = if (isBn) "ফ্যামিলি ক্লাউড" else "Family Cloud",
                            subtitle = if (isBn) "৩ জন সদস্য • QR ও ৬-ডিজিট PIN শেয়ারিং" else "3 members • QR & 6-digit PIN pairing",
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onClick = onOpenFamilyDashboard
                        )
                    }
                    matchedSections.forEach { (section, title, subtitle) ->
                        SettingsNavRow(
                            icon = section.icon,
                            title = title,
                            subtitle = subtitle,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onClick = { onOpenSection(section) }
                        )
                    }
                    if (!matchesFamily && matchedSections.isEmpty()) {
                        Text(
                            text = if (isBn) "কোনো সেটিংস খুঁজে পাওয়া যায়নি" else "No matching settings found",
                            color = secondaryText,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                // Group 1: Account, Family & Security
                SettingsCardContainer(title = if (isBn) "অ্যাকাউন্ট ও নিরাপত্তা" else "Account & security", isLight = isLight, secondaryText = secondaryText) {
                    SettingsNavRow(
                        icon = SettingsSectionType.ACCOUNT.icon,
                        title = stringResource(SettingsSectionType.ACCOUNT.titleRes),
                        subtitle = stringResource(SettingsSectionType.ACCOUNT.subtitleRes),
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = { onOpenSection(SettingsSectionType.ACCOUNT) }
                    )
                    SettingsNavRow(
                        icon = Icons.Filled.FamilyRestroom,
                        title = if (isBn) "ফ্যামিলি ক্লাউড" else "Family Cloud",
                        subtitle = if (prefs.isFamilyMember) {
                            if (isBn) "Family Slot #${prefs.familySlotIndex} • লকড ফ্যামিলি মেম্বার পাস" else "Family Slot #${prefs.familySlotIndex} • locked member pass"
                        } else {
                            if (isBn) "৩ জন সদস্য • ২ জিবি/ফাইল • QR ও PIN শেয়ারিং" else "3 members • 2 GB/file • QR & PIN pairing"
                        },
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = onOpenFamilyDashboard
                    )
                    SettingsNavRow(
                        icon = SettingsSectionType.SECURITY.icon,
                        title = stringResource(SettingsSectionType.SECURITY.titleRes),
                        subtitle = if (isBn) "ভল্ট পিন লক, অ্যাপ লক ও AES-256 এনক্রিপশন" else "Vault PIN lock, app lock & AES-256 encryption",
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = { onOpenSection(SettingsSectionType.SECURITY) }
                    )
                }

                // Group 2: Appearance & Playback
                SettingsCardContainer(title = if (isBn) "থিম ও ডিসপ্লে" else "Appearance & playback", isLight = isLight, secondaryText = secondaryText) {
                    val isSystemDark = systemDarkTheme()
                    val isDarkMode = when (prefs.theme) {
                        AppTheme.LIGHT -> false
                        AppTheme.SYSTEM -> isSystemDark
                        else -> true
                    }
                    val rowShape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp)
                    val chipShape = CutCornerShape(topEnd = 7.dp, bottomStart = 7.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(rowShape)
                            .background(if (isLight) Color.White.copy(alpha = 0.9f) else Color(0xFF10141F))
                            .border(
                                width = 1.dp,
                                color = if (isLight) Color(0xFF0F172A).copy(alpha = 0.10f) else Color.White.copy(alpha = 0.06f),
                                shape = rowShape
                            )
                            .padding(horizontal = 13.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(chipShape)
                                    .background(cyan.copy(alpha = 0.10f))
                                    .border(1.dp, cyan.copy(alpha = 0.25f), chipShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DarkMode,
                                    contentDescription = null,
                                    tint = Color(0xFF7CF0FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Dark Mode",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryText
                                )
                                Text(
                                    text = if (isDarkMode) {
                                        if (isBn) "সক্রিয় রয়েছে (${IplusTheme.palette.name})" else "Active (${IplusTheme.palette.name})"
                                    } else {
                                        if (isBn) "নিষ্ক্রিয় (Clean Light সক্রিয়)" else "Off (Clean Light active)"
                                    },
                                    fontSize = 11.sp,
                                    color = secondaryText
                                )
                            }
                        }
                        LiquidGlassSwitch(
                            checked = isDarkMode,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    settingsRepository.update {
                                        val restoredDark = it.lastSelectedDarkTheme
                                            .takeIf { saved -> saved != AppTheme.LIGHT && saved != AppTheme.SYSTEM }
                                            ?: AppTheme.CYBER_GUARD
                                        it.copy(
                                            theme = if (checked) restoredDark else AppTheme.LIGHT,
                                            lastSelectedDarkTheme = if (checked) restoredDark else it.lastSelectedDarkTheme
                                        )
                                    }
                                }
                            }
                        )
                    }

                    SettingsNavRow(
                        icon = SettingsSectionType.APPEARANCE.icon,
                        title = stringResource(SettingsSectionType.APPEARANCE.titleRes),
                        subtitle = if (isBn) "৪টি প্রিমিয়াম থিম: ${IplusTheme.palette.name}" else "4 premium themes: ${IplusTheme.palette.name}",
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = { onOpenSection(SettingsSectionType.APPEARANCE) }
                    )

                    SettingsNavRow(
                        icon = SettingsSectionType.PLAYBACK.icon,
                        title = stringResource(SettingsSectionType.PLAYBACK.titleRes),
                        subtitle = stringResource(SettingsSectionType.PLAYBACK.subtitleRes),
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = { onOpenSection(SettingsSectionType.PLAYBACK) }
                    )
                }

                // Group 3: Data, Backup & System
                SettingsCardContainer(title = if (isBn) "স্টোরেজ ও ব্যাকআপ" else "Storage & backup", isLight = isLight, secondaryText = secondaryText) {
                    SettingsNavRow(
                        icon = SettingsSectionType.STORAGE.icon,
                        title = stringResource(SettingsSectionType.STORAGE.titleRes),
                        subtitle = stringResource(SettingsSectionType.STORAGE.subtitleRes),
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = { onOpenSection(SettingsSectionType.STORAGE) }
                    )
                    if (SettingsSectionType.BACKUP !in hiddenSections) {
                        SettingsNavRow(
                            icon = SettingsSectionType.BACKUP.icon,
                            title = stringResource(SettingsSectionType.BACKUP.titleRes),
                            subtitle = if (isBn) "অটো ব্যাকআপ ও রাতের বেডটাইম সিঙ্ক (12 AM – 6 AM)" else "Auto backup & bedtime night sync (12 AM – 6 AM)",
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onClick = { onOpenSection(SettingsSectionType.BACKUP) }
                        )
                    }
                    SettingsNavRow(
                        icon = SettingsSectionType.NOTIFICATIONS.icon,
                        title = stringResource(SettingsSectionType.NOTIFICATIONS.titleRes),
                        subtitle = stringResource(SettingsSectionType.NOTIFICATIONS.subtitleRes),
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = { onOpenSection(SettingsSectionType.NOTIFICATIONS) }
                    )
                    if (SettingsSectionType.PERMISSIONS !in hiddenSections) {
                        SettingsNavRow(
                            icon = SettingsSectionType.PERMISSIONS.icon,
                            title = stringResource(SettingsSectionType.PERMISSIONS.titleRes),
                            subtitle = stringResource(SettingsSectionType.PERMISSIONS.subtitleRes),
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onClick = { onOpenSection(SettingsSectionType.PERMISSIONS) }
                        )
                    }
                    SettingsNavRow(
                        icon = SettingsSectionType.ADVANCED.icon,
                        title = stringResource(SettingsSectionType.ADVANCED.titleRes),
                        subtitle = if (isBn) "প্রক্সি, অ্যান্টি-ব্যান সেফ আপলোড স্পিড ও অফলাইন OCR" else "Proxy, anti-ban safe upload speed & offline OCR",
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = { onOpenSection(SettingsSectionType.ADVANCED) }
                    )
                    SettingsNavRow(
                        icon = SettingsSectionType.ABOUT.icon,
                        title = stringResource(SettingsSectionType.ABOUT.titleRes),
                        subtitle = "Kunjo Drive v$appVersion",
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = { onOpenSection(SettingsSectionType.ABOUT) }
                    )
                }

                // Bottom HUSSUU Version Readout Card
                val footerShape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(footerShape)
                        .background(if (isLight) Color(0xFFF1F5F9) else Color(0xFF0B0E16))
                        .border(1.dp, cyan.copy(alpha = 0.22f), footerShape)
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "KUNJO DRIVE v$appVersion",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.0.sp,
                        color = cyan
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "SECURITY PATCH · VAULT 2.0 · HUSSUU UI",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.4.sp,
                        color = secondaryText
                    )
                }
            }

            Spacer(
                Modifier.height(
                    24.dp + padding.calculateBottomPadding() + LocalBottomBarInset.current
                )
            )
        }

        if (showAvatarSheet) {
            AvatarPickerSheet(
                currentType = prefs.avatarType,
                currentValue = prefs.avatarValue,
                onDismiss = { showAvatarSheet = false },
                onSelectAvatar = { type, value, customPath ->
                    coroutineScope.launch {
                        settingsRepository.update {
                            it.copy(
                                avatarType = type,
                                avatarValue = value,
                                profilePhotoPath = customPath
                            )
                        }
                        showAvatarSheet = false
                    }
                },
                onPickFromGallery = {
                    multiFilePicker.pick { paths ->
                        val picked = paths.firstOrNull()
                        if (!picked.isNullOrBlank()) {
                            coroutineScope.launch {
                                settingsRepository.update {
                                    it.copy(
                                        avatarType = "CUSTOM",
                                        avatarValue = "🖼️",
                                        profilePhotoPath = picked
                                    )
                                }
                                showAvatarSheet = false
                            }
                        }
                    }
                }
            )
        }
    }
}

private fun sectionKeywords(section: SettingsSectionType): List<String> = when (section) {
    SettingsSectionType.ACCOUNT -> listOf("account", "অ্যাকাউন্ট", "logout", "channel", "resync", "লগআউট")
    SettingsSectionType.BACKUP -> listOf("backup", "ব্যাকআপ", "bedtime", "বেডটাইম", "night", "wifi", "charging")
    SettingsSectionType.STORAGE -> listOf("storage", "স্টোরেজ", "cache", "ক্যাশ", "trash", "ট্র্যাশ", "download")
    SettingsSectionType.PERMISSIONS -> listOf("permission", "পারমিশন", "files", "media")
    SettingsSectionType.SECURITY -> listOf("security", "নিরাপত্তা", "pin", "পিন", "vault", "ভল্ট", "encrypt", "lock")
    SettingsSectionType.APPEARANCE -> listOf("theme", "থিম", "dark", "light", "language", "ভাষা", "বাংলা", "grid", "cyber", "donut")
    SettingsSectionType.PLAYBACK -> listOf("video", "audio", "music", "stream", "background", "প্লেব্যাক")
    SettingsSectionType.NOTIFICATIONS -> listOf("notification", "নোটিফিকেশন", "alert")
    SettingsSectionType.ADVANCED -> listOf("proxy", "প্রক্সি", "speed", "স্পিড", "ocr", "bandwidth", "debug", "log")
    SettingsSectionType.ABOUT -> listOf("about", "version", "update", "ভার্সন", "আপডেট", "kunjo")
}

@Composable
private fun SettingsCardContainer(
    title: String,
    isLight: Boolean,
    secondaryText: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = secondaryText,
            modifier = Modifier.padding(start = 4.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit
) {
    val palette = LocalIplusPalette.current
    val isLight = palette.theme == AppTheme.LIGHT
    val cyan = palette.accentPrimary
    val rowShape = if (palette.theme == AppTheme.MINIMAL_DRIVE) {
        RoundedCornerShape(14.dp)
    } else {
        CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp)
    }
    val chipShape = CutCornerShape(topEnd = 7.dp, bottomStart = 7.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(if (isLight) Color.White.copy(alpha = 0.92f) else Color(0xFF10141F))
            .border(
                width = 1.dp,
                color = if (isLight) Color(0xFF0F172A).copy(alpha = 0.10f) else Color.White.copy(alpha = 0.06f),
                shape = rowShape
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(28.dp)
                    .background(cyan.copy(alpha = 0.65f))
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(chipShape)
                    .background(cyan.copy(alpha = 0.09f))
                    .border(1.dp, cyan.copy(alpha = 0.25f), chipShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isLight) palette.accentPrimary else Color(0xFF7CF0FF),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = secondaryText
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = secondaryText,
            modifier = Modifier.size(18.dp)
        )
    }
}
