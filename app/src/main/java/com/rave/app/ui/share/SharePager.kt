package com.rave.app.ui.share

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rave.app.R
import com.rave.app.ui.permissions.hasPermission
import com.rave.app.ui.theme.CheckerDark
import com.rave.app.ui.theme.CheckerLight
import com.rave.app.ui.theme.ShareTemplate
import com.rave.app.ui.theme.SharePalette
import com.rave.app.ui.theme.palette
import kotlinx.coroutines.launch

/**
 * Swipeable share-card templates + share targets. Each page records itself into a
 * [GraphicsLayer] so the visible card is exactly what gets exported.
 */
@Composable
fun SharePager(
    fileName: String,
    shareText: String,
    modifier: Modifier = Modifier,
    card: @Composable BoxScope.(ShareTemplate, SharePalette) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val templates = ShareTemplate.entries
    val pager = rememberPagerState { templates.size }
    val layers: List<GraphicsLayer> = templates.map { rememberGraphicsLayer() }
    val saved = stringResource(R.string.share_saved)
    val failed = stringResource(R.string.share_save_failed)

    suspend fun capture() = layers[pager.currentPage].toImageBitmap().asAndroidBitmap()

    val legacyStorage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) scope.launch {
            val ok2 = ShareHelper.saveToGallery(context, capture(), "$fileName-${System.currentTimeMillis()}")
            Toast.makeText(context, if (ok2) saved else failed, Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(state = pager, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 48.dp),
            pageSpacing = 16.dp, modifier = Modifier.weight(1f)) { page ->
            val template = templates[page]
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(Modifier.aspectRatio(9f / 16f).clip(MaterialTheme.shapes.large)) {
                    if (template == ShareTemplate.STICKER) Checkerboard()
                    val palette = template.palette()
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                layers[page].record { this@drawWithContent.drawContent() }
                                drawLayer(layers[page])
                            }
                            .then(if (palette.background != null) Modifier.background(palette.background) else Modifier),
                    ) { card(template, palette) }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            templates.indices.forEach { i ->
                Box(Modifier.size(if (i == pager.currentPage) 10.dp else 7.dp).clip(CircleShape)
                    .background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
            }
        }
        Text(
            stringResource(
                when (templates[pager.currentPage]) {
                    ShareTemplate.WARM -> R.string.template_warm
                    ShareTemplate.DARK -> R.string.template_dark
                    ShareTemplate.MINIMAL -> R.string.template_minimal
                    ShareTemplate.STICKER -> R.string.template_sticker
                },
            ),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ShareTarget(Icons.Rounded.PhotoCamera, stringResource(R.string.share_instagram)) {
                scope.launch {
                    val uri = ShareHelper.cacheImage(context, capture(), fileName)
                    ShareHelper.shareToInstagramStory(context, uri, templates[pager.currentPage] == ShareTemplate.STICKER, shareText)
                }
            }
            ShareTarget(Icons.AutoMirrored.Rounded.Chat, stringResource(R.string.share_whatsapp)) {
                scope.launch { ShareHelper.shareToWhatsApp(context, ShareHelper.cacheImage(context, capture(), fileName), shareText) }
            }
            ShareTarget(Icons.Rounded.Download, stringResource(R.string.share_save)) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !context.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    legacyStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else scope.launch {
                    val ok = ShareHelper.saveToGallery(context, capture(), "$fileName-${System.currentTimeMillis()}")
                    Toast.makeText(context, if (ok) saved else failed, Toast.LENGTH_SHORT).show()
                }
            }
            ShareTarget(Icons.Rounded.IosShare, stringResource(R.string.share_more)) {
                scope.launch { ShareHelper.shareImage(context, ShareHelper.cacheImage(context, capture(), fileName), shareText) }
            }
        }
    }
}

@Composable
private fun ShareTarget(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(56.dp)) { Icon(icon, contentDescription = label) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Checkerboard hinting that the sticker template has a transparent background. */
@Composable
private fun Checkerboard() {
    Canvas(Modifier.fillMaxSize()) {
        val cell = 12.dp.toPx()
        val cols = (size.width / cell).toInt() + 1
        val rows = (size.height / cell).toInt() + 1
        for (r in 0 until rows) for (c in 0 until cols) {
            drawRect(if ((r + c) % 2 == 0) CheckerLight else CheckerDark, Offset(c * cell, r * cell), Size(cell, cell))
        }
    }
}
