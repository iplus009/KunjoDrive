package com.novo.drive.core.telegram

enum class TelegramConnectionState {
    WAITING_FOR_NETWORK,
    CONNECTING,
    UPDATING,
    READY
}
