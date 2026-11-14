package com.novo.drive.presentation.stream

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.novo.drive.core.stream.LocalStreamBridgeServer
import com.novo.drive.core.stream.StreamBridgeStatus
import com.novo.drive.domain.model.AppTheme
import com.novo.drive.presentation.common.CollectSnackbarMessages
import com.novo.drive.presentation.common.UiText
import com.novo.drive.presentation.components.BottomBarSnackbarHost
import com.novo.drive.presentation.components.QrCode
import com.novo.drive.presentation.components.glass.GlassPillButton
import com.novo.drive.presentation.components.glass.LiquidGlassCard
import com.novo.drive.presentation.components.glass.LiquidGlassSwitch
import com.novo.drive.presentation.components.liftedTopAppBarColors
import com.novo.drive.presentation.components.rememberToolbarLift
import com.novo.drive.presentation.theme.LocalIplusPalette
import com.novo.drive.resources.Res
import com.novo.drive.resources.common_back
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

class StreamBridgeViewModel(
    private val streamServer: LocalStreamBridgeServer
) : ViewModel() {

    val status: StateFlow<StreamBridgeStatus> = streamServer.status
    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    fun setRunning(enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                val active = streamServer.start()
                _messages.tryEmit(
                    UiText.Plain(
                        com.novo.drive.core.common.bi(
                            "Wi-Fi Stream started: ${active.browserUrl}",
                            "Wi-Fi Stream চালু হয়েছে: ${active.browserUrl}",
                        )
                    )
                )
            } else {
                streamServer.stop()
                _messages.tryEmit(
                    UiText.Plain(
                        com.novo.drive.core.common.bi(
                            "Wi-Fi Stream stopped",
                            "Wi-Fi Stream বন্ধ করা হয়েছে",
                        )
                    )
                )
            }
        }
    }

    fun regeneratePin() {
        viewModelScope.launch {
            val pin = streamServer.regeneratePin()
            _messages.tryEmit(
                UiText.Plain(
                    com.novo.drive.core.common.bi(
                        "New 4-digit stream PIN: $pin",
                        "নতুন ৪-ডিজিট স্ট্রিম পিন: $pin",
                    )
                )
            )
        }
    }

    fun notifyCopied(label: String) {
        _messages.tryEmit(
            UiText.Plain(
                com.novo.drive.core.common.bi(
                    "$label copied!",
                    "$label কপি করা হয়েছে!",
                )
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamBridgeScreen(
    onBack: () -> Unit,
    viewModel: StreamBridgeViewModel = koinViewModel()
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val palette = LocalIplusPalette.current
    val isLight = palette.theme == AppTheme.LIGHT
    val isBn = com.novo.drive.presentation.common.isBengaliUi()
    val primaryText = if (isLight) Color(0xFF0F172A) else Color.White
    val secondaryText = if (isLight) Color(0xFF475569) else Color.White.copy(alpha = 0.68f)
    val clipboard = LocalClipboardManager.current

    val snackbarHostState = remember { SnackbarHostState() }
    CollectSnackbarMessages(viewModel.messages, snackbarHostState)

    val scrollState = rememberScrollState()
    val lifted by rememberToolbarLift(scrollState)

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { BottomBarSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = liftedTopAppBarColors(lifted),
                title = {
                    Text(
                        text = "Wi-Fi Smart TV & PC Stream",
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.common_back),
                            tint = primaryText
                        )
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                glowColor = if (status.running) palette.accentSecondary else palette.accentPrimary
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = if (status.running) Icons.Filled.Cast else Icons.Filled.Tv,
                            contentDescription = null,
                            tint = if (status.running) palette.accentSecondary else palette.accentPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                        Column {
                            Text(
                                text = if (status.running) {
                                    if (isBn) "লোকাল ওয়াই-ফাই স্ট্রিম চালু" else "Local Wi-Fi Stream Active"
                                } else {
                                    if (isBn) "স্ট্রিম ব্রিজ বন্ধ রয়েছে" else "Stream Bridge Offline"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = primaryText
                            )
                            Text(
                                text = if (status.running) {
                                    if (isBn) {
                                        "সক্রিয় কানেকশন: ${status.activeConnections} • HTTP Range ও WebDAV প্রস্তুত"
                                    } else {
                                        "Active connections: ${status.activeConnections} • HTTP Range & WebDAV ready"
                                    }
                                } else {
                                    if (isBn) {
                                        "চালু করলে একই ওয়াই-ফাইতে থাকা Smart TV, VLC ও PC ব্রাউজারে ক্লাউড ভিডিও চলবে"
                                    } else {
                                        "Enable to stream cloud videos to Smart TV, VLC, and PC browsers on the same Wi-Fi"
                                    }
                                },
                                fontSize = 12.sp,
                                color = secondaryText
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    LiquidGlassSwitch(
                        checked = status.running,
                        onCheckedChange = viewModel::setRunning
                    )
                }
            }

            if (status.running) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.size(196.dp)
                        ) {
                            QrCode(
                                content = status.browserUrl,
                                contentDescription = "Smart TV Stream QR",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            )
                        }
                        Text(
                            text = if (isBn) "৪-ডিজিট অ্যাক্সেস পিন: ${status.pin}" else "4-Digit Access PIN: ${status.pin}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = palette.accentSecondary
                        )
                        GlassPillButton(
                            text = if (isBn) "নতুন পিন তৈরি করুন" else "Generate New PIN",
                            icon = Icons.Filled.Refresh,
                            onClick = viewModel::regeneratePin
                        )
                    }
                }

                StreamEndpointCard(
                    label = "Smart TV / Browser Stream URL",
                    url = status.browserUrl,
                    hint = if (isBn) "টিভি বা পিসির যেকোনো ব্রাউজারে খুলুন" else "Open in any TV or PC web browser",
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onCopy = {
                        clipboard.setText(AnnotatedString(status.browserUrl))
                        viewModel.notifyCopied("Browser Stream URL")
                    }
                )

                StreamEndpointCard(
                    label = "VLC / Kodi / PC WebDAV URL",
                    url = status.webDavUrl,
                    hint = if (isBn) "Password হিসেবে ৪-ডিজিট পিন (${status.pin}) ব্যবহার করুন" else "Use the 4-digit PIN (${status.pin}) as the password",
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onCopy = {
                        clipboard.setText(AnnotatedString(status.webDavUrl))
                        viewModel.notifyCopied("WebDAV URL")
                    }
                )
            }
            Spacer(Modifier.height(24.dp + padding.calculateBottomPadding()))
        }
    }
}

@Composable
private fun StreamEndpointCard(
    label: String,
    url: String,
    hint: String,
    primaryText: Color,
    secondaryText: Color,
    onCopy: () -> Unit
) {
    val palette = LocalIplusPalette.current
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = secondaryText)
            Text(
                text = url,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = palette.accentPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hint,
                    fontSize = 11.sp,
                    color = primaryText.copy(alpha = 0.75f),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                GlassPillButton(
                    text = "Copy",
                    icon = Icons.Filled.ContentCopy,
                    onClick = onCopy
                )
            }
        }
    }
}

