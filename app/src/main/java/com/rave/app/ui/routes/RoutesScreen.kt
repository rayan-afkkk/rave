package com.rave.app.ui.routes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Map
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rave.app.R
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.theme.RaveTheme

/** Activity history. Placeholder until Phase 4. */
@Composable
fun RoutesScreen(contentPadding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = Icons.Rounded.Map,
            title = stringResource(R.string.routes_empty_title),
            message = stringResource(R.string.routes_empty_message),
            tile = RaveTheme.colors.tileBlue,
        )
    }
}
