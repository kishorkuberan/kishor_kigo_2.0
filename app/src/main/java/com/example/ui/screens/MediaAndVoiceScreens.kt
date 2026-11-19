package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.UserProfile
import com.example.media.MediaAttachmentHelper
import com.example.media.VoiceRecorderPlayer
import com.example.ui.components.KigoEmptyState
import com.example.ui.components.KigoSubScreenTopBar
import com.example.ui.components.formatKigoDateTime
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily

@Composable
fun MediaGalleryScreen(
    mediaMessages: List<ChatMessage>,
    onRefresh: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val voiceManager = remember { VoiceRecorderPlayer(context.applicationContext) }
    DisposableEffect(Unit) {
        onDispose { voiceManager.releaseAll() }
    }
    LaunchedEffect(Unit) {
        onRefresh()
    }

    val playingId by voiceManager.playingMessageId.collectAsStateWithLifecycle()
    val isPaused by voiceManager.isPaused.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf("all") } // all, image, document, video, voice
    var previewImageMessage by remember { mutableStateOf<ChatMessage?>(null) }

    val filtered = remember(mediaMessages, filter) {
        if (filter == "all") mediaMessages else mediaMessages.filter { it.type == filter }
    }

    Scaffold(
        topBar = {
            KigoSubScreenTopBar(
                title = "Media Gallery",
                subtitle = "${filtered.size} synced attachments",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onRefresh, modifier = Modifier.testTag("media_refresh_btn")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh media gallery")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    "all" to "All",
                    "image" to "Photos",
                    "document" to "Documents",
                    "video" to "Videos",
                    "voice" to "Voice"
                )
                items(tabs) { (key, label) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        label = { Text(label) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                KigoEmptyState(
                    title = "No Media Yet",
                    subtitle = "Photos, PDFs, videos, and voice clips shared across your conversations will appear here."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { msg ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = when (msg.type) {
                                                "image" -> Icons.Default.Image
                                                "video" -> Icons.Default.Videocam
                                                "voice" -> Icons.Default.GraphicEq
                                                else -> Icons.Default.Description
                                            },
                                            contentDescription = msg.type,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = msg.mediaName.ifBlank { msg.type.uppercase() },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "From ${msg.senderName.ifBlank { "KIGO User" }} · ${formatKigoDateTime(msg.createdAt)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    TextButton(onClick = { onOpenConversation(msg.conversationId) }) {
                                        Text("Open Chat")
                                    }
                                }

                                if (msg.type == "image" && msg.mediaDataBase64.isNotBlank()) {
                                    val bmp = remember(msg.mediaDataBase64) {
                                        MediaAttachmentHelper.decodeImageBitmap(msg.mediaDataBase64)
                                    }
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = msg.mediaName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { previewImageMessage = msg }
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (msg.mediaSizeBytes > 0L) "${(msg.mediaSizeBytes / 1024).coerceAtLeast(1)} KB" else msg.type,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (msg.type == "voice" && msg.mediaDataBase64.isNotBlank()) {
                                            val isPlaying = playingId == msg.id && !isPaused
                                            OutlinedButton(
                                                onClick = { voiceManager.togglePlayPause(msg.id, msg.mediaDataBase64) },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(
                                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Play voice"
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(if (isPlaying) "Pause" else "Play")
                                            }
                                        }
                                        if (msg.mediaDataBase64.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    val res = MediaAttachmentHelper.saveAttachmentToDownloads(
                                                        context = context,
                                                        fileName = msg.mediaName.ifBlank { "kigo_${msg.id}.${if (msg.type == "image") "jpg" else "bin"}" },
                                                        mimeType = msg.mediaMimeType,
                                                        base64Data = msg.mediaDataBase64
                                                    )
                                                    Toast.makeText(
                                                        context,
                                                        res.getOrElse { it.localizedMessage ?: "Save failed" },
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = "Save to device", modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Save")
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
    }

    val previewMsg = previewImageMessage
    if (previewMsg != null) {
        val bmp = remember(previewMsg.mediaDataBase64) {
            MediaAttachmentHelper.decodeImageBitmap(previewMsg.mediaDataBase64)
        }
        AlertDialog(
            onDismissRequest = { previewImageMessage = null },
            title = { Text(previewMsg.mediaName.ifBlank { "Photo Preview" }) },
            text = {
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = previewMsg.mediaName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { previewImageMessage = null }) { Text("Close") }
            }
        )
    }
}

@Composable
fun VoiceMessagesScreen(
    myProfile: UserProfile,
    conversations: List<Conversation>,
    peerProfiles: Map<String, UserProfile>,
    mediaMessages: List<ChatMessage>,
    onRefreshMedia: () -> Unit,
    onSendVoiceToConversation: (
        conversation: Conversation,
        base64: String,
        durationMs: Long
    ) -> Unit,
    onOpenConversation: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val voiceManager = remember { VoiceRecorderPlayer(context.applicationContext) }
    DisposableEffect(Unit) {
        onDispose { voiceManager.releaseAll() }
    }
    LaunchedEffect(Unit) {
        onRefreshMedia()
    }

    val isRecording by voiceManager.isRecording.collectAsStateWithLifecycle()
    val durationMs by voiceManager.recordingDurationMs.collectAsStateWithLifecycle()
    val recordedBase64 by voiceManager.recordedBase64.collectAsStateWithLifecycle()
    val playingId by voiceManager.playingMessageId.collectAsStateWithLifecycle()
    val isPaused by voiceManager.isPaused.collectAsStateWithLifecycle()

    var selectedConv by remember(conversations) { mutableStateOf(conversations.firstOrNull()) }
    var showConvDropdown by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceManager.startRecording(scope).onFailure {
                Toast.makeText(context, it.localizedMessage ?: "Mic unavailable", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice messages", Toast.LENGTH_SHORT).show()
        }
    }

    val voiceClips = remember(mediaMessages) { mediaMessages.filter { it.type == "voice" } }

    val pulseTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.15f else 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "scale"
    )

    Scaffold(
        topBar = { KigoSubScreenTopBar(title = "Voice Studio", subtitle = "Record, preview & send voice clips", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(20.dp)
                    ) {
                        Text(
                            text = if (isRecording) "Recording Voice Note..." else if (recordedBase64 != null) "Clip Ready to Send" else "Tap Microphone to Record",
                            fontFamily = SpaceGroteskFamily,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = formatDurationMmSs(durationMs),
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))

                        Surface(
                            shape = CircleShape,
                            color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(76.dp)
                                .scale(micScale)
                                .testTag("voice_studio_record_button")
                                .clickable {
                                    if (isRecording) {
                                        voiceManager.stopRecording().onFailure {
                                            Toast.makeText(context, it.localizedMessage ?: "Recording failed", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        val hasMic = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasMic) {
                                            voiceManager.startRecording(scope)
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isRecording) "Stop recording" else "Start recording",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        if (!recordedBase64.isNullOrBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        voiceManager.togglePlayPause("studio_preview", recordedBase64!!)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    val isPlaying = playingId == "studio_preview" && !isPaused
                                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isPlaying) "Pause" else "Preview")
                                }
                                OutlinedButton(
                                    onClick = { voiceManager.discardRecording() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Discard")
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            if (conversations.isNotEmpty()) {
                                Box {
                                    OutlinedButton(
                                        onClick = { showConvDropdown = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        val title = selectedConv?.let {
                                            resolveConversationTitle(it, myProfile.userId, peerProfiles)
                                        } ?: "Choose conversation"
                                        Text("Recipient: $title")
                                    }
                                    DropdownMenu(
                                        expanded = showConvDropdown,
                                        onDismissRequest = { showConvDropdown = false }
                                    ) {
                                        conversations.forEach { conv ->
                                            val title = resolveConversationTitle(conv, myProfile.userId, peerProfiles)
                                            DropdownMenuItem(
                                                text = { Text(title) },
                                                onClick = {
                                                    selectedConv = conv
                                                    showConvDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val target = selectedConv ?: return@Button
                                        val b64 = recordedBase64 ?: return@Button
                                        onSendVoiceToConversation(target, b64, durationMs.coerceAtLeast(1000L))
                                        voiceManager.discardRecording()
                                        Toast.makeText(context, "Voice message sent!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Send Voice Message")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Recent Voice Messages (${voiceClips.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (voiceClips.isEmpty()) {
                item {
                    KigoEmptyState(
                        title = "No Voice Messages Yet",
                        subtitle = "Record a clip above or inside any chat to build your voice library."
                    )
                }
            } else {
                items(voiceClips, key = { it.id }) { clip ->
                    val isPlaying = playingId == clip.id && !isPaused
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(14.dp)
                        ) {
                            IconButton(
                                onClick = { voiceManager.togglePlayPause(clip.id, clip.mediaDataBase64) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play voice message",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(clip.senderName.ifBlank { "KIGO User" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${formatDurationMmSs(clip.mediaDurationMs)} · ${formatKigoDateTime(clip.createdAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { onOpenConversation(clip.conversationId) }) {
                                Text("Chat")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDurationMmSs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val m = totalSec / 60L
    val s = totalSec % 60L
    return "%02d:%02d".format(m, s)
}
