package com.rave.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.theme.TileColor
import java.util.Locale

/*
 * Bento Pop building blocks shared by every screen: big rounded colour tiles,
 * uppercase kickers, chunky numbers, pill chips and pill buttons.
 */

val BentoTileShape = RoundedCornerShape(28.dp)
val BentoPillShape = RoundedCornerShape(50)
val BentoSquareShape = RoundedCornerShape(16.dp)
val BentoGap = 10.dp

/** A rounded colour tile. [onClick] makes the whole tile tappable. */
@Composable
fun BentoTile(
    color: Color,
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.clip(BentoTileShape).background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Small uppercase, letter-spaced label at the top of a tile ("STEPS", "HALAL CHECK"). */
@Composable
fun BentoKicker(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp),
        color = color.copy(alpha = 0.7f), modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}

/** Chunky tight number or headline. */
@Composable
fun BentoBig(text: String, color: Color, modifier: Modifier = Modifier, size: TextUnit = 34.sp, maxLines: Int = 2) {
    Text(
        text, color = color, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.displaySmall.copy(
            fontSize = size, fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.04f * size.value).sp, lineHeight = (size.value * 1.02f).sp,
        ),
    )
}

/** Big screen title ("Scan it. Know it.", "Squad", "Profile"). */
@Composable
fun BentoTitle(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onBackground) {
    BentoBig(text, color, modifier, size = 34.sp, maxLines = 3)
}

/** A value + uppercase caption tile (412 KM, 32:18 TIME). */
@Composable
fun BentoStat(value: String, label: String, tile: TileColor, modifier: Modifier = Modifier, valueSize: TextUnit = 24.sp) {
    BentoTile(tile.container, modifier, padding = 14.dp) {
        BentoBig(value, tile.content, size = valueSize, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        BentoKicker(label, tile.content)
    }
}

/** Rounded pill chip ("3 days left", "NOVA 4"). */
@Composable
fun BentoChip(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier.clip(BentoPillShape).background(container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = content, maxLines = 1)
    }
}

/** Full-width chunky pill button. */
@Composable
fun BentoButton(
    text: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 56.dp,
) {
    Row(
        modifier.height(height).clip(BentoPillShape).background(if (enabled) container else container.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(leading, null, tint = content, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold), color = content, maxLines = 1)
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Icon(trailing, null, tint = content, modifier = Modifier.size(22.dp))
        }
    }
}

/** Square soft icon button used in screen headers (settings, bell, filters). */
@Composable
fun BentoIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = RaveTheme.colors.tileSand.container,
    content: Color = RaveTheme.colors.tileSand.content,
    size: Dp = 46.dp,
) {
    Box(
        modifier.size(size).clip(BentoSquareShape).background(container).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, tint = content, modifier = Modifier.size(24.dp)) }
}

/** Rounded-square initial avatar. */
@Composable
fun BentoAvatar(
    name: String,
    tile: TileColor,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    circle: Boolean = false,
) {
    Box(
        modifier.size(size).clip(if (circle) RoundedCornerShape(50) else RoundedCornerShape(size * 0.3f)).background(tile.container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase(Locale.getDefault()).ifEmpty { "?" },
            color = tile.content,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.ExtraBold),
        )
    }
}

/** Picks a stable bento tile colour for a name or id. */
@Composable
fun bentoTileFor(key: String): TileColor {
    val c = RaveTheme.colors
    val tiles = listOf(c.tilePink, c.tileBlue, c.tilePeach, c.tileGreen, c.tileYellow)
    return tiles[(key.hashCode() and Int.MAX_VALUE) % tiles.size]
}

/** Selectable pill ("Check / Timer / Count"); selected = ink. */
@Composable
fun BentoToggleChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = RaveTheme.colors
    BentoChip(
        text,
        container = if (selected) c.tileInk.container else c.tileSand.container,
        content = if (selected) c.tileInk.content else c.tileSand.content,
        modifier = modifier.height(40.dp).padding(0.dp), onClick = onClick,
    )
}

/** Thin outlined white card (feed items). */
@Composable
fun BentoOutlinedCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(BentoTileShape).background(MaterialTheme.colorScheme.surface)
            .border(2.dp, MaterialTheme.colorScheme.onBackground, BentoTileShape).padding(14.dp),
        content = content,
    )
}
