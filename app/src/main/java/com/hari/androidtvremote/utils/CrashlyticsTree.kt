package com.hari.androidtvremote.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashlyticsTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        
        // Log to crashlytics timeline
        crashlytics.log("$priority/${tag ?: "Timber"}: $message")
        
        // Record non-fatal exception if one is provided
        if (t != null) {
            crashlytics.recordException(t)
        }
    }
}
