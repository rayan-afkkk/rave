package com.rave.app.ui.invite

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.MaterialTheme
import com.google.firebase.firestore.FirebaseFirestoreException
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rave.app.R
import com.rave.app.data.cloud.AuthRepository
import com.rave.app.data.cloud.InviteInfo
import com.rave.app.data.cloud.InviteRepository
import com.rave.app.data.cloud.InviteType
import com.rave.app.data.cloud.RedeemResult
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.navigation.InviteLandingRoute
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LandingState(
    val loading: Boolean = true,
    val signedIn: Boolean = false,
    val info: InviteInfo? = null,
    val invalid: Boolean = false,
    val busy: Boolean = false,
    val result: RedeemResult? = null,
    /** String resource explaining why accepting failed, or null. */
    val error: Int? = null,
)

@HiltViewModel
class InviteLandingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val invites: InviteRepository,
    private val auth: AuthRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val code: String = savedState.toRoute<InviteLandingRoute>().code
    private val _state = MutableStateFlow(LandingState())
    val state: StateFlow<LandingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            auth.currentUser.collect { user ->
                _state.update { it.copy(signedIn = user != null) }
                if (user != null && _state.value.info == null) load()
                else if (user == null) _state.update { it.copy(loading = false) }
            }
        }
    }

    private suspend fun load() {
        val info = invites.resolve(code)
        _state.update { it.copy(loading = false, info = info, invalid = info == null) }
    }

    fun accept() {
        val info = _state.value.info ?: return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            invites.redeem(info)
                .onSuccess { r -> settings.setPendingInvite(null); _state.update { it.copy(busy = false, result = r) } }
                .onFailure { e ->
                    Log.w("Invite", "Accepting invite failed", e)
                    val denied = (e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    _state.update { it.copy(busy = false, error = if (denied) R.string.invite_error_rules else R.string.error_generic) }
                }
        }
    }

    fun decline() {
        viewModelScope.launch { settings.setPendingInvite(null) }
    }
}

/** "Rayan invited you to …" with Accept / Decline. Asks guests to sign in first. */
@Composable
fun InviteLandingScreen(
    onClose: () -> Unit,
    onSignIn: () -> Unit,
    onOpenCompetition: (String) -> Unit,
    viewModel: InviteLandingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.result) {
        when (val r = state.result) {
            is RedeemResult.JoinedCompetition -> onOpenCompetition(r.competitionId)
            null -> Unit
            else -> onClose()
        }
    }
    RaveScreen(title = stringResource(R.string.invite_landing_title), onBack = onClose) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            when {
                state.loading -> LoadingState()
                !state.signedIn -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyState(Icons.Rounded.PersonAdd, stringResource(R.string.invite_landing_sign_in_title),
                        stringResource(R.string.invite_landing_sign_in_body, viewModel.code), tile = RaveTheme.colors.tileGreen)
                    PillButton(stringResource(R.string.action_sign_in), onClick = onSignIn, modifier = Modifier.fillMaxWidth())
                }
                state.invalid -> EmptyState(Icons.Rounded.LinkOff, stringResource(R.string.invite_invalid_title), stringResource(R.string.invite_invalid_body),
                    tile = RaveTheme.colors.tilePink, action = { TextButton(onClick = onClose) { Text(stringResource(R.string.action_close)) } })
                else -> {
                    val info = state.info!!
                    val (icon, title) = when (info.type) {
                        InviteType.APP -> Icons.Rounded.PersonAdd to stringResource(R.string.invite_landing_app, info.createdByName)
                        InviteType.COMPETITION -> Icons.Rounded.EmojiEvents to stringResource(R.string.invite_landing_competition, info.createdByName)
                        InviteType.SHARED_STREAK -> Icons.Rounded.Whatshot to stringResource(R.string.invite_landing_shared, info.createdByName)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EmptyState(icon, title, stringResource(R.string.invite_landing_body), tile = RaveTheme.colors.tilePeach)
                        state.error?.let {
                            Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                        PillButton(stringResource(R.string.action_accept), onClick = viewModel::accept, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = { viewModel.decline(); onClose() }) { Text(stringResource(R.string.action_decline)) }
                    }
                }
            }
        }
    }
}
