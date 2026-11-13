package com.novo.drive.core.telegram

interface TdlibDatabaseKeyProvider {
    fun databaseKey(): ByteArray
}
