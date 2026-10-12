package com.rave.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.rave.app.R
import com.rave.app.ui.theme.PillShape
import com.rave.app.ui.theme.RaveTheme

/** Pill showing the current overall streak with a softly pulsing flame. */
@Composable
fun StreakChip(days: Int, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "flame")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (days > 0) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "flameScale",
    )
    val description = pluralStringResource(R.plurals.streak_days, days, days)
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = RaveTheme.colors.streakFlame,
                modifier = Modifier.size(22.dp).scale(scale),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = days.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
