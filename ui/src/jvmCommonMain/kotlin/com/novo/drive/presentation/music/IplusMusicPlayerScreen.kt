package com.novo.drive.presentation.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocaleList
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.novo.drive.core.bridge.AudioPlaybackState
import com.novo.drive.core.bridge.AudioTrackItem
import com.novo.drive.core.bridge.KunjoBridge
import com.novo.drive.domain.model.AppTheme
import com.novo.drive.domain.model.DriveFile
import com.novo.drive.domain.repository.FileRepository
import com.novo.drive.presentation.common.Formatters
import com.novo.drive.presentation.components.EmptyState
import com.novo.drive.presentation.components.glass.LiquidGlassSearchBar
import com.novo.drive.presentation.theme.AcousticStudioPalette
import com.novo.drive.presentation.theme.DedicatedAcousticStudioTheme
import com.novo.drive.presentation.theme.IplusTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

enum class MusicStudioTab { SONGS, CALL_RECORDS, VOICE_NOTES, PLAYLISTS }

private val CALL_RECORD_REGEX = Regex("(?i).*(call|recording|_20\\d{6}_|sound_recorder|record|audiorecord).*")
private val VOICE_NOTE_REGEX = Regex("(?i).*(ptt|opus|voice|note|audiomessage|telegram|whatsapp).*")
private val STRIP_AUDIO_EXT_REGEX = Regex("\\.(mp3|m4a|flac|wav|ogg|aac|opus|wma)$", RegexOption.IGNORE_CASE)

class IplusMusicPlayerViewModel(
    private val fileRepository: FileRepository
) : ViewModel() {
    val tracks: StateFlow<List<DriveFile>> = fileRepository.observeAudioFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite(fileId: String, favorite: Boolean) {
        viewModelScope.launch {
            fileRepository.setFilesFavorite(listOf(fileId), favorite)
        }
    }
}

