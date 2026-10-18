package com.rave.app.ui.legal

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rave.app.BuildConfig
import com.rave.app.R
import com.rave.app.ui.components.BentoTile
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.theme.RaveTheme

/** About Rave: version, privacy policy, terms, credits and contact. */
@Composable
fun AboutScreen(onBack: () -> Unit, onOpen: (LegalDoc) -> Unit) {
    val context = LocalContext.current
    val email = stringResource(R.string.legal_contact_email)
    val subject = stringResource(R.string.about_support_subject, BuildConfig.VERSION_NAME)
    RaveScreen(title = stringResource(R.string.settings_about), onBack = onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            BentoTile(RaveTheme.colors.tilePeach.container, Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = RaveTheme.colors.tilePeach.content)
                Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyMedium,
                    color = RaveTheme.colors.tilePeach.content.copy(alpha = 0.75f))
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.about_blurb), style = MaterialTheme.typography.bodyMedium, color = RaveTheme.colors.tilePeach.content)
            }
            Spacer(Modifier.height(12.dp))
            AboutRow(Icons.Rounded.Shield, stringResource(R.string.legal_privacy)) { onOpen(LegalDoc.PRIVACY) }
            AboutRow(Icons.Rounded.Description, stringResource(R.string.legal_terms)) { onOpen(LegalDoc.TERMS) }
            AboutRow(Icons.Rounded.Favorite, stringResource(R.string.legal_credits)) { onOpen(LegalDoc.CREDITS) }
            AboutRow(Icons.Rounded.Mail, stringResource(R.string.about_contact), email) {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                }
                runCatching { context.startActivity(intent) }
            }
        }
    }
}

@Composable
private fun AboutRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
