package com.novo.drive.presentation.navigation

import org.jetbrains.compose.resources.StringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.novo.drive.resources.Res
import com.novo.drive.resources.nav_dewangram
import com.novo.drive.resources.nav_files
import com.novo.drive.resources.nav_gallery
import com.novo.drive.resources.nav_home
import com.novo.drive.resources.nav_settings

enum class TopLevelDestination(
    val route: Route,
    val labelRes: StringResource,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    HOME(Route.Home, Res.string.nav_home, Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
    FILES(
        Route.Files(),
        Res.string.nav_files,
        Icons.Outlined.FolderShared,
        Icons.Filled.FolderShared
    ),
    DEWANGRAM(
        Route.DewanGram,
        Res.string.nav_dewangram,
        Icons.Outlined.Forum,
        Icons.Filled.Forum
    ),
    GALLERY(Route.Gallery, Res.string.nav_gallery, Icons.Outlined.Collections, Icons.Filled.Collections),
    SETTINGS(Route.Settings, Res.string.nav_settings, Icons.Outlined.Tune, Icons.Filled.Tune)
}