/**
 * IplusMusicPlayerScreen: Frosted Liquid Glass Audio Player powered by iPlus Media Engine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IplusMusicPlayerScreen(
    onBack: () -> Unit,
    initialFileId: String? = null,
    viewModel: IplusMusicPlayerViewModel = koinViewModel(),
    kunjoBridge: KunjoBridge = koinInject()
) {
    DedicatedAcousticStudioTheme {
        val isBn = LocalLocaleList.current.firstOrNull()?.language?.startsWith("bn") == true
        val allTracks by viewModel.tracks.collectAsStateWithLifecycle()
        val currentAudio by kunjoBridge.media.currentAudio.collectAsStateWithLifecycle()
        val isShuffle by kunjoBridge.media.isShuffleEnabled.collectAsStateWithLifecycle()
        val repeatMode by kunjoBridge.media.repeatMode.collectAsStateWithLifecycle()
        val audioSessionId by kunjoBridge.media.audioSessionId.collectAsStateWithLifecycle()
        val eqPreset by kunjoBridge.media.equalizerPreset.collectAsStateWithLifecycle()
        val playbackSpeed by kunjoBridge.media.playbackSpeed.collectAsStateWithLifecycle()

        var activeTab by remember { mutableStateOf(MusicStudioTab.SONGS) }
        var searchQuery by remember { mutableStateOf("") }
        var showNowPlaying by remember { mutableStateOf(initialFileId != null || currentAudio != null) }
        var showQueueSheet by remember { mutableStateOf(false) }
        var showEqualizerSheet by remember { mutableStateOf(false) }
        var showSleepTimerDialog by remember { mutableStateOf(false) }
        var showLyricsView by remember { mutableStateOf(false) }

        val currentTrack = remember(allTracks, currentAudio?.fileId) {
            allTracks.firstOrNull { it.id == currentAudio?.fileId }
        }
        val isFavorite = currentTrack?.isFavorite == true

        LaunchedEffect(initialFileId, allTracks) {
            if (initialFileId != null && allTracks.isNotEmpty()) {
                val target = allTracks.firstOrNull { it.id == initialFileId }
                if (target != null && currentAudio?.fileId != target.id) {
                    val queueItems = allTracks.map {
                        AudioTrackItem(it.id, it.name, sizeBytes = it.sizeBytes, mimeType = it.mimeType)
                    }
                    val targetIndex = allTracks.indexOf(target).coerceAtLeast(0)
                    kunjoBridge.media.playQueue(queueItems, targetIndex)
                    showNowPlaying = true
                }
            }
        }

        // Sleep Timer with smooth 10-second volume fade-out before pausing
        var activeSleepTimerMinutes by remember { mutableIntStateOf(0) }
        LaunchedEffect(activeSleepTimerMinutes) {
            if (activeSleepTimerMinutes > 0) {
                val totalMs = activeSleepTimerMinutes * 60_000L
                val fadeMs = 10_000L.coerceAtMost(totalMs)
                val preFadeMs = totalMs - fadeMs
                if (preFadeMs > 0) {
                    delay(preFadeMs)
                }
                for (step in 10 downTo 1) {
                    delay(fadeMs / 10L)
                    kunjoBridge.media.setPlaybackVolume(step / 10f)
                }
                kunjoBridge.media.pauseAudio()
                kunjoBridge.media.setPlaybackVolume(1.0f)
                activeSleepTimerMinutes = 0
            }
        }

        val filteredTracks = remember(allTracks, activeTab, searchQuery) {
            val byTab = when (activeTab) {
                MusicStudioTab.SONGS -> allTracks.filterNot {
                    it.name.matches(CALL_RECORD_REGEX) || it.name.matches(VOICE_NOTE_REGEX)
                }
                MusicStudioTab.CALL_RECORDS -> allTracks.filter { it.name.matches(CALL_RECORD_REGEX) }
                MusicStudioTab.VOICE_NOTES -> allTracks.filter { it.name.matches(VOICE_NOTE_REGEX) }
                MusicStudioTab.PLAYLISTS -> allTracks.filter { it.isFavorite }
            }
            if (searchQuery.isBlank()) byTab
            else byTab.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07090E))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0C1019),
                            Color(0xFF07090E),
                            Color(0xFF040609)
                        )
                    )
                )
        ) {
            if (showNowPlaying && currentAudio != null) {
                NowPlayingGlassStage(
                    state = currentAudio!!,
                    track = currentTrack,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    playbackSpeed = playbackSpeed,
                    isFavorite = isFavorite,
                    showLyricsView = showLyricsView,
                    isBn = isBn,
                    onToggleFavorite = {
                        currentAudio?.fileId?.let { id ->
                            viewModel.toggleFavorite(id, !isFavorite)
                        }
                    },
                    onToggleLyrics = { showLyricsView = !showLyricsView },
                    onTogglePlay = {
                        if (currentAudio?.isPlaying == true) kunjoBridge.media.pauseAudio()
                        else kunjoBridge.media.resumeAudio()
                    },
                    onSeek = { kunjoBridge.media.seekAudio(it) },
                    onSkipNext = { kunjoBridge.media.skipNext() },
                    onSkipPrevious = { kunjoBridge.media.skipPrevious() },
                    onToggleShuffle = { kunjoBridge.media.toggleShuffle() },
                    onCycleRepeat = { kunjoBridge.media.cycleRepeatMode() },
                    onCycleSpeed = {
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        val idx = speeds.indexOfFirst { kotlin.math.abs(it - playbackSpeed) < 0.01f }
                        val next = speeds.getOrElse(idx + 1) { speeds.first() }
                        kunjoBridge.media.setPlaybackSpeed(next)
                    },
                    onOpenQueue = { showQueueSheet = true },
                    onOpenEqualizer = { showEqualizerSheet = true },
                    onOpenSleepTimer = { showSleepTimerDialog = true },
                    onMinimize = { showNowPlaying = false }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF161F2E),
                                        Color(0xFF0F1520),
                                        Color(0xFF080B10)
                                    )
                                )
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF00E5FF).copy(alpha = 0.15f),
                                            Color.Transparent
                                        ),
                                        radius = 350f
                                    )
                                )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                                    .clickable(onClick = onBack),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                                        .clickable { showSleepTimerDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bedtime,
                                        contentDescription = "Sleep timer",
                                        tint = if (activeSleepTimerMinutes > 0) Color(0xFF00E5FF) else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                                        .clickable { showEqualizerSheet = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Equalizer,
                                        contentDescription = "Equalizer",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                Text(
                                    text = if (isBn) "iPlus অডিও স্টুডিও • লসলেস ক্লাউড" else "iPlus AUDIO STUDIO • LOSSLESS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF),
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = currentAudio?.title?.ifBlank { null }?.replace(STRIP_AUDIO_EXT_REGEX, "")
                                        ?: (filteredTracks.firstOrNull()?.name?.replace(STRIP_AUDIO_EXT_REGEX, "")
                                            ?: if (isBn) "আমার অডিও লাইব্রেরি" else "My Cloud Library"),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (isBn) "${filteredTracks.size} টি অডিও ট্র্যাক পাওয়া গেছে" else "${filteredTracks.size} lossless audio tracks",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.70f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF00E5FF), Color(0xFF0284C7))
                                        )
                                    )
                                    .clickable {
                                        if (filteredTracks.isNotEmpty()) {
                                            val queueItems = filteredTracks.map {
                                                AudioTrackItem(it.id, it.name, sizeBytes = it.sizeBytes, mimeType = it.mimeType)
                                            }
                                            kunjoBridge.media.playQueue(queueItems, 0)
                                            showNowPlaying = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play all",
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudioFilterChip(
                            label = if (isBn) "সব গান" else "Songs",
                            isSelected = activeTab == MusicStudioTab.SONGS,
                            onClick = { activeTab = MusicStudioTab.SONGS }
                        )
                        StudioFilterChip(
                            label = if (isBn) "কল রেকর্ড" else "Recordings",
                            isSelected = activeTab == MusicStudioTab.CALL_RECORDS,
                            onClick = { activeTab = MusicStudioTab.CALL_RECORDS }
                        )
                        StudioFilterChip(
                            label = if (isBn) "ভয়েস নোট" else "Voice notes",
                            isSelected = activeTab == MusicStudioTab.VOICE_NOTES,
                            onClick = { activeTab = MusicStudioTab.VOICE_NOTES }
                        )
                        StudioFilterChip(
                            label = if (isBn) "পছন্দের প্লেলিস্ট" else "Favorites",
                            isSelected = activeTab == MusicStudioTab.PLAYLISTS,
                            onClick = { activeTab = MusicStudioTab.PLAYLISTS }
                        )
                    }

                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        LiquidGlassSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = if (isBn) "অডিও ট্র্যাক খুঁজুন..." else "Search tracks..."
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (activeTab) {
                                MusicStudioTab.PLAYLISTS -> if (isBn) "পছন্দের ট্র্যাকসমূহ" else "Favorite Tracks"
                                else -> if (isBn) "শীর্ষ গানসমূহ" else "Top Songs"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isBn) "${filteredTracks.size} টি ট্র্যাক" else "${filteredTracks.size} tracks",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.60f)
                        )
                    }

                    if (filteredTracks.isEmpty()) {
                        EmptyState(
                            icon = if (activeTab == MusicStudioTab.PLAYLISTS) Icons.Filled.FavoriteBorder else Icons.Filled.Audiotrack,
                            title = if (activeTab == MusicStudioTab.PLAYLISTS) {
                                if (isBn) "কোনো পছন্দের গান নেই" else "No favorite songs yet"
                            } else {
                                if (isBn) "কোনো অডিও ফাইল নেই" else "No audio files"
                            },
                            description = if (activeTab == MusicStudioTab.PLAYLISTS) {
                                if (isBn) "প্লেয়ারে লাভ (♥) আইকনে ট্যাপ করলে গান এখানে যুক্ত হবে।" else "Tap the heart button in Now Playing to add songs to Favorites."
                            } else {
                                if (isBn) "ক্লাউডে কোনো অডিও পাওয়া যায়নি।" else "No audio found in this category."
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = if (currentAudio != null) 90.dp else 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(filteredTracks, key = { _, t -> t.id }) { index, track ->
                                val isCurrent = currentAudio?.fileId == track.id
                                TrackRowItem(
                                    track = track,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && (currentAudio?.isPlaying == true),
                                    onClick = {
                                        val queueItems = filteredTracks.map {
                                            AudioTrackItem(it.id, it.name, sizeBytes = it.sizeBytes, mimeType = it.mimeType)
                                        }
                                        kunjoBridge.media.playQueue(queueItems, index)
                                        showNowPlaying = true
                                    }
                                )
                            }
                        }
                    }
                }

                if (currentAudio != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp)
                    ) {
                        DockedMiniPlayerCard(
                            state = currentAudio!!,
                            isBn = isBn,
                            onExpand = { showNowPlaying = true },
                            onTogglePlay = {
                                if (currentAudio?.isPlaying == true) kunjoBridge.media.pauseAudio()
                                else kunjoBridge.media.resumeAudio()
                            },
                            onNext = { kunjoBridge.media.skipNext() }
                        )
                    }
                }
            }

            if (showQueueSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showQueueSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = Color(0xFF141720)
                ) {
                    QueueSheetContent(
                        tracks = allTracks,
                        currentFileId = currentAudio?.fileId,
                        isBn = isBn,
                        onSelectTrack = { track ->
                            val queueItems = allTracks.map {
                                AudioTrackItem(it.id, it.name, sizeBytes = it.sizeBytes, mimeType = it.mimeType)
                            }
                            val idx = allTracks.indexOf(track).coerceAtLeast(0)
                            kunjoBridge.media.playQueue(queueItems, idx)
                            showQueueSheet = false
                        }
                    )
                }
            }

            if (showEqualizerSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showEqualizerSheet = false },
                    containerColor = Color(0xFF141720)
                ) {
                    EqualizerSheetContent(
                        audioSessionId = audioSessionId,
                        selectedPresetIndex = eqPreset,
                        onPresetSelect = { kunjoBridge.media.applyEqualizerPreset(it) },
                        boostMultiplier = kunjoBridge.media.audioBoostMultiplier.collectAsStateWithLifecycle().value,
                        onBoostChange = { kunjoBridge.media.setAudioBoost(it) },
                        isBn = isBn,
                        onClose = { showEqualizerSheet = false }
                    )
                }
            }

            if (showSleepTimerDialog) {
                SleepTimerDialog(
                    activeMinutes = activeSleepTimerMinutes,
                    isBn = isBn,
                    onSelect = { minutes ->
                        activeSleepTimerMinutes = minutes
                        showSleepTimerDialog = false
                    },
                    onDismiss = { showSleepTimerDialog = false }
                )
            }
        }
    }
}

@Composable
private fun NowPlayingGlassStage(
    state: AudioPlaybackState,
    track: DriveFile?,
    isShuffle: Boolean,
    repeatMode: Int,
    playbackSpeed: Float,
    isFavorite: Boolean,
    showLyricsView: Boolean,
    isBn: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleLyrics: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onCycleSpeed: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onMinimize: () -> Unit
) {
    val palette = AcousticStudioPalette
    val primaryTextColor = Color.White
    val secondaryTextColor = Color.White.copy(alpha = 0.65f)
    val glassPillBg = Color.White.copy(alpha = 0.08f)

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPos by remember { mutableFloatStateOf(0f) }

    val duration = state.durationMs.coerceAtLeast(1L)
    val currentPos = if (isScrubbing) scrubPos.toLong() else state.currentPositionMs
    val progressFraction = (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val codecExt = remember(state.title, track?.name) {
        (track?.name ?: state.title).substringAfterLast('.', "AUDIO").uppercase()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                    .clickable(onClick = onMinimize),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Minimize",
                    tint = primaryTextColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = if (isBn) "এখন বাজছে" else "Now playing",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor.copy(alpha = 0.90f)
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                    .clickable(onClick = onOpenEqualizer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Equalizer,
                    contentDescription = "Equalizer",
                    tint = primaryTextColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (showLyricsView) {
            Box(
                modifier = Modifier
                    .size(275.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF1B0C35).copy(alpha = 0.92f))
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.30f),
                                palette.accentPrimary.copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isBn) "ট্র্যাক তথ্য ও লিরিক্স" else "Track Info & Lyrics",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = state.title.replace(STRIP_AUDIO_EXT_REGEX, ""),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${if (state.sizeBytes > 0) Formatters.bytes(state.sizeBytes) else "—"} • $codecExt • ${
                            if (track?.hasLocalCopy == true) (if (isBn) "অফলাইন" else "Offline")
                            else (if (isBn) "ক্লাউড স্ট্রিম" else "Cloud Stream")
                        }",
                        fontSize = 12.sp,
                        color = palette.accentSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (isBn) "এই অডিও ফাইলের জন্য কোনো এমবেডেড লিরিক্স পাওয়া যায়নি।" else "No embedded lyrics available for this audio file.",
                        fontSize = 12.sp,
                        color = secondaryTextColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(275.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF182030),
                                Color(0xFF0E131E)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color(0xFF00E5FF).copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF222E42),
                                    Color(0xFF0C1019)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Album,
                        contentDescription = null,
                        tint = palette.accentPrimary.copy(alpha = 0.90f),
                        modifier = Modifier.size(105.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.28f)
                                    )
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .size(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(onClick = onToggleFavorite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFFF2A85) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.60f))
                            .border(
                                width = 0.8.dp,
                                color = Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = codecExt,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = palette.accentSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = state.title.replace(STRIP_AUDIO_EXT_REGEX, ""),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            val sizeStr = if (state.sizeBytes > 0) Formatters.bytes(state.sizeBytes) else "Audio"
            Text(
                text = "$sizeStr • $codecExt • ${if (isBn) "iPlus ক্লাউড স্টুডিও" else "iPlus Cloud Studio"}",
                fontSize = 13.sp,
                color = secondaryTextColor,
                maxLines = 1
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
            Slider(
                value = progressFraction,
                onValueChange = { frac ->
                    isScrubbing = true
                    scrubPos = frac * duration.toFloat()
                },
                onValueChangeFinished = {
                    isScrubbing = false
                    onSeek(scrubPos.toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = palette.accentPrimary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, palette.accentPrimary, CircleShape)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatAudioTime(currentPos),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryTextColor
                )
                Text(
                    text = "-${formatAudioTime((duration - currentPos).coerceAtLeast(0L))}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryTextColor
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) palette.accentPrimary else secondaryTextColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    .clickable(onClick = onSkipPrevious),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                palette.accentPrimary,
                                Color(0xFF8B5CF6)
                            )
                        )
                    )
                    .border(
                        width = 2.5.dp,
                        color = Color.White.copy(alpha = 0.90f),
                        shape = CircleShape
                    )
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    .clickable(onClick = onSkipNext),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = onCycleRepeat) {
                Icon(
                    imageVector = if (repeatMode == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeatMode > 0) palette.accentPrimary else secondaryTextColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(glassPillBg)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CapsulePillItem(
                    icon = Icons.Filled.QueueMusic,
                    label = if (isBn) "কিউ" else "Queue",
                    onClick = onOpenQueue
                )
                CapsulePillItem(
                    icon = Icons.Filled.Speed,
                    label = String.format(java.util.Locale.US, "%.2gx", playbackSpeed),
                    onClick = onCycleSpeed
                )
                CapsulePillItem(
                    icon = Icons.Filled.GraphicEq,
                    label = "DSP",
                    onClick = onOpenEqualizer
                )
                CapsulePillItem(
                    icon = Icons.Filled.Subtitles,
                    label = if (isBn) "লিরিক্স" else "Lyrics",
                    isActive = showLyricsView,
                    onClick = onToggleLyrics
                )
                CapsulePillItem(
                    icon = Icons.Filled.Bedtime,
                    label = if (isBn) "টাইমার" else "Timer",
                    onClick = onOpenSleepTimer
                )
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun CapsulePillItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val palette = IplusTheme.palette
    val isLight = palette.theme == AppTheme.LIGHT
    val defaultColor = if (isLight) Color(0xFF0F172A).copy(alpha = 0.70f) else Color.White.copy(alpha = 0.70f)
    Row(
        modifier = Modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) palette.accentPrimary else defaultColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) palette.accentPrimary else defaultColor
        )
    }
}

@Composable
private fun DockedMiniPlayerCard(
    state: AudioPlaybackState,
    isBn: Boolean,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    val palette = IplusTheme.palette
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF161922).copy(alpha = 0.95f))
            .clickable(role = Role.Button, onClick = onExpand)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF222836)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title.replace(STRIP_AUDIO_EXT_REGEX, ""),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (state.isPlaying) (if (isBn) "এখন বাজছে" else "Now playing") else (if (isBn) "স্থগিত" else "Paused"),
                    fontSize = 11.sp,
                    color = palette.accentPrimary
                )
            }
            IconButton(onClick = onTogglePlay) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun TrackRowItem(
    track: DriveFile,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val palette = AcousticStudioPalette
    val rowShape = RoundedCornerShape(14.dp)
    val extLabel = remember(track.name) {
        track.name.substringAfterLast('.', "AUDIO").uppercase()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(if (isCurrent) palette.accentPrimary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                color = if (isCurrent) palette.accentPrimary.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.08f),
                shape = rowShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isCurrent) palette.accentPrimary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
                .border(
                    width = 0.8.dp,
                    color = if (isCurrent) palette.accentPrimary.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                contentDescription = null,
                tint = if (isCurrent) palette.accentPrimary else Color.White.copy(alpha = 0.80f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name.replace(STRIP_AUDIO_EXT_REGEX, ""),
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) palette.accentPrimary else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${Formatters.bytes(track.sizeBytes)} • $extLabel",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.60f)
            )
        }
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.accentPrimary.copy(alpha = 0.20f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isPlaying) "PLAYING" else "ACTIVE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = palette.accentPrimary
                )
            }
        }
    }
}

@Composable
private fun StudioFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val chipShape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .heightIn(min = 38.dp)
            .clip(chipShape)
            .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.16f),
                shape = chipShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun QueueSheetContent(
    tracks: List<DriveFile>,
    currentFileId: String?,
    isBn: Boolean,
    onSelectTrack: (DriveFile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = if (isBn) "পরবর্তী গানসমূহ (কিউ)" else "Playing next (Queue)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(350.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(tracks, key = { _, f -> f.id }) { _, file ->
                val isCur = file.id == currentFileId
                TrackRowItem(
                    track = file,
                    isCurrent = isCur,
                    isPlaying = isCur,
                    onClick = { onSelectTrack(file) }
                )
            }
        }
    }
}

@Composable
private fun EqualizerSheetContent(
    audioSessionId: Int,
    selectedPresetIndex: Int,
    onPresetSelect: (Int) -> Unit,
    boostMultiplier: Float,
    onBoostChange: (Float) -> Unit,
    isBn: Boolean,
    onClose: () -> Unit
) {
    val palette = IplusTheme.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isBn) "DSP ইকুয়ালাইজার ও বেস বুস্ট" else "DSP Equalizer & Bass Boost",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (audioSessionId > 0) {
                    Text(
                        text = "Session #$audioSessionId",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = palette.accentPrimary
                    )
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Text(
            text = if (isBn) "প্রিসেট:" else "Presets:",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.70f)
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf("Flat", "Bass boost", "Vocal", "Rock", "Pop")
            presets.forEachIndexed { idx, name ->
                val selected = selectedPresetIndex == idx
                Box(
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) palette.accentPrimary.copy(alpha = 0.24f)
                            else Color.White.copy(alpha = 0.10f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) palette.accentPrimary else Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onPresetSelect(idx) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) palette.accentPrimary else Color.White
                    )
                }
            }
        }

        Text(
            text = "${if (isBn) "বেস বুস্ট" else "Bass boost"}: ${(boostMultiplier * 100).toInt()}%",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.80f)
        )
        Slider(
            value = boostMultiplier,
            onValueChange = onBoostChange,
            valueRange = 1.0f..2.0f,
            colors = SliderDefaults.colors(
                thumbColor = palette.accentPrimary,
                activeTrackColor = palette.accentPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SleepTimerDialog(
    activeMinutes: Int,
    isBn: Boolean,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(0, 15, 30, 45, 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isBn) "স্লিপ টাইমার" else "Sleep timer", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { mins ->
                    val label = when (mins) {
                        0 -> if (isBn) "বন্ধ" else "Off"
                        else -> if (isBn) "$mins মিনিট" else "$mins minutes"
                    }
                    val isCur = activeMinutes == mins
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCur) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { onSelect(mins) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCur) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBn) "বাতিল" else "Cancel")
            }
        }
    )
}

private fun formatAudioTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

