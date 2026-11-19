package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.crypto.KigoCryptoManager
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.BlockedUser
import com.example.data.model.UserProfile
import com.example.data.model.UserReport
import com.example.data.model.UsernameRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.security.SecureRandom

class KigoUserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val cryptoManager: KigoCryptoManager? = null
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
        cryptoManager = KigoCryptoManager(context.applicationContext)
    )

    private val random = SecureRandom()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    fun observeMyProfile(userId: String): Flow<UserProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots().map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        )
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return try {
            val snap = docRef.get().await()
            val profile = if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Atomically creates a user profile, claims a unique username in `usernames/{key}`,
     * mints a collision-free KIGO-XXXXXX ID via Firestore transaction in `kigoIds/{kigoId}`,
     * and stores PII (`email`) strictly inside `users/{uid}/private/account`.
     */
    suspend fun createProfileWithUniqueKigoId(
        desiredUsername: String,
        displayName: String,
        about: String,
        photoUrl: String,
        email: String?
    ): Result<UserProfile> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val normalizedUsername = normalizeUsername(desiredUsername)
        if (normalizedUsername.length < 3 || normalizedUsername.length > 30) {
            return Result.failure(IllegalArgumentException("Username must be 3-30 characters (letters, numbers, _, .)."))
        }

        val cleanDisplayName = displayName.trim().ifBlank { normalizedUsername }.take(60)
        val cleanAbout = about.trim().ifBlank { "Connect Freely. Chat Your Way." }.take(300)
        val publicKeyB64 = cryptoManager?.getPublicKeyBase64(uid) ?: ""

        // Retry with fresh random KIGO IDs if a collision ever occurs inside the transaction
        repeat(8) {
            val candidateKigoId = generateCandidateKigoId()
            val userRef = db.collection("users").document(uid)
            val usernameRef = db.collection("usernames").document(normalizedUsername)
            val kigoRef = db.collection("kigoIds").document(candidateKigoId)
            val privateRef = userRef.collection("private").document("account")

            try {
                db.runTransaction { txn ->
                    val existingUser = txn.get(userRef)
                    if (existingUser.exists()) {
                        return@runTransaction
                    }
                    val existingUsername = txn.get(usernameRef)
                    if (existingUsername.exists() && existingUsername.getString("userId") != uid) {
                        throw IllegalStateException("USERNAME_TAKEN")
                    }
                    val existingKigo = txn.get(kigoRef)
                    if (existingKigo.exists()) {
                        throw IllegalStateException("KIGO_ID_COLLISION")
                    }

                    val userPayload = mapOf(
                        "userId" to uid,
                        "kigoId" to candidateKigoId,
                        "username" to normalizedUsername,
                        "displayName" to cleanDisplayName,
                        "about" to cleanAbout,
                        "customStatus" to "Available on KIGO CHAT",
                        "photoUrl" to photoUrl,
                        "online" to true,
                        "lastSeen" to FieldValue.serverTimestamp(),
                        "publicKeyBase64" to publicKeyB64,
                        "showOnlineStatus" to true,
                        "showLastSeen" to true,
                        "showReadReceipts" to true,
                        "allowFriendRequests" to true,
                        "allowDiscovery" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    val usernamePayload = mapOf(
                        "userId" to uid,
                        "username" to normalizedUsername,
                        "displayName" to cleanDisplayName,
                        "kigoId" to candidateKigoId,
                        "photoUrl" to photoUrl,
                        "allowDiscovery" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    val kigoPayload = mapOf(
                        "userId" to uid,
                        "kigoId" to candidateKigoId,
                        "username" to normalizedUsername,
                        "displayName" to cleanDisplayName,
                        "photoUrl" to photoUrl,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )

                    val privatePayload = buildMap<String, Any> {
                        put("userId", uid)
                        if (!email.isNullOrBlank()) put("email", email.take(200))
                        put("accountStatus", "active")
                        put("createdAt", FieldValue.serverTimestamp())
                        put("updatedAt", FieldValue.serverTimestamp())
                    }

                    txn.set(userRef, userPayload)
                    txn.set(usernameRef, usernamePayload)
                    txn.set(kigoRef, kigoPayload)
                    txn.set(privateRef, privatePayload)
                }.await()

                val created = userRef.get().await()
                    .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: return Result.failure(IllegalStateException("Profile creation failed"))
                return Result.success(created)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e.message?.contains("KIGO_ID_COLLISION") == true) {
                    // loop and pick another KIGO-XXXXXX
                } else if (e.message?.contains("USERNAME_TAKEN") == true) {
                    return Result.failure(IllegalStateException("Username @$normalizedUsername is already taken."))
                } else {
                    handleFirestoreError(e, OperationType.CREATE, userRef.path)
                    return Result.failure(e)
                }
            }
        }
        return Result.failure(IllegalStateException("Could not allocate a unique KIGO ID. Please retry."))
    }

    suspend fun updateProfile(
        currentProfile: UserProfile,
        displayName: String,
        username: String,
        about: String,
        customStatus: String,
        photoUrl: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val normalizedNewUsername = normalizeUsername(username)
        if (normalizedNewUsername.length < 3 || normalizedNewUsername.length > 30) {
            return Result.failure(IllegalArgumentException("Username must be 3-30 characters."))
        }

        val userRef = db.collection("users").document(uid)
        val oldUsernameRef = db.collection("usernames").document(currentProfile.username.lowercase())
        val newUsernameRef = db.collection("usernames").document(normalizedNewUsername)
        val kigoRef = db.collection("kigoIds").document(currentProfile.kigoId)
        val pubKey = currentProfile.publicKeyBase64.ifBlank { cryptoManager?.getPublicKeyBase64(uid) ?: "" }

        return try {
            db.runTransaction { txn ->
                if (normalizedNewUsername != currentProfile.username.lowercase()) {
                    val targetUsername = txn.get(newUsernameRef)
                    if (targetUsername.exists() && targetUsername.getString("userId") != uid) {
                        throw IllegalStateException("USERNAME_TAKEN")
                    }
                }
                txn.update(
                    userRef,
                    mapOf(
                        "displayName" to displayName.trim().take(60),
                        "username" to normalizedNewUsername,
                        "about" to about.trim().take(300),
                        "customStatus" to customStatus.trim().take(80),
                        "photoUrl" to photoUrl,
                        "publicKeyBase64" to pubKey,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (normalizedNewUsername != currentProfile.username.lowercase()) {
                    txn.delete(oldUsernameRef)
                    txn.set(
                        newUsernameRef,
                        mapOf(
                            "userId" to uid,
                            "username" to normalizedNewUsername,
                            "displayName" to displayName.trim().take(60),
                            "kigoId" to currentProfile.kigoId,
                            "photoUrl" to photoUrl,
                            "allowDiscovery" to currentProfile.allowDiscovery,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    txn.update(
                        oldUsernameRef,
                        mapOf(
                            "displayName" to displayName.trim().take(60),
                            "photoUrl" to photoUrl,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                txn.update(
                    kigoRef,
                    mapOf(
                        "username" to normalizedNewUsername,
                        "displayName" to displayName.trim().take(60),
                        "photoUrl" to photoUrl,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e.message?.contains("USERNAME_TAKEN") == true) {
                Result.failure(IllegalStateException("Username @$normalizedNewUsername is already taken."))
            } else {
                handleFirestoreError(e, OperationType.UPDATE, userRef.path)
                Result.failure(e)
            }
        }
    }

    suspend fun updatePresence(online: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(uid)
        try {
            userRef.update(
                mapOf(
                    "online" to online,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Silently ignore if profile is not yet created
            Log.d("KigoUserRepo", "Presence skipped: ${e.message}")
        }
    }

    suspend fun updatePrivacySettings(
        currentProfile: UserProfile,
        showOnlineStatus: Boolean,
        showLastSeen: Boolean,
        showReadReceipts: Boolean,
        allowFriendRequests: Boolean,
        allowDiscovery: Boolean
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val userRef = db.collection("users").document(uid)
        val usernameRef = db.collection("usernames").document(currentProfile.username.lowercase())
        return try {
            db.runBatch { batch ->
                batch.update(
                    userRef,
                    mapOf(
                        "showOnlineStatus" to showOnlineStatus,
                        "showLastSeen" to showLastSeen,
                        "showReadReceipts" to showReadReceipts,
                        "allowFriendRequests" to allowFriendRequests,
                        "allowDiscovery" to allowDiscovery,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    usernameRef,
                    mapOf(
                        "allowDiscovery" to allowDiscovery,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<List<UserProfile>> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val trimmed = query.trim()
        if (trimmed.isBlank()) return Result.success(emptyList())

        return try {
            val upper = trimmed.uppercase()
            if (upper.matches(Regex("^KIGO-[0-9]{6}$"))) {
                val kigoSnap = db.collection("kigoIds").document(upper).get().await()
                if (!kigoSnap.exists()) return Result.success(emptyList())
                val targetUid = kigoSnap.getString("userId") ?: return Result.success(emptyList())
                if (targetUid == uid) return Result.success(emptyList())
                val profile = db.collection("users").document(targetUid).get().await()
                    .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                return Result.success(listOfNotNull(profile))
            }

            val cleanHandle = trimmed.removePrefix("@").lowercase()
            val results = mutableListOf<UserProfile>()

            // Exact username lookup first
            if (cleanHandle.length >= 3 && cleanHandle.matches(Regex("^[a-z0-9_.]+$"))) {
                val exactSnap = db.collection("usernames").document(cleanHandle).get().await()
                if (exactSnap.exists()) {
                    val targetUid = exactSnap.getString("userId")
                    if (!targetUid.isNullOrBlank() && targetUid != uid) {
                        db.collection("users").document(targetUid).get().await()
                            .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?.let { results.add(it) }
                    }
                }
            }

            // Discovery list query strictly aligned with security rules (allowDiscovery == true)
            val discoverySnap = db.collection("usernames")
                .whereEqualTo("allowDiscovery", true)
                .limit(25)
                .get()
                .await()

            val records = discoverySnap.toObjects(UsernameRecord::class.java)
            val matchedUids = records
                .filter {
                    it.userId != uid &&
                        (it.username.contains(cleanHandle, ignoreCase = true) ||
                            it.displayName.contains(trimmed, ignoreCase = true) ||
                            it.kigoId.contains(trimmed, ignoreCase = true))
                }
                .map { it.userId }
                .distinct()
                .filter { candidateUid -> results.none { it.userId == candidateUid } }
                .take(10)

            for (targetUid in matchedUids) {
                val profile = db.collection("users").document(targetUid).get().await()
                    .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (profile != null) {
                    results.add(profile)
                }
            }

            Result.success(results)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "usernames")
            Result.failure(e)
        }
    }

    fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>> = flow {
        val query = db.collection("blockedUsers").whereEqualTo("userId", userId)
        emitAll(
            query.snapshots().map { snap ->
                snap.toObjects(BlockedUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        )
    }

    suspend fun blockUser(targetUserId: String, targetDisplayName: String, targetKigoId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("blockedUsers").document("${uid}_$targetUserId")
        return try {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "blockedUserId" to targetUserId,
                    "blockedDisplayName" to targetDisplayName.take(60),
                    "blockedKigoId" to targetKigoId.take(20),
                    "createdAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUserId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("blockedUsers").document("${uid}_$targetUserId")
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

    suspend fun submitReport(
        reportedUserId: String,
        reportedMessageId: String = "",
        reason: String,
        details: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("reports").document()
        return try {
            val payload = buildMap<String, Any> {
                put("reporterId", uid)
                put("reportedUserId", reportedUserId)
                if (reportedMessageId.isNotBlank()) put("reportedMessageId", reportedMessageId)
                put("reason", reason.trim().take(100))
                if (details.isNotBlank()) put("details", details.trim().take(500))
                put("status", "open")
                put("createdAt", FieldValue.serverTimestamp())
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun exportUserDataJson(profile: UserProfile): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val privateSnap = db.collection("users").document(uid)
                .collection("private").document("account").get().await()
            val settingsSnap = db.collection("userSettings").document(uid).get().await()
            val friendsSnap = db.collection("friendships")
                .whereArrayContains("participantIds", uid).get().await()

            val export = JSONObject().apply {
                put("exportedAt", System.currentTimeMillis())
                put("app", "KIGO CHAT V1.0")
                put("profile", JSONObject().apply {
                    put("userId", profile.userId)
                    put("kigoId", profile.kigoId)
                    put("username", profile.username)
                    put("displayName", profile.displayName)
                    put("about", profile.about)
                    put("customStatus", profile.customStatus)
                })
                put("privateAccount", JSONObject(privateSnap.data ?: emptyMap<String, Any>()))
                put("settings", JSONObject(settingsSnap.data ?: emptyMap<String, Any>()))
                put("friendCount", friendsSnap.size())
            }

            // Record export timestamp in private doc
            db.collection("users").document(uid).collection("private").document("account")
                .set(
                    mapOf(
                        "userId" to uid,
                        "dataExportRequestedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()

            Result.success(export.toString(2))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun deleteUserFirestoreData(profile: UserProfile?): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            val batch = db.batch()
            val userRef = db.collection("users").document(uid)
            batch.delete(userRef.collection("private").document("account"))
            batch.delete(db.collection("userSettings").document(uid))
            if (profile != null) {
                if (profile.username.isNotBlank()) {
                    batch.delete(db.collection("usernames").document(profile.username.lowercase()))
                }
                if (profile.kigoId.isNotBlank()) {
                    batch.delete(db.collection("kigoIds").document(profile.kigoId))
                }
            }
            batch.delete(userRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }

    private fun generateCandidateKigoId(): String {
        val digits = 100000 + random.nextInt(900000)
        return "KIGO-$digits"
    }

    private fun normalizeUsername(raw: String): String {
        return raw.trim()
            .removePrefix("@")
            .lowercase()
            .replace(Regex("[^a-z0-9_.]"), "")
    }

    private object Log {
        fun d(tag: String, msg: String) = android.util.Log.d(tag, msg)
    }
}
