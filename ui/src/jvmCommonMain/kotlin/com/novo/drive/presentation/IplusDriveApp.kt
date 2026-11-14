package com.novo.drive.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocaleList
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import com.novo.drive.core.common.NotificationDestinations
import com.novo.drive.core.files.PendingShare
import com.novo.drive.domain.model.AppLanguage
import com.novo.drive.domain.model.AppTheme
import com.novo.drive.presentation.applock.LockScreen
import com.novo.drive.presentation.components.LoadingState
import com.novo.drive.presentation.components.LocalCompactLayout
import com.novo.drive.presentation.components.SessionBrokenDialog
import com.novo.drive.presentation.components.UpdateDialog
import com.novo.drive.presentation.components.glass.OpticalChromeBackdropHost
import com.novo.drive.presentation.components.glass.OpticalGlassBackdropHost
import com.novo.drive.presentation.navigation.AppNavHost
import com.novo.drive.presentation.navigation.BottomBarHeight
import com.novo.drive.presentation.navigation.LocalBottomBarHeight
import com.novo.drive.presentation.navigation.Route
import com.novo.drive.presentation.navigation.TopLevelDestination
import com.novo.drive.presentation.navigation.navigateOnce
import com.novo.drive.presentation.platform.LocalAppVersion
import com.novo.drive.presentation.platform.LocalUrlOpener
import com.novo.drive.presentation.splash.IplusSplashScreen
import com.novo.drive.presentation.theme.IplusTheme
import com.novo.drive.presentation.theme.KunjoDriveTheme
import com.novo.drive.presentation.theme.systemDarkTheme
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IplusDriveApp(
    pendingShare: PendingShare,
    notificationDestination: String? = null,
    onDestinationHandled: () -> Unit = {},
    viewModel: AppViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onAppStopped()
                Lifecycle.Event.ON_START -> viewModel.onAppStarted()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val darkTheme = when (state.theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK,
        AppTheme.CYBER_GUARD,
        AppTheme.HUSSUU_CYBER,
        AppTheme.MINIMAL_DRIVE,
        AppTheme.DONUT_NEON -> true
        AppTheme.SYSTEM -> systemDarkTheme()
    }

    val pendingUpdate by viewModel.pendingUpdate.collectAsStateWithLifecycle()
    val sessionBroken by viewModel.sessionBroken.collectAsStateWithLifecycle()
    val urlOpener = LocalUrlOpener.current

    val systemLocaleList = LocalLocaleList.current
    // Language changes must fully remeasure the UI tree, so force a new composition
    // with a changing key rather than only swapping the locale composition local.
    val targetLocaleList = remember(state.language, systemLocaleList) {
        when (state.language) {
            AppLanguage.SYSTEM -> systemLocaleList
            AppLanguage.ENGLISH -> LocaleList(Locale("en"))
            AppLanguage.BENGALI -> LocaleList(Locale("bn"))
        }
    }
    val localeKey = remember(state.language) {
        "locale-${state.language.code}-${System.currentTimeMillis()}"
    }

    val systemDefault = remember { java.util.Locale.getDefault() }
    remember(state.language) {
        java.util.Locale.setDefault(
            when (state.language) {
                AppLanguage.SYSTEM -> systemDefault
                AppLanguage.ENGLISH -> java.util.Locale.forLanguageTag("en")
                AppLanguage.BENGALI -> java.util.Locale.forLanguageTag("bn")
            }
        )
        true
    }

    KunjoDriveTheme(appTheme = state.theme, darkTheme = darkTheme, dynamicColor = state.dynamicColor) {
        CompositionLocalProvider(
            (LocalLocaleList as ProvidableCompositionLocal<LocaleList>) provides targetLocaleList,
            LocalCompactLayout provides state.compactLayout
        ) {
            AppContentRoot(key = localeKey) {
                var showSplash by remember { mutableStateOf(true) }

            when {
                showSplash -> IplusSplashScreen(
                    onSplashFinished = { showSplash = false }
                )
                state.loading -> LoadingState()
                state.locked -> LockScreen(onUnlocked = viewModel::unlock)
                else -> MainScaffold(
                    pendingShare = pendingShare,
                    onboardingComplete = state.onboardingComplete,
                    notificationDestination = notificationDestination,
                    onDestinationHandled = onDestinationHandled,
                    onUpdateRequested = { viewModel.checkForUpdate(force = true) },
                    driveMissing = viewModel.driveMissing
                )
            }
            if (sessionBroken) {
                SessionBrokenDialog(onSignInAgain = viewModel::resetSession)
            }
            pendingUpdate?.takeIf { !state.loading && !state.locked }?.let { release ->
                UpdateDialog(
                    currentVersion = LocalAppVersion.current,
                    onOpenUrl = urlOpener::open,
                    release = release,
                    onDownload = {
                        viewModel.dismissUpdate()
                        urlOpener.open(release.pageUrl)
                    },
                    onDismiss = viewModel::dismissUpdate
                )
            }
            }
        }
    }
}

