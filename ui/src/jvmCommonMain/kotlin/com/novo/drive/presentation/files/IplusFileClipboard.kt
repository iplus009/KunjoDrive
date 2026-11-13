package com.novo.drive.presentation.files

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate

class IplusFileClipboard {

    private val content = MutableStateFlow<IplusCutItems?>(null)

    fun cut(items: IplusCutItems) {
        content.value = items
    }

    fun take(): IplusCutItems? = content.getAndUpdate { null }
}

typealias FileClipboard = IplusFileClipboard

