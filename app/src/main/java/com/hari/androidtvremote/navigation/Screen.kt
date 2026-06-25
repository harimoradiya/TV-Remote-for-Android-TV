package com.hari.androidtvremote.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Main : Screen("main")
    data object Remote : Screen("remote")
    data object Cast : Screen("cast")
    data object Discovery : Screen("discovery")
    data object Settings : Screen("settings")
    data object RemoteControls : Screen("remote_controls")
    data object Appearance : Screen("appearance")
    data object TipsSupport : Screen("tips_support")
    data object CastPlayer : Screen("cast_player")
}
