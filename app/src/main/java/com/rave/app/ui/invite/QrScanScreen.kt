package com.rave.app.ui.invite

import android.Manifest
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.rave.app.R
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.permissions.PermissionGate
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

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
                CameraPreview(onCode)
                Box(Modifier.align(Alignment.Center).size(240.dp).border(3.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large))
                Text(
                    stringResource(R.string.qr_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onCode: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val handled = remember { AtomicBoolean(false) }
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }
    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            executor.shutdown()
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val view = PreviewView(ctx)
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(executor) { proxy ->
                    val media = proxy.image
                    if (media == null || handled.get()) {
                        proxy.close()
                        return@setAnalyzer
                    }
                    val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                    scanner.process(image)
                        .addOnSuccessListener { codes ->
                            val code = codes.firstNotNullOfOrNull { it.rawValue?.let(::inviteCodeFromScan) }
                            if (code != null && handled.compareAndSet(false, true)) {
                                ContextCompat.getMainExecutor(context).execute { onCode(code) }
                            }
                        }
                        .addOnCompleteListener { proxy.close() }
                }
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }
            }, ContextCompat.getMainExecutor(ctx))
            view
        },
    )
}
