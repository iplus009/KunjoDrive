package com.novo.drive.presentation.gallery

import org.jetbrains.compose.resources.StringResource
import com.novo.drive.resources.Res
import com.novo.drive.resources.gallery_tab_albums
import com.novo.drive.resources.gallery_tab_all
import com.novo.drive.resources.gallery_tab_photos
import com.novo.drive.resources.gallery_tab_videos

enum class GalleryTab(val labelRes: StringResource) {
    ALL(Res.string.gallery_tab_all),
    PHOTOS(Res.string.gallery_tab_photos),
    VIDEOS(Res.string.gallery_tab_videos),
    ALBUMS(Res.string.gallery_tab_albums)
}
