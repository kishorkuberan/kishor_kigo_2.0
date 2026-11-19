package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.crypto.KigoCryptoManager
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class KigoChatRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val cryptoManager: KigoCryptoManager? = null,
    private val socialRepository: KigoSocialRepository = KigoSocialRepository(db, auth)
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
        cryptoManager = KigoCryptoManager(context.applicationContext)
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> = flow {
        val query = db.collection("conversations")
            .whereArrayContains("participantIds", userId)
        emitAll(
            query.snapshots().map { snap ->
                snap.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.lastMessageAt ?: it.updatedAt ?: it.createdAt }
            }
        )
    }

    fun observeConversation(conversationId: String): Flow<Conversation?> = flow {
        val docRef = db.collection("conversations").document(conversationId)
        emitAll(
            docRef.snapshots().map { snap ->
                if (!snap.exists()) null
                else snap.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        )
    }

    fun observeMessages(conversationId: String, pageLimit: Long = 40L): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val query = db.collection("conversations").document(conversationId)
            .collection("messages")
            .whereArrayContains("participantIds", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(pageLimit)
        emitAll(
            query.snapshots().map { snap ->
                snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .reversed()
            }
        )
    }

    suspend fun getOrCreateDirectConversation(myProfile: UserProfile, partner: UserProfile): Result<Conversation> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val sortedPair = listOf(uid, partner.userId).sorted()
        val convId = "dm_${sortedPair[0]}_${sortedPair[1]}"
        val convRef = db.collection("conversations").document(convId)

        return try {
            val existing = convRef.get().await()
            if (existing.exists()) {
                val conv = existing.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: return Result.failure(IllegalStateException("Conversation unreadable"))
                return Result.success(conv)
            }

            val payload = mapOf(
                "type" to "direct",
                "title" to partner.displayName.take(100),
                "photoUrl" to partner.photoUrl,
                "participantIds" to sortedPair,
                "adminIds" to sortedPair,
                "createdBy" to uid,
                "lastMessagePreview" to "End-to-end encrypted chat ready",
                "lastMessageType" to "system",
                "lastMessageSenderId" to uid,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "unreadCounts" to sortedPair.associateWith { 0L },
                "archivedBy" to emptyList<String>(),
                "typingUsers" to emptyMap<String, Boolean>(),
                "isEncrypted" to (myProfile.publicKeyBase64.isNotBlank() && partner.publicKeyBase64.isNotBlank()),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            convRef.set(payload).await()
            val created = convRef.get().await()
                .toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Conversation creation failed"))
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, convRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        conversation: Conversation,
        senderProfile: UserProfile,
        peerPublicKeyBase64: String?,
        type: String,
        plainText: String,
        mediaDataBase64: String = "",
        mediaName: String = "",
        mediaMimeType: String = "",
        mediaSizeBytes: Long = 0L,
        mediaDurationMs: Long = 0L,
        locationLat: Double? = null,
        locationLng: Double? = null,
        locationLabel: String = "",
        replyToMessageId: String = "",
        replyToPreview: String = "",
        forwardedFrom: String = ""
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val convRef = db.collection("conversations").document(conversation.id)
        val msgRef = convRef.collection("messages").document()

        val canEncrypt = conversation.type == "direct" &&
            !peerPublicKeyBase64.isNullOrBlank() &&
            type == "text" &&
            plainText.isNotBlank() &&
            cryptoManager != null

        val encrypted = if (canEncrypt) {
            cryptoManager!!.encryptDirectMessage(
                myUserId = uid,
                peerPublicKeyBase64 = peerPublicKeyBase64!!,
                conversationId = conversation.id,
                plaintext = plainText
            )
        } else null

        val payload = buildMap<String, Any> {
            put("conversationId", conversation.id)
            put("senderId", uid)
            put("senderName", senderProfile.displayName.take(60))
            put("participantIds", conversation.participantIds)
            put("type", type)
            if (encrypted != null) {
                put("ciphertext", encrypted.ciphertextBase64)
                put("ivBase64", encrypted.ivBase64)
                put("isEncrypted", true)
            } else {
                put("text", plainText.take(4000))
                put("isEncrypted", false)
            }
            if (mediaDataBase64.isNotBlank()) put("mediaDataBase64", mediaDataBase64)
            if (mediaName.isNotBlank()) put("mediaName", mediaName.take(160))
            if (mediaMimeType.isNotBlank()) put("mediaMimeType", mediaMimeType.take(100))
            if (mediaSizeBytes > 0L) put("mediaSizeBytes", mediaSizeBytes)
            if (mediaDurationMs > 0L) put("mediaDurationMs", mediaDurationMs)
            if (locationLat != null) put("locationLat", locationLat)
            if (locationLng != null) put("locationLng", locationLng)
            if (locationLabel.isNotBlank()) put("locationLabel", locationLabel.take(200))
            if (replyToMessageId.isNotBlank()) put("replyToMessageId", replyToMessageId)
            if (replyToPreview.isNotBlank()) put("replyToPreview", replyToPreview.take(200))
            if (forwardedFrom.isNotBlank()) put("forwardedFrom", forwardedFrom.take(80))
            put("edited", false)
            put("deleted", false)
            put("readBy", listOf(uid))
            put("deliveredTo", listOf(uid))
            put("reactions", emptyMap<String, String>())
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }

        val preview = when {
            encrypted != null -> "Encrypted message"
            type == "image" -> "Photo${if (mediaName.isNotBlank()) ": $mediaName" else ""}"
            type == "video" -> "Video${if (mediaName.isNotBlank()) ": $mediaName" else ""}"
            type == "document" -> "Document: ${mediaName.ifBlank { "File" }}"
            type == "voice" -> "Voice message (${(mediaDurationMs / 1000L).coerceAtLeast(1)}s)"
            type == "location" -> "Shared location"
            else -> plainText.take(120)
        }

        val newUnread = conversation.unreadCounts.toMutableMap()
        for (participant in conversation.participantIds) {
            if (participant != uid) {
                newUnread[participant] = (newUnread[participant] ?: 0L) + 1L
            }
        }

        return try {
            db.runBatch { batch ->
                batch.set(msgRef, payload)
                batch.update(
                    convRef,
                    mapOf(
                        "lastMessagePreview" to preview.take(200),
                        "lastMessageType" to type,
                        "lastMessageSenderId" to uid,
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "unreadCounts" to newUnread,
                        "typingUsers.$uid" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            for (recipient in conversation.participantIds) {
                if (recipient != uid) {
                    val isMention = plainText.contains("@")
                    socialRepository.createNotification(
                        recipientId = recipient,
                        senderId = uid,
                        senderName = senderProfile.displayName,
                        type = if (isMention) "mention" else "message",
                        title = if (conversation.type == "group") "${senderProfile.displayName} in ${conversation.title}" else senderProfile.displayName,
                        body = preview,
                        targetId = conversation.id
                    )
                }
            }

            Result.success(msgRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    fun resolveMessageDisplayText(
        message: ChatMessage,
        myUserId: String,
        peerPublicKeyBase64: String?
    ): String {
        if (message.deleted) return "This message was deleted"
        if (message.isEncrypted && message.ciphertext.isNotBlank()) {
            if (cryptoManager != null && !peerPublicKeyBase64.isNullOrBlank()) {
                val decrypted = cryptoManager.decryptDirectMessage(
                    myUserId = myUserId,
                    peerPublicKeyBase64 = peerPublicKeyBase64,
                    conversationId = message.conversationId,
                    ciphertextBase64 = message.ciphertext,
                    ivBase64 = message.ivBase64
                )
                if (decrypted != null) return decrypted
            }
            return "Encrypted message (syncing key)"
        }
        return message.text
    }

    suspend fun editMessage(
        conversation: Conversation,
        message: ChatMessage,
        newText: String,
        peerPublicKeyBase64: String?
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val msgRef = db.collection("conversations").document(conversation.id)
            .collection("messages").document(message.id)

        val updates = mutableMapOf<String, Any>(
            "edited" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (message.isEncrypted && cryptoManager != null && !peerPublicKeyBase64.isNullOrBlank()) {
            val enc = cryptoManager.encryptDirectMessage(uid, peerPublicKeyBase64, conversation.id, newText)
            if (enc != null) {
                updates["ciphertext"] = enc.ciphertextBase64
                updates["ivBase64"] = enc.ivBase64
            } else {
                updates["text"] = newText.take(4000)
            }
        } else {
            updates["text"] = newText.take(4000)
        }

        return try {
            msgRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        val msgRef = db.collection("conversations").document(conversationId)
            .collection("messages").document(messageId)
        return try {
            msgRef.update(
                mapOf(
                    "deleted" to true,
                    "text" to "",
                    "ciphertext" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleReaction(conversationId: String, message: ChatMessage, emoji: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val msgRef = db.collection("conversations").document(conversationId)
            .collection("messages").document(message.id)
        val updated = message.reactions.toMutableMap()
        if (updated[uid] == emoji) {
            updated.remove(uid)
        } else {
            updated[uid] = emoji
        }
        return try {
            msgRef.update(
                mapOf(
                    "reactions" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun markConversationRead(
        conversation: Conversation,
        messages: List<ChatMessage>,
        sendReadReceipt: Boolean
    ) {
        val uid = auth.currentUser?.uid ?: return
        val convRef = db.collection("conversations").document(conversation.id)
        try {
            if ((conversation.unreadCounts[uid] ?: 0L) > 0L) {
                convRef.update(
                    mapOf(
                        "unreadCounts.$uid" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            for (msg in messages) {
                if (msg.senderId != uid) {
                    val needsDelivery = !msg.deliveredTo.contains(uid)
                    val needsRead = sendReadReceipt && !msg.readBy.contains(uid)
                    if (needsDelivery || needsRead) {
                        val msgRef = convRef.collection("messages").document(msg.id)
                        val updates = mutableMapOf<String, Any>(
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        if (needsDelivery) updates["deliveredTo"] = (msg.deliveredTo + uid).distinct()
                        if (needsRead) updates["readBy"] = (msg.readBy + uid).distinct()
                        msgRef.update(updates).await()
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    suspend fun setTypingIndicator(conversationId: String, isTyping: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val convRef = db.collection("conversations").document(conversationId)
        try {
            convRef.update(
                mapOf(
                    "typingUsers.$uid" to isTyping,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    suspend fun toggleArchiveConversation(conversation: Conversation): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val convRef = db.collection("conversations").document(conversation.id)
        val updated = if (conversation.archivedBy.contains(uid)) {
            conversation.archivedBy.filter { it != uid }
        } else {
            (conversation.archivedBy + uid).distinct()
        }
        return try {
            convRef.update(
                mapOf(
                    "archivedBy" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchAllMediaMessages(userId: String, conversationIds: List<String>): Result<List<ChatMessage>> {
        if (conversationIds.isEmpty()) return Result.success(emptyList())
        return try {
            val collected = mutableListOf<ChatMessage>()
            for (convId in conversationIds.take(15)) {
                val snap = db.collection("conversations").document(convId)
                    .collection("messages")
                    .whereArrayContains("participantIds", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(30)
                    .get()
                    .await()
                val msgs = snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { !it.deleted && it.type in listOf("image", "video", "document", "voice") }
                collected.addAll(msgs)
            }
            Result.success(collected.sortedByDescending { it.createdAt })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "conversations/messages")
            Result.failure(e)
        }
    }
}
