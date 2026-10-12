package com.rave.app.ui.tracking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.rave.app.R
import com.rave.app.domain.model.ActivityType
import com.rave.app.ui.components.AppBackground
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.theme.RaveTheme

/** Placeholder for the full-screen live map (built in Phase 3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(
    activityType: ActivityType,
    onBack: () -> Unit,
) {
    val title = stringResource(
        if (activityType == ActivityType.RUN) R.string.activity_run else R.string.activity_walk,
    )
    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                windowInsets = WindowInsets(0), // status bar handled by the Column
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = if (activityType == ActivityType.RUN) Icons.AutoMirrored.Rounded.DirectionsRun
                    else Icons.AutoMirrored.Rounded.DirectionsWalk,
                    title = stringResource(R.string.tracking_placeholder_title),
                    message = stringResource(R.string.tracking_placeholder_message),
                    tile = RaveTheme.colors.tileGreen,
                )
            }
        }
    }
}
