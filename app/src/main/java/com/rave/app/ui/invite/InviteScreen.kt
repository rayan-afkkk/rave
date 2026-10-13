package com.rave.app.ui.invite

import android.content.ClipData
import com.rave.app.ui.theme.Dimens
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.google.firebase.firestore.FirebaseFirestoreException
import com.rave.app.data.cloud.AuthRepository
import com.rave.app.data.cloud.InviteRepository
import com.rave.app.data.cloud.ProfileRepository
import com.rave.app.data.cloud.UserProfile
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.TonalPillButton
import com.rave.app.ui.share.ShareHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InviteViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    auth: AuthRepository,
    val invites: InviteRepository,
) : ViewModel() {
    val profile: StateFlow<UserProfile?> = profiles.observeMyProfile().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val cloudAvailable = auth.isAvailable
    val signedIn: StateFlow<Boolean> = auth.currentUser.map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), auth.currentUser.value != null)

    /** Why the invite code couldn't be set up (string resource), or null. */
    private val _error = MutableStateFlow<Int?>(null)
    val error: StateFlow<Int?> = _error.asStateFlow()
    private val _working = MutableStateFlow(false)
    val working: StateFlow<Boolean> = _working.asStateFlow()

    init { retry() }

    fun retry() {
        if (!cloudAvailable || !signedIn.value || _working.value) return
        viewModelScope.launch {
            _working.value = true
            _error.value = null
            runCatching { profiles.ensureMyInviteCode() }.onFailure { e ->
                Log.w("Invite", "Invite code setup failed", e)
                _error.value = if ((e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    R.string.invite_error_rules
                } else {
                    R.string.invite_error_generic
                }
            }
            _working.value = false
        }
    }
}

fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("invite", text))
}

/** Personal invite link + short code + QR, with share targets and a pre-filled message. */
@Composable
fun InviteScreen(onBack: () -> Unit, viewModel: InviteViewModel = hiltViewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val working by viewModel.working.collectAsStateWithLifecycle()
    val context = LocalContext.current
    RaveScreen(title = stringResource(R.string.invite_title), onBack = onBack) {
        if (!viewModel.cloudAvailable || !signedIn) {
            EmptyState(Icons.Rounded.IosShare, stringResource(R.string.invite_sign_in_title), stringResource(R.string.invite_sign_in_body))
            return@RaveScreen
        }
        val p = profile
        if (p == null || p.inviteCode.isBlank()) {
            val err = error
            if (err != null && !working) {
                Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding), horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyState(Icons.Rounded.IosShare, stringResource(R.string.invite_error_title), stringResource(err))
                    TonalPillButton(stringResource(R.string.action_retry), onClick = viewModel::retry)
                }
            } else if (p == null) {
                LoadingState()
            } else {
                EmptyState(Icons.Rounded.IosShare, stringResource(R.string.invite_preparing), stringResource(R.string.invite_preparing_body))
            }
            return@RaveScreen
        }
        val link = viewModel.invites.appLink(p.inviteCode)
        val message = viewModel.invites.shareMessage(p.inviteCode)
        val qrColor = MaterialTheme.colorScheme.onSurface.toArgb()
        // The scanner accepts a bare code too, so the QR still works without a link domain.
        val qrText = link ?: p.inviteCode
        val qr = remember(qrText, qrColor) { qrBitmap(qrText, dark = qrColor).asImageBitmap() }
        val copied = stringResource(if (link != null) R.string.invite_copied else R.string.invite_code_copied)

        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.invite_headline), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(stringResource(R.string.invite_body), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            SectionCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(220.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surface)) {
                        Image(qr, contentDescription = stringResource(R.string.cd_invite_qr), modifier = Modifier.size(220.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.invite_your_code), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(p.inviteCode, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    if (link != null) {
                        Text(link, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Target(Icons.AutoMirrored.Rounded.Chat, stringResource(R.string.share_whatsapp)) { ShareHelper.shareToWhatsApp(context, null, message) }
                Target(Icons.Rounded.PhotoCamera, stringResource(R.string.share_instagram_dm)) {
                    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message)
                        .setPackage("com.instagram.android").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }.onFailure { ShareHelper.shareText(context, message) }
                }
                Target(Icons.Rounded.Sms, stringResource(R.string.share_sms)) { ShareHelper.shareSms(context, message) }
                Target(Icons.Rounded.ContentCopy, stringResource(R.string.share_copy)) {
                    copyToClipboard(context, link ?: p.inviteCode)
                    Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                }
                Target(Icons.Rounded.IosShare, stringResource(R.string.share_more)) { ShareHelper.shareText(context, message) }
            }
            Text(stringResource(R.string.invite_reward_note), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Target(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(52.dp)) { Icon(icon, contentDescription = label) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
