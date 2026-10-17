package com.rave.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rave.app.R
import com.rave.app.ui.theme.Dimens
import com.rave.app.ui.theme.PillShape
import com.rave.app.ui.theme.RaveTheme

private val BarHeight = Dimens.NavBarHeight
private val StartButtonSize = 64.dp
/** How far the Start button pops out above the top edge of the bar. */
private val StartLift = 22.dp

/** Space screens leave at the bottom so content isn't hidden behind the bar (system insets come on top). */
val BottomBarClearance = BarHeight + 12.dp + StartLift

private val BarShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/**
 * Bento Pop bottom bar: black, rounded on top, icons in grey with the selected one in lime,
 * and a lime rounded-square Start button in the middle.
 */
@Composable
fun RaveBottomBar(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The Start button sits on top of the bar and pops out above its top edge, so the bar's
    // clip only applies to the bar itself.
    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .padding(top = StartLift)
                .fillMaxWidth()
                .shadow(16.dp, BarShape, ambientColor = RaveTheme.colors.navBarShadow, spotColor = RaveTheme.colors.navBarShadow)
                .clip(BarShape)
                .background(RaveTheme.colors.navBar)
                // Background first so it also fills the area behind the (swipe-to-show) system bar.
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier.fillMaxWidth().height(BarHeight + 12.dp).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (left, right) = TopLevelDestination.entries.let { it.take(2) to it.drop(2) }
                left.forEach { dest -> NavItem(dest, selected == dest, { onSelect(dest) }, Modifier.weight(1f)) }
                Spacer(Modifier.weight(1f))
                right.forEach { dest -> NavItem(dest, selected == dest, { onSelect(dest) }, Modifier.weight(1f)) }
            }
        }
        StartButton(onStartClick, Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun NavItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) RaveTheme.colors.navActive else RaveTheme.colors.navContent,
        label = "navTint",
    )
    val indicator by animateColorAsState(
        targetValue = if (selected) RaveTheme.colors.navActive.copy(alpha = 0.14f) else Color.Transparent,
        label = "navIndicator",
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Material-style pill behind the selected icon.
        Box(
            Modifier.width(52.dp).height(28.dp).clip(PillShape).background(indicator),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                // The label below already describes the item for TalkBack.
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = stringResource(destination.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun StartButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "startScale",
    )
    Box(
        modifier = modifier
            .size(StartButtonSize)
            .scale(scale)
            .shadow(12.dp, CircleShape, ambientColor = RaveTheme.colors.highlight, spotColor = RaveTheme.colors.highlight)
            .clip(CircleShape)
            .background(RaveTheme.colors.highlight)
            // A ring in the bar colour makes the button look cut out of the bar.
            .border(4.dp, RaveTheme.colors.navBar, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = RaveTheme.colors.onHighlight),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = stringResource(R.string.nav_start),
            tint = RaveTheme.colors.onHighlight,
            modifier = Modifier.size(34.dp),
        )
    }
}
