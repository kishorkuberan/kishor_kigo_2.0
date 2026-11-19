package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.UserSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private val Context.kigoPrefsDataStore by preferencesDataStore(name = "kigo_theme_prefs")

class KigoSettingsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val appContext: Context? = null
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
        appContext = context.applicationContext
    )

    fun observeUserSettings(userId: String): Flow<UserSettings> = flow {
        val docRef = db.collection("userSettings").document(userId)
        emitAll(
            docRef.snapshots().map { snap ->
                if (!snap.exists()) UserSettings(userId = userId)
                else snap.toObject(UserSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserSettings(userId = userId)
            }
        )
    }

    fun observeLocalThemeCache(): Flow<String> {
        val ctx = appContext ?: return flow { emit("dark") }
        return ctx.kigoPrefsDataStore.data.map { prefs ->
            prefs[KEY_THEME_MODE] ?: "dark"
        }
    }

    suspend fun saveUserSettings(settings: UserSettings): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("User must be signed in."))
        val docRef = db.collection("userSettings").document(uid)
        return try {
            docRef.set(settings.toWriteMap(uid), SetOptions.merge()).await()
            appContext?.kigoPrefsDataStore?.edit { prefs ->
                prefs[KEY_THEME_MODE] = settings.themeMode
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
