package com.hari.androidtvremote.ui.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest

data class WifiUiState(
    val isConnected: Boolean,
    val ssid: String?
)

@Composable
fun rememberWifiUiState(): State<WifiUiState> {
    val context = LocalContext.current
    return produceState(initialValue = readWifiUiState(context), key1 = context) {
        callbackFlow {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(readWifiUiState(context))
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    trySend(readWifiUiState(context))
                }

                override fun onLost(network: Network) {
                    trySend(readWifiUiState(context))
                }
            }

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

            trySend(readWifiUiState(context))
            connectivityManager.registerNetworkCallback(request, callback)
            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.collectLatest { value = it }
    }
}

private fun readWifiUiState(context: Context): WifiUiState {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
    val isWifiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

    val ssid = if (isWifiConnected) {
        @Suppress("DEPRECATION")
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.connectionInfo?.ssid
            ?.trim('"')
            ?.takeUnless { it.equals("<unknown ssid>", ignoreCase = true) }
    } else {
        null
    }

    return WifiUiState(
        isConnected = isWifiConnected,
        ssid = ssid
    )
}
