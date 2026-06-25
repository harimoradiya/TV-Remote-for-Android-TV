package com.hari.androidtvremote.ui.app

import android.view.SoundEffectConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Large page-title header — matches ReadYou's DisplayText style.
 * Shows a big [displaySmall] title and an optional smaller description below it.
 */
@Composable
fun SettingDisplayText(
    modifier: Modifier = Modifier,
    text: String,
    desc: String = "",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 48.dp, end = 24.dp, bottom = 24.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (desc.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Section label / subtitle between setting groups — like ReadYou's Subtitle.
 */
@Composable
fun SettingSubtitle(
    modifier: Modifier = Modifier,
    text: String,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

/**
 * Clickable group-level setting item with icon, title, description, and a
 * chevron arrow — matches ReadYou's SelectableSettingGroupItem style.
 * Used on the settings *hub* screen to navigate to sub-pages.
 */
@Composable
fun SettingGroupItem(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String,
    desc: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val view = LocalView.current
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        animationSpec = tween(200),
        label = "alpha"
    )

    Surface(
        modifier = modifier
            .alpha(alpha)
            .clickable(enabled = enabled) {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                onClick()
            },
        color = Color.Unspecified,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp, end = 16.dp)
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    maxLines = if (desc == null) 2 else 1,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                desc?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

/**
 * Individual setting row — icon + title + description + optional trailing widget
 * (switch, segmented button, etc.) — matches ReadYou's SettingItem.
 * Used *inside* sub-pages.
 */
@Composable
fun SettingItemRow(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String,
    desc: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit = {},
    action: (@Composable () -> Unit)? = null,
) {
    val view = LocalView.current
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        animationSpec = tween(200),
        label = "alpha"
    )

    Surface(
        modifier = modifier
            .alpha(alpha)
            .clickable(enabled = enabled) {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                onClick()
            },
        color = Color.Unspecified,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    modifier = Modifier.padding(end = 24.dp),
                    imageVector = it,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    maxLines = if (desc == null) 2 else 1,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                desc?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            action?.let {
                Box(modifier = Modifier.padding(start = 16.dp)) {
                    it()
                }
            }
        }
    }
}

/**
 * Collapsing top bar used across all settings pages.
 * Keeps Material 3 behavior, but with a smaller title treatment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
    title: String,
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    LargeTopAppBar(
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            scrolledContainerColor = Color.Transparent,
        ),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        scrollBehavior = scrollBehavior,
    )
}
