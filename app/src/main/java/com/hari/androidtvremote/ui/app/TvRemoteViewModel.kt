package com.hari.androidtvremote.ui.app

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.connectsdk.core.MediaInfo
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.device.ConnectableDeviceListener
import com.connectsdk.discovery.DiscoveryManager
import com.connectsdk.discovery.DiscoveryManagerListener
import com.connectsdk.service.CastService as ConnectSdkCastService
import com.connectsdk.service.DeviceService
import com.connectsdk.service.capability.MediaControl
import com.connectsdk.service.capability.MediaPlayer
import com.connectsdk.service.capability.VolumeControl
import com.connectsdk.service.capability.listeners.ResponseListener
import com.connectsdk.service.command.ServiceCommandError
import com.connectsdk.service.sessions.LaunchSession
import com.hari.androidtvremote.App
import com.hari.androidtvremote.utils.Constant
import com.hari.androidtvremote.androidLib.AndroidRemoteTv
import com.hari.androidtvremote.androidLib.AndroidTvListener
import com.hari.androidtvremote.androidLib.remote.Remotemessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

data class CastPlaybackUiState(
    val activeMedia: MediaItemUi? = null,
    val isCasting: Boolean = false,
    val isBusy: Boolean = false,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val progressFraction: Float = 0f
)

data class TvRemoteUiState(
    val discoveredDevices: List<DeviceUiModel> = emptyList(),
    val connectedDevice: DeviceUiModel? = null,
    val isConnecting: Boolean = false,
    val pairingRequired: Boolean = false,
    val pairingError: String? = null,
    val pairingRequestId: Int = 0,
    val statusMessage: String? = null,
    val isError: Boolean = false,
    val volumeFraction: Float = 0.5f,
    val isVoiceActive: Boolean = false,
    val cast: CastPlaybackUiState = CastPlaybackUiState(),
    val showRatingPrompt: Boolean = false
)

