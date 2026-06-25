package com.hari.androidtvremote.ui.app

import androidx.compose.ui.graphics.Color

enum class RemoteShelfMode(
    val storageValue: String,
    val label: String,
    val description: String
) {
    Applications(
        storageValue = "applications",
        label = "Applications",
        description = "Show quick-launch apps above the remote"
    ),
    MediaButtons(
        storageValue = "media",
        label = "Media buttons",
        description = "Show playback controls above the remote"
    ),
    None(
        storageValue = "none",
        label = "None",
        description = "Hide the top strip on the remote"
    );

    companion object {
        fun fromStorage(value: String?): RemoteShelfMode? {
            return entries.firstOrNull { it.storageValue == value }
        }
    }
}

data class RemoteShortcutApp(
    val id: String,
    val launchName: String,
    val label: String,
    val mark: String,
    val accent: Color,
    val accentSecondary: Color = accent
)

private val defaultRemoteShortcutApps = listOf(
    RemoteShortcutApp(
        id = "youtube",
        launchName = "YouTube",
        label = "YouTube",
        mark = "YT",
        accent = Color(0xFFFF2D20),
        accentSecondary = Color(0xFFFF7A66)
    ),
    RemoteShortcutApp(
        id = "prime_video",
        launchName = "Prime Video",
        label = "Prime Video",
        mark = "PV",
        accent = Color(0xFF1E88FF),
        accentSecondary = Color(0xFF00B8D9)
    ),
    RemoteShortcutApp(
        id = "jiohotstar",
        launchName = "JioHotstar",
        label = "Jio Hotstar",
        mark = "JH",
        accent = Color(0xFF6A54FF),
        accentSecondary = Color(0xFF19C6FF)
    ),
    RemoteShortcutApp(
        id = "sonyliv",
        launchName = "SonyLIV",
        label = "SonyLIV",
        mark = "SL",
        accent = Color(0xFF8E5BFF),
        accentSecondary = Color(0xFFFF4D7A)
    ),
    RemoteShortcutApp(
        id = "hulu",
        launchName = "Hulu",
        label = "Hulu",
        mark = "HU",
        accent = Color(0xFF1CE783),
        accentSecondary = Color(0xFF0B8B4B)
    ),
    RemoteShortcutApp(
        id = "apple_tv",
        launchName = "Apple TV",
        label = "Apple TV",
        mark = "AT",
        accent = Color(0xFF2F3C52),
        accentSecondary = Color(0xFF7D91B3)
    ),
    RemoteShortcutApp(
        id = "hbo_max",
        launchName = "HBO Max",
        label = "HBO Max",
        mark = "HM",
        accent = Color(0xFF7B31FF),
        accentSecondary = Color(0xFF2C0B63)
    ),
    RemoteShortcutApp(
        id = "netflix",
        launchName = "Netflix",
        label = "Netflix",
        mark = "N",
        accent = Color(0xFFD61F2C),
        accentSecondary = Color(0xFF5B0D18)
    )
)

fun defaultRemoteShortcutOrder(): List<String> = defaultRemoteShortcutApps.map(RemoteShortcutApp::id)

fun decodeRemoteShortcutOrder(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return defaultRemoteShortcutOrder()
    val knownIds = defaultRemoteShortcutApps.map(RemoteShortcutApp::id).toSet()
    val savedIds = raw.split(',')
        .map(String::trim)
        .filter(String::isNotBlank)
        .filter { it in knownIds }
        .distinct()
    return (savedIds + defaultRemoteShortcutApps.map(RemoteShortcutApp::id)).distinct()
}

fun encodeRemoteShortcutOrder(order: List<String>): String = decodeRemoteShortcutOrder(
    order.joinToString(",")
).joinToString(",")

fun resolveRemoteShortcutApps(order: List<String>): List<RemoteShortcutApp> {
    val appsById = defaultRemoteShortcutApps.associateBy(RemoteShortcutApp::id)
    return decodeRemoteShortcutOrder(order.joinToString(","))
        .mapNotNull(appsById::get)
}

fun moveRemoteShortcutOrderItem(order: List<String>, fromIndex: Int, direction: Int): List<String> {
    val resolved = decodeRemoteShortcutOrder(order.joinToString(",")).toMutableList()
    val targetIndex = fromIndex + direction
    if (fromIndex !in resolved.indices || targetIndex !in resolved.indices) {
        return resolved
    }
    val movedItem = resolved.removeAt(fromIndex)
    resolved.add(targetIndex, movedItem)
    return resolved
}
