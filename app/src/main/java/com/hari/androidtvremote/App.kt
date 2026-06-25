package com.hari.androidtvremote

import android.app.Application
import android.net.Uri
import android.net.wifi.WifiManager
import com.connectsdk.discovery.DiscoveryManager
import com.connectsdk.discovery.provider.CastDiscoveryProvider
import com.connectsdk.service.CastService
import com.hari.androidtvremote.androidLib.AndroidRemoteTv
import com.hari.androidtvremote.utils.Constant
import com.hari.androidtvremote.utils.WebServer
import com.hari.androidtvremote.utils.CrashlyticsTree
import timber.log.Timber
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

class App : Application() {

    var discoveryManager: DiscoveryManager? = null
    var androidRemoteTv: AndroidRemoteTv? = null

    // IME state (keyboard sync from TV)
    var lastImeText: String = ""
    var lastFieldCounter: Int = -1
    var onImeShowCallback: ((String, Int) -> Unit)? = null

    private var webServer: WebServer? = null
    private var webServerIp: String? = null

    companion object {
        var app: App? = null
        private const val NANO_HTTP_SOCKET_READ_TIMEOUT_MILLIS = 15_000
    }

    override fun onCreate() {
        super.onCreate()
        app = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashlyticsTree())
        }

        // Init must happen on main thread (reads application context only)
        DiscoveryManager.init(applicationContext)
        // Start discovery on a background thread to avoid main-thread network ops
        Thread(::discoveryManagerSetup, "discovery-init").start()
    }

    fun discoveryManagerSetup() {
        discoveryManager = DiscoveryManager.getInstance()
        discoveryManager?.registerDeviceService(
            CastService::class.java,
            CastDiscoveryProvider::class.java
        )
        discoveryManager?.start()
    }

    @Synchronized
    fun getOrCreateAndroidRemoteTv(): AndroidRemoteTv {
        if (androidRemoteTv == null) {
            androidRemoteTv = AndroidRemoteTv()
        }
        return androidRemoteTv!!
    }

    private fun ensureLocalMediaServer(): String? {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) return null
        val ip = getIPAddress(true) ?: return null

        if (webServer != null && webServerIp == ip) return webServerIp

        try {
            webServer?.stop()
        } catch (_: Exception) {
        }

        return try {
            webServer = WebServer(
                context = applicationContext,
                host = ip,
                port = Constant.PORT
            )
            webServer?.start(NANO_HTTP_SOCKET_READ_TIMEOUT_MILLIS, false)
            webServerIp = ip
            Timber.d("Local media server started on $ip:${Constant.PORT}")
            webServerIp
        } catch (e: Exception) {
            Timber.e(e, "Failed to start local media server")
            null
        }
    }

    fun buildCastUrl(uri: Uri, mimeType: String, displayName: String): String? {
        val host = ensureLocalMediaServer() ?: return null
        val path = webServer?.registerMedia(uri, mimeType, displayName) ?: return null
        return "http://$host:${Constant.PORT}$path"
    }

    fun stopLocalMediaServer() {
        try { webServer?.stop() } catch (_: Exception) {}
        webServer?.clearRegisteredMedia()
        webServer = null
        webServerIp = null
    }

    private fun getIPAddress(useIPv4: Boolean): String? {
        return try {
            val interfaces: List<NetworkInterface> =
                Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs: List<InetAddress> = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress ?: continue
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (useIPv4) {
                            if (isIPv4) return sAddr
                        } else {
                            if (!isIPv4) {
                                val delim = sAddr.indexOf('%')
                                return if (delim < 0) sAddr.uppercase(Locale.getDefault())
                                else sAddr.substring(0, delim).uppercase(Locale.getDefault())
                            }
                        }
                    }
                }
            }
            null
        } catch (_: Exception) { null }
    }
}
