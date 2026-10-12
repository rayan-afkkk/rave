package com.rave.app.ui.friends

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.People
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rave.app.R
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.theme.RaveTheme

/** Friends, shared streaks and competitions. Placeholder until Phase 9. */
@Composable
fun FriendsScreen(contentPadding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = Icons.Rounded.People,
            title = stringResource(R.string.friends_empty_title),
            message = stringResource(R.string.friends_empty_message),
            tile = RaveTheme.colors.tilePink,
        )
    }
}
