package com.novo.drive.data.repository

import com.novo.drive.core.telegram.RemoteDocument
import com.novo.drive.data.local.entity.FileEntity
import com.novo.drive.data.remote.telegram.RemoteFileManifest

internal data class KnownRows(
    val manifests: Map<RemoteDocument, RemoteFileManifest?>,
    val byId: Map<String, FileEntity>,
    val byUniqueId: Map<String, FileEntity>,
    val splitCounts: Map<String, Int>,
    val firstPartStored: Set<String>
)
