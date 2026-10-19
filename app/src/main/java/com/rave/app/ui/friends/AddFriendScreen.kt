package com.rave.app.ui.friends

import com.rave.app.ui.theme.Dimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.data.cloud.FriendsRepository
import com.rave.app.data.cloud.ProfileRepository
import com.rave.app.data.cloud.UserProfile
import com.rave.app.ui.components.Avatar
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.TonalPillButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddFriendState(
    val searching: Boolean = false,
    val results: List<UserProfile> = emptyList(),
    val requested: Set<String> = emptySet(),
    val message: Int? = null,
)

@HiltViewModel
class AddFriendViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val friends: FriendsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AddFriendState())
    val state: StateFlow<AddFriendState> = _state.asStateFlow()

    fun search(q: String) {
        _state.update { it.copy(searching = true) }
        viewModelScope.launch {
            val results = runCatching { profiles.searchByUsername(q) }.getOrDefault(emptyList())
            _state.update { it.copy(searching = false, results = results, message = if (results.isEmpty()) R.string.add_friend_no_results else null) }
        }
    }

    fun request(p: UserProfile) {
        viewModelScope.launch {
            friends.sendRequest(p.uid, p.name)
                .onSuccess { _state.update { it.copy(requested = it.requested + p.uid, message = R.string.add_friend_requested) } }
                .onFailure { _state.update { it.copy(message = R.string.error_generic) } }
        }
    }
}

@Composable
fun AddFriendScreen(
    onBack: () -> Unit,
    onScanQr: () -> Unit,
    onRedeemCode: (String) -> Unit,
    viewModel: AddFriendViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var code by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }

    RaveScreen(title = stringResource(R.string.friends_add), onBack = onBack) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
        ) {
            SectionCard {
                Text(stringResource(R.string.add_friend_by_code), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code, onValueChange = { code = it.uppercase().take(12) },
                    placeholder = { Text("RAYAN-7K2Q") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (code.length >= 5) onRedeemCode(code) }),
                    shape = MaterialTheme.shapes.medium,
                )
                Spacer(Modifier.height(8.dp))
                PillButton(stringResource(R.string.action_continue), onClick = { onRedeemCode(code) }, enabled = code.length >= 5,
                    modifier = Modifier.fillMaxWidth())
            }
            TonalPillButton(stringResource(R.string.add_friend_scan), onClick = onScanQr, icon = Icons.Rounded.QrCodeScanner, modifier = Modifier.fillMaxWidth())
            SectionCard {
                Text(stringResource(R.string.add_friend_search), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it.take(20) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) }, placeholder = { Text("@username") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search(query) }),
                    modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                )
                Spacer(Modifier.height(8.dp))
                TonalPillButton(stringResource(R.string.add_friend_search_button), onClick = { viewModel.search(query) },
                    enabled = query.length >= 2 && !state.searching, modifier = Modifier.fillMaxWidth())
                state.results.forEach { ProfileRow(it, it.uid in state.requested) { viewModel.request(it) } }
            }
            state.message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileRow(p: UserProfile, requested: Boolean, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(p.name)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.titleSmall)
            Text("@${p.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onAdd, enabled = !requested) {
            Text(stringResource(if (requested) R.string.add_friend_sent else R.string.action_add))
        }
    }
}
