package com.novo.drive.data.repository

import com.novo.drive.core.publish.PublishScheduler

internal class CountingScheduler : PublishScheduler {
    var kicks = 0
    override fun kick() {
        kicks++
    }
}
