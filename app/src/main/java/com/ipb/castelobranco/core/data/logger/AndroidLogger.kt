package com.ipb.castelobranco.core.data.logger

import android.util.Log
import com.ipb.castelobranco.core.domain.snapshot.Logger

class AndroidLogger : Logger {

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }
}
