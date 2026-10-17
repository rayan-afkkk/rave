package com.rave.app.ui.tracking

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rave.app.R
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.util.Format
import com.rave.app.ui.components.BentoPillShape
import com.rave.app.ui.theme.TileColor
import java.util.Locale

/*
 * Small Bento Pop pieces shared by live tracking and the activity summary.
 */

/** Pace as 9'26" (or the "–:––" placeholder when there is no pace yet). */
@Composable
fun bentoPace(secondsPerKm: Double?, unit: DistanceUnit): String {
    if (secondsPerKm == null || secondsPerKm <= 0 || secondsPerKm.isInfinite() || secondsPerKm.isNaN() || secondsPerKm > 60 * 60) {
        return Format.pace(secondsPerKm, unit)
    }
    val (min, sec) = Format.paceParts(secondsPerKm, unit)
    return stringResource(R.string.btrack_pace, min, sec)
}

/** Compact value + caption tile (32:18 TIME). */
@Composable
fun BentoMiniStat(
    value: String,
    label: String,
    tile: TileColor,
    modifier: Modifier = Modifier,
    radius: Dp = 18.dp,
    padding: Dp = 10.dp,
    valueSize: TextUnit = 20.sp,
    labelSize: TextUnit = 11.sp,
    labelSpacing: TextUnit = 0.sp,
    labelAlpha: Float = 1f,
) {
    Column(modifier.clip(RoundedCornerShape(radius)).background(tile.container).padding(padding)) {
        Text(
            value, color = tile.content, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = valueSize, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.02f * valueSize.value).sp, lineHeight = (valueSize.value * 1.15f).sp,
            ),
        )
        Text(
            label.uppercase(Locale.getDefault()), color = tile.content.copy(alpha = labelAlpha), maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = labelSize, fontWeight = FontWeight.SemiBold, letterSpacing = labelSpacing),
        )
    }
}

/** 58dp chunky pill action with an optional long-press (Finish skips its confirmation). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BentoPillAction(
    text: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier.height(50.dp).clip(BentoPillShape)
            .background(if (enabled) container else container.copy(alpha = 0.4f))
            .combinedClickable(enabled = enabled, onClickLabel = onClickLabel, onLongClick = onLongClick, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(leading, null, tint = content, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text, color = content, maxLines = 1,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
        )
        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            Icon(trailing, null, tint = content, modifier = Modifier.size(24.dp))
        }
    }
}
