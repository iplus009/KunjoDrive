package com.novo.drive.presentation.files

data class IplusShareRequest(val paths: List<String>, val mimeType: String)

typealias ShareRequest = IplusShareRequest

