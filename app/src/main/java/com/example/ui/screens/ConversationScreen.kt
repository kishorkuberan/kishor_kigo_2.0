package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
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
import com.example.ui.components.EncryptionBadge
import com.example.ui.components.KigoAvatar
import com.example.ui.components.formatKigoDateTime
import com.example.ui.components.formatKigoTimestamp
import com.example.ui.theme.LocalKigoSettings
import com.example.ui.theme.parseBubbleColor
import com.example.ui.viewmodel.KigoConversationViewModel
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun IndividualConversationScreen(
    viewModel: KigoConversationViewModel,
    myProfile: UserProfile,
    allConversations: List<Conversation>,
    peerProfiles: Map<String, UserProfile>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = LocalKigoSettings.current
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val peerProfile by viewModel.peerProfile.collectAsStateWithLifecycle()
    val replyingTo by viewModel.replyingTo.collectAsStateWithLifecycle()
    val editingMessage by viewModel.editingMessage.collectAsStateWithLifecycle()
    val sendError by viewModel.sendError.collectAsStateWithLifecycle()

    val voiceManager = remember { VoiceRecorderPlayer(context.applicationContext) }
    DisposableEffect(Unit) {
        onDispose { voiceManager.releaseAll() }
    }
    val isRecording by voiceManager.isRecording.collectAsStateWithLifecycle()
    val recordingDurationMs by voiceManager.recordingDurationMs.collectAsStateWithLifecycle()
    val recordedBase64 by voiceManager.recordedBase64.collectAsStateWithLifecycle()
    val playingMessageId by voiceManager.playingMessageId.collectAsStateWithLifecycle()
    val isPaused by voiceManager.isPaused.collectAsStateWithLifecycle()

    var messageInput by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var showAttachMenu by remember { mutableStateOf(false) }
    var showEmojiBar by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showLocationConsent by remember { mutableStateOf(false) }
    var previewImageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(editingMessage?.id) {
        if (editingMessage != null) {
            messageInput = viewModel.resolveText(editingMessage!!)
        }
    }

    LaunchedEffect(messages.size, conversation?.id) {
        if (messages.isNotEmpty()) {
            viewModel.markVisibleMessagesRead(myProfile.showReadReceipts)
        }
    }

    // Media Launchers
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            MediaAttachmentHelper.prepareDocumentOrVideoFromUri(context, uri)
                .onSuccess { att ->
                    viewModel.sendMediaOrLocation(
                        senderProfile = myProfile,
                        type = att.type,
                        caption = att.fileName,
                        mediaDataBase64 = att.base64Data,
                        mediaName = att.fileName,
                        mediaMimeType = att.mimeType,
                        mediaSizeBytes = att.sizeBytes
                    )
                }
                .onFailure { Toast.makeText(context, it.localizedMessage, Toast.LENGTH_LONG).show() }
        }
    }

    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            MediaAttachmentHelper.prepareDocumentOrVideoFromUri(context, uri, forceType = "document")
                .onSuccess { att ->
                    viewModel.sendMediaOrLocation(
                        senderProfile = myProfile,
                        type = "document",
                        caption = att.fileName,
                        mediaDataBase64 = att.base64Data,
                        mediaName = att.fileName,
                        mediaMimeType = att.mimeType,
                        mediaSizeBytes = att.sizeBytes
                    )
                }
                .onFailure { Toast.makeText(context, it.localizedMessage, Toast.LENGTH_LONG).show() }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCameraUri
        if (ok && uri != null) {
            MediaAttachmentHelper.prepareImageFromUri(context, uri)
                .onSuccess { att ->
                    viewModel.sendMediaOrLocation(
                        senderProfile = myProfile,
                        type = "image",
                        caption = "Camera photo",
                        mediaDataBase64 = att.base64Data,
                        mediaName = att.fileName,
                        mediaMimeType = att.mimeType,
                        mediaSizeBytes = att.sizeBytes
                    )
                }
                .onFailure { Toast.makeText(context, it.localizedMessage, Toast.LENGTH_LONG).show() }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = MediaAttachmentHelper.createCameraCaptureUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos.", Toast.LENGTH_SHORT).show()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            voiceManager.startRecording(scope)
        } else {
            Toast.makeText(context, "Microphone permission is required for voice messages.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchAndSendLocation() {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                viewModel.sendMediaOrLocation(
                    senderProfile = myProfile,
                    type = "location",
                    caption = "Shared live location",
                    locationLat = loc.latitude,
                    locationLng = loc.longitude,
                    locationLabel = "${"%.4f".format(loc.latitude)}, ${"%.4f".format(loc.longitude)}"
                )
            } else {
                Toast.makeText(context, "Location unavailable right now. Ensure GPS is enabled.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Could not fetch location.", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) fetchAndSendLocation()
        else Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && searchText.isBlank()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val displayedMessages = remember(messages, searchText) {
        if (searchText.isBlank()) messages
        else messages.filter {
            viewModel.resolveText(it).contains(searchText, ignoreCase = true) ||
                it.mediaName.contains(searchText, ignoreCase = true)
        }
    }

    val title = conversation?.let { resolveConversationTitle(it, myProfile.userId, peerProfiles) } ?: "Chat"
    val subtitle = when {
        conversation?.typingUsers?.any { (uid, t) -> uid != myProfile.userId && t } == true -> "Typing..."
        peerProfile != null && peerProfile!!.showOnlineStatus && peerProfile!!.online -> "Online · ${peerProfile!!.kigoId}"
        peerProfile != null && peerProfile!!.showLastSeen -> "Last seen ${formatKigoDateTime(peerProfile!!.lastSeen)}"
        conversation?.type == "group" -> "${conversation?.participantIds?.size ?: 0} members"
        else -> "KIGO CHAT"
    }

    val myBubbleColor = parseBubbleColor(settings.bubbleColorHex)
    val wallpaperBrush = when (settings.chatWallpaper) {
        "midnight" -> Brush.verticalGradient(listOf(Color(0xFF040812), Color(0xFF0A1428)))
        "grid" -> Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)))
        "aurora" -> Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), MaterialTheme.colorScheme.background))
        else -> Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KigoAvatar(
                            displayName = title,
                            photoUrl = peerProfile?.photoUrl ?: conversation?.photoUrl.orEmpty(),
                            size = 38.dp,
                            isOnline = peerProfile?.online == true && peerProfile?.showOnlineStatus == true,
                            showOnlineIndicator = conversation?.type == "direct"
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchBar = !showSearchBar }, modifier = Modifier.testTag("chat_search_toggle")) {
                        Icon(Icons.Default.Search, contentDescription = "Search Messages")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(wallpaperBrush)
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Encryption indicator strip
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                EncryptionBadge(isEncrypted = conversation?.type == "direct" && conversation?.isEncrypted == true)
                TextButton(onClick = { viewModel.loadOlderMessages() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Load Older", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (showSearchBar) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Filter messages in this chat...") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { searchText = ""; showSearchBar = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            if (!sendError.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.clearError() }
                ) {
                    Text(
                        text = sendError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(displayedMessages, key = { it.id }) { msg ->
                    val isMine = msg.senderId == myProfile.userId
                    val resolvedText = viewModel.resolveText(msg)
                    MessageBubbleCard(
                        message = msg,
                        resolvedText = resolvedText,
                        isMine = isMine,
                        isGroup = conversation?.type == "group",
                        layoutMode = settings.messageLayout,
                        myBubbleColor = myBubbleColor,
                        isPlayingAudio = playingMessageId == msg.id && !isPaused,
                        onLongPress = { selectedMessage = msg },
                        onToggleAudio = { voiceManager.togglePlayPause(msg.id, msg.mediaDataBase64) },
                        onPreviewImage = { bmp -> previewImageBitmap = bmp },
                        onDownloadAttachment = {
                            MediaAttachmentHelper.saveAttachmentToDownloads(
                                context,
                                msg.mediaName.ifBlank { "kigo_file_${msg.id}" },
                                msg.mediaMimeType,
                                msg.mediaDataBase64
                            ).onSuccess { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show() }
                        },
                        onOpenLocation = { lat, lng ->
                            val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(KIGO+Shared+Location)"))
                            context.startActivity(geoIntent)
                        }
                    )
                }
            }

            // Reply / Edit Banner
            if (replyingTo != null || editingMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (editingMessage != null) Icons.Default.Edit else Icons.AutoMirrored.Filled.Reply,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (editingMessage != null) "Editing message" else "Replying to ${replyingTo!!.senderName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = viewModel.resolveText(editingMessage ?: replyingTo!!),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = {
                            viewModel.setReplyTo(null)
                            viewModel.setEditing(null)
                            messageInput = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                }
            }

            // Emoji quick bar
            if (showEmojiBar) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp)
                ) {
                    listOf("😀", "🔥", "❤️", "👍", "🚀", "🎉", "😎", "🔒").forEach { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .clickable { messageInput += emoji }
                                .padding(4.dp)
                        )
                    }
                }
            }

            // Voice recording active / preview bar
            if (isRecording || recordedBase64 != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isRecording) "Recording: ${recordingDurationMs / 1000}s" else "Voice clip ready (${recordingDurationMs / 1000}s)",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (isRecording) {
                            IconButton(onClick = {
                                voiceManager.stopRecording()
                                    .onFailure { Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show() }
                            }) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop recording")
                            }
                        } else {
                            IconButton(onClick = { voiceManager.togglePlayPause("draft_preview", recordedBase64.orEmpty()) }) {
                                Icon(
                                    imageVector = if (playingMessageId == "draft_preview" && !isPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Preview voice message"
                                )
                            }
                            IconButton(onClick = { voiceManager.discardRecording() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Discard voice clip")
                            }
                            Button(onClick = {
                                val b64 = recordedBase64 ?: return@Button
                                viewModel.sendMediaOrLocation(
                                    senderProfile = myProfile,
                                    type = "voice",
                                    caption = "Voice message",
                                    mediaDataBase64 = b64,
                                    mediaName = "voice_${System.currentTimeMillis()}.m4a",
                                    mediaMimeType = "audio/mp4",
                                    mediaDurationMs = recordingDurationMs
                                )
                                voiceManager.discardRecording()
                            }) {
                                Text("Send")
                            }
                        }
                    }
                }
            }

            // Composer Row
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Box {
                        IconButton(onClick = { showAttachMenu = true }, modifier = Modifier.testTag("chat_attach_btn")) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach media or file")
                        }
                        DropdownMenu(expanded = showAttachMenu, onDismissRequest = { showAttachMenu = false }) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.PhotoLibrary, null) },
                                text = { Text("Photo / Image") },
                                onClick = {
                                    showAttachMenu = false
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Videocam, null) },
                                text = { Text("Video Clip") },
                                onClick = {
                                    showAttachMenu = false
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.CameraAlt, null) },
                                text = { Text("Take Photo") },
                                onClick = {
                                    showAttachMenu = false
                                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        val uri = MediaAttachmentHelper.createCameraCaptureUri(context)
                                        pendingCameraUri = uri
                                        cameraLauncher.launch(uri)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Description, null) },
                                text = { Text("PDF / Document") },
                                onClick = {
                                    showAttachMenu = false
                                    documentPicker.launch(arrayOf("application/pdf", "text/plain", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                                text = { Text("Share Location") },
                                onClick = {
                                    showAttachMenu = false
                                    showLocationConsent = true
                                }
                            )
                        }
                    }

                    IconButton(onClick = { showEmojiBar = !showEmojiBar }) {
                        Icon(Icons.Default.EmojiEmotions, contentDescription = "Toggle emojis")
                    }

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = {
                            messageInput = it
                            viewModel.setTyping(it.isNotBlank())
                        },
                        placeholder = { Text(if (conversation?.type == "direct") "Encrypted message..." else "Message group...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.weight(1f).testTag("chat_message_input")
                    )

                    Spacer(Modifier.width(4.dp))
                    if (messageInput.isBlank() && editingMessage == null) {
                        IconButton(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (granted) voiceManager.startRecording(scope)
                                else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            modifier = Modifier.testTag("chat_voice_record_btn")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Record voice message", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                val text = messageInput
                                messageInput = ""
                                viewModel.setTyping(false)
                                viewModel.sendTextOrSaveEdit(myProfile, text)
                            },
                            modifier = Modifier.testTag("chat_send_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    // Location explicit consent dialog
    if (showLocationConsent) {
        AlertDialog(
            onDismissRequest = { showLocationConsent = false },
            title = { Text("Share Current Location?") },
            text = { Text("KIGO CHAT will read your approximate device location once and send the coordinates into this conversation. Nothing is tracked in the background.") },
            confirmButton = {
                Button(onClick = {
                    showLocationConsent = false
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (granted) fetchAndSendLocation()
                    else locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }) {
                    Text("Consent & Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationConsent = false }) { Text("Cancel") }
            }
        )
    }

    // Message Actions Sheet (React, Reply, Forward, Edit, Delete)
    selectedMessage?.let { msg ->
        ModalBottomSheet(onDismissRequest = { selectedMessage = null }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    listOf("👍", "❤️", "😂", "🔥", "😮", "🙏").forEach { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    viewModel.toggleReaction(msg, emoji)
                                    selectedMessage = null
                                }
                                .padding(8.dp)
                        )
                    }
                }
                TextButton(onClick = {
                    viewModel.setReplyTo(msg)
                    selectedMessage = null
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.Reply, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reply")
                }
                TextButton(onClick = {
                    forwardingMessage = msg
                    selectedMessage = null
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.Forward, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Forward")
                }
                if (msg.senderId == myProfile.userId && !msg.deleted && msg.type == "text") {
                    TextButton(onClick = {
                        viewModel.setEditing(msg)
                        selectedMessage = null
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Edit Message")
                    }
                }
                if (msg.senderId == myProfile.userId && !msg.deleted) {
                    TextButton(onClick = {
                        viewModel.deleteMessage(msg.id)
                        selectedMessage = null
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Message", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // Forward target picker
    forwardingMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { forwardingMessage = null },
            title = { Text("Forward Message To") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allConversations.filter { it.id != conversation?.id }.take(8).forEach { target ->
                        val targetTitle = resolveConversationTitle(target, myProfile.userId, peerProfiles)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.forwardMessage(target, myProfile, msg)
                                forwardingMessage = null
                                Toast.makeText(context, "Forwarded to $targetTitle", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(targetTitle, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { forwardingMessage = null }) { Text("Close") }
            }
        )
    }

    // Full-screen Image Preview
    previewImageBitmap?.let { bmp ->
        AlertDialog(
            onDismissRequest = { previewImageBitmap = null },
            text = {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Image preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(12.dp))
                )
            },
            confirmButton = {
                TextButton(onClick = { previewImageBitmap = null }) { Text("Close") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubbleCard(
    message: ChatMessage,
    resolvedText: String,
    isMine: Boolean,
    isGroup: Boolean,
    layoutMode: String,
    myBubbleColor: Color,
    isPlayingAudio: Boolean,
    onLongPress: () -> Unit,
    onToggleAudio: () -> Unit,
    onPreviewImage: (android.graphics.Bitmap) -> Unit,
    onDownloadAttachment: () -> Unit,
    onOpenLocation: (Double, Double) -> Unit
) {
    val alignment = if (layoutMode == "compact") Alignment.Start else if (isMine) Alignment.End else Alignment.Start
    val bubbleColor = when {
        message.deleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        isMine -> myBubbleColor
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        message.deleted -> MaterialTheme.colorScheme.onSurfaceVariant
        isMine -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    val shape = when (layoutMode) {
        "classic" -> RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = if (isMine) 18.dp else 4.dp,
            bottomEnd = if (isMine) 4.dp else 18.dp
        )
        else -> RoundedCornerShape(16.dp)
    }

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            border = if (!isMine) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null,
            modifier = Modifier
                .widthIn(max = 310.dp)
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if ((isGroup && !isMine) || message.forwardedFrom.isNotBlank()) {
                    Text(
                        text = buildString {
                            if (isGroup && !isMine) append(message.senderName)
                            if (message.forwardedFrom.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append("Forwarded from ${message.forwardedFrom}")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMine) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                }

                if (message.replyToPreview.isNotBlank() && !message.deleted) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = message.replyToPreview,
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                if (!message.deleted && message.type == "image" && message.mediaDataBase64.isNotBlank()) {
                    val bmp = remember(message.mediaDataBase64) { MediaAttachmentHelper.decodeImageBitmap(message.mediaDataBase64) }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = message.mediaName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPreviewImage(bmp) }
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (!message.deleted && message.type == "voice" && message.mediaDataBase64.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleAudio, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play voice message",
                                tint = contentColor
                            )
                        }
                        Text(
                            text = "Voice · ${(message.mediaDurationMs / 1000L).coerceAtLeast(1)}s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor
                        )
                    }
                }

                if (!message.deleted && (message.type == "document" || message.type == "video")) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.14f))
                            .clickable(onClick = onDownloadAttachment)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (message.type == "video") Icons.Default.Videocam else Icons.Default.Description,
                            contentDescription = null,
                            tint = contentColor
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(message.mediaName.ifBlank { "Attachment" }, style = MaterialTheme.typography.bodySmall, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${(message.mediaSizeBytes / 1024L).coerceAtLeast(1)} KB · Tap to save", style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.78f))
                        }
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = contentColor, modifier = Modifier.size(18.dp))
                    }
                }

                if (!message.deleted && message.type == "location" && message.locationLat != null && message.locationLng != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.15f))
                            .clickable { onOpenLocation(message.locationLat, message.locationLng) }
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = contentColor)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Location: ${message.locationLabel}\nTap to open map",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor
                        )
                    }
                }

                if (resolvedText.isNotBlank() && (message.type == "text" || message.deleted)) {
                    Text(
                        text = resolvedText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = if (message.deleted) FontStyle.Italic else FontStyle.Normal,
                        color = contentColor
                    )
                }

                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (message.isEncrypted) {
                        Icon(Icons.Default.Lock, contentDescription = "Encrypted", tint = contentColor.copy(alpha = 0.75f), modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    if (message.edited && !message.deleted) {
                        Text("edited · ", style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
                    }
                    Text(
                        text = formatKigoTimestamp(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.75f)
                    )
                    if (isMine && !message.deleted) {
                        Spacer(Modifier.width(4.dp))
                        val isReadByPeer = message.readBy.size > 1
                        val isDelivered = message.deliveredTo.size > 1
                        Icon(
                            imageVector = if (isReadByPeer || isDelivered) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = if (isReadByPeer) "Read" else "Delivered",
                            tint = if (isReadByPeer) Color(0xFF80F3FF) else contentColor.copy(alpha = 0.75f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = message.reactions.values.distinct().joinToString(" "),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
