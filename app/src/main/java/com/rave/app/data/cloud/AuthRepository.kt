package com.rave.app.data.cloud

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class CloudUser(val uid: String, val name: String, val email: String?, val photoUrl: String?)

/** Optional sign-in (Google via Credential Manager). */
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
        val clientId = webClientId() ?: error("default_web_client_id missing: ensure google-services.json is present")
        val activity = activityContext.findActivity()
            ?: throw IllegalArgumentException("Google Sign-In requires an Activity context")
        val option = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type: ${credential::class.java.name}"
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        afterSignIn(null)
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
        val profileResult = runCatching {
            profiles.get().ensureProfile(user.toCloud(nameOverride))
        }
        if (profileResult.isFailure) {
            val err = profileResult.exceptionOrNull()
            android.util.Log.w("Auth", "Firestore ensureProfile failed: ${err?.message}. Account is authenticated; update Firestore security rules in Firebase Console.", err)
        }
        runCatching { migration.get().onSignedIn() }
    }

    // Looked up by name: the resource only exists when google-services.json is present.
    private fun webClientId(): String? {
        val resIdPkg = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resIdPkg != 0) {
            val v = runCatching { context.getString(resIdPkg) }.getOrNull()
            if (!v.isNullOrBlank()) return v
        }
        val resIdApp = context.resources.getIdentifier("default_web_client_id", "string", "com.rave.app")
        if (resIdApp != 0) {
            val v = runCatching { context.getString(resIdApp) }.getOrNull()
            if (!v.isNullOrBlank()) return v
        }
        return null
    }

    private fun Context.findActivity(): Activity? {
        var c = this
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }

    private fun FirebaseUser.toCloud(nameOverride: String? = null) = CloudUser(
        uid = uid,
        name = nameOverride ?: displayName?.takeIf { it.isNotBlank() } ?: email?.substringBefore('@') ?: "",
        email = email,
        photoUrl = photoUrl?.toString(),
    )
}
