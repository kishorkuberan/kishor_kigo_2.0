package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val kigoId: String = "",
    val username: String = "",
    val displayName: String = "",
    val about: String = "",
    val customStatus: String = "",
    val photoUrl: String = "",
    val online: Boolean = false,
    @ServerTimestamp val lastSeen: Timestamp? = null,
    val publicKeyBase64: String = "",
    val showOnlineStatus: Boolean = true,
    val showLastSeen: Boolean = true,
    val showReadReceipts: Boolean = true,
    val allowFriendRequests: Boolean = true,
    val allowDiscovery: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "kigoId" to kigoId,
        "username" to username,
        "displayName" to displayName,
        "about" to about,
        "customStatus" to customStatus,
        "photoUrl" to photoUrl,
        "online" to online,
        "lastSeen" to FieldValue.serverTimestamp(),
        "publicKeyBase64" to publicKeyBase64,
        "showOnlineStatus" to showOnlineStatus,
        "showLastSeen" to showLastSeen,
        "showReadReceipts" to showReadReceipts,
        "allowFriendRequests" to allowFriendRequests,
        "allowDiscovery" to allowDiscovery,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class UsernameRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val kigoId: String = "",
    val photoUrl: String = "",
    val allowDiscovery: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class KigoIdRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val kigoId: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FriendRequest(
    @DocumentId val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val fromDisplayName: String = "",
    val fromKigoId: String = "",
    val fromPhotoUrl: String = "",
    val toDisplayName: String = "",
    val toKigoId: String = "",
    val participantIds: List<String> = emptyList(),
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Friendship(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val userAId: String = "",
    val userADisplayName: String = "",
    val userAKigoId: String = "",
    val userAPhotoUrl: String = "",
    val userBId: String = "",
    val userBDisplayName: String = "",
    val userBKigoId: String = "",
    val userBPhotoUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun partnerIdFor(myUid: String): String = if (userAId == myUid) userBId else userAId
    fun partnerDisplayNameFor(myUid: String): String = if (userAId == myUid) userBDisplayName else userADisplayName
    fun partnerKigoIdFor(myUid: String): String = if (userAId == myUid) userBKigoId else userAKigoId
    fun partnerPhotoUrlFor(myUid: String): String = if (userAId == myUid) userBPhotoUrl else userAPhotoUrl
}

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val type: String = "direct",
    val title: String = "",
    val description: String = "",
    val photoUrl: String = "",
    val participantIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val createdBy: String = "",
    val lastMessagePreview: String = "",
    val lastMessageType: String = "text",
    val lastMessageSenderId: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    val unreadCounts: Map<String, Long> = emptyMap(),
    val archivedBy: List<String> = emptyList(),
    val typingUsers: Map<String, Boolean> = emptyMap(),
    @field:PropertyName("isEncrypted") @get:PropertyName("isEncrypted") val isEncrypted: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val participantIds: List<String> = emptyList(),
    val type: String = "text",
    val text: String = "",
    val ciphertext: String = "",
    val ivBase64: String = "",
    @field:PropertyName("isEncrypted") @get:PropertyName("isEncrypted") val isEncrypted: Boolean = false,
    val mediaDataBase64: String = "",
    val mediaName: String = "",
    val mediaMimeType: String = "",
    val mediaSizeBytes: Long = 0L,
    val mediaDurationMs: Long = 0L,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationLabel: String = "",
    val replyToMessageId: String = "",
    val replyToPreview: String = "",
    val forwardedFrom: String = "",
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val readBy: List<String> = emptyList(),
    val deliveredTo: List<String> = emptyList(),
    val reactions: Map<String, String> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GroupInfo(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val photoUrl: String = "",
    val conversationId: String = "",
    val createdBy: String = "",
    val adminIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    @field:PropertyName("isPrivate") @get:PropertyName("isPrivate") val isPrivate: Boolean = false,
    val requireAdminApproval: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GroupMember(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val kigoId: String = "",
    val photoUrl: String = "",
    val role: String = "member",
    val memberIds: List<String> = emptyList(),
    @ServerTimestamp val joinedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "message",
    val title: String = "",
    val body: String = "",
    val targetId: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val themeMode: String = "dark",
    val accentPreset: String = "cyan",
    val bubbleColorHex: String = "#00B8D4",
    val fontStyle: String = "jakarta",
    val fontScale: String = "standard",
    val messageLayout: String = "modern",
    val navigationStyle: String = "docked",
    val profileTheme: String = "aurora",
    val chatWallpaper: String = "aurora",
    val notificationSound: String = "pulse",
    val notifyMessages: Boolean = true,
    val notifyFriendRequests: Boolean = true,
    val notifyGroups: Boolean = true,
    val notifyMentions: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "themeMode" to themeMode,
        "accentPreset" to accentPreset,
        "bubbleColorHex" to bubbleColorHex,
        "fontStyle" to fontStyle,
        "fontScale" to fontScale,
        "messageLayout" to messageLayout,
        "navigationStyle" to navigationStyle,
        "profileTheme" to profileTheme,
        "chatWallpaper" to chatWallpaper,
        "notificationSound" to notificationSound,
        "notifyMessages" to notifyMessages,
        "notifyFriendRequests" to notifyFriendRequests,
        "notifyGroups" to notifyGroups,
        "notifyMentions" to notifyMentions,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class BlockedUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val blockedUserId: String = "",
    val blockedDisplayName: String = "",
    val blockedKigoId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val reportedUserId: String = "",
    val reportedMessageId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "open",
    @ServerTimestamp val createdAt: Timestamp? = null
)
