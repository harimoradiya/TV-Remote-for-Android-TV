package com.hari.androidtvremote.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsHelper private constructor(context: Context) {

    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    companion object {
        @Volatile
        private var instance: AnalyticsHelper? = null

        fun getInstance(context: Context): AnalyticsHelper {
            return instance ?: synchronized(this) {
                instance ?: AnalyticsHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    fun logScreenView(screenName: String, screenClass: String = "MainActivity") {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logEvent(eventName: String, params: Bundle? = null) {
        firebaseAnalytics.logEvent(eventName, params)
    }

    fun logDeviceConnection(deviceName: String, success: Boolean, errorMsg: String? = null) {
        val bundle = Bundle().apply {
            putString("device_name", deviceName)
            putBoolean("success", success)
            if (errorMsg != null) {
                putString("error_message", errorMsg)
            }
        }
        firebaseAnalytics.logEvent("device_connection", bundle)
    }

    fun logCastMedia(mediaType: String, success: Boolean) {
        val bundle = Bundle().apply {
            putString("media_type", mediaType)
            putBoolean("success", success)
        }
        firebaseAnalytics.logEvent("cast_media", bundle)
    }

}
