package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.crypto.KigoCryptoManager
import com.example.data.model.UserSettings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class KigoRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createProfile_uniqueKigoIdAndSearch_succeeds() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(6).replace("-", "")
        val aliceEmail = "alice_$suffix@kigo.test"
        val bobEmail = "bob_$suffix@kigo.test"

        val aliceUid = signInTestUser(aliceEmail)
        val crypto = KigoCryptoManager(appContext)
        val aliceUserRepo = KigoUserRepository(firestore, auth, crypto)

        val aliceProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceUserRepo.createProfileWithUniqueKigoId(
                desiredUsername = "alice_$suffix",
                displayName = "Alice $suffix",
                about = "Hello KIGO",
                photoUrl = "",
                email = aliceEmail
            ).getOrThrow()
        }
        assertTrue(aliceProfile.kigoId.matches(Regex("^KIGO-[0-9]{6}$")))
        assertEquals(aliceUid, aliceProfile.userId)

        // Sign in as Bob and search Alice by KIGO ID
        signInTestUser(bobEmail)
        val bobUserRepo = KigoUserRepository(firestore, auth, crypto)
        val found = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobUserRepo.searchUsers(aliceProfile.kigoId).getOrThrow()
        }
        assertEquals(1, found.size)
        assertEquals(aliceProfile.userId, found.first().userId)
    }

    @Test
    fun directMessaging_withE2eeAndReadReceipts_succeedsAndIsolates() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(6).replace("-", "")
        val aliceEmail = "dm_a_$suffix@kigo.test"
        val bobEmail = "dm_b_$suffix@kigo.test"
        val charlieEmail = "dm_c_$suffix@kigo.test"

        val crypto = KigoCryptoManager(appContext)
        val aliceUid = signInTestUser(aliceEmail)
        val aliceUserRepo = KigoUserRepository(firestore, auth, crypto)
        val aliceProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceUserRepo.createProfileWithUniqueKigoId("dma_$suffix", "DM Alice", "", "", aliceEmail).getOrThrow()
        }

        val bobUid = signInTestUser(bobEmail)
        val bobUserRepo = KigoUserRepository(firestore, auth, crypto)
        val bobProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobUserRepo.createProfileWithUniqueKigoId("dmb_$suffix", "DM Bob", "", "", bobEmail).getOrThrow()
        }

        // Alice creates DM and sends encrypted message to Bob
        signInTestUser(aliceEmail)
        val aliceChatRepo = KigoChatRepository(firestore, auth, crypto, KigoSocialRepository(firestore, auth))
        val conv = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceChatRepo.getOrCreateDirectConversation(aliceProfile, bobProfile).getOrThrow()
        }
        assertTrue(conv.isEncrypted)

        val msgId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceChatRepo.sendMessage(
                conversation = conv,
                senderProfile = aliceProfile,
                peerPublicKeyBase64 = bobProfile.publicKeyBase64,
                type = "text",
                plainText = "Secret KIGO hello!"
            ).getOrThrow()
        }
        assertTrue(msgId.isNotBlank())

        // Bob observes and decrypts the message
        signInTestUser(bobEmail)
        val bobChatRepo = KigoChatRepository(firestore, auth, crypto, KigoSocialRepository(firestore, auth))
        val bobMessages = withTimeout(FLOW_TIMEOUT_MS) {
            bobChatRepo.observeMessages(conv.id).first { list -> list.any { it.id == msgId } }
        }
        val received = bobMessages.first { it.id == msgId }
        assertTrue(received.isEncrypted)
        val decrypted = bobChatRepo.resolveMessageDisplayText(received, bobUid, aliceProfile.publicKeyBase64)
        assertEquals("Secret KIGO hello!", decrypted)

        // Charlie cannot read Alice & Bob's conversation or messages
        signInTestUser(charlieEmail)
        var convGetDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("conversations").document(conv.id).get().await()
            }
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            if (fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                convGetDenied = true
            }
        }
        assertTrue("Expected Charlie conversation read to be rejected with PERMISSION_DENIED", convGetDenied)

        val charlieChatRepo = KigoChatRepository(firestore, auth, crypto, KigoSocialRepository(firestore, auth))
        var charlieMsgsCount = -1
        var msgsDenied = false
        try {
            charlieMsgsCount = withTimeout(FLOW_TIMEOUT_MS) {
                charlieChatRepo.observeMessages(conv.id).first().size
            }
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            if (fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                msgsDenied = true
            }
        }
        assertTrue("Expected Charlie messages to be denied or empty", msgsDenied || charlieMsgsCount == 0)

        // Unauthenticated user cannot observe conversations
        auth.signOut()
        var unauthDenied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                KigoChatRepository(firestore, auth, crypto, KigoSocialRepository(firestore, auth))
                    .observeConversations(aliceUid)
                    .first()
            }
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirestoreException on unauthenticated query, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
            unauthDenied = true
        }
        assertTrue(unauthDenied)
    }

    @Test
    fun userSettings_saveAndObserve_succeeds() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(6).replace("-", "")
        val uid = signInTestUser("settings_$suffix@kigo.test")
        val settingsRepo = KigoSettingsRepository(firestore, auth, appContext)
        val custom = UserSettings(
            userId = uid,
            themeMode = "amoled",
            accentPreset = "violet",
            fontStyle = "grotesk",
            fontScale = "large",
            navigationStyle = "floating"
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            settingsRepo.saveUserSettings(custom).getOrThrow()
        }
        val loaded = withTimeout(FLOW_TIMEOUT_MS) {
            settingsRepo.observeUserSettings(uid).first { it.themeMode == "amoled" }
        }
        assertEquals("violet", loaded.accentPreset)
        assertEquals("floating", loaded.navigationStyle)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
