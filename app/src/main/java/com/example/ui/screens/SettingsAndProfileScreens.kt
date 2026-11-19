package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.BlockedUser
import com.example.data.model.NotificationItem
import com.example.data.model.UserProfile
import com.example.data.model.UserSettings
import com.example.media.MediaAttachmentHelper
import com.example.ui.components.KigoAvatar
import com.example.ui.components.KigoEmptyState
import com.example.ui.components.KigoIdChip
import com.example.ui.components.KigoSubScreenTopBar
import com.example.ui.components.formatKigoDateTime
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    onMarkRead: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenFriendRequests: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var notifPermGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifPermGranted = granted
    }

    Scaffold(
        topBar = { KigoSubScreenTopBar(title = "Notifications", subtitle = "Real-time activity feed", onBack = onBack) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!notifPermGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable System Alerts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Grant notification permission to receive alerts for messages and friend requests.", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                            Text("Allow")
                        }
                    }
                }
            }

            if (notifications.isEmpty()) {
                KigoEmptyState(
                    title = "All Caught Up",
                    subtitle = "New messages, friend requests, group updates, and @mentions will appear here."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.read) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                }
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMarkRead(item.id)
                                    when (item.type) {
                                        "friend_request" -> onOpenFriendRequests()
                                        else -> if (item.targetId.isNotBlank()) onOpenConversation(item.targetId)
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Icon(
                                    imageVector = when (item.type) {
                                        "friend_request" -> Icons.Default.PersonAdd
                                        "group" -> Icons.Default.Group
                                        "mention" -> Icons.Default.AlternateEmail
                                        else -> Icons.Default.Notifications
                                    },
                                    contentDescription = item.type,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title.ifBlank { "KIGO Notification" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(item.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(2.dp))
                                    Text(formatKigoDateTime(item.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    myProfile: UserProfile,
    settings: UserSettings,
    onUpdateProfile: (displayName: String, username: String, about: String, customStatus: String, photoUrl: String) -> Unit,
    onToggleOnlineStatus: (Boolean) -> Unit,
    onNavigatePrivacy: () -> Unit,
    onNavigateStudio: () -> Unit,
    onNavigateAppSettings: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var displayName by remember(myProfile) { mutableStateOf(myProfile.displayName) }
    var username by remember(myProfile) { mutableStateOf(myProfile.username) }
    var about by remember(myProfile) { mutableStateOf(myProfile.about) }
    var customStatus by remember(myProfile) { mutableStateOf(myProfile.customStatus) }
    var photoUrl by remember(myProfile) { mutableStateOf(myProfile.photoUrl) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            MediaAttachmentHelper.prepareImageFromUri(context, uri)
                .onSuccess { photoUrl = "data:image/jpeg;base64,${it.base64Data}" }
                .onFailure { Toast.makeText(context, it.localizedMessage ?: "Failed", Toast.LENGTH_SHORT).show() }
        }
    }

    Scaffold(
        topBar = { KigoSubScreenTopBar(title = "My Profile", subtitle = "Identity & KIGO ID", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when (settings.profileTheme) {
                                "neon" -> Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)))
                                "obsidian" -> Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface))
                                else -> Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), MaterialTheme.colorScheme.surface))
                            }
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.clickable {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            KigoAvatar(
                                displayName = displayName,
                                photoUrl = photoUrl,
                                size = 86.dp,
                                isOnline = myProfile.online,
                                showOnlineIndicator = true
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Tap photo to update", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(10.dp))
                        Text(myProfile.displayName, fontFamily = SpaceGroteskFamily, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("@${myProfile.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KigoIdChip(kigoId = myProfile.kigoId)
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("KIGO ID", myProfile.kigoId))
                                    Toast.makeText(context, "Copied ${myProfile.kigoId}", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy KIGO ID")
                            }
                            IconButton(
                                onClick = {
                                    val shareText = "Connect with me on KIGO CHAT! My KIGO ID: ${myProfile.kigoId}\nkigochat://connect?id=${myProfile.kigoId}"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share KIGO ID"))
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share KIGO ID")
                            }
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Edit Profile Details", fontFamily = SpaceGroteskFamily, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' } },
                        label = { Text("Username") },
                        prefix = { Text("@") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customStatus,
                        onValueChange = { customStatus = it },
                        label = { Text("Custom Status (e.g. Available · In Focus)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = about,
                        onValueChange = { about = it },
                        label = { Text("About") },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onUpdateProfile(displayName, username, about, customStatus, photoUrl) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Profile Changes")
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Active Online Status")
                        Switch(checked = myProfile.online, onCheckedChange = onToggleOnlineStatus)
                    }
                    OutlinedButton(onClick = onNavigateStudio, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Brush, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open Customisation Studio")
                    }
                    OutlinedButton(onClick = onNavigatePrivacy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.PrivacyTip, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Privacy & Security Controls")
                    }
                    OutlinedButton(onClick = onNavigateAppSettings, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Security, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Application Settings")
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacySettingsScreen(
    myProfile: UserProfile,
    blockedUsers: List<BlockedUser>,
    onUpdatePrivacy: (
        showOnlineStatus: Boolean,
        showLastSeen: Boolean,
        showReadReceipts: Boolean,
        allowFriendRequests: Boolean,
        allowDiscovery: Boolean
    ) -> Unit,
    onUnblockUser: (String) -> Unit,
    onExportData: ((String) -> Unit) -> Unit,
    onDeleteAccount: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var showOnline by remember(myProfile) { mutableStateOf(myProfile.showOnlineStatus) }
    var showLastSeen by remember(myProfile) { mutableStateOf(myProfile.showLastSeen) }
    var showReadReceipts by remember(myProfile) { mutableStateOf(myProfile.showReadReceipts) }
    var allowRequests by remember(myProfile) { mutableStateOf(myProfile.allowFriendRequests) }
    var allowDiscovery by remember(myProfile) { mutableStateOf(myProfile.allowDiscovery) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { KigoSubScreenTopBar(title = "Privacy Settings", subtitle = "Visibility, blocking & data ownership", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Visibility Controls", fontFamily = SpaceGroteskFamily, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    PrivacySwitchRow("Show Online Status", showOnline) { showOnline = it }
                    PrivacySwitchRow("Show Last Seen Timestamp", showLastSeen) { showLastSeen = it }
                    PrivacySwitchRow("Send Read Receipts", showReadReceipts) { showReadReceipts = it }
                    PrivacySwitchRow("Allow Incoming Friend Requests", allowRequests) { allowRequests = it }
                    PrivacySwitchRow("Discoverable via @username Search", allowDiscovery) { allowDiscovery = it }
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { onUpdatePrivacy(showOnline, showLastSeen, showReadReceipts, allowRequests, allowDiscovery) },
                        modifier = Modifier.fillMaxWidth().testTag("save_privacy_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save Privacy Settings")
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Blocked Users (${blockedUsers.size})", fontFamily = SpaceGroteskFamily, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (blockedUsers.isEmpty()) {
                        Text("No blocked accounts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        blockedUsers.forEach { blocked ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(blocked.blockedDisplayName.ifBlank { "Blocked User" }, style = MaterialTheme.typography.bodyMedium)
                                    Text(blocked.blockedKigoId, style = MaterialTheme.typography.labelSmall, fontFamily = JetBrainsMonoFamily)
                                }
                                TextButton(onClick = { onUnblockUser(blocked.blockedUserId) }) {
                                    Text("Unblock")
                                }
                            }
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your Data & Account", fontFamily = SpaceGroteskFamily, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        onClick = {
                            onExportData { json ->
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_SUBJECT, "KIGO CHAT Data Export (${myProfile.kigoId})")
                                    putExtra(Intent.EXTRA_TEXT, json)
                                }
                                context.startActivity(Intent.createChooser(share, "Export KIGO Data JSON"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export Personal Data (JSON)")
                    }
                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete KIGO Account")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Permanently?") },
            text = {
                Text("This will delete your KIGO profile, private account data, and release your @username reservation. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    }
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PrivacySwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ApplicationSettingsScreen(
    settings: UserSettings,
    keyFingerprint: String,
    onSaveSettings: (UserSettings) -> Unit,
    onNavigateStudio: () -> Unit,
    onNavigatePrivacy: () -> Unit,
    onNavigateHelp: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = { KigoSubScreenTopBar(title = "Application Settings", subtitle = "KIGO CHAT V1.0", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Notification Preferences", fontFamily = SpaceGroteskFamily, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    PrivacySwitchRow("Direct Message Alerts", settings.notifyMessages) {
                        onSaveSettings(settings.copy(notifyMessages = it))
                    }
                    PrivacySwitchRow("Friend Request Alerts", settings.notifyFriendRequests) {
                        onSaveSettings(settings.copy(notifyFriendRequests = it))
                    }
                    PrivacySwitchRow("Group Message Alerts", settings.notifyGroups) {
                        onSaveSettings(settings.copy(notifyGroups = it))
                    }
                    PrivacySwitchRow("Message @Mentions", settings.notifyMentions) {
                        onSaveSettings(settings.copy(notifyMentions = it))
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("E2EE Key Fingerprint (ECDH P-256)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = keyFingerprint,
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Direct text messages use ECDH P-256 + HKDF-SHA256 + AES-256-GCM. Group metadata & media sync use Firestore TLS + security rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(onClick = onNavigateStudio, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Brush, null)
                Spacer(Modifier.width(8.dp))
                Text("Customisation Studio")
            }
            OutlinedButton(onClick = onNavigatePrivacy, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.PrivacyTip, null)
                Spacer(Modifier.width(8.dp))
                Text("Privacy & Data Export")
            }
            OutlinedButton(onClick = onNavigateHelp, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.AutoMirrored.Filled.Help, null)
                Spacer(Modifier.width(8.dp))
                Text("Help & Support")
            }
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("settings_logout_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}

@Composable
fun HelpSupportScreen(
    isAuthenticated: Boolean,
    onSubmitSupportReport: (reason: String, details: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var subject by remember { mutableStateOf("General Support") }
    var details by remember { mutableStateOf("") }

    Scaffold(
        topBar = { KigoSubScreenTopBar(title = "Help & Support", subtitle = "KIGO CHAT V1.0", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HelpCard(
                title = "How KIGO IDs Work",
                body = "When you create your profile, Firestore runs an atomic transaction to mint a unique permanent KIGO-XXXXXX ID. Share it with friends or use kigochat://connect?id=KIGO-XXXXXX."
            )
            HelpCard(
                title = "End-to-End Encryption vs Standard Sync",
                body = "Private 1-on-1 text messages are encrypted locally using ECDH P-256 + HKDF-SHA256 + AES-256-GCM and marked with the E2EE lock badge. Group announcements, profiles, and theme preferences use authenticated Firestore sync."
            )
            HelpCard(
                title = "Media, Voice & Location Consent",
                body = "Photos use the zero-permission Android Photo Picker. Voice recording prompts for Microphone permission, and Location sharing requires explicit consent before querying coarse GPS coordinates."
            )

            if (isAuthenticated) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Contact Support / Submit Feedback", fontFamily = SpaceGroteskFamily, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Topic") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = details,
                            onValueChange = { details = it },
                            label = { Text("Describe your question or issue") },
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (details.isNotBlank()) {
                                    onSubmitSupportReport(subject, details)
                                    details = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Submit Ticket")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpCard(title: String, body: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontFamily = SpaceGroteskFamily, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
