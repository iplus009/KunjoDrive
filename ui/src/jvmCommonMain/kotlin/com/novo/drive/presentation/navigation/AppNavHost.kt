package com.novo.drive.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.novo.drive.core.telegram.TelegramAuthState
import com.novo.drive.domain.model.FileCategory
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.domain.repository.TelegramAuthRepository
import com.novo.drive.presentation.admin.KunjoAdminDashboardScreen
import com.novo.drive.presentation.channels.ChannelsScreen
import com.novo.drive.presentation.cleaner.StorageCleanerScreen
import com.novo.drive.presentation.collection.CollectionScreen
import com.novo.drive.presentation.family.FamilyDashboardScreen
import com.novo.drive.presentation.files.IplusFilesScreen
import com.novo.drive.presentation.files.IplusLocalFileManagerScreen
import com.novo.drive.presentation.gallery.GalleryScreen
import com.novo.drive.presentation.home.HomeScreen
import com.novo.drive.presentation.iplusmessage.IplusChatDetailScreen
import com.novo.drive.presentation.iplusmessage.IplusGroupSettingsSheet
import com.novo.drive.presentation.iplusmessage.GroupMemberItem
import com.novo.drive.data.local.entity.IplusMessageGroupEntity
import com.novo.drive.presentation.iplusmessage.IplusMessageScreen
import com.novo.drive.presentation.iplusmessage.IplusMessageViewModel
import com.novo.drive.presentation.music.IplusMusicPlayerScreen
import com.novo.drive.presentation.navigation.NavigationTransitions.isPeerSwitch
import com.novo.drive.presentation.navigation.NavigationTransitions.movesForward
import com.novo.drive.presentation.note.NoteEditorScreen
import com.novo.drive.presentation.onboarding.OnboardingScreen
import com.novo.drive.presentation.platform.LocalPlatformScreens
import com.novo.drive.presentation.proxy.ProxyScreen
import com.novo.drive.presentation.search.SearchScreen
import com.novo.drive.presentation.settings.ExclusionsScreen
import com.novo.drive.presentation.settings.SettingsScreen
import com.novo.drive.presentation.settings.SettingsSectionScreen
import com.novo.drive.presentation.settings.SettingsSectionType
import com.novo.drive.presentation.stream.StreamBridgeScreen
import com.novo.drive.presentation.transfers.TransfersScreen
import com.novo.drive.presentation.trash.TrashScreen
import com.novo.drive.presentation.vault.DecoyVaultScreen
import com.novo.drive.presentation.video.IplusVideoPlayerScreen
import com.novo.drive.resources.Res
import com.novo.drive.resources.album_not_in_folder
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Route,
    modifier: Modifier = Modifier,
    verticalTabMotion: Boolean = false
) {
    val messageViewModel = koinViewModel<IplusMessageViewModel>()
    val clipboardManager = LocalClipboardManager.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            if (isPeerSwitch()) tabEnterFor(verticalTabMotion, movesForward())
            else NavigationTransitions.sharedAxisEnter(forward = true)
        },
        exitTransition = {
            if (isPeerSwitch()) tabExitFor(verticalTabMotion, movesForward())
            else NavigationTransitions.sharedAxisExit(forward = true)
        },
        popEnterTransition = {
            if (isPeerSwitch()) tabEnterFor(verticalTabMotion, movesForward())
            else NavigationTransitions.sharedAxisEnter(forward = false)
        },
        popExitTransition = {
            if (isPeerSwitch()) tabExitFor(verticalTabMotion, movesForward())
            else NavigationTransitions.sharedAxisExit(forward = false)
        }
    ) {
        composable<Route.Onboarding> {
            OnboardingScreen(
                onOpenProxy = { navController.navigateOnce(Route.Proxy) },
                onFinished = {
                    navController.navigateOnce(Route.Home) {
                        popUpTo(Route.Onboarding) { inclusive = true }
                    }
                }
            )
        }
        composable<Route.Home> {
            UnderBottomBar {
                HomeScreen(
                    onOpenFile = { id, sequence ->
                        navController.navigateOnce(sequence.routeFor(id))
                    },
                    onOpenFolder = { navController.navigateOnce(Route.Files(it)) },
                    onOpenRootFiles = { navController.navigateOnce(Route.Files()) },
                    onOpenCategory = { category ->
                        navController.navigateOnce(Route.Search(category = category.name))
                    },
                    onOpenTransfers = { navController.navigateOnce(Route.Transfers) },
                    onOpenTrash = { navController.navigateOnce(Route.Trash) },
                    onOpenChannels = { navController.navigateOnce(Route.Channels) },
                    onOpenCollection = { navController.navigateOnce(Route.Collection(it.name)) },
                    onOpenBackupSettings = {
                        navController.navigateOnce(
                            Route.SettingsSection(SettingsSectionType.BACKUP.name)
                        )
                    },
                    onOpenFamilyDashboard = { navController.navigateOnce(Route.FamilyDashboard) },
                    onOpenStorageCleaner = { navController.navigateOnce(Route.StorageCleaner) },
                    onOpenMusicPlayer = { navController.navigateOnce(Route.MusicPlayer()) },
                    onOpenStreamBridge = { navController.navigateOnce(Route.StreamBridge) },
                    onOpenDecoyVault = { navController.navigateOnce(Route.DecoyVault) },
                    onOpenLocalFileManager = { navController.navigateOnce(Route.LocalFileManager) },
                    onOpenCinema = { navController.navigateOnce(Route.Cinema) }
                )
            }
        }
        composable<Route.Files> { entry ->
            UnderBottomBar {
                val filesRoute = entry.toRoute<Route.Files>()
                val isRoot = filesRoute.folderId == null
                IplusFilesScreen(
                    focusFileId = filesRoute.focusFileId,
                    onOpenFolder = { navController.navigateOnce(Route.Files(it)) },
                    onOpenCrumb = { folderId ->
                        val route = Route.Files(folderId)
                        if (!navController.popBackStack(route, inclusive = false)) {
                            navController.navigateOnce(route)
                        }
                    },
                    onOpenFile = { id, sequence ->
                        navController.navigateOnce(sequence.routeFor(id))
                    },
                    onOpenSearch = { navController.navigateOnce(Route.Search()) },
                    onNewNote = { folderId ->
                        navController.navigateOnce(Route.NoteEditor(folderId = folderId))
                    },
                    onEditNote = { fileId, title ->
                        navController.navigateOnce(
                            Route.NoteEditor(fileId = fileId, title = title)
                        )
                    },
                    onShareToDewanGram = { paths ->
                        messageViewModel.setPendingShare(paths.firstOrNull())
                        navController.navigateOnce(Route.DewanGram)
                    },
                    onBack = if (isRoot) null else ({ navController.popBackStackOnce() })
                )
            }
        }
        composable<Route.Gallery> {
            UnderBottomBar {
                val unfiledAlbumName = stringResource(Res.string.album_not_in_folder)
                GalleryScreen(
                    onOpenFile = { id, sequence ->
                        navController.navigateOnce(sequence.routeFor(id))
                    },
                    onOpenAlbum = { album ->
                        navController.navigateOnce(
                            Route.GalleryAlbum(album.folderId, album.name ?: unfiledAlbumName)
                        )
                    }
                )
            }
        }
        composable<Route.GalleryAlbum> {
            GalleryScreen(
                onOpenFile = { id, sequence ->
                    navController.navigateOnce(sequence.routeFor(id))
                },
                onBack = { navController.popBackStackOnce() }
            )
        }
        composable<Route.Preview>(
            enterTransition = { NavigationTransitions.previewEnter() },
            exitTransition = { NavigationTransitions.fadeThroughExit() },
            popEnterTransition = { NavigationTransitions.fadeThroughEnter() },
            popExitTransition = { NavigationTransitions.previewExit() }
        ) {
            LocalPlatformScreens.current.Preview(
                onBack = { navController.popBackStackOnce() },
                onEditNote = { fileId, title ->
                    navController.navigateOnce(
                        Route.NoteEditor(fileId = fileId, title = title)
                    )
                },
                onShareToDewanGram = { fileId ->
                    messageViewModel.setPendingShare(fileId)
                    navController.navigateOnce(Route.DewanGram)
                }
            )
        }
        composable<Route.Transfers> {
            TransfersScreen(onBack = { navController.popBackStackOnce() })
        }
        composable<Route.Search> { entry ->
            val searchRoute = entry.toRoute<Route.Search>()
            SearchScreen(
                initialCategory = searchRoute.category?.let { runCatching { FileCategory.valueOf(it) }.getOrNull() },
                onBack = { navController.popBackStackOnce() },
                onOpenFile = { id, sequence ->
                    navController.navigateOnce(sequence.routeFor(id))
                },
                onOpenFolder = { folderId, fileId ->
                    navController.navigateOnce(Route.Files(folderId, fileId))
                }
            )
        }
        composable<Route.Collection> {
            CollectionScreen(
                onBack = { navController.popBackStackOnce() },
                onOpenFile = { id, sequence ->
                    navController.navigateOnce(sequence.routeFor(id))
                },
                onOpenFolder = { navController.navigateOnce(Route.Files(it)) }
            )
        }
        composable<Route.NoteEditor> {
            NoteEditorScreen(onBack = { navController.popBackStackOnce() })
        }
        composable<Route.Channels> {
            ChannelsScreen(onBack = { navController.popBackStackOnce() })
        }
        composable<Route.Trash> {
            TrashScreen(onBack = { navController.popBackStackOnce() })
        }
        composable<Route.Settings> {
            UnderBottomBar {
                SettingsScreen(
                    onBack = null,
                    onOpenSection = { section ->
                        navController.navigateOnce(Route.SettingsSection(section.name))
                    },
                    onOpenFamilyDashboard = {
                        navController.navigateOnce(Route.FamilyDashboard)
                    },
                    onOpenAdminDashboard = {
                        navController.navigateOnce(Route.AdminDashboard)
                    }
                )
            }
        }
        composable<Route.FamilyDashboard> {
            FamilyDashboardScreen(onBack = { navController.popBackStackOnce() })
        }
        composable<Route.StorageCleaner> {
            StorageCleanerScreen(
                onBack = { navController.popBackStackOnce() },
                onOpenFile = { id, sequence ->
                    navController.navigateOnce(sequence.routeFor(id))
                }
            )
        }
        composable<Route.MusicPlayer> { entry ->
            val route = entry.toRoute<Route.MusicPlayer>()
            IplusMusicPlayerScreen(
                initialFileId = route.initialFileId,
                onBack = { navController.popBackStackOnce() }
            )
        }
        composable<Route.StreamBridge> {
            StreamBridgeScreen(onBack = { navController.popBackStackOnce() })
        }
        composable<Route.DecoyVault> {
            DecoyVaultScreen(
                onBack = { navController.popBackStackOnce() },
                onOpenFile = { id, sequence -> navController.navigateOnce(sequence.routeFor(id)) },
                onOpenFolder = { folderId -> navController.navigateOnce(Route.Files(folderId = folderId)) }
            )
        }
        composable<Route.LocalFileManager> {
            IplusLocalFileManagerScreen(onBack = { navController.popBackStackOnce() })
        }
        composable<Route.Cinema> {
            IplusVideoPlayerScreen(
                onBack = { navController.popBackStackOnce() },
                onOpenFile = { id, sequence ->
                    navController.navigateOnce(sequence.routeFor(id))
                }
            )
        }
        composable<Route.SettingsSection> { entry ->
            val route = entry.toRoute<Route.SettingsSection>()
            val section = SettingsSectionType.entries
                .firstOrNull { it.name == route.section }
                ?: SettingsSectionType.ACCOUNT
            SettingsSectionScreen(
                section = section,
                onOpenChannels = { navController.navigateOnce(Route.Channels) },
                onBack = { navController.popBackStackOnce() },
                onOpenExclusions = { navController.navigateOnce(Route.Exclusions) },
                onOpenProxy = { navController.navigateOnce(Route.Proxy) },
                onLoggedOut = {
                    navController.navigateOnce(Route.Onboarding) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable<Route.Exclusions> {
            ExclusionsScreen(onBack = { navController.popBackStackOnce() })
        }
        composable<Route.Proxy> {
            ProxyScreen(onBack = { navController.popBackStackOnce() })
        }
        composable<Route.AdminDashboard> {
            KunjoAdminDashboardScreen(
                onBack = { navController.popBackStackOnce() }
            )
        }
        composable<Route.DewanGram> {
            UnderBottomBar {
                val contacts by messageViewModel.contacts.collectAsStateWithLifecycle()
                val groups by messageViewModel.groups.collectAsStateWithLifecycle()
                val searchResults by messageViewModel.searchResults.collectAsStateWithLifecycle()
                val blockedContacts by messageViewModel.blockedContacts.collectAsStateWithLifecycle()
                val pendingShareFile by messageViewModel.pendingShareFile.collectAsStateWithLifecycle()
                val prefs by messageViewModel.preferences.collectAsStateWithLifecycle()

                IplusMessageScreen(
                    contacts = contacts,
                    groups = groups,
                    searchResults = searchResults,
                    blockedContacts = blockedContacts,
                    pendingShareFile = pendingShareFile,
                    isFamilyMember = prefs.isFamilyMember,
                    kunjoUsername = prefs.kunjoUsername,
                    familyMemberName = prefs.familyMemberName,
                    onClearPendingShare = { messageViewModel.clearPendingShare() },
                    onOpenChat = { chatId, title, isGroup ->
                        navController.navigateOnce(
                            Route.DewanChatDetail(
                                chatId = chatId,
                                title = title,
                                isGroup = isGroup
                            )
                        )
                    },
                    onCreateGroup = { title, username ->
                        messageViewModel.createGroup(title, username) { newGroupId ->
                            navController.navigateOnce(
                                Route.DewanChatDetail(
                                    chatId = newGroupId,
                                    title = title,
                                    isGroup = true
                                )
                            )
                        }
                    },
                    onSearchUser = { query ->
                        messageViewModel.searchUsers(query)
                    },
                    onUnblockUser = { userId ->
                        messageViewModel.unblockUser(userId)
                    }
                )
            }
        }
        composable<Route.DewanChatDetail> { entry ->
            val route = entry.toRoute<Route.DewanChatDetail>()
            val messages by messageViewModel.observeMessages(route.chatId).collectAsStateWithLifecycle(emptyList())
            val recentDriveFiles by messageViewModel.recentDriveFiles.collectAsStateWithLifecycle()
            val groups by messageViewModel.groups.collectAsStateWithLifecycle()
            val prefs by messageViewModel.preferences.collectAsStateWithLifecycle()
            val pendingShareFile by messageViewModel.pendingShareFile.collectAsStateWithLifecycle()
            var showGroupSettings by remember { mutableStateOf(false) }

            val currentGroup = groups.firstOrNull { it.groupId == route.chatId }
            val isReadOnly = currentGroup?.isReadOnly ?: false
            val canSendInReadOnly = (currentGroup == null) || (currentGroup.ownerId == 0L) ||
                    currentGroup.adminIds.split(",").contains("0") ||
                    currentGroup.allowedSenderIds.split(",").contains("0")

            LaunchedEffect(pendingShareFile) {
                pendingShareFile?.let { path ->
                    messageViewModel.sendMessage(
                        chatId = route.chatId,
                        text = "",
                        driveLink = path,
                        isViewOnce = false,
                        contactName = route.title
                    )
                    messageViewModel.clearPendingShare()
                }
            }

            IplusChatDetailScreen(
                chatId = route.chatId,
                title = route.title,
                isGroup = route.isGroup,
                messages = messages,
                isFamilyMember = prefs.isFamilyMember,
                recentDriveFiles = recentDriveFiles,
                isReadOnly = isReadOnly,
                canSendInReadOnly = canSendInReadOnly,
                onBack = { navController.popBackStackOnce() },
                onSendMessage = { text, driveLink, isViewOnce ->
                    messageViewModel.sendMessage(
                        chatId = route.chatId,
                        text = text,
                        driveLink = driveLink,
                        isViewOnce = isViewOnce,
                        contactName = route.title
                    )
                },
                onOpenGroupSettings = { showGroupSettings = true },
                onPlayVideo = { _, _ ->
                    navController.navigateOnce(Route.Cinema)
                },
                onPlayAudio = { fileId, _ ->
                    navController.navigateOnce(Route.MusicPlayer(initialFileId = fileId))
                },
                onOpenPdf = { fileId, _ ->
                    navController.navigateOnce(Route.Preview(fileId = fileId))
                },
                onSaveToMyDrive = { _ -> }
            )

            if (showGroupSettings && route.isGroup) {
                val groupEntity = currentGroup ?: IplusMessageGroupEntity(
                    groupId = route.chatId,
                    title = route.title,
                    ownerId = 0L,
                    memberCount = 1,
                    inviteLink = "dewan://join/g_${route.chatId}"
                )
                val adminSet = groupEntity.adminIds.split(",").filter { it.isNotBlank() }.toSet()
                val allowedSet = groupEntity.allowedSenderIds.split(",").filter { it.isNotBlank() }.toSet()
                val memberList = listOf(
                    GroupMemberItem(
                        userId = 0L,
                        name = "You",
                        username = "me",
                        role = if (groupEntity.ownerId == 0L) "OWNER" else if (adminSet.contains("0")) "ADMIN" else "MEMBER",
                        isAllowedSender = allowedSet.contains("0"),
                        avatarEmoji = "👑"
                    )
                )
                IplusGroupSettingsSheet(
                    group = groupEntity,
                    currentUserId = 0L,
                    members = memberList,
                    onDismiss = { showGroupSettings = false },
                    onAddMember = { },
                    onRemoveMember = { memberId ->
                        messageViewModel.removeMemberWithRoleCheck(route.chatId, 0L, memberId)
                    },
                    onBlockMember = { memberId -> messageViewModel.blockUser(memberId) },
                    onUpdateTitle = { newTitle -> messageViewModel.updateGroupTitle(route.chatId, newTitle) },
                    onUpdateUsername = { username -> messageViewModel.updateGroupUsername(route.chatId, username) },
                    onToggleReadOnly = { readOnly -> messageViewModel.setGroupReadOnly(route.chatId, readOnly) },
                    onToggleAdmin = { memberId, makeAdmin -> messageViewModel.setMemberAdminRole(route.chatId, memberId, makeAdmin) },
                    onToggleSendPermission = { memberId, allowed -> messageViewModel.setMemberSendPermission(route.chatId, memberId, allowed) },
                    onCopyInviteLink = { link ->
                        clipboardManager.setText(AnnotatedString(link))
                    },
                    onDeleteGroup = {
                        messageViewModel.deleteGroup(route.chatId)
                        showGroupSettings = false
                        navController.popBackStackOnce()
                    }
                )
            }
        }
    }
}

private fun tabEnterFor(vertical: Boolean, forward: Boolean) =
    if (vertical) NavigationTransitions.tabEnterVertical(forward)
    else NavigationTransitions.tabEnter(forward)

private fun tabExitFor(vertical: Boolean, forward: Boolean) =
    if (vertical) NavigationTransitions.tabExitVertical(forward)
    else NavigationTransitions.tabExit(forward)

/** Read per screen, so an outgoing screen's padding stays steady while it animates away. */
@Composable
private fun UnderBottomBar(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalBottomBarInset provides LocalBottomBarHeight.current,
        content = content
    )
}
