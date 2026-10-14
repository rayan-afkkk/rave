package com.rave.app.ui.invite

import android.Manifest
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.rave.app.R
import com.rave.app.ui.components.BarcodeCamera
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.permissions.PermissionGate

/** Extracts an invite code from a scanned QR (an invite link or a bare code). */
fun inviteCodeFromScan(raw: String): String? {
    val trimmed = raw.trim()
    val uri = runCatching { Uri.parse(trimmed) }.getOrNull()
    val scheme = uri?.scheme
    if (uri != null && scheme != null && scheme in setOf("https", "http", "rave")) {
        val segments = uri.pathSegments
        val i = segments.indexOf("invite")
        if (i >= 0) return segments.getOrNull(i + 1)
        if (uri.host == "invite") return segments.firstOrNull()
        return null
    }
    return trimmed.takeIf { it.length in 5..16 }
}

/** CameraX preview + ML Kit QR detection. */
@Composable
fun QrScanScreen(onBack: () -> Unit, onCode: (String) -> Unit) {
    RaveScreen(title = stringResource(R.string.add_friend_scan), onBack = onBack) {
        PermissionGate(
            required = Manifest.permission.CAMERA,
            request = listOf(Manifest.permission.CAMERA),
            icon = Icons.Rounded.PhotoCamera,
            title = stringResource(R.string.permission_camera_title),
            body = stringResource(R.string.permission_camera_body),
            onCancel = onBack,
        ) {
            Box(Modifier.fillMaxSize()) {
                BarcodeCamera(formats = listOf(Barcode.FORMAT_QR_CODE), decode = ::inviteCodeFromScan, onValue = onCode)
                Box(Modifier.align(Alignment.Center).size(240.dp).border(3.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large))
                Text(
                    stringResource(R.string.qr_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
                )
            }
        }
    }
}
