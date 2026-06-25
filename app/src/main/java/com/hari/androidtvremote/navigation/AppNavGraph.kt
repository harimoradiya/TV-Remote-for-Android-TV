package com.hari.androidtvremote.navigation

import android.app.Activity
import android.content.Context
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.hari.androidtvremote.utils.AnalyticsHelper
import com.hari.androidtvremote.ui.app.CastPlayerScreen
import com.hari.androidtvremote.ui.app.DeviceDiscoveryScreen
import com.hari.androidtvremote.ui.app.AppearanceScreen
import com.hari.androidtvremote.ui.app.HomeScreen
import com.hari.androidtvremote.ui.app.HomeTab
import com.hari.androidtvremote.ui.app.MediaItemUi
import com.hari.androidtvremote.ui.app.MediaKind
import com.hari.androidtvremote.ui.app.OnboardingScreen
import com.hari.androidtvremote.ui.app.RemotePadMode
import com.hari.androidtvremote.ui.app.RemoteControlSettingsScreen
import com.hari.androidtvremote.ui.app.RemoteShelfMode
import com.hari.androidtvremote.ui.app.SettingsScreen
import com.hari.androidtvremote.ui.app.SplashRoute
import com.hari.androidtvremote.ui.app.TipsSupportScreen
import com.hari.androidtvremote.ui.app.TvRemoteViewModel
import com.hari.androidtvremote.ui.app.decodeRemoteShortcutOrder
import com.hari.androidtvremote.ui.app.encodeRemoteShortcutOrder
import com.hari.androidtvremote.ui.app.queryPhotosInAlbum
import com.hari.androidtvremote.ui.app.resolveRemoteShortcutApps
import com.hari.androidtvremote.utils.Constant
import com.hari.androidtvremote.androidLib.remote.Remotemessage
import com.hari.androidtvremote.ui.app.toggleNumberPad



private const val TRANSITION_DURATION = 300

// Material 3 recommended easings
// Forward enter:  EmphasizedDecelerate  (spring into place)
// Forward exit:   EmphasizedAccelerate  (quick out)
// Pop enter:      EmphasizedDecelerate  (come back smoothly)
// Pop exit:       EmphasizedAccelerate  (quick leave)

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
private const val FADE_THROUGH_DURATION = 300

// Small offset — Material 3 uses ~6–8% of screen width, not full width
private const val SLIDE_OFFSET_FRACTION = 0.06f





