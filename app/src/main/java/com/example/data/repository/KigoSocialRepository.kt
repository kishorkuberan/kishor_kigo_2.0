package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.FriendRequest
import com.example.data.model.Friendship
import com.example.data.model.GroupInfo
import com.example.data.model.GroupMember
import com.example.data.model.NotificationItem
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

class KigoSocialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    fun observeFriendRequests(userId: String): Flow<List<FriendRequest>> = flow {
        val query = db.collection("friendRequests")
            .whereArrayContains("participantIds", userId)
        emitAll(
            query.snapshots().map { snap ->
                snap.toObjects(FriendRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt }
            }
        )
    }

    fun observeFriendships(userId: String): Flow<List<Friendship>> = flow {
        val query = db.collection("friendships")
            .whereArrayContains("participantIds", userId)
        emitAll(
            query.snapshots().map { snap ->
                snap.toObjects(Friendship::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        )
    }

    fun observeGroups(userId: String): Flow<List<GroupInfo>> = flow {
        val query = db.collection("groups")
            .whereArrayContains("memberIds", userId)
        emitAll(
            query.snapshots().map { snap ->
                snap.toObjects(GroupInfo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: it.createdAt }
            }
        )
    }

    fun observeGroupMembers(groupId: String): Flow<List<GroupMember>> = flow {
        val query = db.collection("groups").document(groupId).collection("members")
        emitAll(
            query.snapshots().map { snap ->
                snap.toObjects(GroupMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        )
    }

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> = flow {
        val query = db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
        emitAll(
            query.snapshots().map { snap ->
                snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        )
    }

    suspend fun sendFriendRequest(myProfile: UserProfile, targetProfile: UserProfile): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (targetProfile.userId == uid) {
            return Result.failure(IllegalArgumentException("Cannot send a friend request to yourself."))
        }
        if (!targetProfile.allowFriendRequests) {
            return Result.failure(IllegalStateException("This user is not accepting friend requests."))
        }

        val sortedPair = listOf(uid, targetProfile.userId).sorted()
        val friendshipId = "${sortedPair[0]}_${sortedPair[1]}"
        val requestId = "${uid}_${targetProfile.userId}"
        val reverseRequestId = "${targetProfile.userId}_${uid}"

        val friendshipRef = db.collection("friendships").document(friendshipId)
        val reqRef = db.collection("friendRequests").document(requestId)
        val revReqRef = db.collection("friendRequests").document(reverseRequestId)

        return try {
            val existingFriend = friendshipRef.get().await()
            if (existingFriend.exists()) {
                return Result.failure(IllegalStateException("Already friends with ${targetProfile.displayName}."))
            }
            val reverseSnap = revReqRef.get().await()
            if (reverseSnap.exists() && reverseSnap.getString("status") == "pending") {
                return Result.failure(IllegalStateException("${targetProfile.displayName} already sent you a pending request."))
            }

            val existingReq = reqRef.get().await()
            if (existingReq.exists()) {
                val status = existingReq.getString("status")
                if (status == "pending") {
                    return Result.failure(IllegalStateException("Friend request already pending."))
                }
                reqRef.update(
                    mapOf(
                        "status" to "pending",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                reqRef.set(
                    mapOf(
                        "fromUserId" to uid,
                        "toUserId" to targetProfile.userId,
                        "fromDisplayName" to myProfile.displayName.take(60),
                        "fromKigoId" to myProfile.kigoId,
                        "fromPhotoUrl" to myProfile.photoUrl,
                        "toDisplayName" to targetProfile.displayName.take(60),
                        "toKigoId" to targetProfile.kigoId,
                        "participantIds" to listOf(uid, targetProfile.userId),
                        "status" to "pending",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            createNotification(
                recipientId = targetProfile.userId,
                senderId = uid,
                senderName = myProfile.displayName,
                type = "friend_request",
                title = "New Friend Request",
                body = "${myProfile.displayName} (${myProfile.kigoId}) wants to connect.",
                targetId = requestId
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(request: FriendRequest, myProfile: UserProfile): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val sortedPair = listOf(request.fromUserId, request.toUserId).sorted()
        val friendshipId = "${sortedPair[0]}_${sortedPair[1]}"
        val reqRef = db.collection("friendRequests").document(request.id)
        val friendshipRef = db.collection("friendships").document(friendshipId)

        return try {
            val isUserA = sortedPair[0] == request.fromUserId
            val userADisplayName = if (isUserA) request.fromDisplayName else myProfile.displayName
            val userAKigoId = if (isUserA) request.fromKigoId else myProfile.kigoId
            val userAPhoto = if (isUserA) request.fromPhotoUrl else myProfile.photoUrl
            val userBDisplayName = if (isUserA) myProfile.displayName else request.fromDisplayName
            val userBKigoId = if (isUserA) myProfile.kigoId else request.fromKigoId
            val userBPhoto = if (isUserA) myProfile.photoUrl else request.fromPhotoUrl

            db.runBatch { batch ->
                batch.update(
                    reqRef,
                    mapOf(
                        "status" to "accepted",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    friendshipRef,
                    mapOf(
                        "participantIds" to sortedPair,
                        "userAId" to sortedPair[0],
                        "userADisplayName" to userADisplayName.take(60),
                        "userAKigoId" to userAKigoId.take(20),
                        "userAPhotoUrl" to userAPhoto,
                        "userBId" to sortedPair[1],
                        "userBDisplayName" to userBDisplayName.take(60),
                        "userBKigoId" to userBKigoId.take(20),
                        "userBPhotoUrl" to userBPhoto,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            createNotification(
                recipientId = request.fromUserId,
                senderId = uid,
                senderName = myProfile.displayName,
                type = "friend_accepted",
                title = "Friend Request Accepted",
                body = "${myProfile.displayName} accepted your friend request.",
                targetId = friendshipId
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        val reqRef = db.collection("friendRequests").document(requestId)
        return try {
            reqRef.update(
                mapOf(
                    "status" to "rejected",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun cancelFriendRequest(requestId: String): Result<Unit> {
        val reqRef = db.collection("friendRequests").document(requestId)
        return try {
            reqRef.update(
                mapOf(
                    "status" to "cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeFriend(friendshipId: String): Result<Unit> {
        val docRef = db.collection("friendships").document(friendshipId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createGroup(
        name: String,
        description: String,
        photoUrl: String,
        creator: UserProfile,
        initialFriends: List<UserProfile>,
        isPrivate: Boolean,
        requireAdminApproval: Boolean
    ): Result<GroupInfo> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val memberIds = (listOf(uid) + initialFriends.map { it.userId }).distinct().take(50)
        val convRef = db.collection("conversations").document()
        val groupRef = db.collection("groups").document()

        return try {
            db.runBatch { batch ->
                batch.set(
                    convRef,
                    mapOf(
                        "type" to "group",
                        "title" to name.trim().take(100),
                        "description" to description.trim().take(300),
                        "photoUrl" to photoUrl,
                        "participantIds" to memberIds,
                        "adminIds" to listOf(uid),
                        "createdBy" to uid,
                        "lastMessagePreview" to "Group created by ${creator.displayName}",
                        "lastMessageType" to "system",
                        "lastMessageSenderId" to uid,
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "unreadCounts" to memberIds.associateWith { 0L },
                        "archivedBy" to emptyList<String>(),
                        "typingUsers" to emptyMap<String, Boolean>(),
                        "isEncrypted" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    groupRef,
                    mapOf(
                        "name" to name.trim().take(80),
                        "description" to description.trim().take(300),
                        "photoUrl" to photoUrl,
                        "conversationId" to convRef.id,
                        "createdBy" to uid,
                        "adminIds" to listOf(uid),
                        "memberIds" to memberIds,
                        "isPrivate" to isPrivate,
                        "requireAdminApproval" to requireAdminApproval,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val allUsers = (listOf(creator) + initialFriends).distinctBy { it.userId }
                for (member in allUsers) {
                    val memberRef = groupRef.collection("members").document(member.userId)
                    batch.set(
                        memberRef,
                        mapOf(
                            "groupId" to groupRef.id,
                            "userId" to member.userId,
                            "displayName" to member.displayName.take(60),
                            "kigoId" to member.kigoId,
                            "photoUrl" to member.photoUrl,
                            "role" to if (member.userId == uid) "admin" else "member",
                            "memberIds" to memberIds,
                            "joinedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            for (friend in initialFriends) {
                createNotification(
                    recipientId = friend.userId,
                    senderId = uid,
                    senderName = creator.displayName,
                    type = "group_invite",
                    title = "Added to Group",
                    body = "${creator.displayName} added you to ${name.trim()}",
                    targetId = convRef.id
                )
            }

            val created = groupRef.get().await()
                .toObject(GroupInfo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Group creation failed"))
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, groupRef.path)
            Result.failure(e)
        }
    }

    suspend fun addMemberToGroup(group: GroupInfo, newMember: UserProfile): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (group.memberIds.contains(newMember.userId)) return Result.success(Unit)
        val updatedMembers = (group.memberIds + newMember.userId).distinct().take(50)
        val groupRef = db.collection("groups").document(group.id)
        val convRef = db.collection("conversations").document(group.conversationId)
        val memberRef = groupRef.collection("members").document(newMember.userId)

        return try {
            db.runBatch { batch ->
                batch.update(
                    groupRef,
                    mapOf(
                        "memberIds" to updatedMembers,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    convRef,
                    mapOf(
                        "participantIds" to updatedMembers,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    memberRef,
                    mapOf(
                        "groupId" to group.id,
                        "userId" to newMember.userId,
                        "displayName" to newMember.displayName.take(60),
                        "kigoId" to newMember.kigoId,
                        "photoUrl" to newMember.photoUrl,
                        "role" to "member",
                        "memberIds" to updatedMembers,
                        "joinedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            createNotification(
                recipientId = newMember.userId,
                senderId = uid,
                senderName = group.name,
                type = "group_invite",
                title = "Added to Group",
                body = "You were added to ${group.name}",
                targetId = group.conversationId
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, groupRef.path)
            Result.failure(e)
        }
    }

    suspend fun removeMemberOrLeaveGroup(group: GroupInfo, targetUserId: String): Result<Unit> {
        val updatedMembers = group.memberIds.filter { it != targetUserId }
        val updatedAdmins = group.adminIds.filter { it != targetUserId }
        val groupRef = db.collection("groups").document(group.id)
        val convRef = db.collection("conversations").document(group.conversationId)
        val memberRef = groupRef.collection("members").document(targetUserId)

        return try {
            db.runBatch { batch ->
                batch.update(
                    groupRef,
                    mapOf(
                        "memberIds" to updatedMembers,
                        "adminIds" to updatedAdmins,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    convRef,
                    mapOf(
                        "participantIds" to updatedMembers,
                        "adminIds" to updatedAdmins,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.delete(memberRef)
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, groupRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleGroupAdmin(group: GroupInfo, targetUserId: String): Result<Unit> {
        val isAdmin = group.adminIds.contains(targetUserId)
        val updatedAdmins = if (isAdmin) {
            group.adminIds.filter { it != targetUserId }
        } else {
            (group.adminIds + targetUserId).distinct()
        }
        val groupRef = db.collection("groups").document(group.id)
        val convRef = db.collection("conversations").document(group.conversationId)
        val memberRef = groupRef.collection("members").document(targetUserId)

        return try {
            db.runBatch { batch ->
                batch.update(
                    groupRef,
                    mapOf(
                        "adminIds" to updatedAdmins,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    convRef,
                    mapOf(
                        "adminIds" to updatedAdmins,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    memberRef,
                    mapOf(
                        "role" to if (isAdmin) "member" else "admin"
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, groupRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateGroupDetails(
        group: GroupInfo,
        name: String,
        description: String,
        isPrivate: Boolean
    ): Result<Unit> {
        val groupRef = db.collection("groups").document(group.id)
        val convRef = db.collection("conversations").document(group.conversationId)
        return try {
            db.runBatch { batch ->
                batch.update(
                    groupRef,
                    mapOf(
                        "name" to name.trim().take(80),
                        "description" to description.trim().take(300),
                        "isPrivate" to isPrivate,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    convRef,
                    mapOf(
                        "title" to name.trim().take(100),
                        "description" to description.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, groupRef.path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val ref = db.collection("notifications").document(notificationId)
        return try {
            ref.update("read", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun createNotification(
        recipientId: String,
        senderId: String,
        senderName: String,
        type: String,
        title: String,
        body: String,
        targetId: String
    ) {
        if (recipientId.isBlank() || recipientId == senderId) return
        val ref = db.collection("notifications").document()
        try {
            ref.set(
                mapOf(
                    "recipientId" to recipientId,
                    "senderId" to senderId,
                    "senderName" to senderName.take(60),
                    "type" to type,
                    "title" to title.take(100),
                    "body" to body.take(300),
                    "targetId" to targetId.take(128),
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
        }
    }
}
