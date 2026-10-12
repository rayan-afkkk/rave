package com.rave.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.rave.app.R
import com.rave.app.data.cloud.AuthRepository
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.PillButton
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

data class SignInState(val busy: Boolean = false, val error: String? = null, val done: Boolean = false, val resetSent: Boolean = false)

@HiltViewModel
class SignInViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    val available: Boolean = auth.isAvailable
    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    fun google(activityContext: Context) = execute { auth.signInWithGoogle(activityContext) }
    fun email(email: String, password: String) = execute { auth.signInWithEmail(email, password) }
    fun signUp(name: String, email: String, password: String) = execute { auth.signUpWithEmail(name, email, password) }

    fun reset(email: String) {
        viewModelScope.launch {
            auth.sendPasswordReset(email)
                .onSuccess { _state.update { it.copy(resetSent = true, error = null) } }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    private fun execute(block: suspend () -> Result<Unit>) {
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            block()
                .onSuccess { _state.update { it.copy(busy = false, done = true) } }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message ?: e.javaClass.simpleName) } }
        }
    }
}

/** Optional sign-in. Guests keep full offline habits + tracking. */
@Composable
fun SignInScreen(onDone: () -> Unit, onSkip: () -> Unit, viewModel: SignInViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.done) { if (state.done) onDone() }
    var signUp by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

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
            PillButton(stringResource(R.string.signin_google), onClick = { viewModel.google(context) }, icon = Icons.Rounded.AccountCircle,
                enabled = !state.busy, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f))
                Text("  " + stringResource(R.string.signin_or) + "  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(Modifier.weight(1f))
            }
            if (signUp) {
                OutlinedTextField(name, { name = it.take(40) }, label = { Text(stringResource(R.string.settings_name)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
            }
            OutlinedTextField(email, { email = it.trim() }, label = { Text(stringResource(R.string.signin_email)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
            OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.signin_password)) }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (state.resetSent) Text(stringResource(R.string.signin_reset_sent), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
            TonalPillButton(
                stringResource(if (signUp) R.string.signin_create else R.string.signin_email_button),
                onClick = { if (signUp) viewModel.signUp(name, email, password) else viewModel.email(email, password) },
                enabled = !state.busy && email.contains('@') && password.length >= 6 && (!signUp || name.isNotBlank()),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { signUp = !signUp }) { Text(stringResource(if (signUp) R.string.signin_have_account else R.string.signin_new_account)) }
                if (!signUp) TextButton(onClick = { viewModel.reset(email) }, enabled = email.contains('@')) { Text(stringResource(R.string.signin_forgot)) }
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.signin_continue_guest)) }
        }
    }
}
