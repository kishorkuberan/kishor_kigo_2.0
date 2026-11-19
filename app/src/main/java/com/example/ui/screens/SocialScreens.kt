package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BlockedUser
import com.example.data.model.FriendRequest
import com.example.data.model.Friendship
import com.example.data.model.GroupInfo
import com.example.data.model.GroupMember
import com.example.data.model.UserProfile
import com.example.ui.components.KigoAvatar
import com.example.ui.components.KigoEmptyState
import com.example.ui.components.KigoIdChip
import com.example.ui.components.KigoSubScreenTopBar
import com.example.ui.theme.SpaceGroteskFamily
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    myUserId: String,
    friendships: List<Friendship>,
    peerProfiles: Map<String, UserProfile>,
    blockedUsers: List<BlockedUser>,
    pendingIncomingCount: Int,
    onOpenDirectChat: (String) -> Unit,
    onNavigateUserSearch: () -> Unit,
    onNavigateFriendRequests: () -> Unit,
    onRemoveFriend: (String) -> Unit,
    onBlockUser: (targetId: String, name: String, kigoId: String) -> Unit,
    onReportUser: (targetId: String, reason: String, details: String) -> Unit
) {
    val blockedIds = remember(blockedUsers) { blockedUsers.map { it.blockedUserId }.toSet() }
    var reportTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends", fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onNavigateFriendRequests, modifier = Modifier.testTag("friends_requests_btn")) {
                        Text(if (pendingIncomingCount > 0) "Requests ($pendingIncomingCount)" else "Requests")
                    }
                    IconButton(onClick = onNavigateUserSearch, modifier = Modifier.testTag("friends_search_btn")) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Find Users")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        val visibleFriends = friendships.filter { !blockedIds.contains(it.partnerIdFor(myUserId)) }
        if (visibleFriends.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                KigoEmptyState(
                    title = "No Friends Added Yet",
                    subtitle = "Search by KIGO ID (KIGO-XXXXXX) or @username to connect and start chatting.",
                    actionLabel = "Search Users",
                    onAction = onNavigateUserSearch
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(visibleFriends, key = { it.id }) { fs ->
                    val partnerId = fs.partnerIdFor(myUserId)
                    val peer = peerProfiles[partnerId]
                    val name = peer?.displayName ?: fs.partnerDisplayNameFor(myUserId)
                    val kigoId = peer?.kigoId ?: fs.partnerKigoIdFor(myUserId)
                    val photo = peer?.photoUrl ?: fs.partnerPhotoUrlFor(myUserId)

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(14.dp)
                        ) {
                            KigoAvatar(
                                displayName = name,
                                photoUrl = photo,
                                size = 48.dp,
                                isOnline = peer?.online == true && peer.showOnlineStatus,
                                showOnlineIndicator = true
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                KigoIdChip(kigoId = kigoId)
                            }
                            IconButton(onClick = { onOpenDirectChat(partnerId) }, modifier = Modifier.testTag("friend_chat_$partnerId")) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onBlockUser(partnerId, name, kigoId) }) {
                                Icon(Icons.Default.Block, contentDescription = "Block user", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { reportTarget = partnerId to name }) {
                                Icon(Icons.Default.Flag, contentDescription = "Report user", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onRemoveFriend(fs.id) }) {
                                Icon(Icons.Default.PersonRemove, contentDescription = "Remove friend", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    reportTarget?.let { (targetId, targetName) ->
        ReportUserDialog(
            targetName = targetName,
            onDismiss = { reportTarget = null },
            onSubmit = { reason, details ->
                onReportUser(targetId, reason, details)
                reportTarget = null
            }
        )
    }
}

@Composable
fun UserSearchScreen(
    searchResults: List<UserProfile>,
    isSearching: Boolean,
    friendships: List<Friendship>,
    friendRequests: List<FriendRequest>,
    myUserId: String,
    onSearch: (String) -> Unit,
    onSendRequest: (UserProfile) -> Unit,
    onOpenDirectChat: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var query by remember { mutableStateOf("") }
    val friendIds = remember(friendships) { friendships.map { it.partnerIdFor(myUserId) }.toSet() }
    val pendingTargetIds = remember(friendRequests) {
        friendRequests.filter { it.status == "pending" }.flatMap { it.participantIds }.toSet()
    }

    Scaffold(
        topBar = { KigoSubScreenTopBar(title = "Find People", subtitle = "Search by KIGO ID or @username", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("KIGO-829461 or @username") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).testTag("user_search_input")
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSearch(query) },
                    modifier = Modifier.height(54.dp).testTag("user_search_submit"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Find")
                }
            }

            if (isSearching) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (searchResults.isEmpty()) {
                KigoEmptyState(
                    title = "Discover Friends Securely",
                    subtitle = "Enter an exact KIGO-XXXXXX ID or searchable @username. Private account info like email is never exposed."
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(searchResults, key = { it.userId }) { user ->
                        val isFriend = friendIds.contains(user.userId)
                        val isPending = pendingTargetIds.contains(user.userId)
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(14.dp)
                            ) {
                                KigoAvatar(displayName = user.displayName, photoUrl = user.photoUrl, size = 48.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(4.dp))
                                    KigoIdChip(kigoId = user.kigoId)
                                }
                                when {
                                    isFriend -> Button(onClick = { onOpenDirectChat(user.userId) }) { Text("Chat") }
                                    isPending -> OutlinedButton(onClick = {}, enabled = false) { Text("Pending") }
                                    else -> Button(
                                        onClick = { onSendRequest(user) },
                                        modifier = Modifier.testTag("send_friend_request_${user.userId}")
                                    ) {
                                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Add")
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

@Composable
fun FriendRequestsScreen(
    myUserId: String,
    friendRequests: List<FriendRequest>,
    onAccept: (FriendRequest) -> Unit,
    onReject: (String) -> Unit,
    onCancel: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var tabIndex by remember { mutableIntStateOf(0) }
    val incoming = remember(friendRequests) {
        friendRequests.filter { it.toUserId == myUserId && it.status == "pending" }
    }
    val outgoing = remember(friendRequests) {
        friendRequests.filter { it.fromUserId == myUserId && it.status == "pending" }
    }

    Scaffold(
        topBar = { KigoSubScreenTopBar(title = "Friend Requests", onBack = onBack) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Incoming (${incoming.size})") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Sent (${outgoing.size})") })
            }
            val list = if (tabIndex == 0) incoming else outgoing
            if (list.isEmpty()) {
                KigoEmptyState(
                    title = if (tabIndex == 0) "No Incoming Requests" else "No Sent Requests",
                    subtitle = "Pending friend requests synchronize in real time."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(list, key = { it.id }) { req ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(14.dp)
                            ) {
                                val title = if (tabIndex == 0) req.fromDisplayName else req.toDisplayName
                                val kigo = if (tabIndex == 0) req.fromKigoId else req.toKigoId
                                KigoAvatar(displayName = title, photoUrl = req.fromPhotoUrl, size = 46.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title.ifBlank { "KIGO User" }, style = MaterialTheme.typography.titleMedium)
                                    KigoIdChip(kigoId = kigo)
                                }
                                if (tabIndex == 0) {
                                    IconButton(onClick = { onAccept(req) }, modifier = Modifier.testTag("accept_req_${req.id}")) {
                                        Icon(Icons.Default.Check, contentDescription = "Accept", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { onReject(req.id) }, modifier = Modifier.testTag("reject_req_${req.id}")) {
                                        Icon(Icons.Default.Close, contentDescription = "Reject", tint = MaterialTheme.colorScheme.error)
                                    }
                                } else {
                                    TextButton(onClick = { onCancel(req.id) }) { Text("Cancel") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    myUserId: String,
    groups: List<GroupInfo>,
    friendships: List<Friendship>,
    peerProfiles: Map<String, UserProfile>,
    observeMembers: (String) -> Flow<List<GroupMember>>,
    onOpenConversation: (String) -> Unit,
    onCreateGroup: (name: String, desc: String, friendIds: List<String>, isPrivate: Boolean, requireApproval: Boolean) -> Unit,
    onAddMember: (GroupInfo, String) -> Unit,
    onRemoveOrLeave: (GroupInfo, String) -> Unit,
    onToggleAdmin: (GroupInfo, String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var managingGroup by remember { mutableStateOf<GroupInfo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Groups", fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("groups_create_fab")
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = "Create Group")
            }
        }
    ) { padding ->
        if (groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                KigoEmptyState(
                    title = "No Groups Yet",
                    subtitle = "Create a group to collaborate, share files, and manage members with role controls.",
                    actionLabel = "Create Group",
                    onAction = { showCreateDialog = true }
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(groups, key = { it.id }) { group ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().clickable { onOpenConversation(group.conversationId) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            KigoAvatar(displayName = group.name, photoUrl = group.photoUrl, size = 50.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${group.memberIds.size} members · ${if (group.isPrivate) "Private" else "Standard"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { managingGroup = group }) {
                                Text("Manage")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var groupName by remember { mutableStateOf("") }
        var groupDesc by remember { mutableStateOf("") }
        var isPrivate by remember { mutableStateOf(false) }
        var requireApproval by remember { mutableStateOf(false) }
        val selectedFriends = remember { mutableStateListOf<String>() }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it.take(80) },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("create_group_name_input")
                    )
                    OutlinedTextField(
                        value = groupDesc,
                        onValueChange = { groupDesc = it.take(200) },
                        label = { Text("Description") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Private Group", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Admin Approval", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = requireApproval, onCheckedChange = { requireApproval = it })
                    }
                    if (friendships.isNotEmpty()) {
                        Text("Invite Friends:", style = MaterialTheme.typography.labelMedium)
                        friendships.take(6).forEach { fs ->
                            val pid = fs.partnerIdFor(myUserId)
                            val name = peerProfiles[pid]?.displayName ?: fs.partnerDisplayNameFor(myUserId)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (selectedFriends.contains(pid)) selectedFriends.remove(pid)
                                    else selectedFriends.add(pid)
                                }
                            ) {
                                Checkbox(
                                    checked = selectedFriends.contains(pid),
                                    onCheckedChange = { checked ->
                                        if (checked) selectedFriends.add(pid) else selectedFriends.remove(pid)
                                    }
                                )
                                Text(name, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            onCreateGroup(groupName, groupDesc, selectedFriends.toList(), isPrivate, requireApproval)
                            showCreateDialog = false
                        }
                    },
                    modifier = Modifier.testTag("create_group_confirm")
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    managingGroup?.let { group ->
        val members by observeMembers(group.id).collectAsStateWithLifecycle(initialValue = emptyList())
        val isAdmin = group.adminIds.contains(myUserId)
        val nonMembers = friendships.map { it.partnerIdFor(myUserId) }.filter { !group.memberIds.contains(it) }

        AlertDialog(
            onDismissRequest = { managingGroup = null },
            title = { Text(group.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (group.description.isNotBlank()) {
                        Text(group.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Members (${members.size})", style = MaterialTheme.typography.labelMedium)
                    members.forEach { m ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${m.displayName} (${m.role})", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            if (isAdmin && m.userId != myUserId) {
                                IconButton(onClick = { onToggleAdmin(group, m.userId) }) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = "Toggle admin", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onRemoveOrLeave(group, m.userId) }) {
                                    Icon(Icons.Default.PersonRemove, contentDescription = "Remove member", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    if (isAdmin && nonMembers.isNotEmpty()) {
                        Text("Add Friend:", style = MaterialTheme.typography.labelMedium)
                        nonMembers.take(3).forEach { uid ->
                            val label = peerProfiles[uid]?.displayName ?: "Friend"
                            TextButton(onClick = { onAddMember(group, uid) }) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add $label")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val convId = group.conversationId
                    managingGroup = null
                    onOpenConversation(convId)
                }) { Text("Open Chat") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onRemoveOrLeave(group, myUserId)
                    managingGroup = null
                }) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Leave", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@Composable
fun ReportUserDialog(
    targetName: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit
) {
    var reason by remember { mutableStateOf("Spam or Harassment") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report $targetName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = reason, onValueChange = { reason = it.take(100) }, label = { Text("Reason") }, singleLine = true)
                OutlinedTextField(value = details, onValueChange = { details = it.take(400) }, label = { Text("Details (optional)") }, maxLines = 3)
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(reason, details) }) { Text("Submit Report") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
