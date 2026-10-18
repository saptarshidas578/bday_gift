package com.pixelgift.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelgift.app.R
import com.pixelgift.app.ui.theme.PixelGiftSpacing
import com.pixelgift.app.ui.theme.StatusConnected
import com.pixelgift.app.ui.theme.StatusOffline
import com.pixelgift.app.ui.theme.StatusReconnecting

enum class DeviceConnectionStatus {
    OFFLINE,
    CONNECTING,
    RECONNECTING,
    CONNECTED
}

/**
 * Persistent connection status chip with minimum 48dp touch target
 * and accessibility descriptions per SPEC 9.4.
 */
@Composable
fun ConnectionChip(
    status: DeviceConnectionStatus,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusLabel, statusColor) = when (status) {
        DeviceConnectionStatus.CONNECTED -> stringResource(R.string.status_connected) to StatusConnected
        DeviceConnectionStatus.RECONNECTING -> stringResource(R.string.status_reconnecting) to StatusReconnecting
        DeviceConnectionStatus.CONNECTING -> stringResource(R.string.status_connecting) to StatusReconnecting
        DeviceConnectionStatus.OFFLINE -> stringResource(R.string.status_offline) to StatusOffline
    }

    val chipContentDescription = stringResource(R.string.cd_connection_chip, statusLabel)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        tonalElevation = 2.dp,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = chipContentDescription
            }
            .clickable(
                role = Role.Button,
                onClick = onRetryClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm),
            modifier = Modifier.padding(horizontal = PixelGiftSpacing.md, vertical = PixelGiftSpacing.sm)
        ) {
            // Status indicator dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (status != DeviceConnectionStatus.CONNECTED) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.action_retry_connection),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
