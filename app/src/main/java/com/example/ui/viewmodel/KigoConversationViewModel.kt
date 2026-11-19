package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.unwrapFirestoreException
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.UserProfile
import com.example.data.repository.KigoChatRepository
import com.example.data.repository.KigoUserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class KigoConversationViewModel(
    val conversationId: String,
    val currentUserId: String,
    private val chatRepository: KigoChatRepository,
    private val userRepository: KigoUserRepository
) : ViewModel() {

    private val _pageLimit = MutableStateFlow(40L)

    val conversation: StateFlow<Conversation?> = chatRepository.observeConversation(conversationId)
        .catch { error ->
            if (unwrapFirestoreException(error) == null && error is CancellationException) throw error
            Log.w(TAG, "Conversation stream error", error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val messages: StateFlow<List<ChatMessage>> = _pageLimit
        .flatMapLatest { limit -> chatRepository.observeMessages(conversationId, limit) }
        .catch { error ->
            if (unwrapFirestoreException(error) == null && error is CancellationException) throw error
            Log.w(TAG, "Messages stream error", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _peerProfile = MutableStateFlow<UserProfile?>(null)
    val peerProfile: StateFlow<UserProfile?> = _peerProfile.asStateFlow()

    private val _replyingTo = MutableStateFlow<ChatMessage?>(null)
    val replyingTo: StateFlow<ChatMessage?> = _replyingTo.asStateFlow()

    private val _editingMessage = MutableStateFlow<ChatMessage?>(null)
    val editingMessage: StateFlow<ChatMessage?> = _editingMessage.asStateFlow()

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError.asStateFlow()

    init {
        viewModelScope.launch {
            conversation.collect { conv ->
                if (conv != null && conv.type == "direct") {
                    val peerId = conv.participantIds.firstOrNull { it != currentUserId }
                    if (peerId != null && _peerProfile.value?.userId != peerId) {
                        _peerProfile.value = userRepository.getUserProfile(peerId).getOrNull()
                    }
                }
            }
        }
    }

    fun loadOlderMessages() {
        _pageLimit.value = _pageLimit.value + 30L
    }

    fun resolveText(msg: ChatMessage): String {
        return chatRepository.resolveMessageDisplayText(
            message = msg,
            myUserId = currentUserId,
            peerPublicKeyBase64 = _peerProfile.value?.publicKeyBase64
        )
    }

    fun setReplyTo(msg: ChatMessage?) {
        _editingMessage.value = null
        _replyingTo.value = msg
    }

    fun setEditing(msg: ChatMessage?) {
        _replyingTo.value = null
        _editingMessage.value = msg
    }

    fun clearError() {
        _sendError.value = null
    }

    fun sendTextOrSaveEdit(senderProfile: UserProfile, text: String) {
        val conv = conversation.value ?: return
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val editing = _editingMessage.value
        if (editing != null) {
            viewModelScope.launch {
                val res = chatRepository.editMessage(
                    conversation = conv,
                    message = editing,
                    newText = trimmed,
                    peerPublicKeyBase64 = _peerProfile.value?.publicKeyBase64
                )
                if (res.isFailure) {
                    _sendError.value = res.exceptionOrNull()?.localizedMessage ?: "Edit failed"
                } else {
                    _editingMessage.value = null
                }
            }
            return
        }

        val reply = _replyingTo.value
        val replyPreview = reply?.let { resolveText(it).take(120) } ?: ""
        viewModelScope.launch {
            val res = chatRepository.sendMessage(
                conversation = conv,
                senderProfile = senderProfile,
                peerPublicKeyBase64 = _peerProfile.value?.publicKeyBase64,
                type = "text",
                plainText = trimmed,
                replyToMessageId = reply?.id ?: "",
                replyToPreview = replyPreview
            )
            if (res.isFailure) {
                _sendError.value = res.exceptionOrNull()?.localizedMessage ?: "Send failed"
            } else {
                _replyingTo.value = null
            }
        }
    }

    fun sendMediaOrLocation(
        senderProfile: UserProfile,
        type: String,
        caption: String = "",
        mediaDataBase64: String = "",
        mediaName: String = "",
        mediaMimeType: String = "",
        mediaSizeBytes: Long = 0L,
        mediaDurationMs: Long = 0L,
        locationLat: Double? = null,
        locationLng: Double? = null,
        locationLabel: String = "",
        forwardedFrom: String = ""
    ) {
        val conv = conversation.value ?: return
        viewModelScope.launch {
            val res = chatRepository.sendMessage(
                conversation = conv,
                senderProfile = senderProfile,
                peerPublicKeyBase64 = _peerProfile.value?.publicKeyBase64,
                type = type,
                plainText = caption,
                mediaDataBase64 = mediaDataBase64,
                mediaName = mediaName,
                mediaMimeType = mediaMimeType,
                mediaSizeBytes = mediaSizeBytes,
                mediaDurationMs = mediaDurationMs,
                locationLat = locationLat,
                locationLng = locationLng,
                locationLabel = locationLabel,
                forwardedFrom = forwardedFrom
            )
            if (res.isFailure) {
                _sendError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to send attachment"
            }
        }
    }

    fun forwardMessage(targetConversation: Conversation, senderProfile: UserProfile, message: ChatMessage) {
        viewModelScope.launch {
            val textContent = resolveText(message)
            val peerId = if (targetConversation.type == "direct") {
                targetConversation.participantIds.firstOrNull { it != currentUserId }
            } else null
            val targetPeerKey = peerId?.let { userRepository.getUserProfile(it).getOrNull()?.publicKeyBase64 }

            val res = chatRepository.sendMessage(
                conversation = targetConversation,
                senderProfile = senderProfile,
                peerPublicKeyBase64 = targetPeerKey,
                type = message.type,
                plainText = textContent,
                mediaDataBase64 = message.mediaDataBase64,
                mediaName = message.mediaName,
                mediaMimeType = message.mediaMimeType,
                mediaSizeBytes = message.mediaSizeBytes,
                mediaDurationMs = message.mediaDurationMs,
                locationLat = message.locationLat,
                locationLng = message.locationLng,
                locationLabel = message.locationLabel,
                forwardedFrom = message.senderName.ifBlank { "KIGO User" }
            )
            if (res.isFailure) {
                _sendError.value = res.exceptionOrNull()?.localizedMessage ?: "Forward failed"
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(conversationId, messageId)
        }
    }

    fun toggleReaction(message: ChatMessage, emoji: String) {
        viewModelScope.launch {
            chatRepository.toggleReaction(conversationId, message, emoji)
        }
    }

    fun setTyping(isTyping: Boolean) {
        viewModelScope.launch {
            chatRepository.setTypingIndicator(conversationId, isTyping)
        }
    }

    fun markVisibleMessagesRead(sendReadReceipt: Boolean) {
        val conv = conversation.value ?: return
        val list = messages.value
        viewModelScope.launch {
            chatRepository.markConversationRead(conv, list, sendReadReceipt)
        }
    }

    private companion object {
        const val TAG = "KigoConversationVM"
    }
}
