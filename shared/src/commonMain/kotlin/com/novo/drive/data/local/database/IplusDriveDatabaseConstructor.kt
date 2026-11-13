package com.novo.drive.data.local.database

import androidx.room.RoomDatabaseConstructor

@Suppress("KotlinNoActualForExpect")
expect object IplusDriveDatabaseConstructor : RoomDatabaseConstructor<IplusDriveDatabase> {
    override fun initialize(): IplusDriveDatabase
}

