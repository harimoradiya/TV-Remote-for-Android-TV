package com.hari.androidtvremote.ui.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hari.androidtvremote.BuildConfig
import com.hari.androidtvremote.R

@Preview(showBackground = true)
@Composable
fun TipsSupportScreenPreview() {
    TipsSupportScreen(onBack = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsSupportScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var showDevelopersSheet by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    AppBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                SettingsTopBar(
                    title = "Support & About",
                    onBack = onBack,
                    scrollBehavior = scrollBehavior
                )
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    TipsSupportHero()
                }
                item {
                    SettingSubtitle(text = "Developers")
                }
                item {
                    SettingGroupItem(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        title = "Meet the developers",
                        desc = "Meet the team that built this application",
                        icon = Icons.Filled.People,
                        onClick = { showDevelopersSheet = true }
                    )
                }
                item {
                    SettingSubtitle(text = "Feedback & Actions")
                }
                item {
                    SettingItemRow(
                        title = "Rate App",
                        desc = "Support the project with a 5-star review on Play Store",
                        icon = Icons.Filled.Star,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
                            }
                        }
                    )
                }
                item {
                    SettingItemRow(
                        title = "Share App",
                        desc = "Share this remote control app with friends and family",
                        icon = Icons.Filled.Share,
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Android TV Remote Control")
                                putExtra(Intent.EXTRA_TEXT, "Check out this amazing Android TV Remote app! https://play.google.com/store/apps/details?id=${context.packageName}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share app via"))
                        }
                    )
                }
                item {
                    SettingItemRow(
                        title = "Send Feedback",
                        desc = "Report bugs, suggest features, or get in touch",
                        icon = Icons.Filled.Email,
                        onClick = {
                            val feedbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("harimoradiya123@gmail.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "Feedback on Android TV Remote App")
                                putExtra(Intent.EXTRA_TEXT, "App Version: ${BuildConfig.VERSION_NAME}\n\nFeedback:")
                            }
                            try {
                                context.startActivity(Intent.createChooser(feedbackIntent, "Send Feedback via"))
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    )
                }
                item {
                    SettingSubtitle(text = "About")
                }
                item {
                    SettingItemRow(
                        title = "Application",
                        desc = "Android TV Remote Control",
                        icon = Icons.Filled.Info
                    )
                }
                item {
                    SettingItemRow(
                        title = "Version",
                        desc = BuildConfig.VERSION_NAME,
                        icon = Icons.Filled.Info
                    )
                }
            }
        }
    }

    if (showDevelopersSheet) {
        DevelopersSheet(onDismissRequest = { showDevelopersSheet = false })
    }
}

@Composable
private fun TipsSupportHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(164.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_tv),
                contentDescription = "Android TV Remote app icon",
                modifier = Modifier.size(92.dp)
            )
        }
        Surface(
            modifier = Modifier.padding(top = 20.dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer
        ) {
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Text(
            text = "Android TV Remote",
            modifier = Modifier.padding(top = 18.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Tips, support, and project credits for your remote control app.",
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevelopersSheet(
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current

    fun openInstagram(url: String) {
        // Try to open the Instagram app first, fall back to browser
        try {
            val uri = Uri.parse(url)
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.instagram.android")
            })
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Developers",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Meet the team that designed and built the Android TV Remote app.",
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DeveloperPersonRow(
                initials = "HM",
                name = "Hari Mordiya",
                role = "Developer",
                instagramUrl = "https://www.instagram.com/hk_moradiya/",
                onInstagramClick = { openInstagram("https://www.instagram.com/hk_moradiya/") }
            )
            DeveloperPersonRow(
                initials = "UJ",
                name = "Utsav Jetani",
                role = "Developer",
                instagramUrl = "https://www.instagram.com/r_k_utsav/",
                onInstagramClick = { openInstagram("https://www.instagram.com/r_k_utsav/") }
            )
            FilledTonalButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth()
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun DeveloperPersonRow(
    initials: String,
    name: String,
    role: String,
    instagramUrl: String? = null,
    onInstagramClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle with initials
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        // Name & role
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = role,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (instagramUrl != null) {
                Text(
                    text = "@" + instagramUrl
                        .removePrefix("https://www.instagram.com/")
                        .removeSuffix("/"),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Instagram link button
        if (onInstagramClick != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                IconButton(onClick = onInstagramClick) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = "Open Instagram profile of $name",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
