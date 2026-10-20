package com.rave.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.rave.app.R
import com.rave.app.data.cloud.AuthRepository
import com.rave.app.ui.components.GoogleSignInButton
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.TonalPillButton
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInState(val busy: Boolean = false, val error: String? = null, val done: Boolean = false)

@HiltViewModel
class SignInViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    val available: Boolean = auth.isAvailable
    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    fun google(activityContext: Context) = execute { auth.signInWithGoogle(activityContext) }

    private fun execute(block: suspend () -> Result<Unit>) {
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            block()
                .onSuccess {
                    if (auth.currentUser.value != null) {
                        _state.update { it.copy(busy = false, done = true) }
                    } else {
                        _state.update { it.copy(busy = false) }
                    }
                }
                .onFailure { e ->
                    if (e is androidx.credentials.exceptions.GetCredentialCancellationException) {
                        android.util.Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
                        _state.update { it.copy(busy = false) }
                    } else {
                        android.util.Log.e("Auth", "Sign-in failed", e)
                        val msg = when {
                            e.message?.contains("16") == true || e.message?.contains("Cannot find a matching credential") == true ->
                                "No Google credential found on device. Ensure a Google account is logged in and SHA-1 is added in Firebase."
                            e.message?.contains("10") == true || e.message?.contains("Developer error", ignoreCase = true) == true ->
                                "Google Developer Error (10): Ensure SHA-1 fingerprint is added to your Android app in Firebase Console."
                            e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true || e.message?.contains("insufficient permissions", ignoreCase = true) == true ->
                                "Firestore Permission Error: Update your Firestore Security Rules in Firebase Console to allow read/write access."
                            else -> e.message ?: e.javaClass.simpleName
                        }
                        _state.update { it.copy(busy = false, error = msg) }
                    }
                }
        }
    }
}

/** Optional Google sign-in. Guests keep full offline habits + tracking. */
@Composable
fun SignInScreen(onDone: () -> Unit, onSkip: () -> Unit, viewModel: SignInViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.done) { if (state.done) onDone() }
    val consent = com.rave.app.ui.legal.rememberLegalConsent()

    RaveScreen(title = stringResource(R.string.signin_title), onBack = onSkip) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EmptyState(Icons.Rounded.Groups, stringResource(R.string.signin_headline), stringResource(R.string.signin_body),
                tile = RaveTheme.colors.tilePink)
            if (!viewModel.available) {
                Text(stringResource(R.string.cloud_unavailable), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                TonalPillButton(stringResource(R.string.signin_continue_guest), onClick = onSkip, modifier = Modifier.fillMaxWidth())
                return@RaveScreen
            }
            com.rave.app.ui.legal.LegalConsentRow(consent)
            GoogleSignInButton(onClick = { consent.require { viewModel.google(context) } }, busy = state.busy)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.signin_continue_guest)) }
        }
    }
}
