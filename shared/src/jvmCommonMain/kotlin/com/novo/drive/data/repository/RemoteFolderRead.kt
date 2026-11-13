package com.novo.drive.data.repository

import com.novo.drive.data.remote.telegram.RemoteFolderState

internal sealed interface RemoteFolderRead {
    data class Ok(val state: RemoteFolderState) : RemoteFolderRead
    data object Unreadable : RemoteFolderRead
    data object Unavailable : RemoteFolderRead
}