@Composable
private fun AppContentRoot(
    key: String,
    content: @Composable () -> Unit
) {
    // Recreate the whole content subtree when the language changes so every
    // stringResource / bilingual call site re-resolves with the new locale.
    // The `key` parameter makes Compose treat AppContentRoot as a new,
    // distinct composable when the language changes, disposing all the
    // `remember` state in the content slot and re-running it fresh.
    val _key = key
    Box(modifier = Modifier.fillMaxSize()) {
        content()
    }
}

@Composable
private fun MainScaffold(
    pendingShare: PendingShare,
    onboardingComplete: Boolean,
    notificationDestination: String?,
    onDestinationHandled: () -> Unit,
    onUpdateRequested: () -> Unit,
    driveMissing: Flow<Unit>
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val isTopLevel = TopLevelDestination.entries.any { destination ->
        currentDestination?.hasRoute(destination.route::class) == true
    }
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val useRail = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    )

    val startDestination: Route = if (onboardingComplete) Route.Home else Route.Onboarding

    LaunchedEffect(Unit) {
        driveMissing.collect {
            val graphReady = withTimeoutOrNull(NAV_READY_TIMEOUT_MS.milliseconds) {
                while (navController.currentBackStackEntry == null) delay(NAV_READY_POLL_MS.milliseconds)
                true
            } == true
            if (graphReady) navController.navigate(Route.Channels)
        }
    }

    LaunchedEffect(onboardingComplete) {
        if (onboardingComplete) return@LaunchedEffect
        val graphReady = withTimeoutOrNull(NAV_READY_TIMEOUT_MS.milliseconds) {
            while (navController.currentBackStackEntry == null) delay(NAV_READY_POLL_MS.milliseconds)
            true
        } == true
        if (!graphReady) return@LaunchedEffect
        if (navController.currentDestination?.hasRoute(Route.Onboarding::class) == true) {
            return@LaunchedEffect
        }
        navController.navigate(Route.Onboarding) { popUpTo(0) { inclusive = true } }
    }

    LaunchedEffect(notificationDestination, onboardingComplete) {
        val target = notificationDestination ?: return@LaunchedEffect
        if (!onboardingComplete) return@LaunchedEffect

        val graphReady = withTimeoutOrNull(NAV_READY_TIMEOUT_MS.milliseconds) {
            while (navController.currentBackStackEntry == null) delay(NAV_READY_POLL_MS.milliseconds)
            true
        } == true
        if (!graphReady) return@LaunchedEffect

        when {
            target == NotificationDestinations.TRANSFERS -> navController.navigate(Route.Transfers)
            target == NotificationDestinations.FILES -> navController.navigate(Route.Files())
            target == NotificationDestinations.NOTE -> navController.navigate(
                Route.NoteEditor(sharedText = pendingShare.text.value)
            )
            target == NotificationDestinations.UPDATE -> onUpdateRequested()
            target.startsWith(NotificationDestinations.DEWAN_JOIN_PREFIX) -> {
                val rawJoin = target.removePrefix(NotificationDestinations.DEWAN_JOIN_PREFIX)
                pendingShare.offerText("__dewan_join__$rawJoin")
                navController.navigate(Route.DewanGram)
            }
            else -> Unit
        }
        onDestinationHandled()
    }

    val palette = IplusTheme.palette

    OpticalGlassBackdropHost(
        modifier = Modifier.fillMaxSize(),
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.backgroundBrush)
            ) {
                Box(
                    modifier = Modifier
                        .size(360.dp)
                        .offset(x = (-90).dp, y = (-70).dp)
                        .background(
                            Brush.radialGradient(
                                listOf(palette.accentPrimary.copy(alpha = 0.16f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(340.dp)
                        .offset(x = 80.dp, y = 70.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(palette.accentSecondary.copy(alpha = 0.14f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
            }
        }
    ) {
        if (useRail && isTopLevel) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = palette.surfaceColor.copy(alpha = 0.85f)
                ) {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected =
                            currentDestination?.hasRoute(destination.route::class) == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { if (!selected) navigateTopLevel(navController, destination) },
                            icon = {
                                Icon(
                                    if (selected) destination.selectedIcon else destination.icon,
                                    contentDescription = stringResource(destination.labelRes),
                                    tint = if (selected) palette.accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) }
                        )
                    }
                }
                AppNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier
                        .fillMaxSize()
                        .consumeWindowInsets(WindowInsets.systemBars.only(WindowInsetsSides.Start)),
                    verticalTabMotion = true
                )
            }
        } else {
            OpticalChromeBackdropHost(
                modifier = Modifier.fillMaxSize(),
                pageContent = {
                    CompositionLocalProvider(
                        LocalBottomBarHeight provides BottomBarHeight
                    ) {
                        AppNavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                chromeOverlay = {
                    var navSlots by remember { mutableStateOf(emptyMap<Int, DpRect>()) }
                    val topLevelIndex = TopLevelDestination.entries.indexOfFirst { destination ->
                        currentDestination?.hasRoute(destination.route::class) == true
                    }
                    var selectedNavIndex by remember { mutableIntStateOf(0) }
                    LaunchedEffect(topLevelIndex) {
                        if (topLevelIndex >= 0) selectedNavIndex = topLevelIndex
                    }
                    AnimatedVisibility(
                        visible = isTopLevel,
                        enter = fadeIn(tween(BAR_SLIDE_MS)),
                        exit = fadeOut(tween(BAR_SLIDE_MS)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        val barPalette = IplusTheme.palette
                        val isLight = barPalette.theme == AppTheme.LIGHT
                        val scrimBg = if (isLight) Color(0xFFF8FAFC) else barPalette.backgroundColor
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(BottomBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            scrimBg.copy(alpha = 0.65f),
                                            scrimBg.copy(alpha = 0.94f),
                                            scrimBg
                                        )
                                    )
                                )
                        )
                    }
                    AnimatedVisibility(
                        visible = isTopLevel,
                        enter = slideInVertically(barSlideSpec()) { height -> height } +
                                fadeIn(tween(BAR_SLIDE_MS)),
                        exit = slideOutVertically(barSlideSpec()) { height -> height } +
                                fadeOut(tween(BAR_SLIDE_MS)),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        FloatingNavigationBar(
                            selectedIndex = selectedNavIndex,
                            onNavigate = { navigateTopLevel(navController, it) },
                            slots = navSlots,
                            onSlotMeasured = { index, slot ->
                                if (navSlots[index] != slot) navSlots = navSlots + (index to slot)
                            }
                        )
                    }
                }
            )
        }
    }
}

