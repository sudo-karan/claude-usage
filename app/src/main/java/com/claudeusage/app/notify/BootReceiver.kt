package com.claudeusage.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arms background refresh after a reboot or app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Notifier.ensureChannels(context)
        UsageWorkScheduler.ensurePeriodic(context)
    }
}
