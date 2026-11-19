package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.KigoCryptoManager
import com.example.data.firebase.unwrapFirestoreException
import com.example.data.model.BlockedUser
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.FriendRequest
import com.example.data.model.Friendship
import com.example.data.model.GroupInfo
import com.example.data.model.GroupMember
import com.example.data.model.NotificationItem
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.UserSettings
import com.example.data.repository.KigoAuthRepository
import com.example.data.repository.KigoChatRepository
import com.example.data.repository.KigoSettingsRepository
import com.example.data.repository.KigoSocialRepository
import com.example.data.repository.KigoUserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KigoMainViewModel(
    val currentUserId: String,
    private val userRepository: KigoUserRepository,
    private val socialRepository: KigoSocialRepository,
    private val chatRepository: KigoChatRepository,
    private val settingsRepository: KigoSettingsRepository,
    private val authRepository: KigoAuthRepository,
    private val cryptoManager: KigoCryptoManager
) : ViewModel() {

    val myProfileState: StateFlow<UiState<UserProfile?>> = userRepository.observeMyProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = unwrapFirestoreException(error)
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Profile stream error", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val userSettings: StateFlow<UserSettings> = settingsRepository.observeUserSettings(currentUserId)
        .catch { error ->
            if (unwrapFirestoreException(error) == null && error is CancellationException) throw error
            emit(UserSettings(userId = currentUserId))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UserSettings(userId = currentUserId))

    val conversations: StateFlow<List<Conversation>> = chatRepository.observeConversations(currentUserId)
        .catch { error ->
            if (unwrapFirestoreException(error) == null && error is CancellationException) throw error
            Log.w(TAG, "Conversations error", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val friendships: StateFlow<List<Friendship>> = socialRepository.observeFriendships(currentUserId)
        .catch { error ->
            if (unwrapFirestoreException(error) == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val friendRequests: StateFlow<List<FriendRequest>> = socialRepository.observeFriendRequests(currentUserId)
        .catch { error ->
            if (unwrapFirestoreException(error) == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val groups: StateFlow<List<GroupInfo>> = socialRepository.observeGroups(currentUserId)
        .catch { error ->
            if (unwrapFirestoreException(error) == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val notifications: StateFlow<List<NotificationItem>> = socialRepository.observeNotifications(currentUserId)
        .catch { error ->
            if (unwrapFirestoreException(error) == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val blockedUsers: StateFlow<List<BlockedUser>> = userRepository.observeBlockedUsers(currentUserId)
        .catch { error ->
            if (unwrapFirestoreException(error) == null && error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _peerProfiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())
    val peerProfiles: StateFlow<Map<String, UserProfile>> = _peerProfiles.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserProfile>>(emptyList())
    val searchResults: StateFlow<List<UserProfile>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _mediaGallery = MutableStateFlow<List<ChatMessage>>(emptyList())
    val mediaGallery: StateFlow<List<ChatMessage>> = _mediaGallery.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    init {
        viewModelScope.launch {
            conversations.collect { list ->
                val neededUids = list.flatMap { it.participantIds }.filter { it != currentUserId }.distinct()
                fetchMissingPeerProfiles(neededUids)
            }
        }
        viewModelScope.launch {
            friendships.collect { list ->
                val neededUids = list.map { it.partnerIdFor(currentUserId) }.distinct()
                fetchMissingPeerProfiles(neededUids)
            }
        }
    }

    private fun fetchMissingPeerProfiles(uids: List<String>) {
        val missing = uids.filter { !_peerProfiles.value.containsKey(it) }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            val map = _peerProfiles.value.toMutableMap()
            for (uid in missing) {
                userRepository.getUserProfile(uid).getOrNull()?.let { profile ->
                    map[uid] = profile
                }
            }
            _peerProfiles.value = map
        }
    }

    fun getMyKeyFingerprint(): String {
        val pub = cryptoManager.getPublicKeyBase64(currentUserId)
        return cryptoManager.getKeyFingerprint(pub)
    }

    fun setOnlineStatus(online: Boolean) {
        viewModelScope.launch {
            userRepository.updatePresence(online)
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun showBanner(msg: String) {
        _bannerMessage.value = msg
    }

    fun createProfile(
        username: String,
        displayName: String,
        about: String,
        photoUrl: String,
        email: String?,
        onCreated: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = userRepository.createProfileWithUniqueKigoId(username, displayName, about, photoUrl, email)
            _isBusy.value = false
            res.onSuccess {
                _bannerMessage.value = "Welcome! Assigned KIGO ID: ${it.kigoId}"
                onCreated()
            }.onFailure {
                _bannerMessage.value = it.localizedMessage ?: "Profile creation failed"
            }
        }
    }

    fun updateProfile(
        current: UserProfile,
        displayName: String,
        username: String,
        about: String,
        customStatus: String,
        photoUrl: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = userRepository.updateProfile(current, displayName, username, about, customStatus, photoUrl)
            _isBusy.value = false
            _bannerMessage.value = if (res.isSuccess) "Profile updated" else (res.exceptionOrNull()?.localizedMessage ?: "Update failed")
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            val res = userRepository.searchUsers(query)
            _isSearching.value = false
            res.onSuccess { _searchResults.value = it }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Search failed" }
        }
    }

    fun sendFriendRequest(target: UserProfile) {
        val me = (myProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = socialRepository.sendFriendRequest(me, target)
            _bannerMessage.value = if (res.isSuccess) {
                "Friend request sent to ${target.displayName}"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Could not send request"
            }
        }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        val me = (myProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = socialRepository.acceptFriendRequest(request, me)
            _bannerMessage.value = if (res.isSuccess) "Friend added!" else (res.exceptionOrNull()?.localizedMessage ?: "Failed")
        }
    }

    fun rejectFriendRequest(requestId: String) {
        viewModelScope.launch {
            socialRepository.rejectFriendRequest(requestId)
        }
    }

    fun cancelFriendRequest(requestId: String) {
        viewModelScope.launch {
            socialRepository.cancelFriendRequest(requestId)
        }
    }

    fun removeFriend(friendshipId: String) {
        viewModelScope.launch {
            val res = socialRepository.removeFriend(friendshipId)
            if (res.isSuccess) _bannerMessage.value = "Friend removed"
        }
    }

    fun openDirectChat(partnerId: String, onOpen: (String) -> Unit) {
        val me = (myProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val partner = _peerProfiles.value[partnerId] ?: userRepository.getUserProfile(partnerId).getOrNull()
            if (partner == null) {
                _isBusy.value = false
                _bannerMessage.value = "Could not load user profile"
                return@launch
            }
            _peerProfiles.value = _peerProfiles.value + (partnerId to partner)
            val res = chatRepository.getOrCreateDirectConversation(me, partner)
            _isBusy.value = false
            res.onSuccess { onOpen(it.id) }
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Unable to start chat" }
        }
    }

    fun createGroup(
        name: String,
        description: String,
        photoUrl: String,
        selectedFriendIds: List<String>,
        isPrivate: Boolean,
        requireApproval: Boolean,
        onCreated: (String) -> Unit
    ) {
        val me = (myProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val members = selectedFriendIds.mapNotNull { uid ->
                _peerProfiles.value[uid] ?: userRepository.getUserProfile(uid).getOrNull()
            }
            val res = socialRepository.createGroup(name, description, photoUrl, me, members, isPrivate, requireApproval)
            _isBusy.value = false
            res.onSuccess {
                _bannerMessage.value = "Group '${it.name}' created"
                onCreated(it.conversationId)
            }.onFailure {
                _bannerMessage.value = it.localizedMessage ?: "Failed to create group"
            }
        }
    }

    fun observeGroupMembers(groupId: String): Flow<List<GroupMember>> =
        socialRepository.observeGroupMembers(groupId)

    fun addMemberToGroup(group: GroupInfo, userId: String) {
        viewModelScope.launch {
            val profile = _peerProfiles.value[userId] ?: userRepository.getUserProfile(userId).getOrNull() ?: return@launch
            val res = socialRepository.addMemberToGroup(group, profile)
            _bannerMessage.value = if (res.isSuccess) "Added ${profile.displayName}" else (res.exceptionOrNull()?.localizedMessage ?: "Failed")
        }
    }

    fun removeOrLeaveGroup(group: GroupInfo, userId: String) {
        viewModelScope.launch {
            val res = socialRepository.removeMemberOrLeaveGroup(group, userId)
            _bannerMessage.value = if (res.isSuccess) {
                if (userId == currentUserId) "Left group" else "Member removed"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Action failed"
            }
        }
    }

    fun toggleGroupAdmin(group: GroupInfo, userId: String) {
        viewModelScope.launch {
            socialRepository.toggleGroupAdmin(group, userId)
        }
    }

    fun saveSettings(settings: UserSettings) {
        viewModelScope.launch {
            val res = settingsRepository.saveUserSettings(settings)
            if (res.isFailure) {
                _bannerMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save settings"
            }
        }
    }

    fun updatePrivacy(
        showOnlineStatus: Boolean,
        showLastSeen: Boolean,
        showReadReceipts: Boolean,
        allowFriendRequests: Boolean,
        allowDiscovery: Boolean
    ) {
        val current = (myProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val res = userRepository.updatePrivacySettings(
                current, showOnlineStatus, showLastSeen, showReadReceipts, allowFriendRequests, allowDiscovery
            )
            _bannerMessage.value = if (res.isSuccess) "Privacy preferences saved" else (res.exceptionOrNull()?.localizedMessage ?: "Failed")
        }
    }

    fun blockUser(targetId: String, name: String, kigoId: String) {
        viewModelScope.launch {
            val res = userRepository.blockUser(targetId, name, kigoId)
            _bannerMessage.value = if (res.isSuccess) "$name blocked" else (res.exceptionOrNull()?.localizedMessage ?: "Failed")
        }
    }

    fun unblockUser(targetId: String) {
        viewModelScope.launch {
            userRepository.unblockUser(targetId)
            _bannerMessage.value = "User unblocked"
        }
    }

    fun submitReport(reportedUserId: String, reason: String, details: String, messageId: String = "") {
        viewModelScope.launch {
            val res = userRepository.submitReport(reportedUserId, messageId, reason, details)
            _bannerMessage.value = if (res.isSuccess) "Report submitted securely" else (res.exceptionOrNull()?.localizedMessage ?: "Failed")
        }
    }

    fun toggleArchiveConversation(conversation: Conversation) {
        viewModelScope.launch {
            chatRepository.toggleArchiveConversation(conversation)
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            socialRepository.markNotificationRead(id)
        }
    }

    fun refreshMediaGallery() {
        viewModelScope.launch {
            val convIds = conversations.value.map { it.id }
            val res = chatRepository.fetchAllMediaMessages(currentUserId, convIds)
            res.onSuccess { _mediaGallery.value = it }
        }
    }

    fun exportMyData(onJsonReady: (String) -> Unit) {
        val profile = (myProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = userRepository.exportUserDataJson(profile)
            _isBusy.value = false
            res.onSuccess(onJsonReady)
                .onFailure { _bannerMessage.value = it.localizedMessage ?: "Export failed" }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            userRepository.updatePresence(false)
            authRepository.signOut(context)
        }
    }

    fun deleteAccount(context: Context, onDeleted: () -> Unit) {
        val profile = (myProfileState.value as? UiState.Success)?.data
        viewModelScope.launch {
            _isBusy.value = true
            userRepository.deleteUserFirestoreData(profile)
            val res = authRepository.deleteAuthAccount(context)
            _isBusy.value = false
            res.onSuccess { onDeleted() }
                .onFailure {
                    _bannerMessage.value = it.localizedMessage ?: "Please re-authenticate and retry account deletion."
                }
        }
    }

    private companion object {
        const val TAG = "KigoMainVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