/** A single pill slides between items instead of each item drawing its own indicator. */
@Composable
private fun FloatingNavigationBar(
    selectedIndex: Int,
    onNavigate: (TopLevelDestination) -> Unit,
    slots: Map<Int, DpRect>,
    onSlotMeasured: (Int, DpRect) -> Unit
) {
    val entries = TopLevelDestination.entries
    val density = LocalDensity.current
    val selectedSlot = slots[selectedIndex]

    val pillX by animateDpAsState(
        targetValue = selectedSlot?.left ?: 0.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "pillX"
    )
    val pillWidth by animateDpAsState(
        targetValue = selectedSlot?.width ?: 0.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "pillWidth"
    )

    val palette = IplusTheme.palette
    val isLight = palette.theme == AppTheme.LIGHT
    val barBgColor = if (isLight) Color(0xFFF1F5F9) else Color(0xFF0F1523)

    Box(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = BAR_MARGIN, vertical = BAR_MARGIN)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(BAR_CORNER),
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(BAR_CORNER))
            .background(barBgColor.copy(alpha = 0.94f))
            .border(
                width = 1.dp,
                color = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(BAR_CORNER)
            )
    ) {
        Box(
            modifier = Modifier
                .height(BAR_HEIGHT)
                .padding(horizontal = BAR_PADDING),
            contentAlignment = Alignment.CenterStart
        ) {
            if (selectedSlot != null) {
                Box(
                    modifier = Modifier
                        .offset(x = pillX)
                        .width(pillWidth)
                        .height(PILL_HEIGHT)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (isLight) Color.White else Color(0xFF1E2638)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(22.dp)
                        )
                )
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                entries.forEachIndexed { index, destination ->
                    NavigationPillItem(
                        destination = destination,
                        selected = index == selectedIndex,
                        onClick = { if (index != selectedIndex) onNavigate(destination) },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val left = with(density) {
                                coordinates.positionInParent().x.toDp()
                            }
                            val width = with(density) { coordinates.size.width.toDp() }
                            onSlotMeasured(index, DpRect(left, width))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationPillItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            if (isDark) Color.White else Color(0xFF0F172A)
        } else {
            if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        },
        label = "navItemColor"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (selected) 12.dp else 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) destination.selectedIcon else destination.icon,
            contentDescription = stringResource(destination.labelRes),
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(destination.labelRes),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = contentColor,
                    maxLines = 1
                )
            }
        }
    }
}

private fun barSlideSpec() = tween<IntOffset>(
    durationMillis = BAR_SLIDE_MS,
    easing = FastOutSlowInEasing
)

private val BAR_CORNER = 32.dp
private val BAR_MARGIN = 8.dp
private val BAR_HEIGHT = 64.dp
private val PILL_HEIGHT = 44.dp
private val BAR_PADDING = (BAR_HEIGHT - PILL_HEIGHT) / 2

private fun navigateTopLevel(
    navController: NavHostController,
    destination: TopLevelDestination
) {
    navController.navigateOnce(destination.route) {
        popUpTo(Route.Home) { inclusive = destination.route == Route.Home }
        launchSingleTop = true
    }
}

private const val BAR_SLIDE_MS = 450

private const val NAV_READY_TIMEOUT_MS = 5_000L
private const val NAV_READY_POLL_MS = 50L

