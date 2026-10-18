package com.rave.app.data.repository

import android.content.Context
import androidx.work.WorkManager
import com.rave.app.data.local.AppDatabase
import com.rave.app.data.preferences.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Removes every trace of local data (used by "Delete account" / "Delete all data"). */
@Singleton
class LocalDataWiper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val widget: com.rave.app.widget.WidgetRefresher,
) {
    suspend fun wipe() = withContext(Dispatchers.IO) {
        WorkManager.getInstance(context).cancelAllWork()
        db.clearAllTables()
        settings.clearAll()
        File(context.cacheDir, "shared").deleteRecursively()
        File(context.cacheDir, "map_tiles").deleteRecursively()
        widget.refresh()
    }
}