@Composable
fun AppNavGraph(
    showOnboarding: Boolean,
    onFinishOnboarding: () -> Unit,
) {


    // Lock the initial value — prevents DataStore recompositions from
    // flipping the destination after nav has already started.
    val startWithOnboarding = rememberSaveable { showOnboarding }
    val navController = rememberNavController()
    val context = LocalContext.current

    // Automatically track screens in Firebase Analytics
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            AnalyticsHelper.getInstance(context).logScreenView(route)
        }
    }
    val prefs = remember(context) {
        context.getSharedPreferences(Constant.PREFS_NAME, Context.MODE_PRIVATE)
    }
    val application = context.applicationContext as android.app.Application
    val tvRemoteViewModel: TvRemoteViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    )
    val sessionState by tvRemoteViewModel.uiState.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(HomeTab.Remote) }
    var defaultPadMode by rememberSaveable {
        mutableStateOf(
            prefs.getString(Constant.PREF_DEFAULT_PAD_MODE, RemotePadMode.Touchpad.name)
                ?.let { value -> RemotePadMode.entries.firstOrNull { it.name == value } }
                ?.takeIf { it != RemotePadMode.NumberPad }
                ?: RemotePadMode.Touchpad
        )
    }
    var activePadMode by rememberSaveable { mutableStateOf(defaultPadMode) }
    var autoReconnectEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(Constant.PREF_AUTO_RECONNECT, true))
    }
    var hapticsEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(Constant.PREF_HAPTICS, true))
    }
    var keepScreenAwake by rememberSaveable {
        mutableStateOf(prefs.getBoolean(Constant.PREF_KEEP_SCREEN_AWAKE, false))
    }
    var remoteShelfMode by rememberSaveable {
        mutableStateOf(
            RemoteShelfMode.fromStorage(
                prefs.getString(Constant.PREF_REMOTE_SHELF_MODE, null)
            ) ?: if (prefs.getBoolean(Constant.PREF_SMART_SUGGESTIONS, true)) {
                RemoteShelfMode.Applications
            } else {
                RemoteShelfMode.None
            }
        )
    }
    var remoteAppOrder by rememberSaveable {
        mutableStateOf(
            decodeRemoteShortcutOrder(
                prefs.getString(Constant.PREF_REMOTE_APP_ORDER, null)
            )
        )
    }
    LaunchedEffect(defaultPadMode) {
        prefs.edit { putString(Constant.PREF_DEFAULT_PAD_MODE, defaultPadMode.name) }
    }
    LaunchedEffect(autoReconnectEnabled) {
        prefs.edit { putBoolean(Constant.PREF_AUTO_RECONNECT, autoReconnectEnabled) }
    }
    LaunchedEffect(hapticsEnabled) {
        prefs.edit { putBoolean(Constant.PREF_HAPTICS, hapticsEnabled) }
    }
    LaunchedEffect(keepScreenAwake) {
        prefs.edit { putBoolean(Constant.PREF_KEEP_SCREEN_AWAKE, keepScreenAwake) }
    }
    LaunchedEffect(remoteShelfMode) {
        prefs.edit {
            putString(Constant.PREF_REMOTE_SHELF_MODE, remoteShelfMode.storageValue)
            putBoolean(
                Constant.PREF_SMART_SUGGESTIONS,
                remoteShelfMode == RemoteShelfMode.Applications
            )
        }
    }
    LaunchedEffect(remoteAppOrder) {
        prefs.edit {
            putString(Constant.PREF_REMOTE_APP_ORDER, encodeRemoteShortcutOrder(remoteAppOrder))
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,

        // Forward → new screen grows in (scale 92% → 100%) + fades in
        enterTransition = {
            scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(TRANSITION_DURATION, easing = EmphasizedDecelerate)
            ) + fadeIn(
                animationSpec = tween(TRANSITION_DURATION, easing = EmphasizedDecelerate)
            )
        },

        // Forward → old screen shrinks out (100% → 108%) + fades out
        exitTransition = {
            scaleOut(
                targetScale = 1.08f,
                animationSpec = tween(TRANSITION_DURATION, easing = EmphasizedAccelerate)
            ) + fadeOut(
                animationSpec = tween(TRANSITION_DURATION, easing = EmphasizedAccelerate)
            )
        },

        // Back → previous screen grows back in (92% → 100%) + fades in
        popEnterTransition = {
            scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(TRANSITION_DURATION, easing = EmphasizedDecelerate)
            ) + fadeIn(
                animationSpec = tween(TRANSITION_DURATION, easing = EmphasizedDecelerate)
            )
        },

        // Back → current screen shrinks away (100% → 108%) + fades out
        popExitTransition = {
            scaleOut(
                targetScale = 1.08f,
                animationSpec = tween(TRANSITION_DURATION, easing = EmphasizedAccelerate)
            ) + fadeOut(
                animationSpec = tween(TRANSITION_DURATION, easing = EmphasizedAccelerate)
            )
        },
    )  {
        composable(Screen.Splash.route) {
            SplashRoute(
                showOnboarding = startWithOnboarding,
                onFinished = { shouldShowOnboarding ->
                    navController.navigate(
                        if (shouldShowOnboarding) Screen.Welcome.route else Screen.Main.route
                    ) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Welcome.route) {
            OnboardingScreen(
                onGetStarted = {
                    onFinishOnboarding()
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            HomeScreen(
                currentTab = currentTab,
                activePadMode = activePadMode,
                sessionState = sessionState,
                hapticsEnabled = hapticsEnabled,
                defaultPadMode = defaultPadMode,
                remoteShelfMode = remoteShelfMode,
                remoteApps = resolveRemoteShortcutApps(remoteAppOrder),
                onTabSelected = { tab ->
                    currentTab = tab
                },
                onCyclePadMode = {
                    activePadMode = activePadMode.toggleNumberPad(defaultPadMode)
                },
                onOpenDiscovery = {
                    navController.navigate(Screen.Discovery.route)
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onPower = {
                    tvRemoteViewModel.sendKey(Remotemessage.RemoteKeyCode.KEYCODE_POWER)
                },
                onQuickApp = tvRemoteViewModel::launchQuickApp,
                onRemoteKey = tvRemoteViewModel::sendKey,
                onVolumeChanged = tvRemoteViewModel::setVolume,
                onVolumeUp = tvRemoteViewModel::volumeUp,
                onVolumeDown = tvRemoteViewModel::volumeDown,
                onKeyboardText = tvRemoteViewModel::sendKeyboardText,
                onKeyboardBackspace = tvRemoteViewModel::sendKeyboardBackspace,
                onKeyboardEnter = tvRemoteViewModel::sendKeyboardEnter,
                onToggleVoice = tvRemoteViewModel::toggleVoice,
                onOpenCastPlayer = { mediaItem: MediaItemUi ->
                    tvRemoteViewModel.castMedia(mediaItem)
                    navController.navigate(Screen.CastPlayer.route)
                },
                onUserRated = tvRemoteViewModel::onUserRated,
                onUserFeedbackClicked = tvRemoteViewModel::onUserFeedbackClicked,
                onDismissRatingPrompt = tvRemoteViewModel::dismissRatingPrompt
            )
        }
        composable(Screen.Discovery.route) {
            val initiallyConnectedDeviceId = remember { sessionState.connectedDevice?.id }
            LaunchedEffect(sessionState.connectedDevice?.id) {
                val connectedId = sessionState.connectedDevice?.id
                if (connectedId != null && connectedId != initiallyConnectedDeviceId) {
                    navController.popBackStack()
                }
            }
            DeviceDiscoveryScreen(
                sessionState = sessionState,
                onBack = { navController.popBackStack() },
                onRefresh = tvRemoteViewModel::refreshDiscoveredDevices,
                onConnect = tvRemoteViewModel::connectToDevice,
                onDisconnect = tvRemoteViewModel::disconnectCurrentDevice,
                onSubmitPairingCode = tvRemoteViewModel::submitPairingCode,
                onCancelPairing = tvRemoteViewModel::cancelPairing,
                onClearStatus = tvRemoteViewModel::clearStatus
            )
        }

        // ── Settings ────────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(
                autoReconnectEnabled = autoReconnectEnabled,
                onBack = { navController.popBackStack() },
                onOpenRemoteControls = { navController.navigate(Screen.RemoteControls.route) },
                onOpenAppearance = { navController.navigate(Screen.Appearance.route) },
                onOpenTipsSupport = { navController.navigate(Screen.TipsSupport.route) },
                onAutoReconnectChange = { autoReconnectEnabled = it },
            )
        }
        composable(Screen.RemoteControls.route) {
            RemoteControlSettingsScreen(
                defaultPadMode = defaultPadMode,
                hapticsEnabled = hapticsEnabled,
                keepScreenAwake = keepScreenAwake,
                remoteShelfMode = remoteShelfMode,
                remoteApps = resolveRemoteShortcutApps(remoteAppOrder),
                onBack = { navController.popBackStack() },
                onDefaultPadModeChange = {
                    defaultPadMode = it
                    activePadMode = it
                },
                onHapticsChange = { hapticsEnabled = it },
                onKeepScreenAwakeChange = { keepScreenAwake = it },
                onRemoteShelfModeChange = { remoteShelfMode = it },
                onRemoteAppOrderChange = { newOrder ->
                    remoteAppOrder = newOrder
                },
            )
        }
        composable(Screen.Appearance.route) {
            AppearanceScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.TipsSupport.route) {
            TipsSupportScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Cast player ──────────────────────────────────────────────────────────
        composable(Screen.CastPlayer.route) {
            val activeMedia = sessionState.cast.activeMedia
            val albumItems by produceState(
                initialValue = emptyList<MediaItemUi>(),
                key1 = activeMedia?.collectionId,
                key2 = activeMedia?.kind
            ) {
                value = if (activeMedia?.kind == MediaKind.Photo && !activeMedia.collectionId.isNullOrBlank()) {
                    queryPhotosInAlbum(context, activeMedia.collectionId.orEmpty()).take(30)
                } else emptyList()
            }
            CastPlayerScreen(
                mediaItem = activeMedia,
                deviceName = sessionState.connectedDevice?.name,
                castState = sessionState.cast,
                onBack = {
                    tvRemoteViewModel.clearCastMedia()
                    navController.popBackStack()
                },
                onTogglePlayback = tvRemoteViewModel::togglePlayback,
                onSeekTo = tvRemoteViewModel::seekTo,
                onStopCasting = tvRemoteViewModel::stopCasting,
                albumItems = albumItems,
                onCastOther = { photo ->
                    tvRemoteViewModel.castMedia(photo)
                }
            )
        }
    }
}
