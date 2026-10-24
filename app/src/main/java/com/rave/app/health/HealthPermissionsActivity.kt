package com.rave.app.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rave.app.domain.model.AppStyle
import com.rave.app.ui.legal.LegalDoc
import com.rave.app.ui.legal.LegalScreen
import com.rave.app.ui.theme.RaveTheme

/** Opened by Health Connect when the user asks why Rave wants their data: shows the privacy policy. */
class HealthPermissionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RaveTheme(style = AppStyle.BENTO) { LegalScreen(LegalDoc.PRIVACY, onBack = ::finish) }
        }
    }
}
