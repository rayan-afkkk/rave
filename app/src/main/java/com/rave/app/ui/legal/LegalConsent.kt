package com.rave.app.ui.legal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.rave.app.R
import com.rave.app.data.preferences.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Bump when the Terms or Privacy Policy change in a way users must accept again. */
const val LEGAL_VERSION = "2026-10-18"
private const val KEY = "legal_accepted"

@HiltViewModel
class LegalConsentViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    val accepted = settings.observeString(KEY).map { it == LEGAL_VERSION }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun set(accepted: Boolean) {
        viewModelScope.launch { settings.putString(KEY, if (accepted) LEGAL_VERSION else null) }
    }
}

/** Whether the user accepted the Terms and Privacy Policy; [require] runs an action only once they have. */
class LegalConsent internal constructor(private val state: State<Boolean>, internal val set: (Boolean) -> Unit) {
    val accepted: Boolean get() = state.value
    internal var warn by mutableStateOf(false)

    fun require(action: () -> Unit) {
        if (accepted) action() else warn = true
    }
}

@Composable
fun rememberLegalConsent(viewModel: LegalConsentViewModel = hiltViewModel()): LegalConsent {
    val accepted = viewModel.accepted.collectAsStateWithLifecycle()
    return remember(viewModel) { LegalConsent(accepted, viewModel::set) }
}

/** "☐ I agree to the Terms of Use and Privacy Policy", shown before signing in. */
@Composable
fun LegalConsentRow(consent: LegalConsent, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clickable { consent.set(!consent.accepted); consent.warn = false }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = consent.accepted, onCheckedChange = { consent.set(it); consent.warn = false })
        Column(Modifier.weight(1f)) {
            LegalAgreementText(
                lead = R.string.legal_consent_lead, textAlign = TextAlign.Start,
                color = if (consent.warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (consent.warn) {
                Text(stringResource(R.string.legal_consent_needed), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
