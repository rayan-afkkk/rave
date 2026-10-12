package com.rave.app.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier

/** App-lifetime scope for work that must outlive a screen (saving, syncing). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

fun createApplicationScope() = CoroutineScope(SupervisorJob() + Dispatchers.Default)