class TvRemoteViewModel(application: Application) : AndroidViewModel(application),
    DiscoveryManagerListener {

    private val app = getApplication<App>()
    private val prefs = application.getSharedPreferences(Constant.PREFS_NAME, Context.MODE_PRIVATE)
    private val discoveryManager = DiscoveryManager.getInstance().also { manager ->
        if (app.discoveryManager == null) {
            app.discoveryManager = manager
        }
    }
    private val discoveredDevices = linkedMapOf<String, ConnectableDevice>()
    private val reachableHosts = ConcurrentHashMap.newKeySet<String>()
    private val validatingHosts = ConcurrentHashMap.newKeySet<String>()

    private val _uiState = MutableStateFlow(TvRemoteUiState())
    val uiState: StateFlow<TvRemoteUiState> = _uiState.asStateFlow()

    private var pendingDeviceId: String? = null
    private var activeDeviceId: String? = null
    private var currentConnectableListener: ConnectableDeviceListener? = null
    private var playbackPollJob: Job? = null
    private var mediaControl: MediaControl? = null
    private var launchSession: LaunchSession? = null
    private var connectionGeneration = 0
    private var autoReconnectInFlight = false
    private var manualDisconnectUntilMs = 0L
    private var imeFieldCounter = 0
    private var imeCounter = 0

    init {
        discoveryManager?.addListener(this)
        discoveryManager?.start()
        refreshDiscoveredDevices()
        attemptAutoReconnect()
    }

    fun refreshDiscoveredDevices() {
        synchronized(discoveredDevices) {
            discoveredDevices.clear()
            discoveryManager?.allDevices?.values?.forEach { device ->
                if (device.isChromecastServiceDevice()) {
                    discoveredDevices[device.id] = device
                    validateReachability(device)
                }
            }
        }
        publishDeviceSnapshot()
        attemptAutoReconnect()
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null, isError = false) }
    }

    fun connectToDevice(deviceId: String) {
        connectToDeviceInternal(deviceId, userInitiated = true)
    }

    private fun connectToDeviceInternal(deviceId: String, userInitiated: Boolean) {
        val device = synchronized(discoveredDevices) { discoveredDevices[deviceId] } ?: return
        if (userInitiated) {
            manualDisconnectUntilMs = 0L
            prefs.edit { putBoolean(Constant.PREF_MANUAL_DISCONNECT, false) }
        }
        if (!device.isChromecastServiceDevice()) {
            synchronized(discoveredDevices) {
                discoveredDevices.remove(deviceId)
            }
            publishDeviceSnapshot()
            showStatus("Only Chromecast service devices can be connected.", isError = true)
            return
        }
        if (_uiState.value.isConnecting) {
            return
        }

        if (activeDeviceId != null && activeDeviceId != deviceId) {
            disconnectCurrentDeviceInternal(manual = false)
        }

        val host = device.ipAddress
        if (host.isNullOrBlank()) {
            showStatus("This TV does not expose a valid IP address.", isError = true)
            return
        }

        pendingDeviceId = deviceId
        _uiState.update {
            it.copy(
                isConnecting = true,
                pairingError = null,
                statusMessage = "Connecting to ${device.friendlyName}...",
                isError = false
            )
        }
        publishDeviceSnapshot()

        val isSavedPairing = prefs.getBoolean(Constant.PIN, false)
        val savedHost = prefs.getString(Constant.HOST, null)
        if (isSavedPairing && savedHost == host) {
            reconnectRemote(device, host, allowFreshFallback = true)
        } else {
            connectRemote(device, host)
        }
    }

    fun disconnectCurrentDevice() {
        disconnectCurrentDeviceInternal(manual = true)
    }

    private fun disconnectCurrentDeviceInternal(manual: Boolean) {
        connectionGeneration += 1
        prefs.edit { putBoolean(Constant.PREF_MANUAL_DISCONNECT, manual) }
        if (manual) {
            autoReconnectInFlight = false
            manualDisconnectUntilMs = System.currentTimeMillis() + MANUAL_DISCONNECT_COOLDOWN_MS
        }
        app.androidRemoteTv?.abort()
        app.androidRemoteTv = null
        playbackPollJob?.cancel()
        playbackPollJob = null
        mediaControl = null
        launchSession = null
        currentConnectableDevice()?.disconnect()
        Constant.connectableDevice = null
        activeDeviceId = null
        pendingDeviceId = null
        imeFieldCounter = 0
        imeCounter = 0
        app.lastImeText = ""
        app.lastFieldCounter = -1
        _uiState.update {
            it.copy(
                connectedDevice = null,
                isConnecting = false,
                pairingRequired = false,
                pairingError = null,
                isVoiceActive = false,
                cast = CastPlaybackUiState(),
                statusMessage = "Disconnected",
                isError = false
            )
        }
        Constant.isConnected.value = false
        publishDeviceSnapshot()
    }

    fun submitPairingCode(code: String) {
        if (code.length != 6) {
            showPairingError("Enter the 6-digit code shown on your TV.")
            return
        }
        try {
            app.androidRemoteTv?.sendSecret(code.uppercase())
            _uiState.update {
                it.copy(
                    pairingRequired = false,
                    pairingError = null,
                    statusMessage = "Pairing with your TV...",
                    isError = false
                )
            }
        } catch (error: Exception) {
            handleWrongPairingCode()
        }
    }

    fun cancelPairing() {
        connectionGeneration += 1
        app.androidRemoteTv?.abort()
        pendingDeviceId = null
        _uiState.update {
            it.copy(
                pairingRequired = false,
                pairingError = null,
                isConnecting = false,
                statusMessage = "Pairing cancelled",
                isError = false
            )
        }
        publishDeviceSnapshot()
    }

    fun sendKey(keyCode: Remotemessage.RemoteKeyCode) {
        viewModelScope.launch(Dispatchers.IO) {
            app.androidRemoteTv?.sendCommand(keyCode, Remotemessage.RemoteDirection.SHORT)
        }
    }

    fun sendText(text: String, submit: Boolean) {
        if (text.isBlank() && !submit) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sendImeTextUpdate(text)
                if (submit) {
                    app.androidRemoteTv?.sendImeEnter()
                }
            } catch (error: Exception) {
                Log.e(TAG, "Failed to send text", error)
            }
        }
    }

    fun sendKeyboardText(text: String) {
        if (text.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val remote = app.androidRemoteTv ?: return@launch
                val currentFieldCounter = resolvedImeFieldCounter()
                text.forEach { character ->
                    remote.sendText(character.toString(), imeCounter++, currentFieldCounter)
                }
            } catch (error: Exception) {
                Log.e(TAG, "Failed to send keyboard text", error)
            }
        }
    }

    fun sendKeyboardBackspace(count: Int = 1) {
        if (count <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            repeat(count) {
                app.androidRemoteTv?.sendCommand(
                    Remotemessage.RemoteKeyCode.KEYCODE_DEL,
                    Remotemessage.RemoteDirection.SHORT
                )
            }
        }
    }

    fun sendKeyboardEnter() {
        viewModelScope.launch(Dispatchers.IO) {
            app.androidRemoteTv?.sendImeEnter()
        }
    }

    fun launchQuickApp(appName: String) {
        val appUrl = when (appName) {
            "Netflix" -> "https://www.netflix.com/title.*"
            "YouTube" -> "https://www.youtube.com"
            "Prime", "Prime Video" -> "https://www.primevideo.com/"
            "Disney+" -> "https://www.disneyplus.com/"
            "JioHotstar", "Hotstar" -> "https://www.hotstar.com/"
            "Jio Hotstar" -> "https://www.hotstar.com/"
            "SonyLIV" -> "https://www.sonyliv.com/"
            "Hulu" -> "https://www.hulu.com/"
            "Apple TV" -> "https://tv.apple.com/"
            "HBO Max", "Max" -> "https://max.com/"
            "Spotify" -> "https://open.spotify.com/"
            else -> null
        }
        if (appUrl == null) {
            showStatus("This shortcut is not mapped yet.", isError = true)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            app.androidRemoteTv?.sendAppLink(appUrl)
        }
    }

    fun toggleVoice() {
        val remote = app.androidRemoteTv ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isActive = remote.isVoiceActive
                if (isActive) {
                    remote.stopVoice()
                } else {
                    remote.startVoice()
                }
                _uiState.update { it.copy(isVoiceActive = remote.isVoiceActive) }
            } catch (error: Exception) {
                showStatus(error.message ?: "Voice search is not available right now.", isError = true)
            }
        }
    }

    fun volumeUp() {
        sendKey(Remotemessage.RemoteKeyCode.KEYCODE_VOLUME_UP)
        _uiState.update {
            val next = (it.volumeFraction + 0.05f).coerceAtMost(1f)
            it.copy(volumeFraction = next)
        }
    }

    fun volumeDown() {
        sendKey(Remotemessage.RemoteKeyCode.KEYCODE_VOLUME_DOWN)
        _uiState.update {
            val next = (it.volumeFraction - 0.05f).coerceAtLeast(0f)
            it.copy(volumeFraction = next)
        }
    }

    fun setVolume(targetFraction: Float) {
        val connectableDevice = currentConnectableDevice() ?: return
        val clamped = targetFraction.coerceIn(0f, 1f)
        _uiState.update { it.copy(volumeFraction = clamped) }

        connectableDevice.volumeControl?.setVolume(
            clamped,
            object : ResponseListener<Any> {
                override fun onSuccess(response: Any?) = Unit

                override fun onError(error: ServiceCommandError?) {
                    Log.w(TAG, "setVolume failed: ${error?.message}")
                }
            }
        )
    }

    fun castMedia(mediaItem: MediaItemUi) {
        val connectableDevice = currentConnectableDevice()
        if (connectableDevice == null || !connectableDevice.isConnected) {
            showStatus("Connect to a TV before casting.", isError = true)
            return
        }

        val castUrl = app.buildCastUrl(
            uri = Uri.parse(mediaItem.uri),
            mimeType = mediaItem.mimeType,
            displayName = mediaItem.title
        )
        if (castUrl == null) {
            showStatus("Could not prepare media for casting.", isError = true)
            return
        }

        _uiState.update {
            it.copy(
                cast = it.cast.copy(
                    activeMedia = mediaItem,
                    isBusy = true
                )
            )
        }

        val mediaInfo = MediaInfo.Builder(castUrl, mediaItem.mimeType)
            .setTitle(mediaItem.title)
            .setDescription(mediaItem.subtitle)
            .build()

        if (mediaItem.kind == MediaKind.Photo) {
            connectableDevice.mediaPlayer?.displayImage(
                mediaInfo,
                object : MediaPlayer.LaunchListener {
                    override fun onSuccess(mediaLaunchObject: MediaPlayer.MediaLaunchObject) {
                        launchSession = mediaLaunchObject.launchSession
                        mediaControl = mediaLaunchObject.mediaControl
                        _uiState.update {
                            it.copy(
                                cast = CastPlaybackUiState(
                                    activeMedia = mediaItem,
                                    isCasting = true,
                                    isBusy = false,
                                    isPlaying = true
                                ),
                                statusMessage = "Photo cast started",
                                isError = false
                            )
                        }
                    }

                    override fun onError(error: ServiceCommandError) {
                        showStatus(
                            error.message ?: "Photo casting failed.",
                            isError = true
                        )
                        _uiState.update { it.copy(cast = it.cast.copy(isBusy = false)) }
                    }
                }
            )
        } else {
            connectableDevice.mediaPlayer?.playMedia(
                mediaInfo,
                false,
                object : MediaPlayer.LaunchListener {
                    override fun onSuccess(mediaLaunchObject: MediaPlayer.MediaLaunchObject) {
                        launchSession = mediaLaunchObject.launchSession
                        mediaControl = mediaLaunchObject.mediaControl
                        _uiState.update {
                            it.copy(
                                cast = CastPlaybackUiState(
                                    activeMedia = mediaItem,
                                    isCasting = true,
                                    isBusy = false,
                                    isPlaying = true,
                                    durationMs = mediaItem.durationMs
                                ),
                                statusMessage = "Casting ${mediaItem.title}",
                                isError = false
                            )
                        }
                        startPlaybackPolling()
                    }

                    override fun onError(error: ServiceCommandError) {
                        showStatus(
                            error.message ?: "Media casting failed.",
                            isError = true
                        )
                        _uiState.update { it.copy(cast = it.cast.copy(isBusy = false)) }
                    }
                }
            )
        }
    }

    fun togglePlayback() {
        val control = mediaControl ?: return
        if (_uiState.value.cast.isPlaying) {
            control.pause(emptyResponseListener())
            _uiState.update { it.copy(cast = it.cast.copy(isPlaying = false)) }
        } else {
            control.play(emptyResponseListener())
            _uiState.update { it.copy(cast = it.cast.copy(isPlaying = true)) }
        }
    }

    fun seekTo(progressFraction: Float) {
        val control = mediaControl ?: return
        val duration = _uiState.value.cast.durationMs
        if (duration <= 0L) {
            return
        }
        val position = (duration * progressFraction.coerceIn(0f, 1f)).toLong()
        control.seek(position, emptyResponseListener())
        _uiState.update {
            it.copy(
                cast = it.cast.copy(
                    positionMs = position,
                    progressFraction = progressFraction.coerceIn(0f, 1f)
                )
            )
        }
    }

    fun stopCasting() {
        playbackPollJob?.cancel()
        playbackPollJob = null
        mediaControl?.stop(emptyResponseListener())
        launchSession = null
        mediaControl = null
        // Keep activeMedia so the player screen doesn't go blank.
        // The screen will show a stopped state — user navigates back themselves.
        _uiState.update {
            it.copy(
                cast = it.cast.copy(
                    isCasting = false,
                    isBusy = false,
                    isPlaying = false,
                    progressFraction = 0f,
                    positionMs = 0L
                ),
                statusMessage = "Casting stopped",
                isError = false
            )
        }
    }

    /** Call this when navigating away from the player to truly clear media state. */
    fun clearCastMedia() {
        _uiState.update { it.copy(cast = CastPlaybackUiState()) }
    }

    override fun onDeviceAdded(manager: DiscoveryManager?, device: ConnectableDevice?) {
        device ?: return
        if (!device.isChromecastServiceDevice()) {
            synchronized(discoveredDevices) {
                discoveredDevices.remove(device.id)
            }
            publishDeviceSnapshot()
            return
        }
        synchronized(discoveredDevices) {
            discoveredDevices[device.id] = device
        }
        validateReachability(device)
        publishDeviceSnapshot()
        attemptAutoReconnect()
    }

    override fun onDeviceUpdated(manager: DiscoveryManager?, device: ConnectableDevice?) {
        device ?: return
        synchronized(discoveredDevices) {
            if (device.isChromecastServiceDevice()) {
                discoveredDevices[device.id] = device
                validateReachability(device)
            } else {
                discoveredDevices.remove(device.id)
            }
        }
        publishDeviceSnapshot()
    }

    override fun onDeviceRemoved(manager: DiscoveryManager?, device: ConnectableDevice?) {
        device ?: return
        synchronized(discoveredDevices) {
            discoveredDevices.remove(device.id)
        }
        device.ipAddress?.let { reachableHosts.remove(it) }
        if (activeDeviceId == device.id) {
            disconnectCurrentDevice()
        } else {
            publishDeviceSnapshot()
        }
    }

    override fun onDiscoveryFailed(manager: DiscoveryManager?, error: ServiceCommandError?) {
        showStatus(error?.message ?: "Device discovery failed.", isError = true)
    }

    override fun onCleared() {
        super.onCleared()
        playbackPollJob?.cancel()
        discoveryManager?.removeListener(this)
    }

    private fun connectRemote(
        device: ConnectableDevice,
        host: String,
        onSecretFallbackMessage: String? = null
    ) {
        app.androidRemoteTv?.abort()
        app.androidRemoteTv = null
        val connectionToken = ++connectionGeneration
        val remote = app.getOrCreateAndroidRemoteTv()
        remote.addListener(volumeListener)
        app.androidRemoteTv = remote

        viewModelScope.launch(Dispatchers.IO) {
            try {
                remote.connect(host, createRemoteListener(device, host, onSecretFallbackMessage, connectionToken))
            } catch (error: Exception) {
                if (isActiveConnection(connectionToken)) {
                    handleConnectError(device, error.message ?: "Connection failed.")
                }
            }
        }
    }

    private fun reconnectRemote(
        device: ConnectableDevice,
        host: String,
        allowFreshFallback: Boolean
    ) {
        app.androidRemoteTv?.abort()
        app.androidRemoteTv = null
        val connectionToken = ++connectionGeneration
        val remote = app.getOrCreateAndroidRemoteTv()
        remote.addListener(volumeListener)
        app.androidRemoteTv = remote

        viewModelScope.launch(Dispatchers.IO) {
            try {
                remote.reconnect(host, object : AndroidTvListener {
                    override fun onSessionCreated() = Unit

                    override fun onSecretRequested() {
                        if (!isActiveConnection(connectionToken)) return
                        prefs.edit {
                            remove(Constant.HOST)
                            putBoolean(Constant.PIN, false)
                        }
                        if (allowFreshFallback) {
                            connectRemote(
                                device = device,
                                host = host,
                                onSecretFallbackMessage = "Pairing code needed. Enter it to finish reconnecting."
                            )
                        } else {
                            handleConnectError(device, "Pairing code needed to reconnect.")
                        }
                    }

                    override fun onPaired() = Unit

                    override fun onConnectingToRemote() = Unit

                    override fun onConnected() {
                        if (!isActiveConnection(connectionToken)) return
                        app.androidRemoteTv = remote
                        attachConnectableDevice(device, host, connectionToken)
                    }

                    override fun onDisconnect() {
                        if (!isActiveConnection(connectionToken)) return
                        handleDeviceDisconnected()
                    }

                    override fun onImeShow(text: String, fieldCounter: Int) {
                        if (!isActiveConnection(connectionToken)) return
                        syncImeState(text, fieldCounter)
                    }

                    override fun onError(error: String) {
                        if (!isActiveConnection(connectionToken)) return
                        handleConnectError(device, error)
                    }
                })
            } catch (error: Exception) {
                if (isActiveConnection(connectionToken)) {
                    handleConnectError(device, error.message ?: "Reconnect failed.")
                }
            }
        }
    }

    private fun createRemoteListener(
        device: ConnectableDevice,
        host: String,
        pairingMessage: String?,
        connectionToken: Int
    ): AndroidTvListener {
        return object : AndroidTvListener {
            override fun onSessionCreated() = Unit

            override fun onSecretRequested() {
                if (!isActiveConnection(connectionToken)) return
                _uiState.update {
                    it.copy(
                        pairingRequired = true,
                        pairingError = null,
                        pairingRequestId = it.pairingRequestId + 1,
                        statusMessage = pairingMessage ?: "Enter the pairing code shown on your TV.",
                        isError = false
                    )
                }
            }

            override fun onPaired() = Unit

            override fun onConnectingToRemote() = Unit

            override fun onConnected() {
                if (!isActiveConnection(connectionToken)) return
                app.androidRemoteTv = app.getOrCreateAndroidRemoteTv()
                attachConnectableDevice(device, host, connectionToken)
            }

            override fun onDisconnect() {
                if (!isActiveConnection(connectionToken)) return
                handleDeviceDisconnected()
            }

            override fun onImeShow(text: String, fieldCounter: Int) {
                if (!isActiveConnection(connectionToken)) return
                syncImeState(text, fieldCounter)
            }

            override fun onError(error: String) {
                if (!isActiveConnection(connectionToken)) return
                handleConnectError(device, error)
            }
        }
    }

    private fun attachConnectableDevice(device: ConnectableDevice, host: String, connectionToken: Int) {
        currentConnectableListener?.let { previous ->
            try {
                device.removeListener(previous)
            } catch (_: Exception) {
            }
        }

        val listener = object : ConnectableDeviceListener {
            override fun onDeviceReady(connectableDevice: ConnectableDevice) {
                if (!isActiveConnection(connectionToken)) return
                Constant.connectableDevice = connectableDevice
                Constant.isConnected.value = true
                activeDeviceId = connectableDevice.id
                pendingDeviceId = null
                autoReconnectInFlight = false
                val currentConnections = prefs.getInt("successful_connections", 0) + 1
                val hasRated = prefs.getBoolean("has_rated_or_feedback", false)
                val lastAsked = prefs.getInt("last_asked_connection", 0)
                val shouldPrompt = !hasRated && currentConnections >= 3 && (currentConnections - lastAsked) >= 3

                prefs.edit {
                    putString(Constant.HOST, host)
                    putBoolean(Constant.PIN, true)
                    putBoolean(Constant.PREF_MANUAL_DISCONNECT, false)
                    putInt("successful_connections", currentConnections)
                }
                imeFieldCounter = app.lastFieldCounter.coerceAtLeast(0)
                observeRealVolume()
                _uiState.update {
                    it.copy(
                        connectedDevice = connectableDevice.toUiModel(
                            isPaired = true,
                            isConnected = true,
                            isConnecting = false
                        ),
                        isConnecting = false,
                        pairingRequired = false,
                        pairingError = null,
                        statusMessage = "${connectableDevice.friendlyName} connected",
                        isError = false,
                        showRatingPrompt = shouldPrompt
                    )
                }
                publishDeviceSnapshot()
            }

            override fun onDeviceDisconnected(connectableDevice: ConnectableDevice) {
                if (!isActiveConnection(connectionToken)) return
                handleDeviceDisconnected()
            }

            override fun onPairingRequired(
                connectableDevice: ConnectableDevice,
                service: DeviceService,
                pairingType: DeviceService.PairingType
            ) = Unit

            override fun onCapabilityUpdated(
                connectableDevice: ConnectableDevice,
                added: List<String>,
                removed: List<String>
            ) = Unit

            override fun onConnectionFailed(
                connectableDevice: ConnectableDevice,
                error: ServiceCommandError
            ) {
                if (!isActiveConnection(connectionToken)) return
                handleConnectError(device, error.message ?: "ConnectSDK connection failed.")
            }
        }

        currentConnectableListener = listener
        device.addListener(listener)
        device.connect()
    }

    private fun observeRealVolume() {
        app.androidRemoteTv?.setVolumeChangedCallback { level, maxLevel, _ ->
            val fraction = if (maxLevel <= 0) 0f else level.toFloat() / maxLevel.toFloat()
            _uiState.update { it.copy(volumeFraction = fraction.coerceIn(0f, 1f)) }
        }
        currentConnectableDevice()?.volumeControl?.getVolume(
            object : VolumeControl.VolumeListener {
                override fun onSuccess(volume: Float?) {
                    volume ?: return
                    _uiState.update { it.copy(volumeFraction = volume.coerceIn(0f, 1f)) }
                }

                override fun onError(error: ServiceCommandError?) {
                    Log.w(TAG, "Initial volume fetch failed: ${error?.message}")
                }
            }
        )
    }

    private fun startPlaybackPolling() {
        playbackPollJob?.cancel()
        playbackPollJob = viewModelScope.launch {
            while (true) {
                mediaControl?.getPosition(object : MediaControl.PositionListener {
                    override fun onSuccess(position: Long?) {
                        val safePosition = position ?: 0L
                        _uiState.update { state ->
                            val duration = state.cast.durationMs
                            state.copy(
                                cast = state.cast.copy(
                                    positionMs = safePosition,
                                    progressFraction = if (duration > 0L) {
                                        (safePosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    }
                                )
                            )
                        }
                    }

                    override fun onError(error: ServiceCommandError?) = Unit
                })
                mediaControl?.getDuration(object : MediaControl.DurationListener {
                    override fun onSuccess(duration: Long?) {
                        val safeDuration = duration ?: return
                        _uiState.update { state ->
                            state.copy(
                                cast = state.cast.copy(
                                    durationMs = safeDuration,
                                    progressFraction = if (safeDuration > 0L) {
                                        (state.cast.positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    }
                                )
                            )
                        }
                    }

                    override fun onError(error: ServiceCommandError?) = Unit
                })
                mediaControl?.getPlayState(object : MediaControl.PlayStateListener {
                    override fun onSuccess(playState: MediaControl.PlayStateStatus?) {
                        val isPlaying = playState == MediaControl.PlayStateStatus.Playing ||
                            playState == MediaControl.PlayStateStatus.Buffering
                        _uiState.update {
                            it.copy(
                                cast = it.cast.copy(
                                    isPlaying = isPlaying
                                )
                            )
                        }
                    }

                    override fun onError(error: ServiceCommandError?) = Unit
                })
                delay(1_000)
            }
        }
    }

    private fun attemptAutoReconnect() {
        if (autoReconnectInFlight || _uiState.value.isConnecting || _uiState.value.connectedDevice != null) {
            return
        }
        if (!prefs.getBoolean(Constant.PREF_AUTO_RECONNECT, true) || !prefs.getBoolean(Constant.PIN, false)) {
            return
        }
        if (prefs.getBoolean(Constant.PREF_MANUAL_DISCONNECT, false)) {
            return
        }
        if (System.currentTimeMillis() < manualDisconnectUntilMs) {
            return
        }

        val host = prefs.getString(Constant.HOST, null) ?: return
        val device = synchronized(discoveredDevices) {
            discoveredDevices.values.firstOrNull { it.ipAddress == host }
        } ?: return
        if (!isDeviceReachable(device)) {
            validateReachability(device)
            return
        }

        autoReconnectInFlight = true
        connectToDeviceInternal(device.id, userInitiated = false)
    }

    private fun publishDeviceSnapshot() {
        val pendingId = pendingDeviceId
        val connectedId = activeDeviceId
        val pairedHost = prefs.getString(Constant.HOST, null)
        val snapshot = synchronized(discoveredDevices) {
            discoveredDevices.values
                .filter { it.isChromecastServiceDevice() }
                .filter { device -> activeDeviceId == device.id || isDeviceReachable(device) }
                .map { device ->
                device.toUiModel(
                    isPaired = device.ipAddress == pairedHost && prefs.getBoolean(Constant.PIN, false),
                    isConnected = connectedId == device.id,
                    isConnecting = pendingId == device.id
                )
            }.sortedWith(
                compareByDescending<DeviceUiModel> { it.isConnected }
                    .thenByDescending { it.isPaired }
                    .thenBy { it.name.lowercase() }
            )
        }

        _uiState.update { state ->
            state.copy(
                discoveredDevices = snapshot,
                connectedDevice = snapshot.firstOrNull { it.id == connectedId } ?: state.connectedDevice,
                isConnecting = pendingId != null
            )
        }
    }

    private fun currentConnectableDevice(): ConnectableDevice? {
        val currentId = activeDeviceId ?: return null
        return synchronized(discoveredDevices) { discoveredDevices[currentId] }
    }

    private fun handleConnectError(device: ConnectableDevice, message: String) {
        if (isWrongPairingCodeError(message) && pendingDeviceId == device.id) {
            handleWrongPairingCode()
            return
        }
        autoReconnectInFlight = false
        pendingDeviceId = null
        publishDeviceSnapshot()
        showStatus(message, isError = true)
        if (activeDeviceId == device.id) {
            handleDeviceDisconnected()
        }
    }

    private fun handleDeviceDisconnected() {
        autoReconnectInFlight = false
        pendingDeviceId = null
        activeDeviceId = null
        playbackPollJob?.cancel()
        playbackPollJob = null
        mediaControl = null
        launchSession = null
        Constant.connectableDevice = null
        Constant.isConnected.value = false
        val manuallyDisconnected = prefs.getBoolean(Constant.PREF_MANUAL_DISCONNECT, false)
        imeFieldCounter = 0
        imeCounter = 0
        app.lastImeText = ""
        app.lastFieldCounter = -1
        _uiState.update {
            it.copy(
                connectedDevice = null,
                isConnecting = false,
                pairingRequired = false,
                pairingError = null,
                isVoiceActive = false,
                cast = CastPlaybackUiState(),
                statusMessage = if (manuallyDisconnected) "Disconnected" else "Connection lost",
                isError = !manuallyDisconnected
            )
        }
        publishDeviceSnapshot()
        if (!manuallyDisconnected) {
            attemptAutoReconnect()
        }
    }

    private fun handleWrongPairingCode() {
        val deviceId = pendingDeviceId ?: return showPairingError("Wrong pairing code. Requesting a new code...")
        val device = synchronized(discoveredDevices) { discoveredDevices[deviceId] }
            ?: return showPairingError("Wrong pairing code. Select your TV again to retry.")
        val host = device.ipAddress
        if (host.isNullOrBlank()) {
            showPairingError("Wrong pairing code. Select your TV again to retry.")
            return
        }

        showPairingError("Wrong pairing code. Requesting a new code...")
        app.androidRemoteTv?.abort()
        connectRemote(
            device = device,
            host = host,
            onSecretFallbackMessage = "Wrong pairing code. Enter the new code shown on your TV."
        )
    }

    private fun showPairingError(message: String) {
        _uiState.update {
            it.copy(
                pairingRequired = true,
                pairingError = message,
                pairingRequestId = it.pairingRequestId + 1,
                statusMessage = message,
                isError = true,
                isConnecting = true
            )
        }
    }

    private fun isWrongPairingCodeError(message: String): Boolean {
        return message.contains("STATUS_BAD_SECRET", ignoreCase = true) ||
            message.contains("bad secret", ignoreCase = true) ||
            message.contains("wrong", ignoreCase = true) ||
            message.contains("invalid", ignoreCase = true)
    }

    private fun isActiveConnection(connectionToken: Int): Boolean {
        return connectionToken == connectionGeneration
    }

    private fun validateReachability(device: ConnectableDevice) {
        val host = device.ipAddress?.takeIf { it.isNotBlank() } ?: return
        if (!validatingHosts.add(host)) return

        viewModelScope.launch(Dispatchers.IO) {
            val reachable = isHostPortOpen(host, CAST_PORT) || isHostPortOpen(host, REMOTE_PORT)
            if (reachable) {
                reachableHosts.add(host)
            } else {
                reachableHosts.remove(host)
            }
            validatingHosts.remove(host)
            publishDeviceSnapshot()
            if (reachable) {
                attemptAutoReconnect()
            }
        }
    }

    private fun isDeviceReachable(device: ConnectableDevice): Boolean {
        val host = device.ipAddress ?: return false
        return reachableHosts.contains(host)
    }

    private fun isHostPortOpen(host: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), REACHABILITY_TIMEOUT_MS)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun syncImeState(text: String, fieldCounter: Int) {
        app.lastImeText = text
        app.lastFieldCounter = fieldCounter
        if (fieldCounter > 0) {
            imeFieldCounter = fieldCounter
        }
        imeCounter = 0
    }

    private fun sendImeTextUpdate(text: String) {
        val remote = app.androidRemoteTv ?: return
        val currentCounter = ++imeCounter
        val currentFieldCounter = resolvedImeFieldCounter()
        remote.sendText(text, currentCounter, currentFieldCounter)
        app.lastImeText = text
    }

    private fun resolvedImeFieldCounter(): Int {
        return when {
            imeFieldCounter > 0 -> imeFieldCounter
            app.lastFieldCounter > 0 -> app.lastFieldCounter
            else -> 1
        }
    }

    private fun showStatus(message: String, isError: Boolean, isConnecting: Boolean = false) {
        _uiState.update {
            it.copy(
                statusMessage = message,
                isError = isError,
                isConnecting = isConnecting
            )
        }
    }

    private fun emptyResponseListener(): ResponseListener<Any> {
        return object : ResponseListener<Any> {
            override fun onSuccess(response: Any?) = Unit
            override fun onError(error: ServiceCommandError?) = Unit
        }
    }

    private fun ConnectableDevice.toUiModel(
        isPaired: Boolean,
        isConnected: Boolean,
        isConnecting: Boolean
    ): DeviceUiModel {
        val typeName = when {
            friendlyName.contains("Chromecast", ignoreCase = true) -> "Chromecast"
            friendlyName.contains("Google", ignoreCase = true) -> "Google TV"
            else -> "Android TV"
        }
        val accent = when {
            typeName == "Chromecast" -> Color(0xFFF4B400)
            typeName == "Google TV" -> Color(0xFF0F9D58)
            else -> Color(0xFF4285F4)
        }
        return DeviceUiModel(
            id = id,
            name = friendlyName ?: "Android TV",
            type = typeName,
            ipAddress = ipAddress ?: "",
            isPaired = isPaired,
            supportsCast = true,
            accent = accent,
            isConnected = isConnected,
            isConnecting = isConnecting
        )
    }

    private fun ConnectableDevice.isChromecastServiceDevice(): Boolean {
        if (getServiceByName(ConnectSdkCastService.ID) != null) {
            return true
        }

        val primaryServiceId = serviceDescription?.serviceID
        if (primaryServiceId != null && primaryServiceId.equals(ConnectSdkCastService.ID, ignoreCase = true)) {
            return true
        }

        val connectedServices = connectedServiceNames ?: return false
        return connectedServices
            .split(",")
            .any { it.trim().equals(ConnectSdkCastService.ID, ignoreCase = true) }
    }

    private val volumeListener = object : AndroidTvListener {
        override fun onSessionCreated() = Unit
        override fun onSecretRequested() = Unit
        override fun onPaired() = Unit
        override fun onConnectingToRemote() = Unit
        override fun onConnected() = Unit
        override fun onDisconnect() = Unit
        override fun onImeShow(text: String, fieldCounter: Int) = Unit
        override fun onError(error: String) = Unit
    }

    fun onUserRated() {
        prefs.edit {
            putBoolean("has_rated_or_feedback", true)
        }
        _uiState.update { it.copy(showRatingPrompt = false) }
    }

    fun onUserFeedbackClicked() {
        prefs.edit {
            putBoolean("has_rated_or_feedback", true)
        }
        _uiState.update { it.copy(showRatingPrompt = false) }
    }

    fun dismissRatingPrompt() {
        val currentConnections = prefs.getInt("successful_connections", 0)
        prefs.edit {
            putInt("last_asked_connection", currentConnections)
        }
        _uiState.update { it.copy(showRatingPrompt = false) }
    }

    private companion object {
        const val TAG = "TvRemoteViewModel"
        const val CAST_PORT = 8009
        const val REMOTE_PORT = 6466
        const val REACHABILITY_TIMEOUT_MS = 700
        const val MANUAL_DISCONNECT_COOLDOWN_MS = 15_000L
    }
}
