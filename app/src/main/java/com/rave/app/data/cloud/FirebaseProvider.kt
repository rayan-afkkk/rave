package com.rave.app.data.cloud

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase is optional. Without google-services.json the default FirebaseApp never
 * initialises; every cloud feature then reports itself unavailable instead of crashing.
 */
@Singleton
class FirebaseProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val isConfigured: Boolean by lazy { runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false) }

    val auth: FirebaseAuth? by lazy { if (isConfigured) FirebaseAuth.getInstance() else null }

    val firestore: FirebaseFirestore? by lazy { if (isConfigured) FirebaseFirestore.getInstance() else null }

    fun requireFirestore(): FirebaseFirestore = firestore ?: throw CloudUnavailableException()

    fun requireUid(): String = auth?.currentUser?.uid ?: throw NotSignedInException()

    val uid: String? get() = auth?.currentUser?.uid
}

class CloudUnavailableException : IllegalStateException("Firebase is not configured")
class NotSignedInException : IllegalStateException("Not signed in")
