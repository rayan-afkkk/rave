package com.rave.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rave.app.R
import com.rave.app.ui.theme.RaveTheme

/** Height of the bar itself (not counting the raised Start button). */
private val BarHeight = 68.dp
private val StartButtonSize = 64.dp
/** How far the Start button rises above the bar's top edge. */
private val StartButtonRaise = 22.dp

/**
 * Space screens should leave at the bottom so content isn't hidden behind the
 * floating bar (bar + raise + outer insets). System nav-bar insets come on top.
 */
val FloatingBottomBarClearance = BarHeight + StartButtonRaise + 24.dp

/**
 * Floating, rounded (36dp) bottom navigation inset 12dp from the screen edges, with a
 * raised round orange Start button in the middle.
 */
@Composable
fun FloatingBottomBar(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .height(BarHeight + StartButtonRaise),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(BarHeight)
                // Very soft shadow — the only shadow in the design.
                .shadow(
                    elevation = 18.dp,
                    shape = shape,
                    ambientColor = RaveTheme.colors.navBarShadow,
                    spotColor = RaveTheme.colors.navBarShadow,
                )
                .clip(shape)
                .background(RaveTheme.colors.navBar)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (left, right) = TopLevelDestination.entries.let { it.take(2) to it.drop(2) }
            left.forEach { dest ->
                NavItem(dest, selected == dest, { onSelect(dest) }, Modifier.weight(1f))
            }
            Spacer(Modifier.width(StartButtonSize + 8.dp))
            right.forEach { dest ->
                NavItem(dest, selected == dest, { onSelect(dest) }, Modifier.weight(1f))
            }
        }

        StartButton(
            onClick = onStartClick,
            modifier = Modifier.align(Alignment.TopCenter),
        )
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
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "navTint",
    )
    val label = stringResource(destination.labelRes)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(MaterialTheme.shapes.large)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
            // The label below already describes the item for TalkBack.
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun StartButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "startScale",
    )
    Box(
        modifier = modifier
            .size(StartButtonSize)
            .scale(scale)
            .shadow(10.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.primary,
                spotColor = MaterialTheme.colorScheme.primary)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            // Ring in the bar colour makes the button look "cut into" the bar.
            .border(4.dp, RaveTheme.colors.navBar, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.onPrimary),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = stringResource(R.string.nav_start),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(34.dp),
        )
    }
}
