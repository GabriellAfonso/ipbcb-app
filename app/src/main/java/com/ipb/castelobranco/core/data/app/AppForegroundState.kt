package com.ipb.castelobranco.core.data.app

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the app is on screen. With a single Activity, its start/stop is the app's foreground; push
 * handling reads it to skip notifications the user would see happen anyway.
 */
@Singleton
class AppForegroundState @Inject constructor() {
    @Volatile
    var isForeground: Boolean = false
}
