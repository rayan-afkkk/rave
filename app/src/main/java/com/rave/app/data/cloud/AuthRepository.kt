package com.rave.app.data.cloud

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class CloudUser(val uid: String, val name: String, val email: String?, val photoUrl: String?)

/** Optional sign-in (Google via Credential Manager, or email + password). */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebase: FirebaseProvider,
    private val profiles: Lazy<ProfileRepository>,
    private val migration: Lazy<GuestMigration>,
) {
    private val _user = MutableStateFlow(firebase.auth?.currentUser?.toCloud())
    val currentUser: StateFlow<CloudUser?> = _user.asStateFlow()

    val isAvailable: Boolean get() = firebase.isConfigured

    init {
        firebase.auth?.addAuthStateListener { _user.value = it.currentUser?.toCloud() }
    }

    /** [activityContext] must be an Activity context for the Credential Manager sheet. */
    suspend fun signInWithGoogle(activityContext: Context): Result<Unit> = runCatching {
        val auth = firebase.auth ?: throw CloudUnavailableException()
        val clientId = webClientId() ?: error("default_web_client_id missing: add the Web OAuth client in Firebase")
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type"
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        afterSignIn(null)
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        val auth = firebase.auth ?: throw CloudUnavailableException()
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        afterSignIn(null)
    }

    suspend fun signUpWithEmail(name: String, email: String, password: String): Result<Unit> = runCatching {
        val auth = firebase.auth ?: throw CloudUnavailableException()
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        result.user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build())?.await()
        afterSignIn(name.trim())
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        (firebase.auth ?: throw CloudUnavailableException()).sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun signOut() {
        firebase.auth?.signOut()
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
        _user.value = null
    }

    /** Deletes cloud data then the Firebase account. May fail with "requires recent login". */
    suspend fun deleteAccount(): Result<Unit> = runCatching {
        val user = firebase.auth?.currentUser ?: return@runCatching
        profiles.get().deleteCloudData(user.uid)
        user.delete().await()
        _user.value = null
    }

    private suspend fun afterSignIn(nameOverride: String?) {
        val user = firebase.auth?.currentUser ?: return
        _user.value = user.toCloud(nameOverride)
        profiles.get().ensureProfile(user.toCloud(nameOverride))
        migration.get().onSignedIn()
    }

    private fun webClientId(): String? {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id == 0) null else context.getString(id)
    }

    private fun FirebaseUser.toCloud(nameOverride: String? = null) = CloudUser(
        uid = uid,
        name = nameOverride ?: displayName?.takeIf { it.isNotBlank() } ?: email?.substringBefore('@') ?: "",
        email = email,
        photoUrl = photoUrl?.toString(),
    )
}
