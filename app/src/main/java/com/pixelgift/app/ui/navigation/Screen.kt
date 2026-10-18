package com.pixelgift.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.pixelgift.app.R

sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    @StringRes val contentDescriptionRes: Int,
    val icon: ImageVector
) {
    data object Studio : Screen(
        route = "studio",
        titleRes = R.string.nav_studio,
        contentDescriptionRes = R.string.cd_nav_studio,
        icon = Icons.Outlined.ColorLens
    )

    data object Effects : Screen(
        route = "effects",
        titleRes = R.string.nav_effects,
        contentDescriptionRes = R.string.cd_nav_effects,
        icon = Icons.Outlined.Tune
    )

    data object Magic : Screen(
        route = "magic",
        titleRes = R.string.nav_magic,
        contentDescriptionRes = R.string.cd_nav_magic,
        icon = Icons.Outlined.AutoAwesome
    )

    data object Time : Screen(
        route = "time",
        titleRes = R.string.nav_time,
        contentDescriptionRes = R.string.cd_nav_time,
        icon = Icons.Outlined.Schedule
    )

    data object Device : Screen(
        route = "device",
        titleRes = R.string.nav_device,
        contentDescriptionRes = R.string.cd_nav_device,
        icon = Icons.Outlined.Devices
    )

    companion object {
        val bottomNavItems: List<Screen> = listOf(
            Studio,
            Effects,
            Magic,
            Time,
            Device
        )
    }
}
