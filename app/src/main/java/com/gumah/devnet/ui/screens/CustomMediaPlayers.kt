package com.gumah.devnet.ui.screens

import android.net.Uri
import android.os.Build
import android.widget.VideoView
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DevNetVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(0) }
    var currentPosition by remember { mutableStateOf(0) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var aspectRatioFit by remember { mutableStateOf(true) }
    var mediaPrepared by remember { mutableStateOf(false) }

    // Double tap ripple visual cues
    var showForwardCue by remember { mutableStateOf(false) }
    var showRewindCue by remember { mutableStateOf(false) }

    // State polling
    LaunchedEffect(videoViewInstance, isPlaying) {
        if (isPlaying && videoViewInstance != null) {
            while (isPlaying) {
                videoViewInstance?.let {
                    if (it.isPlaying) {
                        currentPosition = it.currentPosition
                        if (duration == 0 || duration <= 0) {
                            duration = it.duration
                        }
                    } else {
                        isPlaying = false
                    }
                }
                delay(250)
            }
        }
    }

    // Auto-hide controls
    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying) {
            delay(3500)
            isControlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(1.2.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
    ) {
        // Android standard VideoView inside custom box
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(Uri.parse(videoUrl))
                    setOnPreparedListener { mp ->
                        mediaPrepared = true
                        duration = mp.duration
                        mp.isLooping = true
                        start()
                        isPlaying = true
                        videoViewInstance = this@apply
                    }
                    setOnErrorListener { _, _, _ ->
                        isPlaying = false
                        false
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            isControlsVisible = !isControlsVisible
                        },
                        onDoubleTap = { offset ->
                            val width = size.width
                            val x = offset.x
                            if (x > width / 2) {
                                videoViewInstance?.let {
                                    val newPos = (it.currentPosition + 10000).coerceAtMost(duration)
                                    it.seekTo(newPos)
                                    currentPosition = newPos
                                }
                                showForwardCue = true
                            } else {
                                videoViewInstance?.let {
                                    val newPos = (it.currentPosition - 10000).coerceAtLeast(0)
                                    it.seekTo(newPos)
                                    currentPosition = newPos
                                }
                                showRewindCue = true
                            }
                        }
                    )
                },
            update = { view ->
                videoViewInstance = view
            },
            onRelease = { view ->
                view.stopPlayback()
            }
        )

        // Visual notification cues for skipping forward/backward
        LaunchedEffect(showForwardCue) {
            if (showForwardCue) {
                delay(650)
                showForwardCue = false
            }
        }
        LaunchedEffect(showRewindCue) {
            if (showRewindCue) {
                delay(650)
                showRewindCue = false
            }
        }

        if (showForwardCue) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterEnd)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Text("+10s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (showRewindCue) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterStart)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FastRewind, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Text("-10s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Play/Pause overlays
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                IconButton(
                    onClick = {
                        videoViewInstance?.let {
                            val newPos = (it.currentPosition - 10000).coerceAtLeast(0)
                            it.seekTo(newPos)
                            currentPosition = newPos
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                IconButton(
                    onClick = {
                        videoViewInstance?.let {
                            if (it.isPlaying) {
                                it.pause()
                                isPlaying = false
                            } else {
                                it.start()
                                isPlaying = true
                            }
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .border(1.2.dp, Color(0xFF38BDF8), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Playback toggle",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = {
                        videoViewInstance?.let {
                            val newPos = (it.currentPosition + 10000).coerceAtMost(duration)
                            it.seekTo(newPos)
                            currentPosition = newPos
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Custom HUD Controls Bar layout
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Dragging slider element
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onValueChange = { progress ->
                        videoViewInstance?.let {
                            val targetMs = (progress * duration).toInt()
                            it.seekTo(targetMs)
                            currentPosition = targetMs
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF38BDF8),
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatVideoDuration(currentPosition)} / ${formatVideoDuration(duration)}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Filled.VolumeMute else Icons.Filled.VolumeUp,
                                contentDescription = "Muted trigger",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { aspectRatioFit = !aspectRatioFit },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (aspectRatioFit) Icons.Filled.Fullscreen else Icons.Filled.FullscreenExit,
                                contentDescription = "Ratio trigger",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatVideoDuration(ms: Int): String {
    val totalSecs = ms / 1000
    val hours = totalSecs / 3600
    val mins = (totalSecs % 3600) / 60
    val secs = totalSecs % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
    } else {
        String.format(Locale.US, "%d:%02d", mins, secs)
    }
}

@Composable
fun CustomVoiceMessagePlayer(
    audioUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playSpeed by remember { mutableStateOf(1.0f) } // WhatsApp 1x, 1.5x, 2.0x speeds!
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }

    // Pre-allocated random heights for beautiful glowing Soundwave simulation
    val soundwaveHeights = remember {
        val list = mutableListOf<Float>()
        for (i in 0 until 24) {
            list.add((0.2f + Math.random().toFloat() * 0.8f)) // Heights from 20% to 100%
        }
        list.toList()
    }

    DisposableEffect(audioUrl) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Progress updates
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && mediaPlayer != null) {
                try {
                    mediaPlayer?.let {
                        if (it.isPlaying) {
                            currentPosition = it.currentPosition
                        } else {
                            isPlaying = false
                        }
                    }
                } catch (e: Exception) {
                    // Ignore state changes
                }
                delay(200)
            }
        }
    }

    Row(
        modifier = modifier
            .padding(vertical = 4.dp)
            .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
            .border(1.2.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Modern Round Play Button with gradient/glow look
        IconButton(
            onClick = {
                try {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        if (mediaPlayer == null) {
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(audioUrl)
                                prepareAsync()
                                setOnPreparedListener { mp ->
                                    duration = mp.duration
                                    
                                    // Set dynamic playback rate speed (WhatsApp style!)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        try {
                                            playbackParams = playbackParams.setSpeed(playSpeed)
                                        } catch (e: Exception) {}
                                    }
                                    
                                    mp.start()
                                    isPlaying = true
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                    currentPosition = 0
                                }
                                setOnErrorListener { _, _, _ ->
                                    isPlaying = false
                                    Toast.makeText(context, "Error playing audio file", Toast.LENGTH_SHORT).show()
                                    false
                                }
                            }
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                try {
                                    mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(playSpeed) ?: PlaybackParams().setSpeed(playSpeed)
                                } catch (e: Exception) {}
                            }
                            mediaPlayer?.start()
                            isPlaying = true
                        }
                    }
                } catch (e: Exception) {
                    isPlaying = false
                }
            },
            modifier = Modifier
                .background(Color(0xFF10B981), CircleShape)
                .size(34.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play voice message",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.width(165.dp)) {
            // Title
            Text(
                text = "Voice Message",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(6.dp))

            // Premium futuristic glowing soundwave
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
                soundwaveHeights.forEachIndexed { idx, heightProportion ->
                    val barWeight = 1f
                    val isActive = (idx.toFloat() / soundwaveHeights.size) <= progress
                    val barColor = if (isActive) Color(0xFF10B981) else Color(0xFF475569)

                    Box(
                        modifier = Modifier
                            .weight(barWeight)
                            .fillMaxHeight(heightProportion)
                            .clip(RoundedCornerShape(2.dp))
                            .background(barColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Time Tracking duration labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatVideoDuration(currentPosition),
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp
                )
                Text(
                    text = if (duration > 0) formatVideoDuration(duration) else "0:00",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Playback speed cycle pill button (1x -> 1.5x -> 2x!)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF334155))
                .clickable {
                    val nextSpeed = when (playSpeed) {
                        1.0f -> 1.5f
                        1.5f -> 2.0f
                        else -> 1.0f
                    }
                    playSpeed = nextSpeed
                    mediaPlayer?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                if (it.isPlaying) {
                                    it.playbackParams = it.playbackParams.setSpeed(nextSpeed)
                                }
                            } catch (e: Exception) {}
                        }
                    }
                }
                .padding(horizontal = 7.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${playSpeed}x",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
