package com.rave.app.ui.pro

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rave.app.R
import com.rave.app.data.billing.ProRepository
import com.rave.app.data.billing.ProReason
import com.rave.app.ui.components.PillButton
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProViewModel @Inject constructor(private val pro: ProRepository) : ViewModel() {
    val isPro = pro.isPro
    fun upgrade(reason: ProReason) = pro.requestPaywall(reason)
}

/** Whether the user has Pro, and a way to open the paywall from anywhere. */
@Immutable
data class ProAccess(val isPro: Boolean, val upgrade: (ProReason) -> Unit) {
    /** Runs [action] for Pro users; opens the paywall for everyone else. */
    fun gate(reason: ProReason, action: () -> Unit) = if (isPro) action() else upgrade(reason)
}

@Composable
fun rememberPro(viewModel: ProViewModel = hiltViewModel()): ProAccess {
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    return ProAccess(isPro, viewModel::upgrade)
}

/** Small orange "PRO" pill. */
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary).padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.WorkspacePremium, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
        Text(" PRO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary)
    }
}

/**
 * Shows [content] normally for Pro users. For free users it is blurred behind a lock with an
 * "Unlock with Pro" button, so people can see what they'd get.
 */
@Composable
fun ProLock(
    pro: ProAccess,
    reason: ProReason,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (pro.isPro) {
        Box(modifier) { content() }
        return
    }
    Box(modifier) {
        val hide = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(10.dp) else Modifier.alpha(0.18f)
        Box(hide) { content() }
        Column(
            Modifier.matchParentSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
            PillButton(stringResource(R.string.pro_unlock), onClick = { pro.upgrade(reason) }, icon = Icons.Rounded.WorkspacePremium)
        }
    }
}
