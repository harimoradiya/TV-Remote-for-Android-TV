package com.hari.androidtvremote.utils

import androidx.compose.runtime.mutableStateOf
import com.connectsdk.device.ConnectableDevice

object Constant {
    const val PREFS_NAME = "android_tv_prefs"
    const val PREF_FIRST_LAUNCH = "first_launch"
    const val PREF_AUTO_RECONNECT = "auto_reconnect"
    const val PREF_DEFAULT_PAD_MODE = "default_pad_mode"
    const val PREF_DYNAMIC_COLOR = "dynamic_color"
    const val PREF_HAPTICS = "haptics"
    const val PREF_KEEP_SCREEN_AWAKE = "keep_screen_awake"
    const val PREF_MANUAL_DISCONNECT = "manual_disconnect"
    const val PREF_SMART_SUGGESTIONS = "smart_suggestions"
    const val PREF_REMOTE_SHELF_MODE = "remote_shelf_mode"
    const val PREF_REMOTE_APP_ORDER = "remote_app_order"
    const val PREF_SELECTED_DEVICE = "selected_device"
    const val HOST = "HOST"
    const val PIN = "pin"
    const val PORT = 8016

    val isConnected = mutableStateOf(false)
    var connectableDevice: ConnectableDevice? = null
}
