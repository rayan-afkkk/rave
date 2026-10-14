package com.rave.app.ui.food

import android.Manifest
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

/** Product barcodes (EAN-13/8, UPC-A/E) → [onBarcode] with the digits. */
@Composable
fun FoodScanScreen(onBack: () -> Unit, onBarcode: (String) -> Unit) {
    RaveScreen(title = stringResource(R.string.food_scan_title), onBack = onBack) {
        PermissionGate(
            required = Manifest.permission.CAMERA,
            request = listOf(Manifest.permission.CAMERA),
            icon = Icons.Rounded.PhotoCamera,
            title = stringResource(R.string.permission_camera_title),
            body = stringResource(R.string.permission_camera_body_food),
            onCancel = onBack,
        ) {
            Box(Modifier.fillMaxSize()) {
                BarcodeCamera(
                    formats = listOf(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E),
                    decode = { raw -> raw.filter(Char::isDigit).takeIf { it.length in 8..14 } },
                    onValue = onBarcode,
                )
                Box(
                    Modifier.align(Alignment.Center).width(280.dp).height(160.dp)
                        .border(3.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large),
                )
                Text(
                    stringResource(R.string.food_scan_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
                )
            }
        }
    }
}
