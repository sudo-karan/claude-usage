package com.claudeusage.app

import android.app.Application
import com.claudeusage.app.notify.Notifier
import com.claudeusage.app.notify.UsageWorkScheduler

class ClaudeUsageApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannels(this)
        UsageWorkScheduler.ensurePeriodic(this)
    }
}
