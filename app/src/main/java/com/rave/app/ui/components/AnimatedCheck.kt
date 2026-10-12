package com.rave.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Round checkbox with a bouncy tick. 48dp touch target around a 30dp circle. */
@Composable
fun AnimatedCheck(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    skipped: Boolean = false,
    enabled: Boolean = true,
) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "checkScale",
    )
    val fill by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        label = "checkFill",
    )
    val base = modifier.size(48.dp).semantics { this.contentDescription = contentDescription }
    val clickable = if (onCheckedChange != null) {
        base.toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
    } else base
    Box(clickable, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(30.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(fill)
                .border(2.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(checked, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioHighBouncy)), exit = scaleOut()) {
                Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            }
            if (skipped && !checked) {
                Icon(Icons.Rounded.Remove, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}
