package com.novo.drive.data.remote.telegram

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteFolderTombstone(
    @SerialName("id") val id: String,
    @SerialName("at") val deletedAt: Long
)
