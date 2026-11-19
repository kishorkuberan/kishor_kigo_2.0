package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Conversation
import com.example.data.model.Friendship
import com.example.data.model.UserProfile
import com.example.ui.components.KigoAvatar
import com.example.ui.components.KigoEmptyState
import com.example.ui.components.KigoIdChip
import com.example.ui.components.KigoLogoBadge
import com.example.ui.components.formatKigoTimestamp
import com.example.ui.theme.SpaceGroteskFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    myProfile: UserProfile,
    conversations: List<Conversation>,
    friendships: List<Friendship>,
    peerProfiles: Map<String, UserProfile>,
    unreadNotificationCount: Int,
    onOpenConversation: (String) -> Unit,
    onOpenDirectChat: (String) -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateNotifications: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateStudio: () -> Unit,
    onNavigateMediaGallery: () -> Unit,
    onNavigateVoiceHub: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateFriends: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val onlineFriends = remember(friendships, peerProfiles) {
        friendships.mapNotNull { fs ->
            val pid = fs.partnerIdFor(myProfile.userId)
            val peer = peerProfiles[pid]
            if (peer != null && peer.online && peer.showOnlineStatus) peer else null
        }
    }

    val filteredConversations = remember(conversations, searchQuery, peerProfiles) {
        conversations.filter { !it.archivedBy.contains(myProfile.userId) }.filter { conv ->
            if (searchQuery.isBlank()) true
            else {
                val title = resolveConversationTitle(conv, myProfile.userId, peerProfiles)
                title.contains(searchQuery, ignoreCase = true) ||
                    conv.lastMessagePreview.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KigoLogoBadge(size = 34.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "KIGO CHAT",
                                fontFamily = SpaceGroteskFamily,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = myProfile.kigoId,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateSearch, modifier = Modifier.testTag("home_search_users_btn")) {
                        Icon(Icons.Default.PersonSearch, contentDescription = "Find Users by KIGO ID")
                    }
                    IconButton(onClick = onNavigateNotifications, modifier = Modifier.testTag("home_notifications_btn")) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge { Text(unreadNotificationCount.coerceAtMost(99).toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(onClick = onNavigateProfile, modifier = Modifier.testTag("home_profile_btn")) {
                        KigoAvatar(
                            displayName = myProfile.displayName,
                            photoUrl = myProfile.photoUrl,
                            size = 34.dp,
                            isOnline = myProfile.online,
                            showOnlineIndicator = true
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateFriends,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("home_new_chat_fab")
            ) {
                Icon(Icons.Default.AddComment, contentDescription = "Start New Chat")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search conversations or messages...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("home_search_bar")
                )
            }

            // Quick Studio & Media Shortcuts
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        QuickActionPill(
                            icon = { Icon(Icons.Default.Brush, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                            label = "Theme Studio",
                            onClick = onNavigateStudio
                        )
                    }
                    item {
                        QuickActionPill(
                            icon = { Icon(Icons.Default.PhotoLibrary, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                            label = "Media & Docs",
                            onClick = onNavigateMediaGallery
                        )
                    }
                    item {
                        QuickActionPill(
                            icon = { Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                            label = "Voice Hub",
                            onClick = onNavigateVoiceHub
                        )
                    }
                    item {
                        QuickActionPill(
                            icon = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                            label = "Settings",
                            onClick = onNavigateSettings
                        )
                    }
                }
            }

            // Online Contacts Strip
            if (onlineFriends.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "ONLINE NOW (${onlineFriends.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(onlineFriends, key = { it.userId }) { friend ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(68.dp)
                                        .clickable { onOpenDirectChat(friend.userId) }
                                ) {
                                    KigoAvatar(
                                        displayName = friend.displayName,
                                        photoUrl = friend.photoUrl,
                                        size = 54.dp,
                                        isOnline = true,
                                        showOnlineIndicator = true
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = friend.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "RECENT CONVERSATIONS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    KigoIdChip(kigoId = myProfile.kigoId)
                }
            }

            if (filteredConversations.isEmpty()) {
                item {
                    KigoEmptyState(
                        title = "No Active Chats Yet",
                        subtitle = "Search for friends using their KIGO ID (e.g. KIGO-829461) or @username to start an encrypted conversation.",
                        actionLabel = "Find Friends",
                        onAction = onNavigateSearch
                    )
                }
            } else {
                items(filteredConversations, key = { it.id }) { conv ->
                    ConversationRowCard(
                        conversation = conv,
                        myUserId = myProfile.userId,
                        peerProfiles = peerProfiles,
                        onClick = { onOpenConversation(conv.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    myUserId: String,
    conversations: List<Conversation>,
    peerProfiles: Map<String, UserProfile>,
    onOpenConversation: (String) -> Unit,
    onToggleArchive: (Conversation) -> Unit,
    onNewChat: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") } // all, unread, groups, archived

    val displayed = remember(conversations, query, filter, peerProfiles) {
        conversations.filter { conv ->
            val isArchived = conv.archivedBy.contains(myUserId)
            val unread = conv.unreadCounts[myUserId] ?: 0L
            val filterMatch = when (filter) {
                "unread" -> !isArchived && unread > 0L
                "groups" -> !isArchived && conv.type == "group"
                "archived" -> isArchived
                else -> !isArchived
            }
            val title = resolveConversationTitle(conv, myUserId, peerProfiles)
            val searchMatch = query.isBlank() ||
                title.contains(query, ignoreCase = true) ||
                conv.lastMessagePreview.contains(query, ignoreCase = true)
            filterMatch && searchMatch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats", fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewChat,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("chats_new_btn")
            ) {
                Icon(Icons.Default.AddComment, contentDescription = "New Chat")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search chats...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("chats_search_input")
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "All", "unread" to "Unread", "groups" to "Groups", "archived" to "Archived").forEach { (key, label) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (displayed.isEmpty()) {
                KigoEmptyState(
                    title = if (filter == "archived") "No Archived Chats" else "No Matching Conversations",
                    subtitle = "Start a conversation with a friend or create a group.",
                    actionLabel = "Browse Friends",
                    onAction = onNewChat
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(displayed, key = { it.id }) { conv ->
                        ConversationRowCard(
                            conversation = conv,
                            myUserId = myUserId,
                            peerProfiles = peerProfiles,
                            onClick = { onOpenConversation(conv.id) },
                            trailingAction = {
                                val archived = conv.archivedBy.contains(myUserId)
                                IconButton(onClick = { onToggleArchive(conv) }) {
                                    Icon(
                                        imageVector = if (archived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = if (archived) "Unarchive" else "Archive",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            icon()
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ConversationRowCard(
    conversation: Conversation,
    myUserId: String,
    peerProfiles: Map<String, UserProfile>,
    onClick: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    val peerId = if (conversation.type == "direct") {
        conversation.participantIds.firstOrNull { it != myUserId }
    } else null
    val peer = peerId?.let { peerProfiles[it] }
    val title = resolveConversationTitle(conversation, myUserId, peerProfiles)
    val avatarPhoto = peer?.photoUrl ?: conversation.photoUrl
    val isOnline = peer?.let { it.online && it.showOnlineStatus } ?: false
    val unread = (conversation.unreadCounts[myUserId] ?: 0L).toInt()
    val typing = conversation.typingUsers.any { (uid, isTyping) -> uid != myUserId && isTyping }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (unread > 0) 1.2.dp else 1.dp,
            color = if (unread > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("conversation_row_${conversation.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            KigoAvatar(
                displayName = title,
                photoUrl = avatarPhoto,
                size = 50.dp,
                isOnline = isOnline,
                showOnlineIndicator = conversation.type == "direct"
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    if (conversation.type == "group") {
                        Icon(Icons.Default.Group, contentDescription = "Group", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    } else if (conversation.isEncrypted) {
                        Icon(Icons.Default.Lock, contentDescription = "Encrypted", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (typing) "Typing..." else conversation.lastMessagePreview.ifBlank { "Tap to start chatting" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (typing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatKigoTimestamp(conversation.lastMessageAt ?: conversation.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unread > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (unread > 0) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Text(
                            text = unread.coerceAtMost(99).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (trailingAction != null) {
                trailingAction()
            }
        }
    }
}

fun resolveConversationTitle(
    conv: Conversation,
    myUserId: String,
    peerProfiles: Map<String, UserProfile>
): String {
    if (conv.type == "group") return conv.title.ifBlank { "KIGO Group" }
    val peerId = conv.participantIds.firstOrNull { it != myUserId }
    val peer = peerId?.let { peerProfiles[it] }
    return peer?.displayName?.ifBlank { conv.title } ?: conv.title.ifBlank { "Direct Chat" }
}
